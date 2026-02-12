# 日志系统维护指南（本仓库现状）

本文描述 **DDPlayTV（本仓库版本）** 当前的日志输出与抓取方式。

> 说明：本仓库已移除本地 `log.txt / log_old.txt` 的文件落盘能力；日志抓取以 **logcat** 与 **TCP 日志服务** 为主。

## 1. 输出通道

- **logcat（默认）**：始终输出，受“日志级别”设置影响
- **TCP 日志服务（可选，高风险通道）**：仅在“调试会话”开启且用户显式授权后输出

TCP 日志属于高风险输出通道（即使有默认脱敏，也不建议在不可信网络中开启），门禁策略见：`document/architecture/log_redaction_policy.md`。

## 2. App 内开启步骤

1. 打开：`设置` → `开发者设置`
2. 开启：`启用调试会话`
3. 开启：`启用 TCP 日志服务`
   - 默认端口：`17010`
   - 页面会显示 IP/端口，并给出 `nc <ip> <port>` 连接示例

> 调试会话关闭后，TCP 服务会自动停止（但会保留“已授权开启 TCP”的开关状态，便于下次恢复）。

## 3. 连接与抓取（TCP）

确保你的电脑与设备在同一局域网内，然后使用任意 TCP 客户端连接：

### macOS / Linux / WSL

```bash
nc <ip> 17010
```

保存到文件（可选）：

```bash
nc <ip> 17010 | tee ddplaytv.log
```

### Windows（PowerShell / CMD）

建议使用 Nmap 自带的 `ncat`（或其它等价工具）：

```bat
ncat <ip> 17010 > ddplaytv.log
```

## 4. ADB logcat 抓取（默认通道）

logcat 会包含系统与其他应用输出，建议按 tag 过滤。

- 本仓库的 logcat tag 形如：`DDLog-<模块>` 或 `DDLog-<模块>-<子 tag>`（Android tag 长度上限 23，超长会被截断）
- 示例（仅查看 DDLog 前缀）：

```bash
adb logcat | rg "DDLog-"
```

只看某个模块（以实际输出为准）：

```bash
adb logcat -s "DDLog-CORE"
```

> 注意：`adb logcat` 可能很吵；尽量使用 `-s` 或管道过滤，不要直接贴全量日志。

## 5. 脱敏与安全提示

- 默认会对 token/cookie/password/URL query 等敏感字段做脱敏处理
- 规则与扩展方式见：`document/architecture/log_redaction_policy.md`

即便已脱敏，仍建议：

- 仅在可信局域网内开启 TCP
- 排查完成后及时关闭“调试会话 / TCP 日志服务”

## 6. 常见问题

### 6.1 TCP 开启失败：`TCP 日志仅在调试会话中可用`

先开启“调试会话”，再开启“TCP 日志服务”。

### 6.2 已连接但没有输出

- 确认 App 内正在产生新日志（进行一次操作/刷新页面）
- 确认“TCP 日志服务”显示为已开启且正在运行
- 确认 IP/端口与局域网连通性（同网段/无防火墙拦截）

