# 治理 Backlog（可执行任务清单）

- 日期：2026-02-04
- 任务总数：68（P1=26, P2=40, P3=2）

> 说明：本文件面向“指派与跟踪”。来源的模块内任务仍保留在各模块报告中；此处提供全局 G-T 视图与批次建议落点。

## P1

| G-ID | 状态 | 负责人 | 目标 | 关联 G-F | 涉及模块 | 来源任务 |
|---|---|---|---|---|---|---|
| G-T0001 | Done | AI（Codex） | WebDAV 默认启用严格 TLS：替换 `UnsafeOkHttpClient`，并为“特殊场景”提供可控开关（用户显式；Release 允许） | G-F0001 | :core_storage_component | CORE_STORAGE-T002 |
| G-T0002 | Done | AI（Codex） | 下线/收敛 `UnsafeOkHttpClient`：仅允许在明确场景使用，并提供更安全替代（证书导入/Pin/仅 HTTP） | G-F0001 | :core_network_component | CORE_NETWORK-T002 |
| G-T0003 | Done | AI（Codex） | 为所有 repository wrapper AAR 补齐元信息（来源/版本/License/校验和/更新流程） | G-F0017 | :repository:danmaku,:repository:immersion_bar,:repository:panel_switch,:repository:seven_zip,:repository:thunder,:repository:video_cache | REPO_DANMAKU-T001,REPO_IMMERSION_BAR-T001,REPO_PANEL_SWITCH-T001,REPO_SEVEN_ZIP-T001,REPO_THUNDER-T001,REPO_VIDEO_CACHE-T001 |
| G-T0004 | Done | AI（Codex） | 修复 `ActivityHelper#getTopActivity` 的潜在崩溃：返回 `firstOrNull` 并清理已销毁 Activity | G-F0022 | :core_system_component | CORE_SYSTEM-T002 |
| G-T0005 | Done | AI（Codex） | 修复 `ISequentialOutStream` 实现：按“流式追加写入”语义输出文件，避免覆盖与性能劣化 | G-F0023 | :repository:seven_zip | REPO_SEVEN_ZIP-T003 |
| G-T0006 | Done | AI（Codex） | 修复资源释放与线程/取消策略：解压过程在 IO 线程执行，且无论成功/失败/取消都能回收 | G-F0025,G-F0066 | :repository:seven_zip | REPO_SEVEN_ZIP-T002 |
| G-T0007 | Done | AI（Codex） | 去硬编码：将 `APP_KEY/APP_SEC` 改为“构建期注入/本地配置”，并提供可控回退策略（避免与发行物强绑定） | G-F0002 | :bilibili_component | BILIBILI-T001 |
| G-T0008 | Done | AI（Codex） | 将 VLC 代理拉流从 `UnsafeOkHttpClient` 迁移到“默认安全”的 OkHttpClient，并提供可控降级策略 | G-F0004 | :player_component | PLAYER-T002 |
| G-T0009 | Done | AI（Codex） | 将“TV 禁用后台/画中画”的策略显式化并避免误伤移动端 | G-F0013 | :app | APP-T001 |
| G-T0010 | Done | AI（Codex） | 将迅雷 SDK 初始化策略改为按需 + 可降级，降低启动期开销与崩溃面 | G-F0024 | :repository:thunder | REPO_THUNDER-T003 |
| G-T0011 | Done | AI（Codex） | 将遥测聚合/事件构建逻辑迁出 Base：`data_component` 仅保留数据类型与契约 payload | G-F0009 | :data_component | DATA-T001 |
| G-T0012 | Done | AI（Codex） | 建立统一脱敏工具与默认策略，并替换关键链路调用点（网络/播放器/缓存/下载） | G-F0005 | :core_log_component,:core_network_component,:player_component,:repository:thunder,:repository:video_cache | CORE_NETWORK-T003,CORE_LOG-T004,PLAYER-T001,REPO_THUNDER-T004,REPO_VIDEO_CACHE-T002 |
| G-T0013 | Done | AI（Codex） | 建立错误上报脱敏规则并改造登录/用户资料相关上报，避免泄露个人信息 | G-F0007 | :user_component | USER-T002 |
| G-T0014 | Done | AI（Codex） | 开启 Room schema 导出并建立迁移校验门禁（至少覆盖主库版本演进） | G-F0010 | :core_database_component | CORE_DATABASE-T005 |
| G-T0015 | Done | AI（Codex） | 抽取 TV Tab 焦点协调与输入策略为可复用组件，统一 DPAD 行为 | G-F0018 | :anime_component | ANIME-T003 |
| G-T0016 | Done | AI（Codex） | 收敛 Bugly 上报门面：统一由 `BuglyReporter` 触达 `CrashReport`，并规范 `ErrorReportHelper` 职责与注释 | G-F0019 | :core_log_component | CORE_LOG-T003 |
| G-T0017 | Done | AI（Codex） | 收敛可写 `LiveData` 暴露：对外只读 + 统一发送入口 | G-F0015 | :core_contract_component | CORE_CONTRACT-T002 |
| G-T0018 | Done | AI（Codex） | 收敛异常处理口径：移除 `printStackTrace()`，统一使用 `ErrorReportHelper` + `LogFacade` 记录必要信息 | G-F0020 | :core_system_component | CORE_SYSTEM-T003 |
| G-T0019 | Done | AI（Codex） | 明确 Fragment 宿主契约：引入 `LoadingHost`（或等价接口）并替换强制 cast，避免隐藏崩溃点 | G-F0011 | :core_ui_component | CORE_UI-T003 |
| G-T0020 | Done | AI（Codex） | 移除投屏 Sender 整条链路：删除旧实现与全端 stub，清理入口/路由/服务暴露 | G-F0021 | :storage_component | STORAGE-T001 |
| G-T0021 | Done | AI（Codex） | 清理 `printStackTrace()` 并补齐异常上报上下文（引擎/会话/源类型） | G-F0016 | :player_component | PLAYER-T005 |
| G-T0022 | Done | AI（Codex） | 用安全方案替换 `EntropyUtils` 的对称加密（至少支持随机 IV/带认证），并为投屏 UDP 消息引入版本/兼容策略 | G-F0003 | :core_system_component,:storage_component,:core_storage_component,:local_component | CORE_SYSTEM-T001 |
| G-T0023 | Done | AI（Codex） | 移除 `printStackTrace()` 并统一异常上报上下文与脱敏策略（URL/token/password/路径） | G-F0006 | :storage_component | STORAGE-T002 |
| G-T0024 | Done | AI（Codex） | 统一 TLS 安全默认：移除默认路径中的 `hostnameVerifier { _, _ -> true }`，并为“特殊场景”提供可控开关（用户显式；Release 允许） | G-F0001 | :core_network_component,:bilibili_component,:core_storage_component | CORE_NETWORK-T001 |
| G-T0025 | Draft | 待分配（Infra/DB/Log） | 统一手动迁移异常可观测性：替换 `printStackTrace()` 为结构化日志/异常上报，并补充必要上下文 | G-F0014 | :core_database_component | CORE_DATABASE-T003 |
| G-T0026 | Draft | 待分配（Log/Feature） | 统一错误上报上下文的构建与脱敏策略，减少重复样板并降低隐私风险 | G-F0008 | :local_component | LOCAL-T003 |

