# 设计说明：115 Cloud 授权方式支持“扫码 / 手动输入 Token”

## 背景与目标

115 Cloud 目前以扫码换取 Cookie 的方式完成授权。该方式对 TV 场景并不总是友好：用户可能无法扫码或希望直接复用已有 Cookie。

目标：

- 在 115 Cloud 授权入口提供两种方式可选：扫码授权、手动输入 token（Cookie）。
- 手动输入 token 必须在保存前校验有效性，避免把无效授权写入本地导致后续一连串失败。
- 仍遵循既有安全与分层约束：token 不写入 `MediaLibraryEntity`，只进入 `Cloud115AuthStore`，日志必须脱敏。
- 保持扫码流程与 `loginApp` 选择/记忆逻辑不变。

非目标：

- 不引入账号密码登录、短信登录等其他授权方式。
- 不尝试在应用内“自动获取 token”（例如读取第三方 app 数据、WebView 注入等）。

## Token 定义与输入格式

### Token 的定义

本变更中的 token 指 115 Cloud Web API 所需 Cookie 片段（与扫码登录返回一致）：

- `UID`（必填）
- `CID`（必填）
- `SEID`（必填）
- `KID`（可选）

应用内部使用的标准化 cookieHeader 仅包含上述字段，并由 `Cloud115Headers.buildCookieHeader(...)` 生成，以保证与扫码路径一致。

### 接受的输入形态（建议支持）

- 直接粘贴 Cookie 字符串：`UID=...; CID=...; SEID=...; KID=...`
- 带前缀的 Cookie 头：`Cookie: UID=...; CID=...; SEID=...`
- 键名大小写或空格存在差异时仍可解析（例如 `uid=`、分号后有多余空格）

对无法解析/缺字段的输入给出明确提示（缺少 `UID/CID/SEID` 哪一项）。

## 校验与落库

### 校验策略

手动 token 授权流程在落库前调用 Cookie 有效性校验（复用现有 `cookieStatus` 能力）：

- 校验通过：写入 `Cloud115AuthStore`（cookie + userId + updatedAtMs；userName/avatar 为空即可）
- 校验失败：不写入或回滚临时写入，并提示用户 token 已失效/格式错误

> 说明：`Cloud115AuthStore` 按 `storageKey` 隔离；手动 token 流程可先解析出 `UID`，再构造 `115cloud://uid/<UID>` 作为媒体库 URL，从而得到稳定的 `storageKey`。

### `loginApp` 的处理

手动 token 授权不涉及扫码确认接口，因此不要求用户选择 `loginApp`：

- 若该账号已有 `loginApp` 历史值（来自扫码授权），保持不变。
- 否则保持为空，未来需要扫码时仍按既有逻辑回退 `tv`。

## UI 与交互

### 授权入口

在 `Cloud115StorageEditDialog` 的授权按钮点击后弹出选择：

- `扫码授权`：进入现有 `Cloud115LoginDialog`（含 app 选择/记忆）
- `手动输入 token`：弹出输入框（建议单输入框粘贴 Cookie）

按钮文案建议由“扫码授权”调整为更中性的“授权/重新授权”，避免误导。

### 手动输入 token 弹窗

建议实现形式：

- 使用 `CommonEditDialog`（或新建轻量 `Cloud115TokenLoginDialog`）提供粘贴输入
- 输入提示说明 token 的期望格式与敏感性（避免分享/截图）
- 确认后显示 loading，并在校验结束后给出明确成功/失败反馈

TV/遥控器注意事项：

- 选择弹窗与输入框需要可聚焦、DPAD 可达
- 避免新增复杂表单（多字段拆分输入）以降低 TV 输入成本

## 文案与日志安全

- 用户提示文案：将“请先扫码授权/重新扫码”调整为“请先完成授权/重新授权”，扫码仍为一种方式但不再是唯一方式。
- 日志与上报：
  - 任何日志输出 token 使用 `Cloud115Headers.redactCookie(...)`
  - 异常上下文与 ErrorReportHelper 也必须脱敏后再写入

