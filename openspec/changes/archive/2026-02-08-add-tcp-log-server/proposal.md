## Why

在部分 TV 设备上，系统层关闭了 ADB（或 ADB logcat）能力，开发侧无法通过 `adb logcat` 获取运行日志；同时现有本地调试日志（`log.txt` / `log_old.txt`）写入到 Download/MediaStore 目录时，在某些 ROM/权限/存储实现下容易失败或难以取回。

为了在“无 ADB、难取本地日志”的环境中仍能快速定位问题，需要提供一种**不依赖 ADB、不依赖外部存储权限**的日志获取方式。

## What Changes

- 在“开发者设置”中新增一个可开关的 **TCP 日志 Server**。
- 开关开启后，应用在默认端口 `17010` 监听 TCP 连接，并向所有已连接客户端**实时广播** DDPlayTV 的应用日志（由 `LogSystem` 输出）。
- 输出采用**逐行文本协议（UTF-8 + `\n`）**，优先复用现有 `LogFormatter` 的单行格式，使其与 `log.txt` 一致、便于脚本解析与问题回溯。
- 默认关闭；仅在用户显式开启后生效，并在 UI 中提示潜在隐私/安全风险。

## Capabilities

### New Capabilities

- `tcp-log-server`: 开发者可控的 TCP 日志实时输出服务。

### Modified Capabilities

- （无）

## Impact

- 影响模块：
  - `core_log_component`: 新增 TCP server 组件 + 持久化开关配置 + 与 `LogWriter` 的广播接入
  - `user_component`: 开发者设置页新增开关与连接说明
  - `app`: 启动阶段根据持久化状态恢复 server（复用既有 `LogSystem.init/loadPolicyFromStorage` 链路）
- 主要风险：
  - **安全/隐私**：日志可能包含 URL/账号信息；开启后会在局域网暴露监听端口。缓解：默认关闭、仅开发者设置中显式开启、UI 强提示；如需更强保护可后续引入 token/白名单。
  - **性能**：高频日志可能导致网络输出压力。缓解：复用现有日志策略（级别/采样），并确保发送线程不阻塞 UI 与日志写入线程。
- 待确认（如需收敛范围可按最小实现先落地）：
  - 日志范围：仅 `LogSystem`（DDLog）日志 vs 尝试读取系统 `logcat` 全量缓冲区（后者在非系统权限下通常不可行）。
  - 是否需要简单鉴权（例如一次性 token）与是否需要可配置端口（默认 `17010`）。

