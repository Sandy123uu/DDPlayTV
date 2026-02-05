# 模块排查报告：:core_database_component

- 模块：:core_database_component
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：core_database_component/src/main/java/com/xyoye/common_component/database/（含 `migration/`）+ `media3/`

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`CORE_DATABASE-F###`  
> - Task：`CORE_DATABASE-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 提供全局 Room 数据库实例（`DatabaseInfo` + `DatabaseManager`）与各业务 DAO（播放历史/媒体库/搜索历史/弹幕屏蔽等）。
  - 提供少量“跨模块可复用”的本地存储包装（例如 `Media3LocalStore` 作为 Media3 灰度/下载校验的本地读写入口）。
  - 提供必要的迁移能力：Room `Migration`（自动迁移链路）+ `ManualMigration`（历史遗留的手动数据搬迁）。
- 模块职责（不做什么）
  - 不承载 UI/页面逻辑；不应把“特性业务”耦合进 DB 模块（否则会导致 infra 层被迫跟随 feature 漂移）。
  - 不应在迁移失败时静默吞掉异常（迁移关系到用户数据一致性，必须可观测）。
- 关键入口/关键路径（示例）
  - `core_database_component/src/main/java/com/xyoye/common_component/database/DatabaseInfo.kt` + `DatabaseInfo`（Room `@Database` 声明、entities/version）
  - `core_database_component/src/main/java/com/xyoye/common_component/database/DatabaseManager.kt` + `DatabaseManager`（`Room.databaseBuilder(...)`、迁移注册）
  - `core_database_component/src/main/java/com/xyoye/common_component/database/migration/ManualMigration.kt` + `ManualMigration#migrate_6_7`（手动迁移）
  - `core_database_component/src/main/java/com/xyoye/common_component/database/migration/ManualMigrationInitializer.kt` + `ManualMigrationInitializer#create`（启动期触发迁移）
  - `core_database_component/src/main/java/com/xyoye/common_component/media3/Media3LocalStore.kt` + `Media3LocalStore#recordSnapshot/upsertDownloadCheck`
