# 模块排查报告：:anime_component

- 模块：:anime_component
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：anime_component/src/main/java/com/xyoye/anime_component/

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`ANIME-F###`  
> - Task：`ANIME-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 番剧相关 UI 与交互：首页推荐/时间表、番剧搜索、详情页、季度/标签、追番与历史。
  - 资源搜索（磁力/字幕组筛选）与跳转播放：通过 `RouteTable` 导航到播放器或存储文件详情。
  - TV 端优先：页面包含 TabLayout+ViewPager2、RecyclerView 列表等，需保证 DPAD 焦点可达/可见/可反馈。
- 模块职责（不做什么）
  - 不直接持有跨模块的运行时全局状态；跨模块协作通过 `:core_contract_component` 的 `RouteTable`/Service 契约完成。
  - 不建议在 ViewModel 里直接操作数据库 DAO（更适合收敛到 repository/usecase 层，便于复用与测试）。
- 关键入口/关键符号（示例）
  - `anime_component/src/main/java/com/xyoye/anime_component/ui/fragment/home/HomeFragment.kt` + `HomeFragment`（入口路由：`RouteTable.Anime.HomeFragment`）
  - `anime_component/src/main/java/com/xyoye/anime_component/ui/activities/search/SearchActivity.kt` + `SearchActivity`（番剧/资源搜索聚合页）
  - `anime_component/src/main/java/com/xyoye/anime_component/ui/activities/anime_detail/AnimeDetailActivity.kt` + `AnimeDetailActivity`
  - `anime_component/src/main/java/com/xyoye/anime_component/ui/fragment/search_anime/SearchAnimeFragmentViewModel.kt` + `SearchAnimeFragmentViewModel`
  - `anime_component/src/main/java/com/xyoye/anime_component/ui/fragment/search_magnet/SearchMagnetFragmentViewModel.kt` + `SearchMagnetFragmentViewModel`
  - `anime_component/src/main/java/com/xyoye/anime_component/ui/fragment/anime_episode/AnimeEpisodeFragmentViewModel.kt` + `AnimeEpisodeFragmentViewModel`
- 依赖边界
  - 对内（依赖）：`core_ui/system/log/network/database/storage/contract/data`（见 `build.gradle.kts`），并依赖 ARouter 编译期注入。
  - 对外（被依赖）：通常不被其他模块直接依赖（feature 模块），但其路由入口会被 `:app` 或其他模块通过 `RouteTable` 访问。
  - 边界疑点：
    - ViewModel 直接访问 `DatabaseManager` 与 DAO，容易在多个页面重复“插入/删除/查询历史”的细节逻辑。
    - TV 焦点与键盘策略存在多处手写分支，若各页面自行演进易出现交互不一致。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - `rg`：定位路由（`RouteTable`）使用点、ViewModel 与 repository/dao 的耦合点、TV 端焦点分流点。
  - ast-grep：按语法定位重复的错误上报调用（`ErrorReportHelper.postCatchedExceptionWithContext(...)`）、数据库入口（`DatabaseManager.instance`）、TV Tab 焦点协调器（`TabLayoutViewPager2DpadFocusCoordinator(...)`）。

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| ANIME-F001 | Redundancy | ViewModel 的失败处理/错误上报样板代码重复，口径易漂移（含上下文/脱敏） | `anime_component/src/main/java/com/xyoye/anime_component/ui/fragment/home/HomeFragmentViewModel.kt` + `HomeFragmentViewModel#getWeeklyAnime`；`anime_component/src/main/java/com/xyoye/anime_component/ui/fragment/search_magnet/SearchMagnetFragmentViewModel.kt` + `SearchMagnetFragmentViewModel#search`；`anime_component/src/main/java/com/xyoye/anime_component/ui/activities/anime_detail/AnimeDetailViewModel.kt` + `AnimeDetailViewModel#getAnimeDetail` | N/A | Unify | `:core_log_component`（统一错误上报 helper/脱敏策略）或 `:core_ui_component`（UI 侧 toast/loading 统一封装） | Medium | Medium | P2 | 需要确认不同页面的“提示语/埋点字段”是否可标准化；避免误丢失关键上下文 |
| ANIME-F002 | ArchitectureRisk | ViewModel 直接操作 `DatabaseManager`/DAO（搜索历史、播放历史），持久化细节散落在多个页面 | `anime_component/src/main/java/com/xyoye/anime_component/ui/fragment/search_anime/SearchAnimeFragmentViewModel.kt` + `SearchAnimeFragmentViewModel#search`/`deleteAllSearchHistory`；`anime_component/src/main/java/com/xyoye/anime_component/ui/fragment/search_magnet/SearchMagnetFragmentViewModel.kt` + `SearchMagnetFragmentViewModel#search`；`anime_component/src/main/java/com/xyoye/anime_component/ui/fragment/anime_episode/AnimeEpisodeFragmentViewModel.kt` + `AnimeEpisodeFragmentViewModel#episodeHistoryFlow` | N/A | Unify | `:core_database_component`（提供 anime/magnet/history 的 repository）或在 `:anime_component` 内建立 `data/repository` 子层 | Medium | Medium | P2 | 迁移需要关注线程调度（IO/Main）与 LiveData/Flow 的返回类型一致性 |
| ANIME-F003 | ReuseOpportunity | TV 端 TabLayout+ViewPager2 焦点协调/按键策略在多页重复实现，后续维护易出现交互不一致 | `anime_component/src/main/java/com/xyoye/anime_component/ui/fragment/home/HomeFragment.kt` + `TabLayoutViewPager2DpadFocusCoordinator`；`anime_component/src/main/java/com/xyoye/anime_component/ui/activities/search/SearchActivity.kt` + `TabLayoutViewPager2DpadFocusCoordinator`/`applyDpadSearchEditPolicy`；`anime_component/src/main/java/com/xyoye/anime_component/ui/activities/anime_detail/AnimeDetailActivity.kt` + `TabLayoutViewPager2DpadFocusCoordinator` | N/A | Unify | `:core_ui_component`（抽取可复用的 TV Tab 焦点/键盘策略 helper） | Medium | Small | P1 | TV 交互是强约束：需回归 DPAD 导航路径、默认焦点与返回语义，避免出现焦点陷阱 |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| ANIME-T001 | ANIME-F001 | 抽取统一的“Result 失败处理 + 上下文上报 + toast”助手，减少 ViewModel 样板与口径漂移 | 以 `ErrorReportHelper.postCatchedExceptionWithContext(...)` 为核心：在 `:core_log_component` 或 `:core_ui_component` 提供可复用封装（含脱敏策略），并逐步替换 `anime_component` 内重复代码 | 1) anime_component 内失败分支代码显著收敛（同类逻辑不再复制粘贴）；2) 上报字段包含必要上下文且不输出敏感信息；3) 行为不变（toast/loading 时机一致）；4) 全仓编译通过 | Medium | Medium | P2 | 待分配（Feature/UI/Log） | Draft |
| ANIME-T002 | ANIME-F002 | 将搜索历史/播放历史等持久化细节从 ViewModel 迁移到 repository/usecase 层 | 建立 `AnimeSearchHistoryRepository` / `MagnetSearchHistoryRepository` / `EpisodeHistoryRepository`（落点建议 `:core_database_component`），ViewModel 仅调用抽象 API | 1) ViewModel 不再直接调用 `DatabaseManager.instance.get*Dao()`；2) repository 的返回类型与现有 LiveData/Flow 对齐；3) 线程调度统一（IO/Main）；4) 功能回归：搜索历史新增/删除/查询、剧集历史补全正常 | Medium | Medium | P2 | 待分配（Feature/DB） | Draft |
| ANIME-T003 | ANIME-F003 | 抽取 TV Tab 焦点协调与输入策略为可复用组件，统一 DPAD 行为 | 基于 `TabLayoutViewPager2DpadFocusCoordinator`：在 `:core_ui_component` 增加 helper（例如 `TabLayoutViewPager2DpadFocusCoordinator.attachIfTelevision(...)` / `applyDpadEditTextPolicy(...)` / `bindDpadDownToTabFocus(...)`），并在 Home/Search/Detail 等页面复用 | 1) 仅 TV UI mode 生效（`isTelevisionUiMode()` 分流）；2) DPAD 导航路径无死角，默认焦点明确；3) BACK/MENU 语义与页面一致；4) 相关页面回归通过 | Medium | Small | P1 | AI（Codex） | Done |

## 5) 风险与回归关注点

- TV 焦点回归成本高：涉及 Tab/列表/输入框，多处页面改动需要全程仅用 DPAD 验证“可达/可见/可反馈/可返回”。
- 搜索/历史链路：搜索历史写入/删除与 UI 展示强耦合；迁移到 repository 后需关注 LiveData/Flow 的订阅生命周期与线程切换。
- 错误上报统一化：需要在“信息足够定位问题”和“避免泄露敏感信息（域名/关键字/用户标识）”之间取得平衡。

## 6) 备注（历史背景/待确认点）

- 本报告为 AI 辅助生成的初稿，建议按 `document/code_quality_audit/global/review_checklist.md` 做一次人工复核后再标记为 Done。
