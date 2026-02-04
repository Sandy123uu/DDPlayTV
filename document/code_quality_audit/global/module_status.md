# 模块覆盖率 / 状态跟踪表

本表用于跟踪“纳入构建范围”的模块排查覆盖率与推进状态，便于分工与复核。

## 状态约定（建议）

- `Todo`：未开始
- `Doing`：进行中
- `Review`：已完成初稿，待复核
- `Done`：复核通过
- `Observing`：仅观察（通常用于非构建范围项；本表默认不使用）

> 提示：模块清单以 `settings.gradle.kts` 的 `include(...)` 为准；脚本会据此生成/更新模块报告骨架与本表。

<!-- MODULE_STATUS:BEGIN -->
| 模块 | 分组 | ID 前缀 | Owner | Status | 报告 |
|---|---|---|---|---|---|
| :core_contract_component | Base（data/contract） | CORE_CONTRACT | 待分配（Base） | Todo | document/code_quality_audit/modules/core_contract_component.md |
| :data_component | Base（data/contract） | DATA | 待分配（Base） | Todo | document/code_quality_audit/modules/data_component.md |
| :core_log_component | Runtime（system/log） | CORE_LOG | 待分配（Runtime） | Todo | document/code_quality_audit/modules/core_log_component.md |
| :core_system_component | Runtime（system/log） | CORE_SYSTEM | 待分配（Runtime） | Todo | document/code_quality_audit/modules/core_system_component.md |
| :bilibili_component | Infrastructure（network/db/storage/bilibili） | BILIBILI | 待分配（Infra） | Todo | document/code_quality_audit/modules/bilibili_component.md |
| :core_database_component | Infrastructure（network/db/storage/bilibili） | CORE_DATABASE | 待分配（Infra） | Todo | document/code_quality_audit/modules/core_database_component.md |
| :core_network_component | Infrastructure（network/db/storage/bilibili） | CORE_NETWORK | 待分配（Infra） | Todo | document/code_quality_audit/modules/core_network_component.md |
| :core_storage_component | Infrastructure（network/db/storage/bilibili） | CORE_STORAGE | 待分配（Infra） | Todo | document/code_quality_audit/modules/core_storage_component.md |
| :core_ui_component | UI Foundation（core_ui） | CORE_UI | 待分配（UI） | Todo | document/code_quality_audit/modules/core_ui_component.md |
| :anime_component | Feature（业务组件） | ANIME | 待分配（Feature） | Todo | document/code_quality_audit/modules/anime_component.md |
| :local_component | Feature（业务组件） | LOCAL | 待分配（Feature） | Todo | document/code_quality_audit/modules/local_component.md |
| :player_component | Feature（业务组件） | PLAYER | 待分配（Feature） | Todo | document/code_quality_audit/modules/player_component.md |
| :storage_component | Feature（业务组件） | STORAGE | 待分配（Feature） | Todo | document/code_quality_audit/modules/storage_component.md |
| :user_component | Feature（业务组件） | USER | 待分配（Feature） | Todo | document/code_quality_audit/modules/user_component.md |
| :app | App Shell | APP | 待分配（App） | Todo | document/code_quality_audit/modules/app.md |
| :repository:danmaku | repository wrappers（第三方 AAR 封装） | REPO_DANMAKU | 待分配（Repo） | Todo | document/code_quality_audit/modules/repository__danmaku.md |
| :repository:immersion_bar | repository wrappers（第三方 AAR 封装） | REPO_IMMERSION_BAR | 待分配（Repo） | Todo | document/code_quality_audit/modules/repository__immersion_bar.md |
| :repository:panel_switch | repository wrappers（第三方 AAR 封装） | REPO_PANEL_SWITCH | 待分配（Repo） | Todo | document/code_quality_audit/modules/repository__panel_switch.md |
| :repository:seven_zip | repository wrappers（第三方 AAR 封装） | REPO_SEVEN_ZIP | 待分配（Repo） | Todo | document/code_quality_audit/modules/repository__seven_zip.md |
| :repository:thunder | repository wrappers（第三方 AAR 封装） | REPO_THUNDER | 待分配（Repo） | Todo | document/code_quality_audit/modules/repository__thunder.md |
| :repository:video_cache | repository wrappers（第三方 AAR 封装） | REPO_VIDEO_CACHE | 待分配（Repo） | Todo | document/code_quality_audit/modules/repository__video_cache.md |
<!-- MODULE_STATUS:END -->

## 备注

- “观察项（Out of Build Scope）”请维护在 `document/code_quality_audit/global/observing.md`（Phase 3/US1）。
- 本表中 Owner 允许先填“待分配（组名）”，但开始排查前必须替换为具体负责人（人名/账号/小组）。

### 建议排期（可按实际调整）

> 说明：这里给出“按分组推进”的建议截止时间；实际执行可按团队资源调整。

| 模块 | 建议完成日期 |
|---|---|
| :core_contract_component | 2026-02-04 |
| :data_component | 2026-02-04 |
| :core_system_component | 2026-02-04 |
| :core_log_component | 2026-02-04 |
| :core_network_component | 2026-02-05 |
| :core_database_component | 2026-02-05 |
| :core_storage_component | 2026-02-05 |
| :bilibili_component | 2026-02-05 |
| :core_ui_component | 2026-02-06 |
| :player_component | 2026-02-07 |
| :local_component | 2026-02-07 |
| :anime_component | 2026-02-07 |
| :storage_component | 2026-02-07 |
| :user_component | 2026-02-07 |
| :app | 2026-02-08 |
| :repository:danmaku | 2026-02-08 |
| :repository:immersion_bar | 2026-02-08 |
| :repository:panel_switch | 2026-02-08 |
| :repository:seven_zip | 2026-02-08 |
| :repository:thunder | 2026-02-08 |
| :repository:video_cache | 2026-02-08 |

