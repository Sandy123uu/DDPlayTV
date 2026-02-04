# cloud115-auth-method-selector Specification

## Purpose
TBD - created by archiving change add-cloud115-auth-method-selector. Update Purpose after archive.
## Requirements
### Requirement: 115 Cloud 授权入口支持选择方式

系统 SHALL 在 115 Cloud 存储库的授权入口提供鉴权方式选择，至少包含：

- 扫码授权
- 手动输入 token（Cookie）

#### Scenario: 用户在授权入口看到两种方式

- **GIVEN** 用户正在添加或编辑一个 115 Cloud 存储库
- **WHEN** 用户点击“授权/重新授权”
- **THEN** 系统展示可选的鉴权方式列表
- **AND** 列表至少包含“扫码授权”和“手动输入 token”

### Requirement: 手动 token 授权需解析并校验后才可生效

系统 SHALL 支持用户手动输入 token（Cookie）用于授权，并在持久化授权态之前完成解析与有效性校验；当校验失败时系统 SHALL 给出明确提示且不应持久化无效授权态。

#### Scenario: 手动 token 授权成功

- **GIVEN** 用户输入包含 `UID/CID/SEID` 的有效 token
- **WHEN** 用户确认提交
- **THEN** 系统校验通过并将授权态写入本地存储
- **AND** 系统将该账号标记为“已授权”

#### Scenario: token 缺字段或无效时不保存

- **GIVEN** 用户输入的 token 缺少 `UID/CID/SEID` 中任一字段或已失效
- **WHEN** 用户确认提交
- **THEN** 系统提示具体失败原因
- **AND** 系统不应写入/更新该账号的授权态

### Requirement: token 不应写入媒体库通用字段

系统 SHALL 将 115 Cloud token（Cookie）存储在专用授权态存储中（例如 `Cloud115AuthStore`），而非写入媒体库 `MediaLibraryEntity` 的通用字段，以避免语义污染与泄露风险。

#### Scenario: 授权后媒体库不包含 token 明文

- **GIVEN** 用户已通过扫码或手动 token 完成授权
- **WHEN** 系统保存媒体库记录
- **THEN** token 不应出现在媒体库的通用字段中

### Requirement: token 在日志与上报中必须脱敏

系统 SHALL 避免在日志与错误上报中输出 token 明文；当需要记录调试上下文时系统 SHALL 使用脱敏后的 token 表达。

#### Scenario: 授权流程日志不包含 token 明文

- **GIVEN** 系统在授权成功/失败路径输出日志或错误上下文
- **WHEN** 日志/上报内容包含 token 信息
- **THEN** token 必须为脱敏形式（例如仅保留键名与长度信息）

