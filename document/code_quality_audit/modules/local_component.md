# 模块排查报告：:local_component

- 模块：:local_component
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：local_component/src/main/java/com/xyoye/local_component/（含 `local_component/src/main/java/com/xyoye/common_component/utils/subtitle/`）

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`LOCAL-F###`  
> - Task：`LOCAL-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 本地媒体库入口：展示媒体库列表、添加/编辑/删除媒体库，并通过 `RouteTable` 跳转到文件浏览/播放历史等页面。
  - 播放历史与播放入口：本地/串流/磁链等历史记录列表，恢复来源并触发播放（通过 `VideoSourceManager` 设定 source）。
  - 外挂资源绑定：为视频绑定弹幕（DanmuFinder）/字幕（Shooter/Thunder 匹配与搜索）。
  - 工具型能力：B 站弹幕下载（解析页面获取 CID，拉取 XML 并保存）。
- 模块职责（不做什么）
  - 不应承载“可跨模块复用”的通用字幕/弹幕工具（否则会逼迫其他 feature 依赖 `:local_component`，违反 feature ↔ feature 禁令）；更适合落在 `:core_storage_component`/`bilibili_component` 等基础层。
  - 不建议在 ViewModel 里直连 DAO/落地持久化细节（建议收敛到 repository/usecase，避免历史与绑定逻辑散落）。
- 关键入口/关键符号（示例）
  - `local_component/src/main/java/com/xyoye/local_component/ui/fragment/media/MediaFragment.kt` + `MediaFragment`（入口路由：`RouteTable.Local.MediaFragment`）
  - `local_component/src/main/java/com/xyoye/local_component/ui/activities/play_history/PlayHistoryActivity.kt` + `PlayHistoryActivity`（入口路由：`RouteTable.Local.PlayHistory`）
  - `local_component/src/main/java/com/xyoye/local_component/ui/fragment/bind_subtitle/BindSubtitleSourceFragmentViewModel.kt` + `BindSubtitleSourceFragmentViewModel`（字幕搜索/下载/绑定）
  - `local_component/src/main/java/com/xyoye/local_component/ui/fragment/bind_danmu/BindDanmuSourceFragmentViewModel.kt` + `BindDanmuSourceFragmentViewModel`（弹幕匹配/下载/绑定）
  - `local_component/src/main/java/com/xyoye/local_component/ui/activities/bilibili_danmu/BilibiliDanmuViewModel.kt` + `BilibiliDanmuViewModel`（解析 URL/下载弹幕）
- 依赖边界
  - 对内（依赖）：`core_ui/system/log/network/database/storage/bilibili/contract/data`（见 `local_component/build.gradle.kts`）。
  - 对外（被依赖）：作为 feature 模块通常不被其他模块直接依赖，入口通过 `RouteTable.Local.*` 暴露给 `:app` 或导航层。
  - 边界疑点：
    - `local_component` 内存在 `com.xyoye.common_component.utils.subtitle.*`（字幕匹配/搜索/Hash）工具类，包名呈现“共享层”语义，但物理落点在 feature 内，易造成复用与分层认知错位。
    - 多处 ViewModel 直接操作 `DatabaseManager.instance.get*Dao()`，导致“播放历史/绑定信息”的写入口径分散。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - `rg`：定位 `@Route` 入口、历史/绑定链路（PlayHistoryDao）、字幕/弹幕工具与网络调用（Jsoup/ResourceRepository）。
  - ast-grep：按语法定位 `ErrorReportHelper.postCatchedExceptionWithContext(...)`、`DatabaseManager.instance` 等高频模式，避免文本误报。

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| LOCAL-F001 | ReuseOpportunity | 字幕匹配/搜索/Hash 工具“看似共享（common_component 包名）但实际锁在 feature”，不利于跨模块复用且易误导分层 | `local_component/src/main/java/com/xyoye/common_component/utils/subtitle/SubtitleSearchHelper.kt` + `SubtitleSearchHelper`；`local_component/src/main/java/com/xyoye/common_component/utils/subtitle/SubtitleMatchHelper.kt` + `SubtitleMatchHelper#matchSubtitle`；`local_component/src/main/java/com/xyoye/common_component/utils/subtitle/SubtitleHashUtils.kt` + `SubtitleHashUtils#getThunderHash`；`local_component/src/main/java/com/xyoye/local_component/ui/fragment/bind_subtitle/BindSubtitleSourceFragmentViewModel.kt` + `BindSubtitleSourceFragmentViewModel#matchSubtitle` | N/A | Unify | `:core_storage_component`（与 `ResourceRepository`/`SubtitleUtils` 同层收敛） | Medium | Medium | P2 | 迁移涉及包名/引用调整；需确认是否有其它模块隐式依赖该包（当前应避免 feature 被依赖） |
| LOCAL-F002 | ArchitectureRisk | ViewModel 直接读写 DAO（播放历史/媒体库/绑定信息），持久化细节散落，口径易漂移且难复用 | `local_component/src/main/java/com/xyoye/local_component/ui/fragment/media/MediaViewModel.kt` + `MediaViewModel#initLocalStorage`；`local_component/src/main/java/com/xyoye/local_component/ui/activities/play_history/PlayHistoryViewModel.kt` + `PlayHistoryViewModel#updatePlayHistory`/`setupHistorySource`；`local_component/src/main/java/com/xyoye/local_component/ui/fragment/bind_subtitle/BindSubtitleSourceFragmentViewModel.kt` + `BindSubtitleSourceFragmentViewModel#databaseSubtitle`；`local_component/src/main/java/com/xyoye/local_component/ui/fragment/bind_danmu/BindDanmuSourceFragmentViewModel.kt` + `BindDanmuSourceFragmentViewModel#databaseDanmu` | N/A | Unify | `:core_database_component`（提供 PlayHistory/MediaLibrary 的 repository/usecase） | Medium | Medium | P2 | 迁移需要回归：历史列表、打开历史播放、绑定/解绑字幕弹幕；涉及线程（IO/Main）与数据一致性 |
| LOCAL-F003 | SecurityPrivacyRisk | 错误上报上下文自由拼接，包含 URL/文件路径/存储地址等信息，存在隐私与口径漂移风险 | `local_component/src/main/java/com/xyoye/local_component/ui/activities/play_history/PlayHistoryViewModel.kt` + `PlayHistoryViewModel#removeHistory`（含 URL）/`openStreamLink`；`local_component/src/main/java/com/xyoye/local_component/ui/fragment/bind_subtitle/BindSubtitleSourceFragmentViewModel.kt` + `BindSubtitleSourceFragmentViewModel#downloadSearchSubtitle`（含 URL）/`databaseSubtitle`（含 file path）；`local_component/src/main/java/com/xyoye/local_component/ui/fragment/media/MediaViewModel.kt` + `MediaViewModel#checkScreenDeviceRunning`（含地址端口） | N/A | Unify | `:core_log_component`（统一“上下文构建 + 脱敏策略 + 上报入口”） | Medium | Small | P1 | 需要明确脱敏策略（例如 URL 仅保留 host/协议、路径截断，文件路径仅保留文件名）；避免影响定位效率 |
| LOCAL-F004 | ArchitectureRisk | `BilibiliDanmuViewModel` 在 ViewModel 内直接进行 Jsoup 网络访问 + HTML/JS 解析 + 下载编排，UI 与数据/网络耦合偏重 | `local_component/src/main/java/com/xyoye/local_component/ui/activities/bilibili_danmu/BilibiliDanmuViewModel.kt` + `BilibiliDanmuViewModel#findVideoCidInJavaScript`/`findAnimeCidInJavaScript`/`saveEpisodeDanmu` | N/A | Unify | `:bilibili_component`（承接 B 站解析/下载 usecase），或 `:core_storage_component`（承接通用下载与落盘） | Medium | Medium | P2 | Jsoup 行为与 OkHttp 栈不同，迁移需确保 UA/超时/重定向/反爬策略一致；需回归 BV/AV/番剧 URL 三条链路 |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| LOCAL-T001 | LOCAL-F001 | 将字幕匹配/搜索/Hash 能力从 `:local_component` 收敛到基础层，避免 feature 锁死复用 | 将 `local_component/src/main/java/com/xyoye/common_component/utils/subtitle/*` 迁移到 `:core_storage_component`（或明确的 core 模块）并整理包结构；`BindSubtitleSourceFragmentViewModel` 只依赖 core 层 API | 1) `:local_component` 不再包含 `com.xyoye.common_component.utils.subtitle` 的实现文件；2) 本地视频字幕“匹配/搜索/下载/解压/绑定”功能回归正常；3) 不引入 feature ↔ feature 依赖；4) 全仓编译通过 | Medium | Medium | P2 | 待分配（Storage/Feature） | Draft |
| LOCAL-T002 | LOCAL-F002 | 抽取 PlayHistory/MediaLibrary 的 repository/usecase，收敛 DAO 访问与写入口径 | 在 `:core_database_component` 增加 `PlayHistoryRepository`（历史查询/删除/清空/更新绑定字段）与 `MediaLibraryRepository`；`MediaViewModel`/`PlayHistoryViewModel`/Bind*ViewModel 使用仓库接口 | 1) 目标 ViewModel 不再直接调用 `DatabaseManager.instance.get*Dao()`；2) 行为一致：历史列表/打开历史播放/解绑字幕弹幕；3) 线程调度一致（IO/Main）；4) 全仓编译通过 | Medium | Medium | P2 | 待分配（DB/Feature） | Draft |
| LOCAL-T003 | LOCAL-F003 | 统一错误上报上下文的构建与脱敏策略，减少重复样板并降低隐私风险 | 在 `:core_log_component`（或 `:core_ui_component`）提供 `safeReport(...)`/`LogContext` 等封装；逐步替换本模块直接拼接 URL/文件路径的上报 | 1) 上报上下文不包含完整 URL/绝对路径/完整存储地址（按策略脱敏）；2) 调用点明显收敛（减少重复 try/catch + post）；3) 关键定位信息仍可用（模块/动作/关键 ID）；4) 全仓编译通过 | Medium | Small | P1 | 待分配（Log/Feature） | Draft |
| LOCAL-T004 | LOCAL-F004 | 将 B 站弹幕解析/下载编排从 ViewModel 下沉到 repository/usecase，降低 UI 耦合 | 抽取 `BilibiliDanmuUseCase`（解析 URL→CID 列表、下载 XML、保存）到 `:bilibili_component` 或 `:core_storage_component`；ViewModel 仅负责 UI 状态与消息流 | 1) ViewModel 不包含 Jsoup.connect/正则抽取 JSON 等解析细节；2) BV/AV/番剧 URL 三条链路功能回归；3) 异常上报上下文符合脱敏策略；4) 全仓编译通过 | Medium | Medium | P2 | 待分配（Bilibili/Feature） | Draft |

## 5) 风险与回归关注点

- DPAD 焦点回归：`MediaFragment` 使用 `RecyclerViewFocusDelegate` 安装 MENU/SETTINGS 键行为；TV 端必须验证默认焦点与可达性。
- 播放历史链路：涉及 `StorageFactory.createStorage(...)` + `VideoSourceManager.setSource(...)`，迁移 repository 时要避免破坏“来源恢复/鉴权/headers”。
- 字幕/弹幕绑定：涉及网络下载、解压（SevenZip）、文件落盘与 DB 字段更新；需准备本地媒体文件与多种字幕压缩包做回归。
- 隐私脱敏：上报内容一旦收敛/脱敏，需确保仍能定位问题（建议保留 host、episodeId、historyId 等非敏感关键信息）。

## 6) 备注（历史背景/待确认点）

- 本报告为 AI 辅助生成的初稿，建议按 `document/code_quality_audit/global/review_checklist.md` 做一次人工复核后再标记为 Done。
