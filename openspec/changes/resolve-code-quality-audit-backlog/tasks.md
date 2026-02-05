# Tasks: resolve-code-quality-audit-backlog

> 说明：本变更的“唯一任务清单”来自 `document/code_quality_audit/global/summary.md` / `document/code_quality_audit/global/backlog.md`。  
> 本文件用于按批次勾选落地进度；每个 PR/提交建议在说明中引用所关闭的 `G-T####`，并同步更新 Backlog/模块报告状态。

## 1. P1（安全/隐私/稳定性基线）

- [x] 1.1 关闭 `G-T0001`：WebDAV 默认启用严格 TLS，提供可控开关（用户显式；Release 允许）
- [x] 1.2 关闭 `G-T0002`：收敛/下线 `UnsafeOkHttpClient`，提供更安全替代方案与文档
- [x] 1.3 关闭 `G-T0003`：为所有 repository wrapper AAR 补齐元信息与升级流程
- [x] 1.4 关闭 `G-T0004`：修复 `ActivityHelper#getTopActivity` 潜在崩溃
- [x] 1.5 关闭 `G-T0005`：修复 `ISequentialOutStream`（解压流式写入语义）
- [x] 1.6 关闭 `G-T0006`：解压线程/取消/资源释放策略（IO 线程 + 失败回收）
- [x] 1.7 关闭 `G-T0007`：去硬编码 `APP_KEY/APP_SEC`（构建期注入/本地配置 + 可控回退）
- [x] 1.8 关闭 `G-T0008`：VLC 代理拉流迁移到默认安全 OkHttpClient（可控降级）
- [x] 1.9 关闭 `G-T0009`：TV 禁用后台/画中画策略显式化，避免误伤移动端
- [x] 1.10 关闭 `G-T0010`：迅雷 SDK 初始化改为按需 + 可降级
- [x] 1.11 关闭 `G-T0011`：遥测聚合/事件构建逻辑迁出 Base（`data_component` 仅保留 payload）
- [x] 1.12 关闭 `G-T0012`：建立统一脱敏工具并迁移关键链路调用点（网络/播放器/缓存/下载）
- [x] 1.13 关闭 `G-T0013`：错误上报脱敏 + 登录/用户资料上报改造（避免泄露个人信息）
- [x] 1.14 关闭 `G-T0014`：开启 Room schema 导出 + 迁移校验门禁
- [x] 1.15 关闭 `G-T0015`：抽取 TV Tab 焦点协调与输入策略组件，统一 DPAD 行为
- [x] 1.16 关闭 `G-T0016`：收敛 Bugly 上报门面（`BuglyReporter` 统一触达 `CrashReport`）
- [x] 1.17 关闭 `G-T0017`：收敛可写 `LiveData` 暴露（对外只读 + 统一发送入口）
- [x] 1.18 关闭 `G-T0018`：`core_system_component` 移除 `printStackTrace()`，统一异常处理口径
- [x] 1.19 关闭 `G-T0019`：引入 `LoadingHost`（或等价）并替换强制 cast
- [x] 1.20 关闭 `G-T0020`：移除投屏 Sender 整条链路（删除 stub/注释旧实现与对外暴露入口）
- [x] 1.21 关闭 `G-T0021`：`player_component` 清理 `printStackTrace()` 并补齐异常上报上下文
- [x] 1.22 关闭 `G-T0022`：替换 `EntropyUtils` 加密实现（随机 IV/带认证）+ UDP 版本/兼容策略
- [x] 1.23 关闭 `G-T0023`：`storage_component` 移除 `printStackTrace()` + 统一脱敏上下文
- [x] 1.24 关闭 `G-T0024`：统一 TLS 安全默认（移除默认 `hostnameVerifier { _, _ -> true }`）
- [x] 1.25 关闭 `G-T0025`：手动迁移异常可观测性统一（结构化日志/异常上报）
- [x] 1.26 关闭 `G-T0026`：统一错误上报上下文构建与脱敏策略（减少重复样板）

## 2. P2（架构收敛 / 复用治理）