## P2

| G-ID | 状态 | 负责人 | 目标 | 关联 G-F | 涉及模块 | 来源任务 |
|---|---|---|---|---|---|---|
| G-T0027 | Draft | 待分配（Security/System/Bilibili） | 为 Cookie/Token 引入加密存储：落地统一的密钥管理与数据迁移，避免明文落盘 | G-F0028 | :bilibili_component | BILIBILI-T002 |
| G-T0028 | Draft | 待分配（Base/Storage） | 为媒体库远程凭据建立统一的安全存储策略，避免 DB 明文落盘 | G-F0029 | :data_component | DATA-T002 |
| G-T0029 | Draft | 待分配（App） | 以显式结果/能力开关替代 “禁用功能即 throw”，降低误调用崩溃风险 | G-F0040 | :app | APP-T002 |
| G-T0030 | Draft | 待分配（UI） | 定位并修复 DiffUtil 异常根因：约束数据模型或改造 diff 机制，降低回退刷新与上报噪音 | G-F0036 | :core_ui_component | CORE_UI-T002 |
| G-T0031 | Draft | 待分配（Runtime/User） | 对“加密失败保存明文”制定策略：默认安全优先（禁用/提示/二次确认），并提供可追踪的迁移/清理机制 | G-F0026 | :core_system_component | CORE_SYSTEM-T005 |
| G-T0032 | Draft | 待分配（Storage） | 封装/隔离迅雷 SDK 类型，避免第三方类型在上层模块显式出现 | G-F0046 | :repository:thunder | REPO_THUNDER-T002 |
| G-T0033 | Draft | 待分配（UI） | 封装状态栏/沉浸式能力：上层不再直接 import `ImmersionBar`，并尽量减少 `api(...)` 依赖透传 | G-F0035 | :core_ui_component | CORE_UI-T001 |
| G-T0034 | Draft | 待分配（Bilibili/Feature） | 将 B 站弹幕解析/下载编排从 ViewModel 下沉到 repository/usecase，降低 UI 耦合 | G-F0037 | :local_component | LOCAL-T004 |
| G-T0035 | Draft | 待分配（Infra/Network） | 将 `Retrofit` 单例改为可注入 Provider/Factory，提升可测试性并降低全局静态耦合 | G-F0039 | :core_network_component | CORE_NETWORK-T004 |
| G-T0036 | Draft | 待分配（Storage/Feature） | 将字幕匹配/搜索/Hash 能力从 `:local_component` 收敛到基础层，避免 feature 锁死复用 | G-F0056 | :local_component | LOCAL-T001 |
| G-T0037 | Draft | 待分配（DB/Storage/User） | 将扫描扩展目录/过滤配置与刷新逻辑下沉到 repository/usecase，统一 DAO 访问口径 | G-F0042 | :user_component | USER-T003 |
| G-T0038 | Draft | 待分配（Feature/DB） | 将搜索历史/播放历史等持久化细节从 ViewModel 迁移到 repository/usecase 层 | G-F0033 | :anime_component | ANIME-T002 |
| G-T0039 | Draft | 待分配（Infra/DB） | 引入“数据库访问契约/Provider”，逐步替换跨模块对 `DatabaseManager.instance` 的直接引用 | G-F0041 | :core_database_component | CORE_DATABASE-T001 |
| G-T0040 | Draft | AI（Codex） | 打通“写入层磁盘错误→全局状态”链路，确保 `LogSystem`/持久化/Writer 三者一致 | G-F0045 | :core_log_component | CORE_LOG-T002 |
| G-T0041 | Draft | 待分配（DB/Feature） | 抽取 PlayHistory/MediaLibrary 的 repository/usecase，收敛 DAO 访问与写入口径 | G-F0034 | :local_component | LOCAL-T002 |
| G-T0042 | Draft | AI（Codex） | 抽取 `LogLevel` 优先级/比较逻辑为单一实现，移除重复 `levelPriority` | G-F0051 | :core_log_component | CORE_LOG-T001 |
| G-T0043 | Draft | 待分配（UI） | 抽取可复用的 PreferenceDataStore/映射抽象，减少 key→config 样板与 drift | G-F0063 | :user_component | USER-T001 |
| G-T0044 | Draft | 待分配（App/UI） | 抽取壳层 Fragment 装载/切换逻辑，减少重复实现与差异漂移 | G-F0050 | :app | APP-T003 |
| G-T0045 | Draft | 待分配（Storage/Infra） | 抽取投屏协议与 server 公共能力到 core 层，减少 NanoHTTPD/UDP 重复与策略漂移 | G-F0053 | :storage_component | STORAGE-T003 |
| G-T0046 | Draft | 待分配（Infra/Network） | 抽取统一 OkHttpClientFactory：集中维护 timeout/拦截器链/安全策略，减少跨模块漂移 | G-F0049 | :core_network_component | CORE_NETWORK-T005 |
| G-T0047 | Draft | 待分配（Feature/UI/Log） | 抽取统一的“Result 失败处理 + 上下文上报 + toast”助手，减少 ViewModel 样板与口径漂移 | G-F0058 | :anime_component | ANIME-T001 |
| G-T0048 | Draft | 待分配（Bilibili） | 拆分 `BilibiliRepository`：按子域抽取组件并引入单测/契约化接口，提高可维护性 | G-F0038 | :bilibili_component | BILIBILI-T003 |
| G-T0049 | Draft | 待分配（DB/Feature） | 收敛 MediaLibrary/PlayHistory 的写入口径：提供 repository/usecase 并替换 feature 直连 DAO | G-F0031 | :storage_component | STORAGE-T004 |
| G-T0050 | Draft | AI（Codex） | 收敛 contract 层的运行时实现：将可变状态/副作用迁移到 runtime 层，仅保留契约 | G-F0030 | :core_contract_component | CORE_CONTRACT-T001 |
| G-T0051 | Draft | 待分配（Infra/Player/Storage） | 收敛本地代理/HTTP server 能力，减少多实现与策略漂移 | G-F0054 | :player_component | PLAYER-T003 |
| G-T0052 | Draft | 待分配（Infra/Storage） | 收敛第三方协议库依赖泄漏：减少/消除 `api(...)`，上层模块不再直接 import 协议库类型 | G-F0032 | :core_storage_component | CORE_STORAGE-T001 |
| G-T0053 | Draft | 待分配（UI） | 收敛第三方类型扩散：以 `core_ui_component` 提供统一状态栏/沉浸式配置入口 | G-F0047 | :repository:immersion_bar | REPO_IMMERSION_BAR-T002 |
| G-T0054 | Draft | 待分配（Player） | 移除“发送弹幕”整条链路：删除相关 UI/代码并移除 `:repository:panel_switch` 依赖 | G-F0062 | :repository:panel_switch | REPO_PANEL_SWITCH-T002 |
| G-T0055 | Draft | 待分配（System/User） | 明确登录态单一事实源：用 UserSessionManager 统一 token/登录态更新与观察 | G-F0044 | :user_component | USER-T004 |
| G-T0056 | Draft | 待分配（Infra/DB） | 统一 MD5/hex 工具：复用 `CacheKeyMapper`（或抽取 `HashUtils`），移除 `ManualMigration#md5Hex` 重复实现 | G-F0052 | :core_database_component | CORE_DATABASE-T004 |
| G-T0057 | Draft | 待分配（Runtime） | 统一 MMKV 初始化策略：明确“Startup 初始化”与“BaseApplication 初始化”的职责边界，避免重复与时序不一致 | G-F0057 | :core_system_component | CORE_SYSTEM-T004 |
| G-T0058 | Draft | AI（Codex） | 统一 Service 包结构（service/services）：提升 discoverability 并减少迁移成本 | G-F0060 | :core_contract_component | CORE_CONTRACT-T003 |
| G-T0059 | Draft | 待分配（Network/Player） | 统一 Telemetry repository 的模块归属与包命名，避免“core_network 包名却在 feature 模块” | G-F0012 | :player_component | PLAYER-T004 |
| G-T0060 | Draft | 待分配（Build/Repo） | 统一 prebuilt AAR wrapper 的 Gradle 封装方式，减少脚本重复与漂移 | G-F0064 | :repository:danmaku | REPO_DANMAKU-T002 |
| G-T0061 | Draft | 待分配（Infra/Storage/Log） | 统一异常处理与脱敏：移除 `printStackTrace()`，统一到 `LogFacade`/`ErrorReportHelper` 并建立敏感字段脱敏规则 | G-F0043 | :core_storage_component | CORE_STORAGE-T004 |
| G-T0062 | Draft | 待分配（Player/Storage/Network） | 统一本地代理能力：抽取通用 Proxy 服务（headers/range/tls 策略一致），减少 `HttpPlayServer`/`VlcProxyServer` 逻辑重复 | G-F0055 | :core_storage_component | CORE_STORAGE-T005 |
| G-T0063 | Draft | 待分配（Player） | 若继续使用 PanelSwitchHelper：补齐生命周期释放/资源回收，避免潜在泄漏 | G-F0048 | :repository:panel_switch | REPO_PANEL_SWITCH-T003 |
| G-T0064 | Draft | 待分配（Infra/Security/Storage） | 迁移存储凭证到安全存储：DB 中不再保存明文 `password/remoteSecret`（改为引用/加密存储） | G-F0027 | :core_storage_component | CORE_STORAGE-T003 |
| G-T0065 | Draft | 待分配（Infra/Network） | 重命名自定义 `Retrofit` 包装类（或对 `retrofit2.Retrofit` 使用别名 import），降低阅读歧义 | G-F0065 | :core_network_component | CORE_NETWORK-T006 |
| G-T0066 | Draft | 待分配（Infra/DB） | 重构 `DatabaseManager` holder 命名与结构，降低阅读歧义 | G-F0059 | :core_database_component | CORE_DATABASE-T002 |

## P3

| G-ID | 状态 | 负责人 | 目标 | 关联 G-F | 涉及模块 | 来源任务 |
|---|---|---|---|---|---|---|
| G-T0067 | Draft | 待分配（QA/Storage） | 增加可复现的回归用例：覆盖多格式/多文件/大文件/异常压缩包的解压行为 | G-F0023,G-F0067 | :repository:seven_zip | REPO_SEVEN_ZIP-T004 |
| G-T0068 | Draft | 待分配（Base） | 收敛 media3/字幕遥测相关包结构，提升可发现性与复用一致性 | G-F0068 | :data_component | DATA-T003 |

## 使用建议

- 优先从 `P1` 里选“影响面大且回归路径清晰”的任务，先做安全基线与可观测性收敛（避免治理过程中泄露敏感信息）。
- 跨模块治理（尤其是网络/TLS、日志脱敏、proxy 统一）建议先落 `:core_*` 的统一口径，再迁移调用方。
- 每次落地建议在 PR 描述中引用 `G-T####`，并在对应模块报告里把来源任务状态同步为 Done。
