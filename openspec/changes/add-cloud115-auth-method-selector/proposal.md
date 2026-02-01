## Why

当前 115 Cloud 存储库只支持“扫码授权”。在 TV 场景或用户无法方便扫码（没有 115 App / 设备不在身边 / 远程调试）时，新增/更新授权的门槛较高；同时部分高级用户可能已经拥有可用的 Cookie（UID/CID/SEID/KID）并希望直接粘贴完成授权。

本变更希望在保持默认扫码流程不变的前提下，新增“手动输入 token（Cookie）”作为可选鉴权方式，让用户根据场景选择更适合的授权路径。

## What Changes

- 在 115 Cloud 存储库的授权入口提供“授权方式选择”：
  - 扫码授权（现有流程，默认推荐）
  - 手动输入 token（粘贴 Cookie：UID/CID/SEID/KID）
- 手动 token 授权在保存前进行有效性校验：
  - 解析并规范化输入（提取 UID/CID/SEID/KID）
  - 调用 Cookie 校验接口确认授权有效后才落库
- 文案与提示去除“只能扫码”的强绑定：
  - 将“请先扫码授权/重新扫码”调整为“请先完成授权/重新授权”等更中性表述
- 安全性：避免在日志/崩溃上报中输出明文 token，所有输出使用脱敏后的 token。

## Capabilities

### New Capabilities

- `cloud115-auth-method-selector`：115 Cloud 授权支持“扫码 / 手动输入 token”两种方式可选。

### Modified Capabilities

- （无）

## Impact

- 影响模块：
  - `storage_component`：115 Cloud 存储库编辑弹窗新增“授权方式选择”与“手动 token 授权”交互。
  - `core_storage_component`：增加 token 解析/规范化与校验复用逻辑；授权态仍写入 `Cloud115AuthStore`。
  - `core_network_component`：无需新增接口（复用现有 cookieStatus）。
- 风险与权衡：
  - token 输入格式存在差异：通过“解析规范化 + 明确错误提示”降低失败成本。
  - token 属于敏感信息：需要严格脱敏、避免日志泄露，并尽量降低 UI 暴露面。
  - TV/遥控器输入成本较高：优先使用“方式选择 + 简单粘贴输入”实现，避免复杂多步表单。

