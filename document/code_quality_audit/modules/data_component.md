# 模块排查报告：:data_component

- 模块：:data_component
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：data_component/src/main/java/com/xyoye/data_component/

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`DATA-F###`  
> - Task：`DATA-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 为全仓提供“稳定的数据定义层”：Room 实体/枚举/Converter、跨模块 DTO（含 Moshi `@JsonClass`）、少量契约数据（例如 Media3 会话/遥测相关 payload）。
  - 聚合多后端/多协议的数据模型：Bilibili、AList、百度网盘、115（Open/Cloud）、远程扫描/串流等，供 `core_*` 与 feature 模块复用。
  - 提供解析一致性辅助：Moshi `@JsonQualifier`（例如 `NullToEmptyString`）与少量数据转换工具。
- 模块职责（不做什么）
  - 不应承载“可变状态/并发控制/业务聚合逻辑”（否则会让 Base 层语义漂移、增加被依赖面的回归成本）。
  - 不应承载 UI 语义常量（建议落在 UI foundation）。
- 关键入口/关键符号（示例）
  - `data_component/src/main/java/com/xyoye/data_component/entity/MediaLibraryEntity.kt` + `MediaLibraryEntity`
  - `data_component/src/main/java/com/xyoye/data_component/entity/PlayHistoryEntity.kt` + `PlayHistoryEntity`
  - `data_component/src/main/java/com/xyoye/data_component/data/media3/Media3SessionData.kt` + `PlaybackSessionRequestData` / `PlaybackSessionResponseData`
  - `data_component/src/main/java/com/xyoye/data_component/entity/media3/TelemetryEvent.kt` + `TelemetryEvent`
  - `data_component/src/main/java/com/xyoye/data_component/media3/mapper/TelemetryEventMapper.kt` + `TelemetryEventMapper`
  - `data_component/src/main/java/com/xyoye/data_component/repository/subtitle/SubtitleTelemetryRepository.kt` + `SubtitleTelemetryRepository`
  - `data_component/src/main/java/com/xyoye/data_component/helper/moshi/NullToEmptyString.kt` + `NullToEmptyString`
- 依赖边界
  - 对外（被依赖）：几乎所有模块都会直接/间接依赖 `:data_component`（类型复用面大，API 变更成本高）。
  - 对内（依赖）：Room、Moshi、ARouter API + 基础 Kotlin/AndroidX（按 `build.gradle.kts`）。
  - 边界疑点：出现“Repository/Mapper/并发状态”类时，往往意味着 Base 层开始承担运行时职责，需要评估迁移落点以保持分层一致。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - `rg`：按后端/协议分组快速扫 `data/*`，按关键字（`password/token/secret`）抽查明显敏感字段。
  - ast-grep：按语法定位 Room `@Entity`/`@TypeConverters` 的实体定义、并发原语（如 `Mutex()`）使用点，避免纯文本误判。

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| DATA-F001 | ArchitectureRisk | Base 数据模块混入“运行时遥测/并发聚合/映射”逻辑，分层语义变弱 | `data_component/src/main/java/com/xyoye/data_component/repository/subtitle/SubtitleTelemetryRepository.kt` + `SubtitleTelemetryRepository`；`data_component/src/main/java/com/xyoye/data_component/media3/mapper/TelemetryEventMapper.kt` + `TelemetryEventMapper`；`data_component/src/main/java/com/xyoye/data_component/helper/Loading.kt` + `Loading` | N/A | Unify | `:core_log_component`（遥测聚合/事件构建）；`:core_ui_component`（Loading 常量） | High | Medium | P1 | 迁移需要批量改调用方 import；需确认不会引入 feature↔feature 依赖并跑 `verifyModuleDependencies` |
| DATA-F002 | SecurityPrivacyRisk | Room 实体存储明文凭据（password/remoteSecret），存在泄露风险 | `data_component/src/main/java/com/xyoye/data_component/entity/MediaLibraryEntity.kt` + `MediaLibraryEntity#password`/`MediaLibraryEntity#remoteSecret` | N/A | Unify | `:core_storage_component`（凭据管理/加密存储能力落点） | High | Large | P2 | 需要数据迁移策略（DB→安全存储）；回归面涉及远程存储登录/串流/网盘 |
| DATA-F003 | Redundancy | Media3/字幕遥测相关类型分散在多个包（data/entity/media3/mapper/repository），边界不清影响检索与复用 | `data_component/src/main/java/com/xyoye/data_component/data/media3/Media3SessionData.kt` + `PlaybackSessionRequestData`；`data_component/src/main/java/com/xyoye/data_component/entity/media3/TelemetryEvent.kt` + `TelemetryEvent`；`data_component/src/main/java/com/xyoye/data_component/media3/mapper/TelemetryEventMapper.kt` + `TelemetryEventMapper` | N/A | Unify | `:data_component`（以 `media3/*` 单一命名空间收敛） | Low | Medium | P3 | 包名迁移属于高影响“源码级破坏性变更”，需分批迁移与全仓回归 |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| DATA-T001 | DATA-F001 | 将遥测聚合/事件构建逻辑迁出 Base：`data_component` 仅保留数据类型与契约 payload | 迁移 `data_component/.../SubtitleTelemetryRepository.kt`、`data_component/.../TelemetryEventMapper.kt`、`data_component/.../Loading.kt` 到合适模块（建议 `:core_log_component` / `:core_ui_component`），并更新 `player_component` 等调用方 | 1) `:data_component` 不再包含“Repository/Mapper/可变并发状态”类；2) 调用方功能等价（字幕/Media3 遥测仍可用）；3) `./gradlew verifyModuleDependencies` 通过；4) 相关单测（若迁移）在新模块可运行 | High | Medium | P1 | AI（Codex） | Draft |
| DATA-T002 | DATA-F002 | 为媒体库远程凭据建立统一的安全存储策略，避免 DB 明文落盘 | 围绕 `MediaLibraryEntity` 的 `password/remoteSecret`：设计迁移方案（DB 字段废弃/置空 + 安全存储 keyed by libraryId），并在存储实现层收敛读写入口 | 1) 不再在 Room 中持久化明文凭据（或对字段加密后再落盘）；2) 有可回滚/可升级的数据迁移策略；3) 全端登录/挂载流程回归通过；4) 明确禁止日志输出完整凭据对象（必要时提供 redaction helper） | High | Large | P2 | 待分配（Base/Storage） | Draft |
| DATA-T003 | DATA-F003 | 收敛 media3/字幕遥测相关包结构，提升可发现性与复用一致性 | 以 `com.xyoye.data_component.media3` 为根：将 `data/media3`、`entity/media3`、`media3/mapper`、字幕遥测相关 bean 逐步归并为明确子包（例如 `media3.dto`/`media3.entity`/`media3.telemetry`） | 1) 新包结构有明确规则并写入 `document/code_quality_audit/config/audit_dimensions.md` 或模块报告备注；2) 迁移按批次进行，单批次可编译通过；3) 迁移后全仓检索路径清晰（同类类型不再散落） | Low | Medium | P3 | 待分配（Base） | Draft |

## 5) 风险与回归关注点

- Base 层迁移风险：`data_component` 被广泛依赖，任何包名/类移动都会引发大量调用方改动；建议优先“迁移运行时逻辑”而非大规模 package 重构。
- 遥测链路风险：字幕/Media3 遥测通常与播放生命周期绑定，迁移时需确保线程/时钟/窗口语义不变，并关注 TV 端长时播放场景。
- 安全存储改造风险：凭据迁移涉及“升级/降级/清空重登”的体验；需要明确失败兜底（例如迁移失败时要求重新登录）。

## 6) 备注（历史背景/待确认点）

- 本报告为 AI 辅助生成的初稿，建议按 `document/code_quality_audit/global/review_checklist.md` 做一次人工复核后再标记为 Done。
