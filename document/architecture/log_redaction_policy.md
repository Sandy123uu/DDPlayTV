# 日志与异常上报脱敏策略（统一工具）

## 背景

仓库在 `document/code_quality_audit` 中明确了“日志/异常上报默认脱敏”的治理目标（对应 `G-T0012`）。  
核心诉求是：在 **logcat / 文件日志 / TCP 日志 / 异常上报上下文** 中，默认不泄露 token、cookie、密码、secret、URL query 等敏感信息，同时仍保留足够的定位线索（host/path、指纹等）。

## 统一入口

- 统一脱敏工具：`core_log_component/src/main/java/com/xyoye/common_component/log/privacy/SensitiveDataSanitizer.kt`
- 统一接入点（默认生效）：
  - `LogFormatter`：对日志 `message/context/throwable` 做脱敏后再落盘/输出
  - `ErrorReportHelper`：对调试输出的 tag/extraInfo 做脱敏（避免本地调试阶段泄露）

## 默认规则（摘要）

### 1) Key-based 脱敏（context/params/header）

当 key 命中以下敏感字段（大小写不敏感）时，value 会直接替换为 `<redacted>`：

- `token` / `access_token` / `refresh_token`
- `authorization` / `proxy-authorization`
- `cookie` / `set-cookie`
- `password` / `passwd`
- `secret` / `appsecret` / `app_sec` / `appsec`
- `x-appid` / `x-appsecret`（避免泄露构建期注入或运行期拼装的敏感 header）

> 说明：如需要扩展敏感 key，统一在 `SensitiveDataSanitizer.sensitiveKeys` 中维护，避免各模块重复实现。

### 2) URL 默认策略

- 默认模式（SAFE）：仅保留 `scheme://host[:port]/path`，**不输出 query/fragment**
- “仅保留 query key”模式（KEYS_ONLY）：保留 query key，但 value 统一 `<redacted>`（fragment 仍不输出）

### 3) 磁力链（magnet）

- 默认仅保留 `btih` hash：`magnet:btih=<hash>`
- 无法提取 hash 时输出：`magnet:btih=<redacted>`

### 4) 本地路径

- 默认输出 `文件名#指纹`，避免输出完整目录结构：`movie.mp4#1a2b3c4d`

## 调试会话与 TCP 日志门禁

TCP 日志属于高风险输出通道：

- 仅在 **调试会话开启** 且 **用户显式授权开启 TCP server** 时允许运行
- 调试会话关闭后，TCP server 会自动停止（保留用户开关状态用于下次会话）

## 调用建议

### 业务日志（推荐）

- 使用 `LogFacade` + `context` 传递结构化字段
- 不要把完整 URL query/token/cookie 拼到 message；即使拼了，Formatter 也会做兜底脱敏，但更推荐让上下文结构化、可控

### 异常上报上下文（推荐）

- 在构造 `extraInfo` 时，优先传“可定位但不敏感”的信息：
  - URL：使用 SAFE（host/path）+ 可选 fingerprint
  - 磁力链：仅 hash
  - 路径：仅文件名或 fingerprint

