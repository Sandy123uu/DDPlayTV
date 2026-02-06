# 全局问题清单与治理路线图（增量）

- 日期：2026-02-04
- 已汇总模块报告：
  - `:anime_component@2026-02-04`（`document/code_quality_audit/modules/anime_component.md`）
  - `:app@2026-02-04`（`document/code_quality_audit/modules/app.md`）
  - `:bilibili_component@2026-02-04`（`document/code_quality_audit/modules/bilibili_component.md`）
  - `:core_contract_component@2026-02-04`（`document/code_quality_audit/modules/core_contract_component.md`）
  - `:core_database_component@2026-02-04`（`document/code_quality_audit/modules/core_database_component.md`）
  - `:core_log_component@2026-02-04`（`document/code_quality_audit/modules/core_log_component.md`）
  - `:core_network_component@2026-02-04`（`document/code_quality_audit/modules/core_network_component.md`）
  - `:core_storage_component@2026-02-04`（`document/code_quality_audit/modules/core_storage_component.md`）
  - `:core_system_component@2026-02-04`（`document/code_quality_audit/modules/core_system_component.md`）
  - `:core_ui_component@2026-02-04`（`document/code_quality_audit/modules/core_ui_component.md`）
  - `:data_component@2026-02-04`（`document/code_quality_audit/modules/data_component.md`）
  - `:local_component@2026-02-04`（`document/code_quality_audit/modules/local_component.md`）
  - `:player_component@2026-02-04`（`document/code_quality_audit/modules/player_component.md`）
  - `:repository:danmaku@2026-02-04`（`document/code_quality_audit/modules/repository__danmaku.md`）
  - `:repository:immersion_bar@2026-02-04`（`document/code_quality_audit/modules/repository__immersion_bar.md`）
  - `:repository:panel_switch@2026-02-04`（`document/code_quality_audit/modules/repository__panel_switch.md`）
  - `:repository:seven_zip@2026-02-04`（`document/code_quality_audit/modules/repository__seven_zip.md`）
  - `:repository:thunder@2026-02-04`（`document/code_quality_audit/modules/repository__thunder.md`）
  - `:repository:video_cache@2026-02-04`（`document/code_quality_audit/modules/repository__video_cache.md`）
  - `:storage_component@2026-02-04`（`document/code_quality_audit/modules/storage_component.md`）
  - `:user_component@2026-02-04`（`document/code_quality_audit/modules/user_component.md`）

> 说明：本文件作为“增量汇总”的单一落点。每次新增/更新模块报告后，按去重合并结果追加/更新本表，并维护来源映射（必须维护）。

## 0) 合并口径（本轮）

- 本轮以“全量覆盖”为第一目标：所有模块内 Finding/Task 均被纳入来源映射。
- 去重合并采用“谨慎策略”：仅对跨模块高度同质、且合并不会丢失语义的信息进行合并；其余保持一对一映射，避免误合并导致后续治理方向摇摆。

## 1) 全局 Findings（去重合并后）

> 全局 ID：`G-F####`（合并后分配），并保留来源映射（必须维护）。

