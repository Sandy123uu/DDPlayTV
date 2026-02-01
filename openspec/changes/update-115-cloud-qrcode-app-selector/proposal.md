## Why

当前 115 Cloud 扫码授权流程将登录确认接口的设备来源 `app` 固定为 `tv`。但 115 Cloud 的登录态通常会按“设备来源/客户端类型”区分与互斥：当用户在其他端（例如移动端/网页端/小程序端）保持常用登录态时，固定 `app` 可能带来“顶掉已登录设备/需要重新登录”的负面体验。

本变更希望将“设备来源（app）”暴露为用户可选项，并在同一账号维度记住该选择，减少重复操作与意外顶号风险，同时保持默认行为不变（默认仍为 `tv`）。

## What Changes

- 在“115 Cloud 扫码授权”弹窗中增加“设备来源（app）”选择：
  - 提供常用/推荐选项（例如 `tv / wechatmini / alipaymini / qandroid`）
  - 允许用户输入自定义 `app`（用于覆盖更多服务端支持值）
- 扫码登录确认（`qrcodeLogin`）时，使用用户选中的 `app`：
  - URL path：`/app/1.0/{app}/1.0/login/qrcode`
  - 表单字段：`app=<app>`（与 path 一致）
- 设备来源（app）按“账号”维度持久化记忆：
  - 以媒体库账号标识 `115cloud://uid/<user_id>` 作为隔离边界
  - 下次对同一账号再次扫码授权时默认回显上次选择
- 兼容性：对历史已添加账号（没有存储过 `app`）按 `tv` 回退，避免升级后行为改变或崩溃。

## Capabilities

### New Capabilities

- `cloud115-qrcode-auth-app-selector`：115 Cloud 扫码授权支持选择设备来源（app）并按账号记忆。

### Modified Capabilities

- （无）

## Impact

- 影响模块：
  - `storage_component`：扫码授权弹窗 UI 与交互（新增 app 选择 + 回显）
  - `core_storage_component`：115 Cloud 授权态存储（按账号记录 app）
  - `core_network_component`：无新增接口（现有 `qrcodeLogin` 已支持 `{app}`），但需要确保调用链路传参一致
- 风险与权衡：
  - “服务端支持的 app 值”可能随时间变化：通过“预置列表 + 自定义输入”降低兼容风险
  - TV/遥控器交互：新增控件必须保证 DPAD 可达，避免焦点陷阱

