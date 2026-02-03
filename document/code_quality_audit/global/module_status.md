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
| :core_contract_component | Base（data/contract） | CORE_CONTRACT |  | Todo | document/code_quality_audit/modules/core_contract_component.md |
| :data_component | Base（data/contract） | DATA |  | Todo | document/code_quality_audit/modules/data_component.md |
| :core_log_component | Runtime（system/log） | CORE_LOG |  | Todo | document/code_quality_audit/modules/core_log_component.md |
| :core_system_component | Runtime（system/log） | CORE_SYSTEM |  | Todo | document/code_quality_audit/modules/core_system_component.md |
| :bilibili_component | Infrastructure（network/db/storage/bilibili） | BILIBILI |  | Todo | document/code_quality_audit/modules/bilibili_component.md |
| :core_database_component | Infrastructure（network/db/storage/bilibili） | CORE_DATABASE |  | Todo | document/code_quality_audit/modules/core_database_component.md |
| :core_network_component | Infrastructure（network/db/storage/bilibili） | CORE_NETWORK |  | Todo | document/code_quality_audit/modules/core_network_component.md |
| :core_storage_component | Infrastructure（network/db/storage/bilibili） | CORE_STORAGE |  | Todo | document/code_quality_audit/modules/core_storage_component.md |
| :core_ui_component | UI Foundation（core_ui） | CORE_UI |  | Todo | document/code_quality_audit/modules/core_ui_component.md |
| :anime_component | Feature（业务组件） | ANIME |  | Todo | document/code_quality_audit/modules/anime_component.md |
| :local_component | Feature（业务组件） | LOCAL |  | Todo | document/code_quality_audit/modules/local_component.md |
| :player_component | Feature（业务组件） | PLAYER |  | Todo | document/code_quality_audit/modules/player_component.md |
| :storage_component | Feature（业务组件） | STORAGE |  | Todo | document/code_quality_audit/modules/storage_component.md |
| :user_component | Feature（业务组件） | USER |  | Todo | document/code_quality_audit/modules/user_component.md |
| :app | App Shell | APP |  | Todo | document/code_quality_audit/modules/app.md |
| :repository:danmaku | repository wrappers（第三方 AAR 封装） | REPO_DANMAKU |  | Todo | document/code_quality_audit/modules/repository__danmaku.md |
| :repository:immersion_bar | repository wrappers（第三方 AAR 封装） | REPO_IMMERSION_BAR |  | Todo | document/code_quality_audit/modules/repository__immersion_bar.md |
| :repository:panel_switch | repository wrappers（第三方 AAR 封装） | REPO_PANEL_SWITCH |  | Todo | document/code_quality_audit/modules/repository__panel_switch.md |
| :repository:seven_zip | repository wrappers（第三方 AAR 封装） | REPO_SEVEN_ZIP |  | Todo | document/code_quality_audit/modules/repository__seven_zip.md |
| :repository:thunder | repository wrappers（第三方 AAR 封装） | REPO_THUNDER |  | Todo | document/code_quality_audit/modules/repository__thunder.md |
| :repository:video_cache | repository wrappers（第三方 AAR 封装） | REPO_VIDEO_CACHE |  | Todo | document/code_quality_audit/modules/repository__video_cache.md |
<!-- MODULE_STATUS:END -->

## 备注

- “观察项（Out of Build Scope）”请维护在 `document/code_quality_audit/global/observing.md`（Phase 3/US1）。