| G-ID | 类别 | 标题 | 涉及模块 | 来源（模块内 ID 列表） | 结论 | 建议落点 | P |
|---|---|---|---|---|---|---|---|
| G-F0001 | SecurityPrivacy | OkHttp/TLS 默认路径存在禁用校验/信任所有证书（hostnameVerifier/UnsafeOkHttpClient）风险外溢 | :core_network_component,:core_storage_component | CORE_NETWORK-F001,CORE_NETWORK-F002,CORE_STORAGE-F002 | Unify | :core_network_component（安全策略与 OkHttpClientFactory）+ 调用方迁移（bilibili/storage/player） | P1 |
| G-F0002 | SecurityPrivacy | TV 客户端 `APP_KEY/APP_SEC` 在源码硬编码（等同固定凭证），存在泄露/封禁风险，且不利于多渠道/多策略切换 | :bilibili_component | BILIBILI-F001 | Unify | `:core_system_component`（构建期注入/配置隔离）+ `:bilibili_component`（消费端改造） | P1 |
| G-F0003 | SecurityPrivacy | `EntropyUtils` 提供不安全的加密/摘要实现（固定 IV + 默认 Key + MD5），且被投屏 UDP 组播直接使用 | :core_system_component | CORE_SYSTEM-F001 | Deprecate | `:core_system_component`（提供安全实现）+ `:storage_component`（迁移调用） | P1 |
| G-F0004 | SecurityPrivacy | `VlcProxyServer` 使用 `UnsafeOkHttpClient` 代理拉流，默认信任所有证书存在 MITM 风险 | :player_component | PLAYER-F002 | Unify | `:core_network_component`（统一 OkHttpClientFactory/TLS 策略）+ `:player_component`（迁移使用） | P1 |
| G-F0005 | SecurityPrivacy | 日志/异常上报缺少统一脱敏策略（URL/query、Header、请求参数/JSON、本地路径、磁力链） | :core_log_component,:core_network_component,:player_component,:repository:thunder,:repository:video_cache | CORE_NETWORK-F003,CORE_LOG-F004,PLAYER-F001,REPO_THUNDER-F004,REPO_VIDEO_CACHE-F002 | Unify | :core_log_component（脱敏工具/策略）+ :core_network_component/:player_component/:core_storage_component（统一替换调用点） | P1 |
| G-F0006 | SecurityPrivacy | 模块内 `printStackTrace()` 分布广且错误上报上下文拼接分散，存在隐私泄露与可观测性口径漂移风险 | :storage_component | STORAGE-F002 | Unify | `:core_log_component`（统一异常上报入口 + 脱敏策略）+ 本模块逐步迁移替换 | P1 |
| G-F0007 | SecurityPrivacyRisk | 错误上报上下文包含账号/昵称等个人信息，可能进入日志/崩溃上报（隐私风险） | :user_component | USER-F002 | Unify | `:core_log_component`（统一脱敏策略 + 上报入口） | P1 |
| G-F0008 | SecurityPrivacyRisk | 错误上报上下文自由拼接，包含 URL/文件路径/存储地址等信息，存在隐私与口径漂移风险 | :local_component | LOCAL-F003 | Unify | `:core_log_component`（统一“上下文构建 + 脱敏策略 + 上报入口”） | P1 |
| G-F0009 | ArchitectureRisk | Base 数据模块混入“运行时遥测/并发聚合/映射”逻辑，分层语义变弱 | :data_component | DATA-F001 | Unify | `:core_log_component`（遥测聚合/事件构建）；`:core_ui_component`（Loading 常量） | P1 |
| G-F0010 | ArchitectureRisk | Room schema 未导出（`exportSchema = false`），迁移链路缺少“可回放/可验证”的证据与门禁，长期演进风险高 | :core_database_component | CORE_DATABASE-F005 | Unify | `:core_database_component`（schema 导出 + 迁移校验） | P1 |
| G-F0011 | ArchitectureRisk | `BaseAppFragment#onAttach` 强制将宿主 Activity cast 为 `BaseAppCompatActivity`，复用边界不清且存在 `ClassCastException` 风险 | :core_ui_component | CORE_UI-F003 | Unify | `:core_ui_component`（引入 `LoadingHost` 等契约/安全校验） | P1 |
| G-F0012 | ArchitectureRisk | `Media3TelemetryRepository` 放在 player 模块但使用 `com.xyoye.common_component.network.repository`（core_network 命名空间），导致模块语义与包归属错位 | :player_component | PLAYER-F004 | Unify | 将 Telemetry repository 下沉到 `:core_network_component` 或调整包名为 `com.xyoye.player_component.*`，明确 ownership | P1 |
| G-F0013 | ArchitectureRisk | “TV 适配禁用”逻辑未做 TV 判定，可能误伤移动端能力（后台播放/画中画） | :app | APP-F001 | Unify | `:app`（按 `Context.isTelevisionUiMode()` 或配置开关分流） | P1 |
| G-F0014 | ArchitectureRisk | 启动期手动迁移异常仅 `printStackTrace()`，缺少统一日志/上报与上下文；迁移失败可能静默，难排障 | :core_database_component | CORE_DATABASE-F003 | Unify | `:core_database_component`（统一上报）+ `:core_log_component`（接入） | P1 |
| G-F0015 | ArchitectureRisk | 桥接契约暴露 `MutableLiveData`（可写），外部可直接写入导致状态不可控 | :core_contract_component | CORE_CONTRACT-F002 | Unify | `:core_contract_component`（仅保留只读 API） | P1 |
| G-F0016 | ArchitectureRisk | 模块内仍存在 `printStackTrace()`，与统一异常上报/脱敏口径不一致，且容易在崩溃场景遗漏上下文 | :player_component | PLAYER-F005 | Unify | `:core_log_component` 统一 `safeReport(...)`；player 侧替换并补齐上下文（引擎/源类型/会话） | P1 |
| G-F0017 | ArchitectureRisk | 预编译 AAR wrapper 缺少可追溯元信息（来源/版本/License/校验和/更新流程） | :repository:danmaku,:repository:immersion_bar,:repository:panel_switch,:repository:seven_zip,:repository:thunder,:repository:video_cache | REPO_DANMAKU-F001,REPO_IMMERSION_BAR-F001,REPO_PANEL_SWITCH-F001,REPO_SEVEN_ZIP-F001,REPO_THUNDER-F001,REPO_VIDEO_CACHE-F001 | Unify | repository/*（补齐 README 元信息）+ 全仓统一规范（建议由 buildSrc 或文档约束） | P1 |
| G-F0018 | ReuseOpportunity | TV 端 TabLayout+ViewPager2 焦点协调/按键策略在多页重复实现，后续维护易出现交互不一致 | :anime_component | ANIME-F003 | Unify | `:core_ui_component`（抽取可复用的 TV Tab 焦点/键盘策略 helper） | P1 |
| G-F0019 | Redundancy | Bugly 上报入口分散且约定不一致：`BuglyReporter` + `ErrorReportHelper` 并存，且存在引用不存在文档的注释 | :core_log_component | CORE_LOG-F003 | Unify | `:core_log_component`（收敛为单一门面） | P1 |
| G-F0020 | Redundancy | 多处异常处理仍 `printStackTrace()`，与 `ErrorReportHelper`/`LogFacade` 重复且易污染日志 | :core_system_component | CORE_SYSTEM-F005 | Unify | `:core_system_component` | P1 |
| G-F0021 | Redundancy | 投屏 Sender 处于“全端静默 stub + 大段注释旧实现”状态（不可达死链路） | :storage_component | STORAGE-F001 | Deprecate | 直接移除整条链路：删除 `ScreencastProvideService*` 与相关入口/路由/注释实现 | P1 |
| G-F0022 | StabilityRisk | `ActivityHelper#getTopActivity` 使用 `first {}` 可能在无存活 Activity 时抛异常，后台服务调用存在崩溃风险 | :core_system_component | CORE_SYSTEM-F002 | Unify | `:core_system_component` | P1 |
| G-F0023 | BugRisk | `ISequentialOutStream` 的写入语义可疑：每次 `write()` 都新建 `FileOutputStream` 且不追加，存在文件被覆盖/性能差的风险 | :repository:seven_zip | REPO_SEVEN_ZIP-F003 | Unify | `:core_storage_component`（按“流式输出”语义实现：持有同一输出流并追加写入，或改用更合适的 out stream 实现） | P1 |
| G-F0024 | PerformanceRisk | 使用 AndroidX Startup 在进程启动期初始化迅雷 SDK，可能引入启动耗时与崩溃面（尽管已做 ABI 守卫与 try/catch） | :repository:thunder | REPO_THUNDER-F003 | Unify | 优先改为“按需初始化”（首次使用磁力/BT 能力时初始化），并把初始化结果缓存；Startup 仅做轻量预检/开关（或移除） | P1 |
| G-F0025 | BugRisk | 解压实现存在资源释放缺口：`RandomAccessFile`/`IInArchive` 未关闭，协程取消亦不释放（潜在 FD 泄漏/崩溃） | :repository:seven_zip | REPO_SEVEN_ZIP-F002 | Unify | `:core_storage_component`（统一资源管理：关闭 archive/stream，并在取消时回收） | P1 |
| G-F0026 | SecurityPrivacy | `DeveloperCredentialStore` 在加密失败时会回退保存明文凭证，存在凭证泄露风险 | :core_system_component | CORE_SYSTEM-F004 | Unify | `:core_system_component`（存储策略）+ `:user_component`（提示/开关） | P2 |
| G-F0027 | SecurityPrivacy | 媒体库配置包含明文账号/密码字段（Room 表 `media_library`），协议实现直接读取使用，存在本地泄露风险 | :core_storage_component | CORE_STORAGE-F003 | Unify | `:core_system_component`（安全存储/KeyStore）+ `:data_component`（数据模型迁移）+ `:core_storage_component`（读写路径改造） | P2 |
| G-F0028 | SecurityPrivacy | 登录态（Cookie/Token）使用 MMKV 明文持久化，缺少加密/密钥管理与迁移策略 | :bilibili_component | BILIBILI-F002 | Unify | `:core_system_component`（通用加密存储能力，参考 `DeveloperCredentialStore`）+ `:bilibili_component`（迁移与落地） | P2 |
| G-F0029 | SecurityPrivacyRisk | Room 实体存储明文凭据（password/remoteSecret），存在泄露风险 | :data_component | DATA-F002 | Unify | `:core_storage_component`（凭据管理/加密存储能力落点） | P2 |
| G-F0030 | ArchitectureRisk | Contract 模块混入运行时可变状态/副作用，分层语义变弱 | :core_contract_component | CORE_CONTRACT-F001 | Unify | `:core_system_component`（或 `:core_ui_component`） | P2 |
| G-F0031 | ArchitectureRisk | DAO 访问散落在 ViewModel/Dialog/Controller 中，数据写入口径分散（MediaLibrary/PlayHistory），迁移与一致性维护成本高 | :storage_component | STORAGE-F004 | Unify | `:core_database_component` 提供 repository/usecase（PlayHistory/MediaLibrary），feature 侧只依赖接口 | P2 |
| G-F0032 | ArchitectureRisk | Gradle `api(...)` 暴露底层协议库，导致上层模块直接依赖实现细节（边界泄漏/升级困难） | :core_storage_component | CORE_STORAGE-F001 | Unify | `:core_storage_component`（隐藏第三方类型、提供抽象/封装）+ 上层模块迁移 | P2 |
| G-F0033 | ArchitectureRisk | ViewModel 直接操作 `DatabaseManager`/DAO（搜索历史、播放历史），持久化细节散落在多个页面 | :anime_component | ANIME-F002 | Unify | `:core_database_component`（提供 anime/magnet/history 的 repository）或在 `:anime_component` 内建立 `data/repository` 子层 | P2 |
| G-F0034 | ArchitectureRisk | ViewModel 直接读写 DAO（播放历史/媒体库/绑定信息），持久化细节散落，口径易漂移且难复用 | :local_component | LOCAL-F002 | Unify | `:core_database_component`（提供 PlayHistory/MediaLibrary 的 repository/usecase） | P2 |
| G-F0035 | ArchitectureRisk | `:core_ui_component` 通过 `api(...)` 透传大量 UI/第三方依赖（含 ImmersionBar），导致上层模块形成隐式依赖与边界泄漏 | :core_ui_component | CORE_UI-F001 | Unify | `:core_ui_component`（封装状态栏/主题 API，减少第三方类型外泄） | P2 |
| G-F0036 | ArchitectureRisk | `BaseAdapter#setData` 对 DiffUtil 异常使用 try/catch 回退 `notifyDataSetChanged()`（带 TODO），根因未定位可能造成性能抖动与上报噪音 | :core_ui_component | CORE_UI-F002 | Unify | `:core_ui_component`（收敛为稳定的 diff 策略/数据约束） | P2 |
| G-F0037 | ArchitectureRisk | `BilibiliDanmuViewModel` 在 ViewModel 内直接进行 Jsoup 网络访问 + HTML/JS 解析 + 下载编排，UI 与数据/网络耦合偏重 | :local_component | LOCAL-F004 | Unify | `:bilibili_component`（承接 B 站解析/下载 usecase），或 `:core_storage_component`（承接通用下载与落盘） | P2 |
| G-F0038 | ArchitectureRisk | `BilibiliRepository` 单类过大且多职责（登录/风控/播放/历史/直播弹幕等），演进与测试成本高 | :bilibili_component | BILIBILI-F003 | Unify | `:bilibili_component`（按子域拆分：Auth/Risk/Playback/History/Live 等） | P2 |
| G-F0039 | ArchitectureRisk | `Retrofit` 以全局单例形式暴露 Service（静态入口），可测试性与可替换性较弱 | :core_network_component | CORE_NETWORK-F004 | Unify | `:core_network_component`（引入 Factory/Provider）+ `:core_contract_component`（如需抽象契约） | P2 |
| G-F0040 | ArchitectureRisk | “TV 构建禁用”的能力以 `UnsupportedOperationException` 兜底，误调用会直接崩溃 | :app | APP-F002 | Unify | `:app` + `:data_component`（用显式能力开关/结果类型替代 throw；或按构建变体裁剪入口） | P2 |
| G-F0041 | ArchitectureRisk | 全仓大量直接依赖 `DatabaseManager.instance`（静态单例）访问 DAO，跨模块耦合强、可测试性差 | :core_database_component | CORE_DATABASE-F001 | Unify | `:core_contract_component`（定义 DB 访问契约）+ `:core_database_component`（实现）+ 调用方模块迁移 | P2 |
| G-F0042 | ArchitectureRisk | 扫描/过滤等维护逻辑在 ViewModel 内直连 DAO 与触发扫描副作用，缺少可复用的 repository/usecase 层 | :user_component | USER-F003 | Unify | `:core_database_component`（ScanSettings/FolderFilter repository）+ `:core_storage_component`（扫描/刷新 usecase） | P2 |
| G-F0043 | ArchitectureRisk | 模块内 `printStackTrace()` 广泛存在（含网络/文件/解析路径），错误可观测性与脱敏策略不一致 | :core_storage_component | CORE_STORAGE-F004 | Unify | `:core_storage_component`（统一异常处理/日志）+ `:core_log_component`（上报接口） | P2 |
| G-F0044 | ArchitectureRisk | 登录态/用户信息“单一事实源”不清晰（UserConfig + UserInfoHelper + Service），且 `UserInfoHelper` 位于 feature 内但包名为 common_component，存在分层认知错位 | :user_component | USER-F004 | Unify | `:core_system_component`（UserSessionManager）+ `:core_contract_component`（只暴露契约/接口） | P2 |
| G-F0045 | ArchitectureRisk | 磁盘写入失败后状态机与持久化/对外状态不同步：Writer 内部熔断但 `LogSystem` 未必反映 | :core_log_component | CORE_LOG-F002 | Unify | `:core_log_component`（以 `LogSystem` 为单一真源） | P2 |
| G-F0046 | ArchitectureRisk | 第三方 SDK 类型在 `:core_storage_component` 中被继承/暴露，存在“第三方类型泄漏到上层模块”的边界风险 | :repository:thunder | REPO_THUNDER-F002 | Unify | 将 `com.xunlei.*` 类型收敛在 `utils/thunder` 与内部实现层：对外使用自定义 model/interface（例如 `TorrentMeta`/`TorrentEntry`），避免上层模块被迫感知第三方类型 | P2 |
| G-F0047 | ArchitectureRisk | 第三方类型通过 `core_ui_component` 的 `api(...)` 扩散到多模块，耦合面大且升级成本高 | :repository:immersion_bar | REPO_IMMERSION_BAR-F002 | Unify | `:core_ui_component`（提供抽象/门面，逐步收敛到统一入口） | P2 |
| G-F0048 | ArchitectureRisk | 调用侧未持有 `PanelSwitchHelper` 实例，生命周期/资源释放不可控（潜在泄漏/窗口监听残留） | :repository:panel_switch | REPO_PANEL_SWITCH-F003 | Unify | `:player_component`（若继续保留该能力，封装为可释放的成员并与 Dialog 生命周期绑定） | P2 |
| G-F0049 | Duplication | OkHttpClient 构建参数/拦截器链在多个模块重复（timeout、解压/动态域名/日志等），缺少统一工厂导致“安全口径漂移” | :core_network_component | CORE_NETWORK-F005 | Unify | `:core_network_component`（提取 OkHttpClientFactory） | P2 |
| G-F0050 | Duplication | TV/移动壳层都实现了“ARouter 装载 Fragment + show/hide”流程，逻辑重复且易漂移 | :app | APP-F003 | Unify | `:app`（抽取壳层导航/Fragment 交换策略为可复用组件，保留 UI 差异） | P2 |
| G-F0051 | Duplication | `LogLevel` 优先级映射重复实现（两处 `levelPriority`），存在口径漂移风险 | :core_log_component | CORE_LOG-F001 | Unify | `:core_log_component`（建议收敛到 `LogLevel`） | P2 |
| G-F0052 | Duplication | 多处存在 MD5/hex 计算工具的重复实现，`ManualMigration#md5Hex` 复刻了已有能力，建议统一到共享工具 | :core_database_component | CORE_DATABASE-F004 | Unify | `:core_system_component`（统一 hash 工具）+ 调用方模块（迁移） | P2 |
| G-F0053 | Duplication | 投屏协议/服务器实现分散：本模块自建 NanoHTTPD/UDP 发现，与 core 层的投屏存储与本地代理能力存在重叠，安全/Range/鉴权策略难统一 | :storage_component | STORAGE-F003 | Unify | 以 `:core_storage_component` 作为投屏 domain 的统一落点（协议/常量/鉴权/错误口径），feature 仅负责 UI 与服务编排 | P2 |
| G-F0054 | Duplication | 本地代理/HTTP server 能力存在多实现且策略不一致：VLC 代理（player）与 core/storage 的本地代理/投屏 server 存在重叠 | :player_component | PLAYER-F003 | Unify | 以 `:core_storage_component`（或明确的 core 模块）收敛“代理/headers/range/鉴权/错误口径”，player 侧只做引擎适配 | P2 |
| G-F0055 | Duplication | 本地代理能力存在多实现：`HttpPlayServer`（mpv 等）与 `VlcProxyServer`（VLC）均基于 NanoHTTPD 且均涉及不安全 OkHttpClient，策略不一致 | :core_storage_component | CORE_STORAGE-F005 | Unify | `:core_storage_component`（统一 proxy 能力）或 `:player_component`（按播放器收敛），并统一 TLS 策略落点到 `:core_network_component` | P2 |
| G-F0056 | ReuseOpportunity | 字幕匹配/搜索/Hash 工具“看似共享（common_component 包名）但实际锁在 feature”，不利于跨模块复用且易误导分层 | :local_component | LOCAL-F001 | Unify | `:core_storage_component`（与 `ResourceRepository`/`SubtitleUtils` 同层收敛） | P2 |
| G-F0057 | Redundancy | MMKV 初始化入口重复（Startup `BaseInitializer` 与 `BaseApplication#onCreate` 均调用 `MMKV.initialize`） | :core_system_component | CORE_SYSTEM-F003 | Unify | `:core_system_component` | P2 |
| G-F0058 | Redundancy | ViewModel 的失败处理/错误上报样板代码重复，口径易漂移（含上下文/脱敏） | :anime_component | ANIME-F001 | Unify | `:core_log_component`（统一错误上报 helper/脱敏策略）或 `:core_ui_component`（UI 侧 toast/loading 统一封装） | P2 |
| G-F0059 | Redundancy | `DatabaseManager` 使用“类同名嵌套 object（`private object DatabaseManager`）”实现 holder，命名造成阅读/检索混淆 | :core_database_component | CORE_DATABASE-F002 | Unify | `:core_database_component` | P2 |
| G-F0060 | Redundancy | `IProvider` Service 分散在 `service`/`services` 两个包，命名不一致影响检索与维护 | :core_contract_component | CORE_CONTRACT-F003 | Unify | `:core_contract_component` | P2 |
| G-F0061 | Redundancy | wrapper 模块重复“手写 default artifact”封装方式，构建脚本口径易漂移 | :repository:immersion_bar | REPO_IMMERSION_BAR-F003 | Unify | `buildSrc`（统一 prebuilt-aar 约定插件/脚本） | P2 |
| G-F0062 | Redundancy | 依赖处于“编译期存在、运行期不可达”：发送弹幕入口被禁用，相关 UI/代码疑似废弃 | :repository:panel_switch | REPO_PANEL_SWITCH-F002 | Deprecate | `:player_component`（直接移除整条链路：删除相关 UI/代码并移除依赖） | P2 |
| G-F0063 | Redundancy | 多个 PreferenceFragment 各自实现 `PreferenceDataStore`（字符串 key→Config 映射），重复样板多且易 drift | :user_component | USER-F001 | Unify | `:core_ui_component`（提供可复用 DataStore/映射抽象），配置写入封装可落在 `:core_system_component` | P2 |
| G-F0064 | Redundancy | 多个 wrapper 模块重复使用“手写 default artifact”封装方式，构建脚本口径易漂移 | :repository:danmaku | REPO_DANMAKU-F002 | Unify | `buildSrc` 提供统一 prebuilt-aar 约定插件/脚本，减少每个 wrapper 重复配置 | P2 |
| G-F0065 | Redundancy | 模块内自定义类名 `Retrofit` 与 `retrofit2.Retrofit` 同名，阅读/检索时易混淆 | :core_network_component | CORE_NETWORK-F006 | Unify | `:core_network_component` | P2 |
| G-F0066 | PerformanceRisk | `suspend` 解压函数内部执行同步阻塞式解压，且未显式切到 IO 线程；调用方若在主线程调用会卡 UI | :repository:seven_zip | REPO_SEVEN_ZIP-F004 | Unify | `:core_storage_component`（统一通过 `Dispatchers.IO` 执行阻塞解压，并补齐取消/超时策略） | P2 |
| G-F0067 | SecurityRisk | 解压缺少“体积/数量/路径”防护策略，可能被压缩炸弹/超大文件拖垮存储与性能 | :repository:seven_zip | REPO_SEVEN_ZIP-F005 | Unify | `:core_storage_component`（增加限额与校验：最大文件数/最大总解压体积/失败回收；并更严格清洗路径字符） | P2 |
| G-F0068 | Redundancy | Media3/字幕遥测相关类型分散在多个包（data/entity/media3/mapper/repository），边界不清影响检索与复用 | :data_component | DATA-F003 | Unify | `:data_component`（以 `media3/*` 单一命名空间收敛） | P3 |

## 2) 全局 Tasks（治理 Backlog）

| G-ID | 目标 | 关联 G-F | 范围 | 验收标准 | P | 负责人 | 状态 | 依赖 |
|---|---|---|---|---|---|---|---|---|
| G-T0001 | WebDAV 默认启用严格 TLS：替换 `UnsafeOkHttpClient`，并为“特殊场景”提供可控开关（用户显式；Release 允许） | G-F0001 | `core_storage_component/.../WebDavStorage.kt`；`core_network_component/...`（OkHttpClientFactory/安全策略） | 1) release 默认不再使用“信任所有证书”的客户端；2) 若需要放宽校验，必须是用户显式开关（Release 允许）；3) WebDAV 连接/列表/播放用例可回归 | P1 | AI（Codex） | Done | - |
| G-T0002 | 下线/收敛 `UnsafeOkHttpClient`：仅允许在明确场景使用，并提供更安全替代（证书导入/Pin/仅 HTTP） | G-F0001 | `core_network_component/.../OkHttpTlsPolicy.kt`、`core_network_component/.../OkHttpTlsConfigurer.kt`、`core_network_component/.../WebDavOkHttpClientFactory.kt`、`document/architecture/network_tls_policy.md`；调用方：`core_storage_component/.../WebDavStorage.kt`、`player_component/.../VlcProxyServer.kt` | 1) release 默认不再使用“信任所有证书”的客户端；2) WebDAV/代理仍可用（或提供替代配置路径）；3) 文档化风险与使用方式 | P1 | AI（Codex） | Done | - |
| G-T0003 | 为所有 repository wrapper AAR 补齐元信息（来源/版本/License/校验和/更新流程） | G-F0017 | [:repository:danmaku] 新增 `repository/danmaku/README.md`（中文）；可选新增 `repository/danmaku/LICENSE` 或在 README 中明确 License 与引用位置；[:repository:immersion_bar] 新增 `repository/immersion_bar/README.md`（中文）；在 README 中引用/关联 `user_component/src/main/assets/license/ImmersionBar.txt`，并补充 AAR 的 SHA256 与升级步骤；[:repository:panel_switch] 新增 `repository/panel_switch/README.md`（中文）；在 README 中引用/关联 `user_component/src/main/assets/license/PanelSwitchHelper.txt`，并记录 AAR SHA256（当前：`ace2e33a70f393388c041ce428f497ce0379b0367a4279fae2dc9f8d9f671c00`）与升级步骤；[:repository:seven_zip] 新增 `repository/seven_zip/README.md`（中文）；在 README 中引用/关联 `user_component/src/main/assets/license/7-Zip-JBinding.txt`，并记录 AAR SHA256（当前：`def3f43296b21e148b8bcc4bb7cf40c99352488c73ded7e2faa258fbabf17caa`）、支持 ABI、升级步骤；[:repository:thunder] 新增 `repository/thunder/README.md`（中文）；可选新增 `repository/thunder/LICENSE` 或在 README 中明确 License 与引用位置；[:repository:video_cache] 新增 `repository/video_cache/README.md`（中文）；可选新增 `repository/video_cache/LICENSE` 或在 README 中明确 License 与引用位置 | [:repository:danmaku] 1) README 明确：上游项目/下载地址、版本号、License、AAR SHA256、更新步骤；2) 任意人可按文档复现升级；3) 不影响现有依赖解析；[:repository:immersion_bar] 1) README 明确：上游项目/下载地址、版本号、License、AAR SHA256、更新步骤；2) 任意人可按文档复现升级；3) 不影响现有依赖解析；[:repository:panel_switch] 1) README 明确：上游项目/下载地址、版本号、License、AAR SHA256、更新步骤；2) 任意人可按文档复现升级；3) 不影响现有依赖解析；[:repository:seven_zip] 1) README 明确：上游项目/下载地址、版本号、License、AAR SHA256、ABI/最低要求、更新步骤；2) 任意人可按文档复现升级；3) 不影响现有依赖解析；[:repository:thunder] 1) README 明确：上游项目/下载地址、版本号、License、AAR SHA256、更新步骤；2) 任意人可按文档复现升级；3) 不影响现有依赖解析；[:repository:video_cache] 1) README 明确：上游项目/下载地址、版本号、License、AAR SHA256、更新步骤；2) 任意人可按文档复现升级；3) 不影响现有依赖解析 | P1 | AI（Codex） | Done | （见来源任务） |
| G-T0004 | 修复 `ActivityHelper#getTopActivity` 的潜在崩溃：返回 `firstOrNull` 并清理已销毁 Activity | G-F0022 | `core_system_component/src/main/java/com/xyoye/common_component/utils/ActivityHelper.kt` + `ActivityHelper#getTopActivity`；相关调用方（如 `storage_component/.../ScreencastReceiveService.kt`） | 1) `getTopActivity()` 在无可用 Activity 时返回 `null` 不抛异常；2) 保持现有调用方语义（已做 null 判断的逻辑不变）；3) 覆盖投屏接收“前台/后台/无界面”场景回归 | P1 | AI（Codex） | Done | - |
| G-T0005 | 修复 `ISequentialOutStream` 实现：按“流式追加写入”语义输出文件，避免覆盖与性能劣化 | G-F0023 | 重构 `core_storage_component/.../seven_zip/SequentialOutStream.kt`：在同一条流上追加写入（可通过 `FileOutputStream(outFile, true)` 或缓存输出流到对象生命周期）；同时补齐“写入完成/失败”后的关闭策略（必要时配合 `ArchiveExtractCallback` 提供 close 钩子） | 1) 解压后的文件内容完整可用；2) 大文件解压无明显性能退化；3) 失败时能回收临时文件/输出流 | P1 | AI（Codex） | Done | - |
| G-T0006 | 修复资源释放与线程/取消策略：解压过程在 IO 线程执行，且无论成功/失败/取消都能回收 | G-F0025,G-F0066 | 重构 `core_storage_component/.../seven_zip/SevenZipUtils.kt`：使用 `withContext(Dispatchers.IO)` 包裹阻塞解压；对 `RandomAccessFile`、`RandomAccessFileInStream`、`IInArchive` 做 `try/finally` 关闭；在 `suspendCancellableCoroutine` 中 `invokeOnCancellation { ... }` 回收资源并中断 | 1) 压缩包不存在/解压失败/协程取消时不泄漏文件句柄；2) 主线程调用不会发生长时间卡顿（至少能明确切线程）；3) 失败时返回值与异常口径一致并有日志 | P1 | AI（Codex） | Done | - |
| G-T0007 | 去硬编码：将 `APP_KEY/APP_SEC` 改为“构建期注入/本地配置”，并提供可控回退策略（避免与发行物强绑定） | G-F0002 | `core_system_component`（新增/扩展 BuildConfig 注入与读取入口）；`bilibili_component`（`BilibiliTvClient` 改造为配置读取 + 校验） | 1) 仓库源码中不再出现明文 `APP_SEC`；2) 未配置时给出明确错误与降级策略（例如禁用 TV 登录/提示）；3) 配置到位时登录/播放签名链路可回归 | P1 | AI（Codex） | Done | - |
| G-T0008 | 将 VLC 代理拉流从 `UnsafeOkHttpClient` 迁移到“默认安全”的 OkHttpClient，并提供可控降级策略 | G-F0004 | 在 `:core_network_component` 提供可复用的 OkHttpClientFactory（若已有则复用）；`VlcProxyServer` 使用安全 client；如需信任自签证书，必须受用户显式配置控制（Release 允许） | 1) release 默认不再使用 trust-all client；2) VLC 播放带 headers 的直链可回归；3) 若开启降级策略，入口明确且可审计；4) 全仓编译通过 | P1 | AI（Codex） | Done | - |
| G-T0009 | 将“TV 禁用后台/画中画”的策略显式化并避免误伤移动端 | G-F0013 | 调整 `Media3BackgroundCoordinator#sync`：仅 TV 下强制清空；非 TV 按 `PlayerCapabilityContract` 同步能力；必要时引入配置开关（例如 `AppConfig`/`BuildConfig`） | 1) TV 模式下 capability 同步后 `sessionCommands/backgroundModes` 为空；2) 非 TV 模式下按契约同步（可用单测/仪表测试覆盖）；3) 不引入跨 feature 直接依赖；4) 行为变更有文档说明 | P1 | AI（Codex） | Done | - |
| G-T0010 | 将迅雷 SDK 初始化策略改为按需 + 可降级，降低启动期开销与崩溃面 | G-F0024 | 在 `ThunderManager` 内实现“只初始化一次”的幂等保护与按需初始化，并移除进程启动期的 `storage_component/.../ThunderInitializer.kt` | 1) 无迅雷 SDK 支持的设备不崩溃；2) 支持设备上磁力/BT 功能首次使用可正常工作；3) 若初始化失败可明确降级（提示/禁用入口），不影响其它功能 | P1 | AI（Codex） | Done | - |
| G-T0011 | 将遥测聚合/事件构建逻辑迁出 Base：`data_component` 仅保留数据类型与契约 payload | G-F0009 | 迁移 `data_component/.../SubtitleTelemetryRepository.kt`、`data_component/.../TelemetryEventMapper.kt`、`data_component/.../Loading.kt` 到合适模块（建议 `:core_log_component` / `:core_ui_component`），并更新 `player_component` 等调用方 | 1) `:data_component` 不再包含“Repository/Mapper/可变并发状态”类；2) 调用方功能等价（字幕/Media3 遥测仍可用）；3) `./gradlew verifyModuleDependencies` 通过；4) 相关单测（若迁移）在新模块可运行 | P1 | AI（Codex） | Done | - |
| G-T0012 | 建立统一脱敏工具与默认策略，并替换关键链路调用点（网络/播放器/缓存/下载） | G-F0005 | [:core_network_component] `core_network_component/src/main/java/com/xyoye/common_component/network/request/Request.kt`；`core_network_component/src/main/java/com/xyoye/common_component/utils/JsonHelper.kt`；`core_network_component/src/main/java/com/xyoye/common_component/network/helper/LoggerInterceptor.kt`；（必要时）`core_log_component`；[:core_log_component] `core_log_component/src/main/java/com/xyoye/common_component/log/LogFormatter.kt`；`core_log_component/src/main/java/com/xyoye/common_component/log/tcp/*`；`core_log_component/src/main/java/com/xyoye/common_component/log/LogSystem.kt`；[:player_component] 增加 URL 脱敏工具（建议落 `:core_log_component` 或 `:core_system_component`）；改造 `Media3Diagnostics#logHttpOpen` 与 `LoggingHttpDataSource` 调用链，确保 log/emit 均使用脱敏后的 URL；[:repository:thunder] 修改 `ThunderManager/TorrentStorage/PlayTaskManager` 的错误上下文字符串：磁力链 → 仅 hash；URL → 去 query；路径 → 仅文件名或 hash；必要时在 `:core_log_component` 提供统一脱敏工具供复用；[:repository:video_cache] `player_component/src/main/java/com/xyoye/cache/OkHttpUrlSource.kt`（替换日志内容）；若已在 `:core_log_component` 提供 URL 脱敏工具则直接复用，否则先补齐该工具并统一替换调用点 | [:core_network_component] 1) `Request` 上报不再包含完整 `$requestParams/$requestJson`（至少按 key 脱敏）；2) `LoggerInterceptor#sanitizeHeader` 覆盖 `X-AppId/X-AppSecret`；3) 抽查关键链路不泄露 token/cookie/secret；[:core_log_component] 1) 常见敏感 key（token/cookie/authorization 等）自动脱敏；2) TCP server 仅在调试会话启用且策略允许时输出；3) 补齐单测/文档说明；[:player_component] 1) 日志与遥测不包含 query/fragment；2) 仍可定位问题（保留 host/path + 可选 hash）；3) 可通过开关控制详细程度；4) 全仓编译通过；[:repository:thunder] 1) 上报/日志中不出现原始 magnet/URL query/token/完整本地路径；2) 仍可定位问题（保留 hash/host/path）；3) 全仓日志策略一致；[:repository:video_cache] 1) 相关日志不包含 query/fragment/token；2) 仍可定位问题（保留 host/path + 可选 hash）；3) 与 `PLAYER-T001` 使用同一套脱敏策略；4) 全仓编译通过 | P1 | AI（Codex） | Done | （见来源任务） |
| G-T0013 | 建立错误上报脱敏规则并改造登录/用户资料相关上报，避免泄露个人信息 | G-F0007 | 在 `:core_log_component` 增加脱敏工具（例如 mask/hash），并改造 `LoginViewModel`/`UserInfoViewModel` 的 context 字符串：不再输出明文账号/昵称/用户输入 | 1) 上报上下文不包含明文账号/昵称/新昵称；2) 仍保留可定位信息（例如请求类型/错误码/匿名 ID）；3) 行为不变（toast/loading）；4) 全仓编译通过 | P1 | AI（Codex） | Done | - |
| G-T0014 | 开启 Room schema 导出并建立迁移校验门禁（至少覆盖主库版本演进） | G-F0010 | `DatabaseInfo`（`exportSchema=true`）；Gradle Room schemaLocation 配置；新增 schema 输出目录（例如 `core_database_component/schemas/`）与迁移测试/校验脚本 | 1) schema 文件随版本更新可追踪（可 review）；2) 至少提供“迁移可跑通”的验证方式（MigrationTest 或脚本）；3) CI/门禁可执行且不显著拖慢开发编译 | P1 | AI（Codex） | Done | - |
| G-T0015 | 抽取 TV Tab 焦点协调与输入策略为可复用组件，统一 DPAD 行为 | G-F0018 | 基于 `TabLayoutViewPager2DpadFocusCoordinator`：在 `:core_ui_component` 增加 helper（例如 `TabLayoutViewPager2DpadFocusCoordinator.attachIfTelevision(...)` / `applyDpadEditTextPolicy(...)` / `bindDpadDownToTabFocus(...)`），并在 Home/Search/Detail 等页面复用 | 1) 仅 TV UI mode 生效（`isTelevisionUiMode()` 分流）；2) DPAD 导航路径无死角，默认焦点明确；3) BACK/MENU 语义与页面一致；4) 相关页面回归通过 | P1 | AI（Codex） | Done | - |
| G-T0016 | 收敛 Bugly 上报门面：统一由 `BuglyReporter` 触达 `CrashReport`，并规范 `ErrorReportHelper` 职责与注释 | G-F0019 | `core_log_component/src/main/java/com/xyoye/common_component/log/BuglyReporter.kt`；`core_log_component/src/main/java/com/xyoye/common_component/utils/ErrorReportHelper.kt` + 调用方（全仓） | 1) `CrashReport.*` 访问集中（或有明确分层）；2) 删除/替换不存在文档引用；3) `extraInfo` 能进入可追踪上下文（userData 或封装异常 message） | P1 | AI（Codex） | Done | - |
| G-T0017 | 收敛可写 `LiveData` 暴露：对外只读 + 统一发送入口 | G-F0015 | `core_contract_component/src/main/java/com/xyoye/common_component/bridge/LoginObserver.kt`；`core_contract_component/src/main/java/com/xyoye/common_component/bridge/PlayTaskBridge.kt` + 调用方 | 1) `LoginObserver` 返回 `LiveData<LoginData>`；2) `PlayTaskBridge` 对外暴露 `LiveData<Long>`（或只读 Flow）且外部无法直接 `postValue`；3) 全仓编译通过 | P1 | AI（Codex） | Done | - |
| G-T0018 | 收敛异常处理口径：移除 `printStackTrace()`，统一使用 `ErrorReportHelper` + `LogFacade` 记录必要信息 | G-F0020 | `core_system_component/src/main/java/com/xyoye/common_component/**`（见 Finding 证据点） | 1) `core_system_component` 内不再出现 `printStackTrace()`；2) 保留必要的异常上报与可定位信息（路径+符号+上下文）；3) 不引入新的敏感信息日志输出 | P1 | AI（Codex） | Done | - |
| G-T0019 | 明确 Fragment 宿主契约：引入 `LoadingHost`（或等价接口）并替换强制 cast，避免隐藏崩溃点 | G-F0011 | `BaseAppFragment` + 所有继承链（`BaseFragment/BasePreferenceFragmentCompat` 等）及其宿主 Activity | 1) 不再出现 `context as BaseAppCompatActivity<*>` 的强制转换；2) 宿主不满足契约时能明确报错（Fail Fast）；3) 全仓编译通过，页面 loading 行为不变 | P1 | AI（Codex） | Done | - |
| G-T0020 | 移除投屏 Sender 整条链路：删除旧实现与全端 stub，清理入口/路由/服务暴露 | G-F0021 | 删除 `storage_component/.../ScreencastProvideService.kt` 与 `storage_component/.../ScreencastProvideServiceImpl.kt`（以及相关入口/路由/依赖暴露点），并清理注释旧实现；如未来需要 Sender，另行立项恢复 | 1) 全仓无 Sender 入口与服务暴露（不再存在“全端 stub + 对外仍暴露接口”形态）；2) 不保留大段注释旧实现；3) 投屏接收端（Receiver）链路不受影响；4) 全仓编译通过 | P1 | AI（Codex） | Done | - |
| G-T0021 | 清理 `printStackTrace()` 并补齐异常上报上下文（引擎/会话/源类型） | G-F0016 | 替换 `PlayerFoundationInitializer/PlayRecorder/VlcVideoPlayer/ExternalSubtitleManager/...` 的 `printStackTrace` 为统一上报；对可能包含 URL/header 的内容做脱敏 | 1) player_component 内无 `printStackTrace()`；2) 异常上报有足够上下文但不泄露敏感信息；3) 关键路径行为不变；4) 全仓编译通过 | P1 | AI（Codex） | Done | - |
| G-T0022 | 用安全方案替换 `EntropyUtils` 的对称加密（至少支持随机 IV/带认证），并为投屏 UDP 消息引入版本/兼容策略 | G-F0003 | `core_system_component/src/main/java/com/xyoye/common_component/utils/EntropyUtils.kt`（新增/替换实现）；`storage_component/src/main/java/com/xyoye/storage_component/utils/screencast/receiver/UdpServer.kt`；`storage_component/src/main/java/com/xyoye/storage_component/utils/screencast/receiver/HttpServer.kt`；`core_storage_component/src/main/java/com/xyoye/common_component/network/repository/ScreencastRepository.kt` | 1) 不再使用固定 `IvParameterSpec(ByteArray(16))` 与默认 Key；2) UDP 加密载荷包含必要的 IV/版本信息；3) 新旧版本至少具备可控的兼容/降级策略；4) 投屏收发链路可回归验证 | P1 | AI（Codex） | Done | - |
| G-T0023 | 移除 `printStackTrace()` 并统一异常上报上下文与脱敏策略（URL/token/password/路径） | G-F0006 | 在 `:core_log_component` 增加 `safeReport(...)`/`LogContext`（若已有则复用）；本模块替换 `e.printStackTrace()` 与分散的 try/catch 上报逻辑 | 1) `storage_component` 内无 `printStackTrace()`；2) 上报上下文满足脱敏策略；3) 关键失败仍可定位（模块/动作/关键 ID） | P1 | AI（Codex） | Done | - |
| G-T0024 | 统一 TLS 安全默认：移除默认路径中的 `hostnameVerifier { _, _ -> true }`，并为“特殊场景”提供可控开关（用户显式；Release 允许） | G-F0001 | `core_network_component/src/main/java/com/xyoye/common_component/network/RetrofitManager.kt`；`bilibili_component/src/main/java/com/xyoye/common_component/bilibili/net/BilibiliOkHttpClientFactory.kt`；`core_storage_component/src/main/java/com/xyoye/common_component/storage/file/helper/LocalProxy.kt`；`core_storage_component/src/main/java/com/xyoye/common_component/storage/file/helper/HttpPlayServer.kt`；相关 OkHttp 构建点 | 1) 默认 OkHttpClient 使用系统 TLS 校验；2) 若需要“放宽校验”，必须是用户显式开关（Release 允许）；3) 全仓编译通过且核心网络功能可回归 | P1 | AI（Codex） | Done | - |
| G-T0025 | 统一手动迁移异常可观测性：替换 `printStackTrace()` 为结构化日志/异常上报，并补充必要上下文 | G-F0014 | `core_database_component/src/main/java/com/xyoye/common_component/database/migration/ManualMigrationInitializer.kt`；（必要时）`core_log_component` 上报接口；`ManualMigration#migrate_6_7` 关键路径 | 1) 迁移失败会记录到日志/上报（含模块与迁移版本信息）；2) 不记录敏感字段明文（账号/密码等）；3) 启动链路稳定（不引入 ANR/主线程阻塞） | P1 | AI（Codex） | Done | - |
| G-T0026 | 统一错误上报上下文的构建与脱敏策略，减少重复样板并降低隐私风险 | G-F0008 | 在 `:core_log_component`（或 `:core_ui_component`）提供 `safeReport(...)`/`LogContext` 等封装；逐步替换本模块直接拼接 URL/文件路径的上报 | 1) 上报上下文不包含完整 URL/绝对路径/完整存储地址（按策略脱敏）；2) 调用点明显收敛（减少重复 try/catch + post）；3) 关键定位信息仍可用（模块/动作/关键 ID）；4) 全仓编译通过 | P1 | AI（Codex） | Done | - |
| G-T0027 | 为 Cookie/Token 引入加密存储：落地统一的密钥管理与数据迁移，避免明文落盘 | G-F0028 | `core_system_component`（抽取通用 `EncryptedStore`/`Crypto`，可参考 `DeveloperCredentialStore`）；`bilibili_component`（替换 `MMKV.mmkvWithID` 明文存储、迁移旧数据） | 1) Cookie/Token 落盘为密文（不可直接 grep 出明文）；2) 旧数据可迁移且不强制用户重新登录（可接受可控场景下的回退）；3) 登录/播放/心跳/直播弹幕等关键流程不回退 | P2 | AI（Codex） | Done | - |
| G-T0028 | 为媒体库远程凭据建立统一的安全存储策略，避免 DB 明文落盘 | G-F0029 | 围绕 `MediaLibraryEntity` 的 `password/remoteSecret`：设计迁移方案（DB 字段废弃/置空 + 安全存储 keyed by libraryId），并在存储实现层收敛读写入口 | 1) 不再在 Room 中持久化明文凭据（或对字段加密后再落盘）；2) 有可回滚/可升级的数据迁移策略；3) 全端登录/挂载流程回归通过；4) 明确禁止日志输出完整凭据对象（必要时提供 redaction helper） | P2 | AI（Codex） | Done | 凭据迁移到加密存储（MMKV+Keystore），DB 字段置空 |
| G-T0029 | 以显式结果/能力开关替代 “禁用功能即 throw”，降低误调用崩溃风险 | G-F0040 | `Media3CastManager#prepareCastSession`：改为返回 `CastSessionPrepareResult`（`Ready/Disabled/UnsupportedTarget`），并通过 `castSenderEnabled` 控制能力开关；底层不再直接抛异常 | 1) 任何 UI/业务入口都不会因误调用导致崩溃；2) TV 模式下保持“投屏发送不可用”的可理解反馈（`Disabled.message`）；3) 新增 JVM 单测 `Media3CastManagerTest` 覆盖关键分支 | P2 | AI（Codex） | Done | - |
| G-T0030 | 定位并修复 DiffUtil 异常根因：约束数据模型或改造 diff 机制，降低回退刷新与上报噪音 | G-F0036 | `BaseAdapter`/`AdapterDiffCreator`/`AdapterDiffCallBack`；`core_ui_component/src/test/java/com/xyoye/common_component/adapter/AdapterDiffCreatorTest.kt`；`core_ui_component/src/test/java/com/xyoye/common_component/adapter/AdapterDiffCallBackSafetyTest.kt` | 1) `BaseAdapter#setData` 不再依赖 try/catch“吞异常”；2) 列表更新动画与焦点恢复稳定；3) Diff 失败率显著下降（可通过线上/日志统计或回归验证） | P2 | AI（Codex） | Done | 2026-02-06：移除 `BaseAdapter#setData` 的异常回退；为 `AdapterDiffCreator` 增加“跨类型短路 + 比较器异常兜底”；补齐回归单测。 |
| G-T0031 | 对“加密失败保存明文”制定策略：默认安全优先（禁用/提示/二次确认），并提供可追踪的迁移/清理机制 | G-F0026 | `core_system_component/src/main/java/com/xyoye/common_component/config/DeveloperCredentialStore.kt`；相关设置项 UI（`user_component`） | 1) 明确明文兜底的启用条件（例如仅 debug / 显式开关）；2) 存量明文可被识别并提示迁移；3) 不在日志/上报中泄露凭证 | P2 | AI（Codex） | Done | 2026-02-06：默认禁用明文兜底；新增仅 Debug 显式开关+二次确认；新增历史明文迁移/清理入口。 |
| G-T0032 | 封装/隔离迅雷 SDK 类型，避免第三方类型在上层模块显式出现 | G-F0046 | `:core_storage_component` 内部新增自定义 model；将 `TorrentBean/TorrentStorageFile` 迁移为 `internal` 或改为仅暴露 `StorageFile` 抽象；封装 `TorrentInfo/TorrentFileInfo/XLTaskInfo` 的读取逻辑到 `utils/thunder` | 1) 上层模块无需 `import com.xunlei.*` 也无需依赖其类型；2) 相关能力仍可完成：列种子文件/生成播放 URL/任务状态查询；3) `./gradlew :core_storage_component:assembleDebug` 通过 | P2 | AI（Codex） | Done | 2026-02-06：新增 `ThunderTorrentInfo/ThunderTorrentFileInfo/ThunderTaskInfo` 内部模型；`TorrentBean/TorrentStorageFile` 改为 internal；`PlayTaskManager` 改为通过 `ThunderManager.readTaskInfo` 读取状态，第三方类型收敛到 `utils/thunder`。 |
| G-T0033 | 封装状态栏/沉浸式能力：上层不再直接 import `ImmersionBar`，并尽量减少 `api(...)` 依赖透传 | G-F0035 | `core_ui_component`（新增 `StatusBarStyle`/扩展 API、调整 `build.gradle.kts`）；迁移 `anime_component/player_component/storage_component/app` 等直接引用 `ImmersionBar` 的页面 | 1) 上层模块不再直接引用 `com.gyf.immersionbar.ImmersionBar`；2) `:core_ui_component` 对外暴露的 API 清晰且可替换；3) 全仓编译通过，关键页面状态栏样式在 TV/移动端一致 | P2 | AI（Codex） | Done | - |
| G-T0034 | 将 B 站弹幕解析/下载编排从 ViewModel 下沉到 repository/usecase，降低 UI 耦合 | G-F0037 | 抽取 `BilibiliDanmuUseCase`（解析 URL→CID 列表、下载 XML、保存）到 `:bilibili_component` 或 `:core_storage_component`；ViewModel 仅负责 UI 状态与消息流 | 1) ViewModel 不包含 Jsoup.connect/正则抽取 JSON 等解析细节；2) BV/AV/番剧 URL 三条链路功能回归；3) 异常上报上下文符合脱敏策略；4) 全仓编译通过 | P2 | 待分配（Bilibili/Feature） | Draft | - |
| G-T0035 | 将 `Retrofit` 单例改为可注入 Provider/Factory，提升可测试性并降低全局静态耦合 | G-F0039 | `core_network_component/src/main/java/com/xyoye/common_component/network/RetrofitManager.kt` + 调用方 | 1) 调用方可通过注入/服务定位获取网络 Service；2) 支持在测试中替换实现；3) 不引入依赖环且 `verifyModuleDependencies` 通过 | P2 | AI（Codex） | Done | 新增 `RetrofitServiceProvider`/`RetrofitServiceLocator`，`RetrofitManager` 支持 Provider 替换与重置，并补充替换注入单测 |
| G-T0036 | 将字幕匹配/搜索/Hash 能力从 `:local_component` 收敛到基础层，避免 feature 锁死复用 | G-F0056 | 将 `local_component/src/main/java/com/xyoye/common_component/utils/subtitle/*` 迁移到 `:core_storage_component`（或明确的 core 模块）并整理包结构；`BindSubtitleSourceFragmentViewModel` 只依赖 core 层 API | 1) `:local_component` 不再包含 `com.xyoye.common_component.utils.subtitle` 的实现文件；2) 本地视频字幕“匹配/搜索/下载/解压/绑定”功能回归正常；3) 不引入 feature ↔ feature 依赖；4) 全仓编译通过 | P2 | 待分配（Storage/Feature） | Draft | - |
| G-T0037 | 将扫描扩展目录/过滤配置与刷新逻辑下沉到 repository/usecase，统一 DAO 访问口径 | G-F0042 | 在 `:core_database_component` 提供 `ScanSettingsRepository`（ExtendFolder/FolderFilter CRUD）；在 `:core_storage_component` 提供 `VideoScanRefreshUseCase`（触发刷新）；ViewModel 仅编排 UI 状态 | 1) ViewModel 不直接访问 `DatabaseManager.instance.get*Dao()`；2) 扩展目录增删、过滤开关与刷新行为一致；3) 线程调度一致；4) 全仓编译通过 | P2 | 待分配（DB/Storage/User） | Draft | - |
| G-T0038 | 将搜索历史/播放历史等持久化细节从 ViewModel 迁移到 repository/usecase 层 | G-F0033 | 建立 `AnimeSearchHistoryRepository` / `MagnetSearchHistoryRepository` / `EpisodeHistoryRepository`（落点建议 `:core_database_component`），ViewModel 仅调用抽象 API | 1) ViewModel 不再直接调用 `DatabaseManager.instance.get*Dao()`；2) repository 的返回类型与现有 LiveData/Flow 对齐；3) 线程调度统一（IO/Main）；4) 功能回归：搜索历史新增/删除/查询、剧集历史补全正常 | P2 | 待分配（Feature/DB） | Draft | - |
| G-T0039 | 引入“数据库访问契约/Provider”，逐步替换跨模块对 `DatabaseManager.instance` 的直接引用 | G-F0041 | 新增 `:core_contract_component` 契约（例如 `DatabaseProvider`/`DaoProvider`）；`core_database_component` 提供实现；迁移 `local_component/storage_component/bilibili_component/...` 的调用点 | 1) 新契约不引入依赖环且 `./gradlew verifyModuleDependencies` 通过；2) 核心调用点可通过 Provider 获取 DAO（支持测试替换/注入）；3) 全仓编译通过 | P2 | 待分配（Infra/DB） | Draft | - |
| G-T0040 | 打通“写入层磁盘错误→全局状态”链路，确保 `LogSystem`/持久化/Writer 三者一致 | G-F0045 | `core_log_component/src/main/java/com/xyoye/common_component/log/LogWriter.kt`；`core_log_component/src/main/java/com/xyoye/common_component/log/LogSystem.kt`；`core_log_component/src/androidTest/java/com/xyoye/common_component/log/LogDiskErrorInstrumentedTest.kt`；`core_log_component/src/test/java/com/xyoye/common_component/log/LogSystemDiskErrorPropagationTest.kt` | 1) 写入失败后 `LogSystem.getRuntimeState().debugToggleState==DISABLED_DUE_TO_ERROR`；2) 配置表持久化更新；3) 既有 instrumented test 与新增用例通过 | P2 | AI（Codex） | Done | 2026-02-06：补齐 `LogWriter -> LogSystem` 回传与 `LogSystemDiskErrorPropagationTest`，验证运行时状态与持久化一致。 |
| G-T0041 | 抽取 PlayHistory/MediaLibrary 的 repository/usecase，收敛 DAO 访问与写入口径 | G-F0034 | 在 `:core_database_component` 增加 `PlayHistoryRepository`（历史查询/删除/清空/更新绑定字段）与 `MediaLibraryRepository`；`MediaViewModel`/`PlayHistoryViewModel`/Bind*ViewModel 使用仓库接口 | 1) 目标 ViewModel 不再直接调用 `DatabaseManager.instance.get*Dao()`；2) 行为一致：历史列表/打开历史播放/解绑字幕弹幕；3) 线程调度一致（IO/Main）；4) 全仓编译通过 | P2 | 待分配（DB/Feature） | Draft | - |
| G-T0042 | 抽取 `LogLevel` 优先级/比较逻辑为单一实现，移除重复 `levelPriority` | G-F0051 | `core_log_component/src/main/java/com/xyoye/common_component/log/LogWriter.kt`；`core_log_component/src/main/java/com/xyoye/common_component/log/LogSampler.kt`；`core_log_component/src/main/java/com/xyoye/common_component/log/model/LogLevel.kt` | 1) 全仓仅保留一个优先级实现；2) 单测覆盖 DEBUG/INFO/WARN/ERROR 映射；3) 现有策略筛选行为不变 | P2 | AI（Codex） | Draft | - |
| G-T0043 | 抽取可复用的 PreferenceDataStore/映射抽象，减少 key→config 样板与 drift | G-F0063 | 在 `:core_ui_component` 提供通用 `MappingPreferenceDataStore`（集中 try/catch + 上报）与 key 常量组织方式；迁移 `App/Player/Subtitle/Danmu/Developer` 设置页逐步接入 | 1) 目标 Fragment 内不再手写大量 `when(key)`；2) 旧 key 保持兼容（不丢配置）；3) 关键设置读写行为一致；4) 全仓编译通过 | P2 | 待分配（UI） | Draft | - |
| G-T0044 | 抽取壳层 Fragment 装载/切换逻辑，减少重复实现与差异漂移 | G-F0050 | 抽取 `FragmentSwitcher`/delegate（建议落在 `:core_ui_component` 或 `:app` 内部包），统一 `findFragmentByTag/add/show/hide` 与状态处理；TV 专有的导航数据结构保留在 `TvMainActivity` | 1) `MainActivity` 与 `TvMainActivity` 使用同一套切换实现；2) 旋转/进程重建后不出现错乱（当前已 `findAndRemoveFragment` 规避）；3) TV DPAD 焦点可达、可见、可返回；4) 不改变现有路由路径与功能入口 | P2 | 待分配（App/UI） | Draft | - |
| G-T0045 | 抽取投屏协议与 server 公共能力到 core 层，减少 NanoHTTPD/UDP 重复与策略漂移 | G-F0053 | 将投屏协议（常量/鉴权/header/range/错误响应）统一到 `:core_storage_component`；`storage_component` 的 UDP/HTTP server 仅保留编排或改为调用 core 的实现 | 1) core 层定义稳定 API；2) feature 不再重复实现同一协议细节；3) 投屏接收链路可回归（发现→鉴权→播放→进度回写）；4) 全仓编译通过 | P2 | 待分配（Storage/Infra） | Draft | - |
| G-T0046 | 抽取统一 OkHttpClientFactory：集中维护 timeout/拦截器链/安全策略，减少跨模块漂移 | G-F0049 | 新增 `core_network_component` 工厂类；迁移 `RetrofitManager.kt`、`WebDavOkHttpClientFactory.kt`、`ProxyOkHttpClientFactory.kt` 与 `BilibiliOkHttpClientFactory.kt` | 1) 至少收敛公共参数（timeout、解压、动态域名、日志）；2) 允许按域扩展（CookieJar/Headers/签名）；3) 全仓编译通过 | P2 | AI（Codex） | Done | - |
| G-T0047 | 抽取统一的“Result 失败处理 + 上下文上报 + toast”助手，减少 ViewModel 样板与口径漂移 | G-F0058 | 以 `ErrorReportHelper.postCatchedExceptionWithContext(...)` 为核心：在 `:core_log_component` 或 `:core_ui_component` 提供可复用封装（含脱敏策略），并逐步替换 `anime_component` 内重复代码 | 1) anime_component 内失败分支代码显著收敛（同类逻辑不再复制粘贴）；2) 上报字段包含必要上下文且不输出敏感信息；3) 行为不变（toast/loading 时机一致）；4) 全仓编译通过 | P2 | AI（Codex） | Done | 新增 `Result.reportAndToastOnFailure` 并迁移 anime_component |
| G-T0048 | 拆分 `BilibiliRepository`：按子域抽取组件并引入单测/契约化接口，提高可维护性 | G-F0038 | `bilibili_component`（新增 `AuthRepository/RiskRepository/PlaybackRepository/HistoryRepository/LiveRepository` 等）；对外保留 facade 以降低调用方改动 | 1) 对外 API 基本不变（或提供 deprecate 迁移期）；2) 关键签名/存储/URL 脱敏具备单测覆盖；3) 全仓编译通过 | P2 | 待分配（Bilibili） | Draft | - |
| G-T0049 | 收敛 MediaLibrary/PlayHistory 的写入口径：提供 repository/usecase 并替换 feature 直连 DAO | G-F0031 | `:core_database_component` 新增 `MediaLibraryRepository/PlayHistoryRepository`；迁移 `StoragePlusViewModel/StorageFileFragmentViewModel/ServerController` 等调用点 | 1) 目标类不再直接调用 `DatabaseManager.instance.get*Dao()`；2) 行为一致（存储增删改/续播/绑定字段更新）；3) 线程与事务边界一致；4) 全仓编译通过 | P2 | 待分配（DB/Feature） | Draft | - |
| G-T0050 | 收敛 contract 层的运行时实现：将可变状态/副作用迁移到 runtime 层，仅保留契约 | G-F0030 | `core_contract_component/src/main/java/com/xyoye/common_component/media3/Media3SessionStore.kt`；`core_contract_component/src/main/java/com/xyoye/common_component/config/PlayerActions.kt`；`core_contract_component/src/main/java/com/xyoye/common_component/bridge/*` + 调用方 | 1) contract 层只保留接口/数据类型；2) 迁移后调用方仍可访问同等能力；3) 不引入 feature↔feature 依赖且 `./gradlew verifyModuleDependencies` 通过 | P2 | AI（Codex） | Draft | - |
| G-T0051 | 收敛本地代理/HTTP server 能力，减少多实现与策略漂移 | G-F0054 | 抽取 “proxy + headers + range + 鉴权 + 错误口径” 到 `:core_storage_component`（或新 core 模块）；player/storage/screencast 复用统一实现 | 1) 同类 server 代码显著减少；2) 多引擎（VLC/mpv/Media3）可播放且 Range/headers 行为一致；3) 默认 TLS 与日志策略统一；4) 全仓编译通过 | P2 | 待分配（Infra/Player/Storage） | Draft | - |
| G-T0052 | 收敛第三方协议库依赖泄漏：减少/消除 `api(...)`，上层模块不再直接 import 协议库类型 | G-F0032 | `core_storage_component/build.gradle.kts`（调整 `api`→`implementation` 或仅保留“确实出现在公共 API 的类型”）；迁移 `storage_component/...` 等直接引用协议库的代码 | 1) 业务模块不再直接依赖 `SMBClient/FTPClient/NanoHTTPD/...` 等第三方类型；2) 依赖声明清晰（需要时显式依赖）；3) 全仓编译通过且关键存储配置/测试连接可用 | P2 | 待分配（Infra/Storage） | Draft | - |
| G-T0053 | 收敛第三方类型扩散：以 `core_ui_component` 提供统一状态栏/沉浸式配置入口 | G-F0047 | 在 `:core_ui_component` 增加统一门面（例如 `StatusBarStyleApplier` / `ImmersionBarFacade`），并将 feature 侧直接 `ImmersionBar.with(...)` 的调用逐步迁移；迁移完成后评估是否可将 `api(project(":repository:immersion_bar"))` 降级为 `implementation(...)` | 1) 新增入口可覆盖现有常见用法（透明/fitSystemWindows/字体颜色等）；2) 至少迁移 2 个调用点验证可行（例如 `BaseActivity` 与 `BaseSplashActivity`）；3) 不影响 TV/移动端交互与样式；4) 迁移过程保持渐进（允许双栈共存一段时间） | P2 | 待分配（UI） | Draft | - |
| G-T0054 | 移除“发送弹幕”整条链路：删除相关 UI/代码并移除 `:repository:panel_switch` 依赖 | G-F0062 | 删除 `player_component/.../SendDanmuDialog.kt` 与 `player_component/src/main/res/layout/layout_send_danmu.xml`；移除 `player_component` 对 `:repository:panel_switch` 的依赖，并清理入口/引用点 | 1) 依赖与调用点一致（不再存在“依赖存在但入口不可达”）；2) `rg "com\\.effective\\.android\\.panel" -n` 无命中；3) 全仓编译通过 | P2 | AI（Codex） | Done | 已移除入口/实现/资源并清理依赖 |
| G-T0055 | 明确登录态单一事实源：用 UserSessionManager 统一 token/登录态更新与观察 | G-F0044 | 在 `:core_system_component` 增加 `UserSessionManager`（包装 `UserConfig`/token 更新与状态流），通过 `:core_contract_component` 暴露只读接口；`user_component` 仅负责 UI 与 `UserSessionServiceImpl` 触发刷新 | 1) `PersonalFragment` 等页面通过统一入口观察登录态（避免多源）；2) 刷新 token 与登录态恢复路径清晰（Service→Manager）；3) 不引入 feature ↔ feature 依赖；4) 全仓编译通过 | P2 | 待分配（System/User） | Draft | - |
| G-T0056 | 统一 MD5/hex 工具：复用 `CacheKeyMapper`（或抽取 `HashUtils`），移除 `ManualMigration#md5Hex` 重复实现 | G-F0052 | `core_database_component/src/main/java/com/xyoye/common_component/database/migration/ManualMigration.kt`；`core_system_component/src/main/java/com/xyoye/common_component/utils/CacheKeyMapper.kt` | 1) `unique_key` 的计算结果与历史一致（或提供兼容处理）；2) 编译通过；3) 复用工具在全仓可被统一引用 | P2 | AI（Codex） | Done | 抽取 `HashUtils`，迁移 `ManualMigration` 与 `CacheKeyMapper` 复用 |
| G-T0057 | 统一 MMKV 初始化策略：明确“Startup 初始化”与“BaseApplication 初始化”的职责边界，避免重复与时序不一致 | G-F0057 | `core_system_component/src/main/java/com/xyoye/common_component/base/app/BaseInitializer.kt`；`core_system_component/src/main/java/com/xyoye/common_component/base/app/BaseApplication.kt`；`app/src/main/java/com/okamihoro/ddplaytv/app/IApplication.kt` | 1) 明确唯一初始化入口（或保证幂等且只记录一次）；2) 不破坏 MultiDex/Startup 时序；3) 全仓编译通过 | P2 | 待分配（Runtime） | Draft | - |
| G-T0058 | 统一 Service 包结构（service/services）：提升 discoverability 并减少迁移成本 | G-F0060 | 移动 `core_contract_component/src/main/java/com/xyoye/common_component/service/*` 或 `services/*`，统一到单一包名并批量改引用 | 1) 仅保留一个包名（建议 `...services`）；2) ARouter path 常量保持不变（`RouteTable`）；3) 相关模块编译通过 | P2 | AI（Codex） | Done | - |
| G-T0059 | 统一 Telemetry repository 的模块归属与包命名，避免“core_network 包名却在 feature 模块” | G-F0012 | 方案 A：移动 `Media3TelemetryRepository` 到 `:core_network_component`；方案 B：保留在 player，但改包名为 `com.xyoye.player_component.*` 并通过接口在 core 层暴露；必要时提供 `@Deprecated` 过渡层 | 1) 包与模块语义一致；2) 外部引用不受影响或有清晰迁移路径；3) 不引入依赖环；4) 全仓编译通过 | P2 | AI（Codex） | Done | 2026-02-06：迁移到 `com.xyoye.player_component.media3.telemetry`，并提供 `typealias + @Deprecated` 兼容层。 |
| G-T0060 | 统一 prebuilt AAR wrapper 的 Gradle 封装方式，减少脚本重复与漂移 | G-F0064 | 在 `buildSrc` 提供约定插件（`setup.prebuilt-aar`），并迁移 `repository/*` wrapper 的 `build.gradle.kts` 使用统一写法（覆盖 6 个 AAR wrapper） | 1) wrapper 模块仍可被正常依赖（`assembleDebug` 可通过）；2) wrapper 脚本结构一致、可读；3) 不引入额外 module 依赖；4) `./gradlew verifyModuleDependencies` 通过 | P2 | AI（Codex） | Done | 已新增 buildSrc 约定插件并迁移 6 个 wrapper |
| G-T0061 | 统一异常处理与脱敏：移除 `printStackTrace()`，统一到 `LogFacade`/`ErrorReportHelper` 并建立敏感字段脱敏规则 | G-F0043 | `core_storage_component` 全模块（storage impl + utils）；必要时抽取 `StorageErrorReporter` | 1) `core_storage_component` 内不再出现 `printStackTrace()`；2) 错误日志包含足够上下文（模块/协议/关键参数 hash），但不包含敏感字段明文；3) 关键路径行为不变（失败仍返回 null/empty 等约定） | P2 | AI（Codex） | Done | 清理全模块 `printStackTrace()`；对 address/port 等关键参数改为 params 触发脱敏 |
| G-T0062 | 统一本地代理能力：抽取通用 Proxy 服务（headers/range/tls 策略一致），减少 `HttpPlayServer`/`VlcProxyServer` 逻辑重复 | G-F0055 | `core_storage_component/.../HttpPlayServer.kt`；`player_component/.../VlcProxyServer.kt`；`core_network_component`（OkHttp 安全策略） | 1) mpv/VLC 均可通过统一入口获得可播放 URL；2) Range/headers 行为在主要站点/协议下稳定；3) 默认 TLS 策略安全且可配置；4) 全仓编译通过 | P2 | 待分配（Player/Storage/Network） | Draft | - |
| G-T0063 | 若继续使用 PanelSwitchHelper：补齐生命周期释放/资源回收，避免潜在泄漏 | G-F0048 | 调整 `SendDanmuDialog` 持有 helper 实例，并在 `dismiss()`/`onStop()` 中按上游 API 做释放；必要时封装为 `PanelSwitchController`（内部持有 helper）以统一管理 | 1) Dialog 关闭后无残留监听（LeakCanary/手动验证）；2) Back 键与沉浸式状态栏恢复行为不回退；3) 不影响现有 UI | P2 | 待分配（Player） | Draft | - |
| G-T0064 | 迁移存储凭证到安全存储：DB 中不再保存明文 `password/remoteSecret`（改为引用/加密存储） | G-F0027 | `data_component`（`MediaLibraryEntity` 字段策略 + migration）；`core_storage_component`（读取/保存逻辑）；相关 UI（新增/编辑存储） | 1) DB 不再持久化明文密码/secret；2) 旧用户数据可迁移且不丢失；3) 任一协议（WebDAV/FTP/SMB/Remote）连接/播放可回归 | P2 | AI（Codex） | Done | 与 G-T0028 同批次落地 |
| G-T0065 | 重命名自定义 `Retrofit` 包装类（或对 `retrofit2.Retrofit` 使用别名 import），降低阅读歧义 | G-F0065 | `core_network_component/src/main/java/com/xyoye/common_component/network/RetrofitManager.kt`（类名与引用点） | 1) 对外 API 命名清晰；2) IDE/grep 检索不再混淆；3) 全仓编译通过 | P2 | AI（Codex） | Done | 重命名为 `RetrofitManager` |
| G-T0066 | 重构 `DatabaseManager` holder 命名与结构，降低阅读歧义 | G-F0059 | `core_database_component/src/main/java/com/xyoye/common_component/database/DatabaseManager.kt`（将 `private object DatabaseManager` 改为 `Holder/InstanceHolder` 等） | 1) `DatabaseManager.instance` 行为不变；2) 编译通过；3) 无额外反射/Proguard 规则需求 | P2 | AI（Codex） | Done | 重命名为 `Holder`，避免同名遮蔽 |
| G-T0067 | 增加可复现的回归用例：覆盖多格式/多文件/大文件/异常压缩包的解压行为 | G-F0023,G-F0067 | 在 `core_storage_component/src/androidTest/...` 增加 instrumentation 用例（更适配 native so）；准备最小化样例压缩包（可放 `core_storage_component/src/androidTest/assets/` 或 `document/code_quality_audit/fixtures/` 并在 README 说明来源/生成方式） | 1) 覆盖：zip/7z（至少 2 种），以及“多次 write”场景；2) 验证：输出文件 hash/大小正确；3) 验证：异常压缩包/取消时资源回收 | P3 | 待分配（QA/Storage） | Draft | - |
| G-T0068 | 收敛 media3/字幕遥测相关包结构，提升可发现性与复用一致性 | G-F0068 | 以 `com.xyoye.data_component.media3` 为根：将 `data/media3`、`entity/media3`、`media3/mapper`、字幕遥测相关 bean 逐步归并为明确子包（例如 `media3.dto`/`media3.entity`/`media3.telemetry`） | 1) 新包结构有明确规则并写入 `document/code_quality_audit/config/audit_dimensions.md` 或模块报告备注；2) 迁移按批次进行，单批次可编译通过；3) 迁移后全仓检索路径清晰（同类类型不再散落） | P3 | 待分配（Base） | Draft | - |

## 3) 来源映射（必须维护）

- G-F0001: [CORE_NETWORK-F001, CORE_NETWORK-F002, CORE_STORAGE-F002]
- G-F0002: [BILIBILI-F001]
- G-F0003: [CORE_SYSTEM-F001]
- G-F0004: [PLAYER-F002]
- G-F0005: [CORE_NETWORK-F003, CORE_LOG-F004, PLAYER-F001, REPO_THUNDER-F004, REPO_VIDEO_CACHE-F002]
- G-F0006: [STORAGE-F002]
- G-F0007: [USER-F002]
- G-F0008: [LOCAL-F003]
- G-F0009: [DATA-F001]
- G-F0010: [CORE_DATABASE-F005]
- G-F0011: [CORE_UI-F003]
- G-F0012: [PLAYER-F004]
- G-F0013: [APP-F001]
- G-F0014: [CORE_DATABASE-F003]
- G-F0015: [CORE_CONTRACT-F002]
- G-F0016: [PLAYER-F005]
- G-F0017: [REPO_DANMAKU-F001, REPO_IMMERSION_BAR-F001, REPO_PANEL_SWITCH-F001, REPO_SEVEN_ZIP-F001, REPO_THUNDER-F001, REPO_VIDEO_CACHE-F001]
- G-F0018: [ANIME-F003]
- G-F0019: [CORE_LOG-F003]
- G-F0020: [CORE_SYSTEM-F005]
- G-F0021: [STORAGE-F001]
- G-F0022: [CORE_SYSTEM-F002]
- G-F0023: [REPO_SEVEN_ZIP-F003]
- G-F0024: [REPO_THUNDER-F003]
- G-F0025: [REPO_SEVEN_ZIP-F002]
- G-F0026: [CORE_SYSTEM-F004]
- G-F0027: [CORE_STORAGE-F003]
- G-F0028: [BILIBILI-F002]
- G-F0029: [DATA-F002]
- G-F0030: [CORE_CONTRACT-F001]
- G-F0031: [STORAGE-F004]
- G-F0032: [CORE_STORAGE-F001]
- G-F0033: [ANIME-F002]
- G-F0034: [LOCAL-F002]
- G-F0035: [CORE_UI-F001]
- G-F0036: [CORE_UI-F002]
- G-F0037: [LOCAL-F004]
- G-F0038: [BILIBILI-F003]
- G-F0039: [CORE_NETWORK-F004]
- G-F0040: [APP-F002]
- G-F0041: [CORE_DATABASE-F001]
- G-F0042: [USER-F003]
- G-F0043: [CORE_STORAGE-F004]
- G-F0044: [USER-F004]
- G-F0045: [CORE_LOG-F002]
- G-F0046: [REPO_THUNDER-F002]
- G-F0047: [REPO_IMMERSION_BAR-F002]
- G-F0048: [REPO_PANEL_SWITCH-F003]
- G-F0049: [CORE_NETWORK-F005]
- G-F0050: [APP-F003]
- G-F0051: [CORE_LOG-F001]
- G-F0052: [CORE_DATABASE-F004]
- G-F0053: [STORAGE-F003]
- G-F0054: [PLAYER-F003]
- G-F0055: [CORE_STORAGE-F005]
- G-F0056: [LOCAL-F001]
- G-F0057: [CORE_SYSTEM-F003]
- G-F0058: [ANIME-F001]
- G-F0059: [CORE_DATABASE-F002]
- G-F0060: [CORE_CONTRACT-F003]
- G-F0061: [REPO_IMMERSION_BAR-F003]
- G-F0062: [REPO_PANEL_SWITCH-F002]
- G-F0063: [USER-F001]
- G-F0064: [REPO_DANMAKU-F002]
- G-F0065: [CORE_NETWORK-F006]
- G-F0066: [REPO_SEVEN_ZIP-F004]
- G-F0067: [REPO_SEVEN_ZIP-F005]
- G-F0068: [DATA-F003]
- G-T0001: [CORE_STORAGE-T002]
- G-T0002: [CORE_NETWORK-T002]
- G-T0003: [REPO_DANMAKU-T001, REPO_IMMERSION_BAR-T001, REPO_PANEL_SWITCH-T001, REPO_SEVEN_ZIP-T001, REPO_THUNDER-T001, REPO_VIDEO_CACHE-T001]
- G-T0004: [CORE_SYSTEM-T002]
- G-T0005: [REPO_SEVEN_ZIP-T003]
- G-T0006: [REPO_SEVEN_ZIP-T002]
- G-T0007: [BILIBILI-T001]
- G-T0008: [PLAYER-T002]
- G-T0009: [APP-T001]
- G-T0010: [REPO_THUNDER-T003]
- G-T0011: [DATA-T001]
- G-T0012: [CORE_NETWORK-T003, CORE_LOG-T004, PLAYER-T001, REPO_THUNDER-T004, REPO_VIDEO_CACHE-T002]
- G-T0013: [USER-T002]
- G-T0014: [CORE_DATABASE-T005]
- G-T0015: [ANIME-T003]
- G-T0016: [CORE_LOG-T003]
- G-T0017: [CORE_CONTRACT-T002]
- G-T0018: [CORE_SYSTEM-T003]
- G-T0019: [CORE_UI-T003]
- G-T0020: [STORAGE-T001]
- G-T0021: [PLAYER-T005]
- G-T0022: [CORE_SYSTEM-T001]
- G-T0023: [STORAGE-T002]
- G-T0024: [CORE_NETWORK-T001]
- G-T0025: [CORE_DATABASE-T003]
- G-T0026: [LOCAL-T003]
- G-T0027: [BILIBILI-T002]
- G-T0028: [DATA-T002]
- G-T0029: [APP-T002]
- G-T0030: [CORE_UI-T002]
- G-T0031: [CORE_SYSTEM-T005]
- G-T0032: [REPO_THUNDER-T002]
- G-T0033: [CORE_UI-T001]
- G-T0034: [LOCAL-T004]
- G-T0035: [CORE_NETWORK-T004]
- G-T0036: [LOCAL-T001]
- G-T0037: [USER-T003]
- G-T0038: [ANIME-T002]
- G-T0039: [CORE_DATABASE-T001]
- G-T0040: [CORE_LOG-T002]
- G-T0041: [LOCAL-T002]
- G-T0042: [CORE_LOG-T001]
- G-T0043: [USER-T001]
- G-T0044: [APP-T003]
- G-T0045: [STORAGE-T003]
- G-T0046: [CORE_NETWORK-T005]
- G-T0047: [ANIME-T001]
- G-T0048: [BILIBILI-T003]
- G-T0049: [STORAGE-T004]
- G-T0050: [CORE_CONTRACT-T001]
- G-T0051: [PLAYER-T003]
- G-T0052: [CORE_STORAGE-T001]
- G-T0053: [REPO_IMMERSION_BAR-T002]
- G-T0054: [REPO_PANEL_SWITCH-T002]
- G-T0055: [USER-T004]
- G-T0056: [CORE_DATABASE-T004]
- G-T0057: [CORE_SYSTEM-T004]
- G-T0058: [CORE_CONTRACT-T003]
- G-T0059: [PLAYER-T004]
- G-T0060: [REPO_DANMAKU-T002]
- G-T0061: [CORE_STORAGE-T004]
- G-T0062: [CORE_STORAGE-T005]
- G-T0063: [REPO_PANEL_SWITCH-T003]
- G-T0064: [CORE_STORAGE-T003]
- G-T0065: [CORE_NETWORK-T006]
- G-T0066: [CORE_DATABASE-T002]
- G-T0067: [REPO_SEVEN_ZIP-T004]
- G-T0068: [DATA-T003]

## 4) 路线图（批次建议）

- 详见：`document/code_quality_audit/global/roadmap.md`（按批次/依赖/风险展开，并引用 `G-T####`）。
