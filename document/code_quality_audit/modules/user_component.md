# 模块排查报告：:user_component

- 模块：:user_component
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：user_component/src/main/java/com/xyoye/user_component/（含 `user_component/src/main/java/com/xyoye/common_component/utils/UserInfoHelper.kt`）

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`USER-F###`  
> - Task：`USER-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 用户中心入口：个人页、登录/退出登录、用户资料修改、主题切换、关于/许可证等页面。
  - 设置中心：播放器设置/字幕设置/弹幕设置/应用设置/开发者设置（基于 PreferenceFragment）。
  - 扫描/缓存管理：扫描扩展目录、文件夹过滤、缓存管理等“维护类”能力入口。
  - 契约服务实现：通过 ARouter 暴露 `UserSessionService`（刷新 token 与恢复登录态）、`DeveloperMenuService`（注入开发者菜单）。
- 模块职责（不做什么）
  - 不应成为“全局用户会话/登录态”的唯一实现落点（否则其他模块若想观察登录态会被迫依赖 feature）；更合适的落点是 `:core_system_component` + `:core_contract_component` 契约暴露。
  - 不建议把大量 key→config 映射与异常上报散落在每个 PreferenceFragment 内部（更适合抽象出复用的 data store/映射层，减少 drift）。
- 关键入口/关键符号（示例）
  - `user_component/src/main/java/com/xyoye/user_component/ui/fragment/personal/PersonalFragment.kt` + `PersonalFragment`（入口路由：`RouteTable.User.PersonalFragment`）
  - `user_component/src/main/java/com/xyoye/user_component/ui/activities/login/LoginActivity.kt` + `LoginActivity`（入口路由：`RouteTable.User.UserLogin`）
  - `user_component/src/main/java/com/xyoye/user_component/ui/activities/login/LoginViewModel.kt` + `LoginViewModel#login`
  - `user_component/src/main/java/com/xyoye/user_component/ui/fragment/PlayerSettingFragment.kt` + `PlayerSettingFragment`（Preference 设置页）
  - `user_component/src/main/java/com/xyoye/user_component/services/UserSessionServiceImpl.kt` + `UserSessionServiceImpl#refreshTokenAndLogin`