- 依赖边界（与哪些模块交互，是否存在边界疑点）
  - 依赖：`:data_component`（Room entities/enums/converters），`:core_system_component`（`BaseApplication`、startup 初始化依赖）。
  - 被依赖：几乎所有 feature/infra（例如 `local_component/storage_component/bilibili_component` 等都直接调用 `DatabaseManager.instance.getXxxDao()`）。
  - 边界疑点：
    - `DatabaseManager` 以全局静态单例 + 直接暴露 DAO 的方式被大量调用，导致“跨模块对 DB 的直接耦合”过强、可测试性弱，且难以做“按域拆分/多库/替换实现”。
    - 迁移触发分散在多模块 Initializer 中（本模块亦有 `ManualMigrationInitializer`），启动顺序与可观测性依赖实现细节，易漂移。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - 本轮使用 ast-grep 确证：`Room.databaseBuilder(...)` 的构建点、`printStackTrace()` 的异常处理形态。

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE_DATABASE-F001 | ArchitectureRisk | 全仓大量直接依赖 `DatabaseManager.instance`（静态单例）访问 DAO，跨模块耦合强、可测试性差 | `core_database_component/src/main/java/com/xyoye/common_component/database/DatabaseManager.kt` + `DatabaseManager#instance`；调用示例：`local_component/src/main/java/com/xyoye/local_component/ui/activities/play_history/PlayHistoryViewModel.kt` + `PlayHistoryViewModel`；`storage_component/src/main/java/com/xyoye/storage_component/ui/activities/storage_plus/StoragePlusViewModel.kt` + `StoragePlusViewModel` | N/A | Unify | `:core_contract_component`（定义 DB 访问契约）+ `:core_database_component`（实现）+ 调用方模块迁移 | Medium | Large | P2 | 需要分阶段迁移调用点；避免引入 feature ↔ feature 依赖；需兼容现有线程模型（IO 调度） |
| CORE_DATABASE-F002 | Redundancy | `DatabaseManager` 使用“类同名嵌套 object（`private object DatabaseManager`）”实现 holder，命名造成阅读/检索混淆 | `core_database_component/src/main/java/com/xyoye/common_component/database/DatabaseManager.kt` + `private object DatabaseManager`（同名遮蔽） | N/A | Unify | `:core_database_component` | Low | Small | P2 | 重命名需确保二进制/反射无依赖（通常无）；注意保持对外访问方式不变（`DatabaseManager.instance`） |
| CORE_DATABASE-F003 | ArchitectureRisk | 启动期手动迁移异常仅 `printStackTrace()`，缺少统一日志/上报与上下文；迁移失败可能静默，难排障 | `core_database_component/src/main/java/com/xyoye/common_component/database/migration/ManualMigrationInitializer.kt` + `ManualMigrationInitializer#create`（`runCatching { ... }.onFailure { it.printStackTrace() }`） | N/A | Unify | `:core_database_component`（统一上报）+ `:core_log_component`（接入） | Medium | Small | P1 | 需要确认“迁移失败时的策略”：仅上报 vs 阻断启动；避免泄露敏感数据（路径/账号） |
| CORE_DATABASE-F004 | Duplication | 多处存在 MD5/hex 计算工具的重复实现，`ManualMigration#md5Hex` 复刻了已有能力，建议统一到共享工具 | `core_database_component/src/main/java/com/xyoye/common_component/database/migration/ManualMigration.kt` + `ManualMigration#md5Hex`；对比：`core_system_component/src/main/java/com/xyoye/common_component/utils/CacheKeyMapper.kt` + `CacheKeyMapper#toSafeFileName/md5Hex` | Unintentional | Unify | `:core_system_component`（统一 hash 工具）+ 调用方模块（迁移） | Low | Small | P2 | 需确保 hash 输出一致（历史数据依赖 `unique_key`）；必要时保留兼容逻辑 |
| CORE_DATABASE-F005 | ArchitectureRisk | Room schema 未导出（`exportSchema = false`），迁移链路缺少“可回放/可验证”的证据与门禁，长期演进风险高 | `core_database_component/src/main/java/com/xyoye/common_component/database/DatabaseInfo.kt` + `@Database(... exportSchema = false)`；`core_database_component/src/main/java/com/xyoye/common_component/database/DatabaseManager.kt` + `MIGRATION_*`（大量 `execSQL`） | N/A | Unify | `:core_database_component`（schema 导出 + 迁移校验） | High | Medium | P1 | 需要确定 schema 输出目录与 Git 策略；建议配套 MigrationTest（可能需要 instrumentation） |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| CORE_DATABASE-T001 | CORE_DATABASE-F001 | 引入“数据库访问契约/Provider”，逐步替换跨模块对 `DatabaseManager.instance` 的直接引用 | 新增 `:core_contract_component` 契约（例如 `DatabaseProvider`/`DaoProvider`）；`core_database_component` 提供实现；迁移 `local_component/storage_component/bilibili_component/...` 的调用点 | 1) 新契约不引入依赖环且 `./gradlew verifyModuleDependencies` 通过；2) 核心调用点可通过 Provider 获取 DAO（支持测试替换/注入）；3) 全仓编译通过 | Medium | Large | P2 | 待分配（Infra/DB） | Draft |
| CORE_DATABASE-T002 | CORE_DATABASE-F002 | 重构 `DatabaseManager` holder 命名与结构，降低阅读歧义 | `core_database_component/src/main/java/com/xyoye/common_component/database/DatabaseManager.kt`（将 `private object DatabaseManager` 改为 `Holder/InstanceHolder` 等） | 1) `DatabaseManager.instance` 行为不变；2) 编译通过；3) 无额外反射/Proguard 规则需求 | Low | Small | P2 | 待分配（Infra/DB） | Draft |
| CORE_DATABASE-T003 | CORE_DATABASE-F003 | 统一手动迁移异常可观测性：替换 `printStackTrace()` 为结构化日志/异常上报，并补充必要上下文 | `core_database_component/src/main/java/com/xyoye/common_component/database/migration/ManualMigrationInitializer.kt`；（必要时）`core_log_component` 上报接口；`ManualMigration#migrate_6_7` 关键路径 | 1) 迁移失败会记录到日志/上报（含模块与迁移版本信息）；2) 不记录敏感字段明文（账号/密码等）；3) 启动链路稳定（不引入 ANR/主线程阻塞） | Medium | Small | P1 | AI（Codex） | Done |
| CORE_DATABASE-T004 | CORE_DATABASE-F004 | 统一 MD5/hex 工具：复用 `CacheKeyMapper`（或抽取 `HashUtils`），移除 `ManualMigration#md5Hex` 重复实现 | `core_database_component/src/main/java/com/xyoye/common_component/database/migration/ManualMigration.kt`；`core_system_component/src/main/java/com/xyoye/common_component/utils/CacheKeyMapper.kt` | 1) `unique_key` 的计算结果与历史一致（或提供兼容处理）；2) 编译通过；3) 复用工具在全仓可被统一引用 | Low | Small | P2 | 待分配（Infra/DB） | Draft |
| CORE_DATABASE-T005 | CORE_DATABASE-F005 | 开启 Room schema 导出并建立迁移校验门禁（至少覆盖主库版本演进） | `DatabaseInfo`（`exportSchema=true`）；Gradle Room schemaLocation 配置；新增 schema 输出目录（例如 `core_database_component/schemas/`）与迁移测试/校验脚本 | 1) schema 文件随版本更新可追踪（可 review）；2) 至少提供“迁移可跑通”的验证方式（MigrationTest 或脚本）；3) CI/门禁可执行且不显著拖慢开发编译 | High | Medium | P1 | AI（Codex） | Done |

## 5) 风险与回归关注点

- 行为回退风险（哪些场景最敏感）
  - 数据库访问入口变更（从静态单例迁移到 Provider）可能影响初始化时序与线程模型，需重点回归“播放历史/媒体库/扫描与筛选”等高频路径。
  - 迁移策略调整（尤其是 schema 导出与迁移测试引入）需确保不影响 release 包体与构建速度。
- 回归成本（需要的账号/媒体文件/设备）
  - 需要至少 1 个包含播放历史/媒体库数据的本地环境（或导出的 db）验证数据不丢失。
  - 若涉及 `Media3LocalStore`，需要验证 Media3 灰度/下载校验相关功能链路（可用最小用例）。

## 6) 备注（历史背景/待确认点）

- 本报告为初稿，建议按 `document/code_quality_audit/global/review_checklist.md` 复核后再将 `module_status` 标记为 Done。
