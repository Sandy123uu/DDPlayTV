## ADDED Requirements

### Requirement: 依赖治理门禁必须持续通过且避免依赖泄漏

系统 MUST 满足模块依赖治理规则（无环、feature 不互相依赖、跨模块协作走契约/路由），并持续通过依赖治理门禁（例如 `verifyModuleDependencies`）。  
系统 SHOULD 避免第三方依赖通过 `api(...)` 透传到上层模块；若确需透传 MUST 有明确理由与范围说明。

#### Scenario: 变更合入后依赖治理校验通过

- **GIVEN** 完成任意一批 Backlog 治理任务的代码变更
- **WHEN** 执行 `./gradlew verifyModuleDependencies`
- **THEN** 构建输出以 `BUILD SUCCESSFUL` 结束

### Requirement: Room schema 导出与迁移校验必须具备门禁

系统 SHALL 开启 Room schema 导出，并建立迁移校验门禁，至少覆盖主库版本演进；迁移异常必须可观测且不使用 `printStackTrace()` 作为唯一输出。

#### Scenario: Schema 与迁移门禁可在 CI/本地复现

- **GIVEN** 数据库发生版本演进或新增 migration
- **WHEN** 执行迁移校验相关 Gradle 任务
- **THEN** schema 被导出且校验通过（失败时给出可定位错误信息）

### Requirement: 统一基础设施应集中到 core 层并支持渐进迁移

系统 SHALL 将横切基础设施集中到 `core_*` 层（例如 OkHttpClientFactory、脱敏、Result 失败处理助手、PreferenceDataStore 映射、投屏协议公共能力），并提供可渐进迁移路径（facade/Deprecated wrapper），避免在 feature 模块内形成新的重复实现与口径漂移。

#### Scenario: 渐进迁移不中断既有功能

- **GIVEN** 某能力存在旧实现与新实现的迁移期
- **WHEN** 调用方逐步迁移到新入口
- **THEN** 迁移过程中功能可用且无依赖环新增

### Requirement: 已确认废弃的能力必须移除整条链路并清理依赖

系统 MUST 对已确认废弃的能力执行“整条链路删除”，包括但不限于：入口 UI、路由/服务暴露、实现类、资源文件、第三方依赖与 wrapper 模块引用。  
系统 MUST 不保留“大段注释旧实现”或“运行期不可达但编译期仍依赖”的死代码形态；如需保留历史信息，SHOULD 以文档记录而非注释块保留。

#### Scenario: 移除“发送弹幕”链路后无残留依赖与引用

- **GIVEN** “发送弹幕”能力被确认废弃
- **WHEN** 完成治理落地并执行全仓构建
- **THEN** `player_component` 不再依赖 `:repository:panel_switch`
- **AND** 全仓检索 `com.effective.android.panel` 无命中

#### Scenario: 移除投屏 Sender 链路后不再暴露不可达服务

- **GIVEN** 投屏 Sender 能力被确认废弃
- **WHEN** 完成治理落地并执行全仓构建
- **THEN** 不再存在对外暴露的 Sender 服务入口（不存在“全端 stub + 对外仍暴露接口”形态）
- **AND** 不保留大段注释旧实现