- 依赖边界
  - 对内（依赖）：`core_ui/system/log/network/database/storage/bilibili/contract/data`（见 `user_component/build.gradle.kts`）。
  - 对外（被依赖）：作为 feature 模块通常不被其他模块直接依赖；通过 `RouteTable.User.*`（页面）与 `RouteTable.User.*Service`（契约服务）对外可达。
  - 边界疑点：
    - 登录态/用户信息：同时使用 `UserConfig`（KV）、`UserInfoHelper`（单例 + LiveData）与 `UserSessionService`（Service）三套机制，单一事实源不够清晰。
    - 多个 PreferenceFragment 各自维护 `PreferenceDataStore`（key→config 映射），存在重复样板与 drift 风险。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - `rg`：定位 `@Route` 入口、登录态（UserConfig/UserInfoHelper）、Preference key 与 DataStore 分布、DB 访问点（DatabaseManager）。
  - ast-grep：按语法定位 `PreferenceDataStore` 子类、`ErrorReportHelper.postCatchedExceptionWithContext(...)`、`DatabaseManager.instance` 等高频模式。

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| USER-F001 | Redundancy | 多个 PreferenceFragment 各自实现 `PreferenceDataStore`（字符串 key→Config 映射），重复样板多且易 drift | `user_component/src/main/java/com/xyoye/user_component/ui/fragment/AppSettingFragment.kt` + `AppSettingFragment.AppSettingDataStore`；`user_component/src/main/java/com/xyoye/user_component/ui/fragment/PlayerSettingFragment.kt` + `PlayerSettingFragment.PlayerSettingDataStore`；`user_component/src/main/java/com/xyoye/user_component/ui/fragment/SubtitleSettingFragment.kt` + `SubtitleSettingFragment.SubtitleSettingDataStore`；`user_component/src/main/java/com/xyoye/user_component/ui/fragment/DanmuSettingFragment.kt` + `DanmuSettingFragment.DanmuSettingDataStore`；`user_component/src/main/java/com/xyoye/user_component/ui/fragment/DeveloperSettingFragment.kt` + `DeveloperSettingFragment.DeveloperSettingDataStore` | N/A | Unify | `:core_ui_component`（提供可复用 DataStore/映射抽象），配置写入封装可落在 `:core_system_component` | Medium | Medium | P2 | 迁移需保证默认值/枚举值兼容；Preference key 变更会影响已有用户设置 |
| USER-F002 | SecurityPrivacyRisk | 错误上报上下文包含账号/昵称等个人信息，可能进入日志/崩溃上报（隐私风险） | `user_component/src/main/java/com/xyoye/user_component/ui/activities/login/LoginViewModel.kt` + `LoginViewModel#login`（context 含 account）；`user_component/src/main/java/com/xyoye/user_component/ui/activities/user_info/UserInfoViewModel.kt` + `UserInfoViewModel#updateScreenName`/`updatePassword`（context 含 userName/screenName） | N/A | Unify | `:core_log_component`（统一脱敏策略 + 上报入口） | Medium | Small | P1 | 需明确“可定位信息”保留策略（例如仅保留 userId/hash，不保留明文账号/昵称） |
| USER-F003 | ArchitectureRisk | 扫描/过滤等维护逻辑在 ViewModel 内直连 DAO 与触发扫描副作用，缺少可复用的 repository/usecase 层 | `user_component/src/main/java/com/xyoye/user_component/ui/fragment/scan_extend/ScanExtendFragmentViewModel.kt` + `ScanExtendFragmentViewModel#getExtendFolder`/`addExtendFolder`/`removeExtendFolder`；`user_component/src/main/java/com/xyoye/user_component/ui/fragment/scan_filter/ScanFilterFragmentViewModel.kt` + `ScanFilterFragmentViewModel#updateFolder` | N/A | Unify | `:core_database_component`（ScanSettings/FolderFilter repository）+ `:core_storage_component`（扫描/刷新 usecase） | Medium | Medium | P2 | 迁移需回归：扩展目录增删、过滤开关、扫描刷新触发；注意线程（IO/Main）与 UI 刷新时机 |
| USER-F004 | ArchitectureRisk | 登录态/用户信息“单一事实源”不清晰（UserConfig + UserInfoHelper + Service），且 `UserInfoHelper` 位于 feature 内但包名为 common_component，存在分层认知错位 | `user_component/src/main/java/com/xyoye/common_component/utils/UserInfoHelper.kt` + `UserInfoHelper`；`user_component/src/main/java/com/xyoye/user_component/ui/fragment/personal/PersonalFragment.kt` + `PersonalFragment#applyLoginData`/`checkLoggedIn`；`user_component/src/main/java/com/xyoye/user_component/services/UserSessionServiceImpl.kt` + `UserSessionServiceImpl#refreshTokenAndLogin` | N/A | Unify | `:core_system_component`（UserSessionManager）+ `:core_contract_component`（只暴露契约/接口） | Medium | Medium | P2 | 迁移需确保网络层 token 使用方式不变；需明确“观察登录态”的跨模块方案（Flow/LiveData/Service 回调） |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| USER-T001 | USER-F001 | 抽取可复用的 PreferenceDataStore/映射抽象，减少 key→config 样板与 drift | 在 `:core_ui_component` 提供通用 `MappingPreferenceDataStore`（集中 try/catch + 上报）与 key 常量组织方式；迁移 `App/Player/Subtitle/Danmu/Developer` 设置页逐步接入 | 1) 目标 Fragment 内不再手写大量 `when(key)`；2) 旧 key 保持兼容（不丢配置）；3) 关键设置读写行为一致；4) 全仓编译通过 | Medium | Medium | P2 | 待分配（UI） | Draft |
| USER-T002 | USER-F002 | 建立错误上报脱敏规则并改造登录/用户资料相关上报，避免泄露个人信息 | 在 `:core_log_component` 增加脱敏工具（例如 mask/hash），并改造 `LoginViewModel`/`UserInfoViewModel` 的 context 字符串：不再输出明文账号/昵称/用户输入 | 1) 上报上下文不包含明文账号/昵称/新昵称；2) 仍保留可定位信息（例如请求类型/错误码/匿名 ID）；3) 行为不变（toast/loading）；4) 全仓编译通过 | Medium | Small | P1 | 待分配（Log/User） | Draft |
| USER-T003 | USER-F003 | 将扫描扩展目录/过滤配置与刷新逻辑下沉到 repository/usecase，统一 DAO 访问口径 | 在 `:core_database_component` 提供 `ScanSettingsRepository`（ExtendFolder/FolderFilter CRUD）；在 `:core_storage_component` 提供 `VideoScanRefreshUseCase`（触发刷新）；ViewModel 仅编排 UI 状态 | 1) ViewModel 不直接访问 `DatabaseManager.instance.get*Dao()`；2) 扩展目录增删、过滤开关与刷新行为一致；3) 线程调度一致；4) 全仓编译通过 | Medium | Medium | P2 | 待分配（DB/Storage/User） | Draft |
| USER-T004 | USER-F004 | 明确登录态单一事实源：用 UserSessionManager 统一 token/登录态更新与观察 | 在 `:core_system_component` 增加 `UserSessionManager`（包装 `UserConfig`/token 更新与状态流），通过 `:core_contract_component` 暴露只读接口；`user_component` 仅负责 UI 与 `UserSessionServiceImpl` 触发刷新 | 1) `PersonalFragment` 等页面通过统一入口观察登录态（避免多源）；2) 刷新 token 与登录态恢复路径清晰（Service→Manager）；3) 不引入 feature ↔ feature 依赖；4) 全仓编译通过 | Medium | Medium | P2 | 待分配（System/User） | Draft |

## 5) 风险与回归关注点

- 设置项回归面大：播放器/字幕/弹幕/开发者设置改动需回归关键播放链路（不同播放器后端、字幕渲染/偏移、弹幕开关/过滤）。
- 登录态链路：涉及 token 刷新、UserConfig 持久化与 UI 展示；改造“单一事实源”时需回归：登录/退出、资料修改、需要登录的入口跳转。
- 扫描与本地库刷新：`ScanExtendFragmentViewModel` 会触发 `StorageFactory.createStorage(MediaLibraryEntity.LOCAL)` 并刷新目录；迁移 usecase 时需关注 IO 线程与 UI 刷新时机。
- TV 端焦点：个人页（`PersonalFragment`）存在“默认焦点/恢复焦点”逻辑；设置入口在 TV 端需验证 DPAD 可达性与返回语义。

## 6) 备注（历史背景/待确认点）

- 本报告为 AI 辅助生成的初稿，建议按 `document/code_quality_audit/global/review_checklist.md` 做一次人工复核后再标记为 Done。
