# 模块排查报告：:core_contract_component

- 模块：:core_contract_component
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：core_contract_component/src/main/java/com/xyoye/common_component/

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`CORE_CONTRACT-F###`  
> - Task：`CORE_CONTRACT-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 统一维护 ARouter 路由常量（`RouteTable`），避免字符串散落与跨模块硬编码。
  - 定义壳层/特性模块可通过 ARouter 获取的 `IProvider` Service 契约（例如 `UserSessionService`/`DeveloperMenuService`/`Media3CapabilityProvider`），用于解除 `:app` ↔ feature 的直接依赖。
  - 定义跨模块可复用的契约类型：存储抽象（`Storage`/`StorageFile`）、Media3 Session 合同（`Media3SessionClient`/`Media3SessionServiceProvider`）、播放扩展（`PlaybackAddon*`）、字幕管线接口（`SubtitlePipelineApi`）等。
  - 提供少量“跨模块桥接/通知”能力（`PlayTaskBridge`/`ServiceLifecycleBridge`/`PlayerActions`/`Media3SessionStore`）。
- 模块职责（不做什么）
  - 不承载具体业务实现（网络/数据库/播放器内核/具体存储实现等）。
  - 不引入 feature ↔ feature 依赖（跨模块协作通过 contract + `core_*` 落点表达）。
- 关键入口/关键路径（示例）
  - `core_contract_component/src/main/java/com/xyoye/common_component/config/RouteTable.kt` + `RouteTable`
  - `core_contract_component/src/main/java/com/xyoye/common_component/service/Media3CapabilityProvider.kt` + `Media3CapabilityProvider#prepareSession`
  - `core_contract_component/src/main/java/com/xyoye/common_component/storage/Storage.kt` + `Storage#createPlayUrl`
  - `core_contract_component/src/main/java/com/xyoye/common_component/playback/addon/PlaybackEvent.kt` + `PlaybackEvent.SourceChanged`
  - `core_contract_component/src/main/java/com/xyoye/common_component/subtitle/pipeline/SubtitlePipelineApi.kt` + `SubtitlePipelineApi#init`
- 依赖边界
  - 对外（被依赖）：系统/基础设施/业务特性/壳层模块均可能依赖本模块的公开 API（契约需要稳定、变更成本高）。
  - 对内（依赖）：`:data_component`（公开 API 引用的数据类型）、ARouter API（`IProvider`）、少量 AndroidX/Lifecycle/Coroutines（接口签名与桥接）。
  - 边界疑点：Contract 层混入运行时对象（LiveData/StateFlow 单例/广播）会弱化“纯契约层”语义，且让依赖关系更难推断。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - `rg`：定位跨模块引用点，评估 API 变更影响面。
  - ast-grep：按语法定位 `object` 单例、`IProvider` 接口、`MutableLiveData` 暴露点（避免纯文本误判）。

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE_CONTRACT-F001 | ArchitectureRisk | Contract 模块混入运行时可变状态/副作用，分层语义变弱 | `core_contract_component/src/main/java/com/xyoye/common_component/media3/Media3SessionStore.kt` + `Media3SessionStore`；`core_contract_component/src/main/java/com/xyoye/common_component/bridge/PlayTaskBridge.kt` + `PlayTaskBridge`；`core_contract_component/src/main/java/com/xyoye/common_component/config/PlayerActions.kt` + `PlayerActions#sendExitPlayer` | N/A | Unify | `:core_system_component`（或 `:core_ui_component`） | Medium | Medium | P2 | 迁移涉及多模块引用，需批量改动 import/依赖并跑 `verifyModuleDependencies` |
| CORE_CONTRACT-F002 | ArchitectureRisk | 桥接契约暴露 `MutableLiveData`（可写），外部可直接写入导致状态不可控 | `core_contract_component/src/main/java/com/xyoye/common_component/bridge/LoginObserver.kt` + `LoginObserver#getLoginLiveData`；`core_contract_component/src/main/java/com/xyoye/common_component/bridge/PlayTaskBridge.kt` + `PlayTaskBridge.taskRemoveLiveData` | N/A | Unify | `:core_contract_component`（仅保留只读 API） | Medium | Small | P1 | 需要改动接口签名与少量调用方（`app/`、`user_component/`、`core_storage_component/`） |
| CORE_CONTRACT-F003 | Redundancy | `IProvider` Service 分散在 `service`/`services` 两个包，命名不一致影响检索与维护 | `core_contract_component/src/main/java/com/xyoye/common_component/service/Media3SessionServiceProvider.kt` + `Media3SessionServiceProvider`；`core_contract_component/src/main/java/com/xyoye/common_component/services/UserSessionService.kt` + `UserSessionService` | N/A | Unify | `:core_contract_component` | Low | Small | P2 | 主要是批量移动文件/更新 import；注意保持 ARouter path 常量不变 |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| CORE_CONTRACT-T001 | CORE_CONTRACT-F001 | 收敛 contract 层的运行时实现：将可变状态/副作用迁移到 runtime 层，仅保留契约 | `core_contract_component/src/main/java/com/xyoye/common_component/media3/Media3SessionStore.kt`；`core_contract_component/src/main/java/com/xyoye/common_component/config/PlayerActions.kt`；`core_contract_component/src/main/java/com/xyoye/common_component/bridge/*` + 调用方 | 1) contract 层只保留接口/数据类型；2) 迁移后调用方仍可访问同等能力；3) 不引入 feature↔feature 依赖且 `./gradlew verifyModuleDependencies` 通过 | Medium | Medium | P2 | AI（Codex） | Draft |
| CORE_CONTRACT-T002 | CORE_CONTRACT-F002 | 收敛可写 `LiveData` 暴露：对外只读 + 统一发送入口 | `core_contract_component/src/main/java/com/xyoye/common_component/bridge/LoginObserver.kt`；`core_contract_component/src/main/java/com/xyoye/common_component/bridge/PlayTaskBridge.kt` + 调用方 | 1) `LoginObserver` 返回 `LiveData<LoginData>`；2) `PlayTaskBridge` 对外暴露 `LiveData<Long>`（或只读 Flow）且外部无法直接 `postValue`；3) 全仓编译通过 | Medium | Small | P1 | AI（Codex） | Done |
| CORE_CONTRACT-T003 | CORE_CONTRACT-F003 | 统一 Service 包结构（service/services）：提升 discoverability 并减少迁移成本 | 移动 `core_contract_component/src/main/java/com/xyoye/common_component/service/*` 或 `services/*`，统一到单一包名并批量改引用 | 1) 仅保留一个包名（建议 `...services`）；2) ARouter path 常量保持不变（`RouteTable`）；3) 相关模块编译通过 | Low | Small | P2 | AI（Codex） | Draft |

## 5) 风险与回归关注点

- API 变更影响面大：contract 是“被多模块依赖”的稳定层，任何签名/位置调整都需要全仓迁移与回归。
- ARouter 相关：移动/重命名不应改变 `RouteTable` 的 path 值；需要关注 `IProvider` 注册与获取是否受影响。
- 桥接类迁移：`PlayTaskBridge`/`ServiceLifecycleBridge` 属于跨模块通信关键路径，迁移需确保生命周期与线程语义不变。

## 6) 备注（历史背景/待确认点）

- 本报告为 AI 辅助生成的初稿，建议按 `document/code_quality_audit/global/review_checklist.md` 做一次人工复核后再标记为 Done。
