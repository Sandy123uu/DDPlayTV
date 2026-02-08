## Context

当前工程的应用日志统一通过 `core_log_component` 的 `LogSystem/LogFacade` 输出，并由 `LogWriter` 写入 logcat，必要时写入本地调试文件（`log.txt` / `log_old.txt`，由 `LogFileManager` 管理）。

但在部分 TV 设备上：

- ADB/`adb logcat` 被系统层禁用，导致开发侧无法直接抓取运行日志。
- 本地日志写入/取回依赖 Download/MediaStore，在部分 ROM/权限/存储实现下不稳定，难以作为唯一诊断通道。

因此需要一个“应用自带、可开关、无需外部存储权限”的远程日志获取能力。

## Goals / Non-Goals

**Goals**

1. 在“开发者设置”中提供开关，显式启用/关闭 TCP 日志服务。
2. 开启后默认监听 `17010` 端口，并支持多个客户端同时连接。
3. 将 `LogSystem` 输出的日志实时广播到所有已连接客户端，客户端断开/异常不影响其他连接。
4. 线程模型清晰：不在主线程执行阻塞式 IO；日志写入线程不被网络慢客户端拖垮（必要时可丢弃/踢掉慢客户端）。
5. 开关状态持久化：应用重启后能按上次设置自动恢复启用状态。

**Non-Goals**

- 不尝试在普通应用权限下读取“全量系统 logcat 缓冲区”（该能力通常需要系统权限）。
- 不提供加密传输/完善鉴权（如需要可后续加能力：token/白名单/仅局域网提示）。
- 不保证在进程被系统杀死后仍持续输出（非前台服务场景）。

## Decisions

### 1) 日志来源：复用 LogSystem 事件流（DDLog）

TCP 日志服务以 `LogSystem` 的 `LogEvent` 为唯一数据源，复用 `LogWriter` 的过滤/采样策略，避免引入新的“日志分叉”与不可控的系统 logcat 权限问题。

### 2) 输出格式：与 log.txt 对齐的一行一条

协议采用纯文本（UTF-8），每条日志占一行，以 `\n` 结尾；内容优先复用 `LogFormatter.format(event)`，使 TCP 输出与本地 `log.txt` 内容一致，便于同一套脚本/工具解析。

可选增强（非首版必需）：新客户端连接时先回放一小段内存 ring buffer（例如最近 200 行）作为上下文。

### 3) 生命周期：由 LogSystem 统一托管

- 开关配置持久化在 `core_log_component` 的 `LogConfigTable`（避免引入 `core_log_component -> core_system_component` 的反向依赖）。
- `LogSystem.init/loadPolicyFromStorage` 阶段恢复并按配置启动/停止 TCP server。
- UI 侧仅调用 `LogSystem` 暴露的 API 变更状态，不直接管理线程/Socket。

### 4) 线程模型：accept 与 broadcast 解耦

- `ServerSocket.accept()` 运行在独立线程（或单独 executor）。
- 广播路径与 `LogWriter` 解耦：`LogWriter` 仅将已格式化的单行日志投递到 TCP server 的队列；TCP server 在自身线程中向客户端写出。
- 单客户端写入失败/超时应快速摘除连接；慢客户端不得阻塞整体广播。

## Risks / Trade-offs

- **安全/隐私**：日志可能包含敏感信息；TCP 端口暴露在局域网。权衡：默认关闭 + 开发者设置中显式开启 + UI 警示；如需更强保护可演进 token/白名单。
- **性能**：高日志量 + 多客户端可能带来额外 CPU/内存/IO。权衡：复用现有日志策略（级别/采样）；对慢客户端采用限流/丢弃策略。
- **网络可达性**：部分网络环境（隔离网段/IPv6-only）可能导致连接困难。权衡：首版以“设备同局域网可直连”为主要场景，并在 UI/文档提供排查指引。

## Validation

- 编译/静态检查：`./gradlew :app:assembleDebug`、`./gradlew lint`、`./gradlew verifyModuleDependencies`
- 单元测试：覆盖 TCP server 启停、广播、多客户端不串扰、慢/断连处理的关键路径
- 手工验收（需真机/局域网，本环境不执行）：
  - 在开发者设置打开开关后，使用 `nc <device-ip> 17010` 能收到实时日志
  - 同时连接两个客户端，均能收到同一批日志，断开其中一个不影响另一个