- [x] 2.1 关闭 `G-T0027`：Cookie/Token 加密存储（密钥管理 + 迁移）
- [ ] 2.2 关闭 `G-T0028`：媒体库远程凭据安全存储策略（避免 DB 明文）
- [ ] 2.3 关闭 `G-T0029`：以显式结果/能力开关替代“禁用功能即 throw”
- [ ] 2.4 关闭 `G-T0030`：定位并修复 DiffUtil 异常根因
- [ ] 2.5 关闭 `G-T0031`：制定“加密失败保存明文”策略与迁移/清理机制
- [ ] 2.6 关闭 `G-T0032`：封装/隔离迅雷 SDK 类型（避免上层显式依赖）
- [ ] 2.7 关闭 `G-T0033`：封装状态栏/沉浸式能力（上层不再直接 import `ImmersionBar`）
- [ ] 2.8 关闭 `G-T0034`：B 站弹幕解析/下载编排下沉到 repository/usecase
- [ ] 2.9 关闭 `G-T0035`：`Retrofit` 单例改为可注入 Provider/Factory
- [ ] 2.10 关闭 `G-T0036`：字幕匹配/搜索/Hash 能力收敛到基础层
- [ ] 2.11 关闭 `G-T0037`：扫描扩展目录/过滤配置与刷新逻辑下沉到 repository/usecase
- [ ] 2.12 关闭 `G-T0038`：搜索历史/播放历史持久化从 ViewModel 迁移到 repository/usecase
- [ ] 2.13 关闭 `G-T0039`：引入数据库访问 Provider，替换跨模块直连 `DatabaseManager.instance`
- [ ] 2.14 关闭 `G-T0040`：打通“写入层磁盘错误→全局状态”链路（`LogSystem` 一致）
- [ ] 2.15 关闭 `G-T0041`：抽取 PlayHistory/MediaLibrary repository/usecase，收敛 DAO 访问
- [ ] 2.16 关闭 `G-T0042`：`LogLevel` 优先级/比较逻辑单一实现
- [ ] 2.17 关闭 `G-T0043`：抽取 PreferenceDataStore/映射抽象（减少 drift）
- [ ] 2.18 关闭 `G-T0044`：抽取壳层 Fragment 装载/切换逻辑（减少重复实现）
- [ ] 2.19 关闭 `G-T0045`：抽取投屏协议与 server 公共能力到 core 层
- [ ] 2.20 关闭 `G-T0046`：抽取统一 OkHttpClientFactory（timeout/拦截器/安全策略集中）
- [ ] 2.21 关闭 `G-T0047`：抽取统一“Result 失败处理 + 上报 + toast”助手
- [ ] 2.22 关闭 `G-T0048`：拆分 `BilibiliRepository`（子域组件 + 单测 + facade）
- [ ] 2.23 关闭 `G-T0049`：收敛 MediaLibrary/PlayHistory 写入口径（替换 feature 直连 DAO）
- [ ] 2.24 关闭 `G-T0050`：收敛 contract 层运行时实现（副作用迁移到 runtime）
- [ ] 2.25 关闭 `G-T0051`：收敛本地代理/HTTP server 能力（减少多实现漂移）
- [ ] 2.26 关闭 `G-T0052`：收敛第三方协议库依赖泄漏（减少/消除 `api(...)`）
- [ ] 2.27 关闭 `G-T0053`：收敛第三方类型扩散（统一状态栏/沉浸式入口）
- [x] 2.28 关闭 `G-T0054`：移除“发送弹幕”整条链路（删除相关 UI/代码并移除依赖）
- [ ] 2.29 关闭 `G-T0055`：登录态单一事实源（UserSessionManager 统一 token/状态）
- [ ] 2.30 关闭 `G-T0056`：统一 MD5/hex 工具（移除重复实现）
- [ ] 2.31 关闭 `G-T0057`：统一 MMKV 初始化策略（Startup vs BaseApplication）
- [x] 2.32 关闭 `G-T0058`：统一 Service 包结构（service/services）
- [ ] 2.33 关闭 `G-T0059`：统一 Telemetry repository 的模块归属与包命名
- [ ] 2.34 关闭 `G-T0060`：统一 prebuilt AAR wrapper 的 Gradle 封装方式
- [ ] 2.35 关闭 `G-T0061`：`core_storage_component` 统一异常处理与脱敏（移除 `printStackTrace()`）
- [ ] 2.36 关闭 `G-T0062`：统一本地代理能力（Proxy 服务抽取，headers/range/tls 一致）
- [ ] 2.37 关闭 `G-T0063`：PanelSwitchHelper 生命周期释放/资源回收
- [ ] 2.38 关闭 `G-T0064`：迁移存储凭证到安全存储（DB 不再明文）
- [ ] 2.39 关闭 `G-T0065`：重命名自定义 `Retrofit` 包装类（避免与 `retrofit2.Retrofit` 同名）
- [ ] 2.40 关闭 `G-T0066`：重构 `DatabaseManager` holder 命名与结构

## 3. P3（回归用例 / 包结构治理）

- [ ] 3.1 关闭 `G-T0067`：增加可复现的解压回归 instrumentation 用例（含压缩炸弹/异常包）
- [ ] 3.2 关闭 `G-T0068`：收敛 media3/字幕遥测相关包结构

## 4. Validation（必须记录最终 BUILD 状态）

- [x] 4.1 `./gradlew verifyModuleDependencies`
- [x] 4.2 `./gradlew ktlintCheck`
- [x] 4.3 `./gradlew lint`（或 `lintDebug`）
- [x] 4.4 `./gradlew testDebugUnitTest`
- [x] 4.5 `./gradlew :app:assembleDebug`
- [ ] 4.6（如涉及 instrumentation）`./gradlew connectedDebugAndroidTest`
