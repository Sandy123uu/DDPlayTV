# cloud115-qrcode-auth-app-selector Specification

## Purpose
TBD - created by archiving change update-115-cloud-qrcode-app-selector. Update Purpose after archive.
## Requirements
### Requirement: 扫码授权前可选择设备来源（app）

系统 SHALL 在 115 Cloud 扫码授权弹窗中提供“设备来源（app）”选择能力，并提供可用的预置选项列表；同时系统 SHOULD 允许用户输入自定义 `app` 值以覆盖更多服务端支持值。

#### Scenario: 新增账号默认使用 tv 并可切换

- **GIVEN** 用户正在新增一个 115 Cloud 存储库并打开扫码授权弹窗
- **WHEN** 用户未进行任何额外配置
- **THEN** 系统默认选择的 `app` 为 `tv`
- **AND** 用户可以将 `app` 切换为预置列表中的其他值或自定义值

### Requirement: 登录确认请求使用用户选中的 app

系统 SHALL 在“二维码确认登录换取 Cookie”步骤中使用用户选中的 `app` 值发起请求，并确保 URL path 与表单字段中的 `app` 值一致。

#### Scenario: 使用 wechatmini 进行登录确认

- **GIVEN** 用户在扫码授权弹窗中选择 `app=wechatmini`
- **WHEN** 用户扫码并在 115 客户端确认授权
- **THEN** 系统应使用 `wechatmini` 发起登录确认请求
- **AND** 请求的 URL path 与表单字段 `app` 均为 `wechatmini`

### Requirement: 设备来源（app）按账号记忆并默认回显

系统 SHALL 将用户选择的 `app` 按 115 Cloud 账号维度持久化，并在对同一账号再次扫码授权时默认回显上次选择的 `app`；当历史数据缺失该值时系统 SHALL 回退为 `tv`。

#### Scenario: 同一账号再次授权回显上次 app

- **GIVEN** 用户已为某个 115 Cloud 账号完成过扫码授权，且上次选择的 `app` 为 `qandroid`
- **WHEN** 用户对同一账号再次打开扫码授权弹窗
- **THEN** 系统默认回显 `app=qandroid`

#### Scenario: 旧账号未存储 app 时回退 tv

- **GIVEN** 用户在旧版本中已添加过某个 115 Cloud 账号（历史数据未存储 `app`）
- **WHEN** 用户升级后对该账号打开扫码授权弹窗
- **THEN** 系统默认选择的 `app` 为 `tv`

