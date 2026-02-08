## ADDED Requirements

### Requirement: 日志与异常上报必须默认脱敏且无 `printStackTrace()` 漏口

系统 MUST 对所有日志输出（文件/TCP/logcat）与异常上报上下文默认应用同一套脱敏策略，避免泄露：

- token/cookie/authorization
- password/secret/appSecret
- URL query、请求参数/JSON 中的敏感字段
- 本地路径/磁力链等可能具备隐私属性的信息

系统 MUST 不在 Release 中使用 `printStackTrace()` 作为异常处理手段；异常输出必须通过统一门面（例如 `LogFacade` / `ErrorReportHelper`）并保证脱敏策略生效。

#### Scenario: 日志输出自动脱敏

- **GIVEN** 某模块记录一条包含 `token=...` 或 `Authorization: Bearer ...` 的日志
- **WHEN** 日志被写入文件或通过 TCP 输出
- **THEN** 敏感字段被遮蔽（可保留 hash/fingerprint 以便定位），不出现明文

#### Scenario: 异常上报上下文不包含敏感信息

- **GIVEN** 网络请求因鉴权失败抛出异常且上下文包含 header/URL
- **WHEN** 系统通过异常上报门面上报该异常
- **THEN** 上报内容包含可定位信息（模块/接口标识/错误码等）
- **AND** 不包含明文 token/cookie/password

### Requirement: Crash 上报入口必须收敛为单一门面

系统 SHALL 将 Crash/异常上报的第三方 SDK 触达（例如 Bugly `CrashReport`）集中到单一门面（例如 `BuglyReporter`），并对外提供稳定 API；历史调用点可在迁移期保留 Deprecated wrapper，但 MUST 遵循同一套脱敏与上下文构建规则。

#### Scenario: 任意模块上报异常时路径一致

- **GIVEN** `core_*` 与 feature 模块均需要上报异常
- **WHEN** 调用异常上报 API
- **THEN** 最终都通过统一门面触达第三方 SDK

