# 模块排查报告：:app

- 模块：:app
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：app/

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`APP-F###`  
> - Task：`APP-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 作为组合根（composition root）聚合所有 feature/core 模块，提供最终 APK；承载应用级入口（`Application`/`Activity`/`Service`）与跨模块组装逻辑。
  - 提供 TV/移动双入口与导航容器：
    - 启动页：`SplashActivity` / `TvSplashActivity`
    - 壳层页：`MainActivity` / `TvMainActivity`（负责装载各业务模块的 Fragment）
  - 提供 Media3 会话后台服务与绑定入口（当前以轻量 StateFlow 形态暴露能力态，后续可扩展 MediaSession/通知等集成）。
- 模块职责（不做什么）
  - 不承载业务模块的领域逻辑与数据层实现（应留在 `*_component` / `core_*` / `data_component`）。
  - 不应作为被其它模块依赖的公共能力落点（避免出现“反向依赖 app”导致依赖环与不可复用）。
- 关键入口/关键路径（示例）
  - App 入口：`app/src/main/AndroidManifest.xml` + `<application android:name="...IApplication">`
  - Application 初始化：`app/src/main/java/com/okamihoro/ddplaytv/app/IApplication.kt` + `IApplication#onCreate`
  - 启动分流：
    - `app/src/main/java/com/okamihoro/ddplaytv/ui/splash/SplashActivity.kt` + `SplashActivity#createLaunchIntent`
    - `app/src/main/java/com/okamihoro/ddplaytv/ui/splash/TvSplashActivity.kt` + `TvSplashActivity#createLaunchIntent`
  - 壳层导航（装载各模块 Fragment）：
    - `app/src/main/java/com/okamihoro/ddplaytv/ui/main/MainActivity.kt` + `MainActivity#switchFragment` / `MainActivity#getFragment`
    - `app/src/main/java/com/okamihoro/ddplaytv/ui/tv/TvMainActivity.kt` + `TvMainActivity#switchSection` / `TvMainActivity#switchFragment`
  - ARouter 注入与服务启动：
    - `app/src/main/java/com/okamihoro/ddplaytv/ui/shell/BaseShellActivity.kt` + `BaseShellActivity#initShell` / `BaseShellActivity#initScreencastReceive`
  - Media3 会话后台服务：
    - `app/src/main/java/com/okamihoro/ddplaytv/app/service/Media3SessionService.kt` + `Media3SessionService#onBind`
    - `app/src/main/java/com/okamihoro/ddplaytv/app/service/Media3SessionServiceProviderImpl.kt` + `Media3SessionServiceProviderImpl#createBindIntent`
- 依赖边界
  - 对外（被依赖）：原则上无（`:app` 不应被任何模块依赖）。
  - 对内（依赖）：聚合全部工程模块（见 `app/build.gradle.kts` 的 `implementation(project(...))`）。
  - 边界疑点：
    - `:app` 同时承载“TV 裁剪/禁用”逻辑时，必须明确“仅 TV 禁用 vs 全端禁用”的策略与判定（建议统一走 `Context.isTelevisionUiMode()`），否则容易误伤移动端能力（见 Findings）。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - `rg`：快速定位入口与跨模块装载路径  
    - `rg "class (IApplication|SplashActivity|TvSplashActivity|MainActivity|TvMainActivity)" -n app/src/main/java`  
    - `rg "ARouter\\.getInstance\\(\\)" -n app/src/main/java`  
  - ast-grep：确证结构敏感的注解/注入点（避免纯文本误判）  
    - Kotlin：`@Autowired\nlateinit var $NAME: $TYPE`（定位 `BaseShellActivity`/`MainActivity` 的注入字段）  
    - Kotlin：`@Route($$$)\nclass $NAME : $TYPE`（定位 `Media3SessionServiceProviderImpl` 的路由发布）

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| APP-F001 | ArchitectureRisk | “TV 适配禁用”逻辑未做 TV 判定，可能误伤移动端能力（后台播放/画中画） | `app/src/main/java/com/okamihoro/ddplaytv/app/service/Media3BackgroundCoordinator.kt` + `Media3BackgroundCoordinator#sync`（当前无条件 `emitCommands(emptySet())` / `emitModes(emptySet())`，注释为 “TV adaptation”） | N/A | Unify | `:app`（按 `Context.isTelevisionUiMode()` 或配置开关分流） | High | Small | P1 | 若移动端仍需后台/画中画，会被静默禁用；需同步梳理产品策略与能力契约（`PlayerCapabilityContract`） |
| APP-F002 | ArchitectureRisk | “TV 构建禁用”的能力以 `UnsupportedOperationException` 兜底，误调用会直接崩溃 | `app/src/main/java/com/okamihoro/ddplaytv/app/cast/Media3CastManager.kt` + `Media3CastManager#prepareCastSession`（直接 `throw UnsupportedOperationException("Cast sender is disabled for TV builds")`） | N/A | Unify | `:app` + `:data_component`（用显式能力开关/结果类型替代 throw；或按构建变体裁剪入口） | Medium | Small | P2 | 当前未见生产调用点（仅 androidTest），但未来接入投屏发送入口时容易遗漏导致崩溃 |
| APP-F003 | Duplication | TV/移动壳层都实现了“ARouter 装载 Fragment + show/hide”流程，逻辑重复且易漂移 | `app/src/main/java/com/okamihoro/ddplaytv/ui/main/MainActivity.kt` + `MainActivity#switchFragment`；`app/src/main/java/com/okamihoro/ddplaytv/ui/tv/TvMainActivity.kt` + `TvMainActivity#switchFragment` | Intentional | Unify | `:app`（抽取壳层导航/Fragment 交换策略为可复用组件，保留 UI 差异） | Medium | Medium | P2 | 需回归 TV 焦点路径（DPAD）与移动端底部导航；抽取时避免破坏 `FragmentTransaction` 语义与状态恢复 |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| APP-T001 | APP-F001 | 将“TV 禁用后台/画中画”的策略显式化并避免误伤移动端 | 调整 `Media3BackgroundCoordinator#sync`：仅 TV 下强制清空；非 TV 按 `PlayerCapabilityContract` 同步能力；必要时引入配置开关（例如 `AppConfig`/`BuildConfig`） | 1) TV 模式下 capability 同步后 `sessionCommands/backgroundModes` 为空；2) 非 TV 模式下按契约同步（可用单测/仪表测试覆盖）；3) 不引入跨 feature 直接依赖；4) 行为变更有文档说明 | High | Small | P1 | AI（Codex） | Done |
| APP-T002 | APP-F002 | 以显式结果/能力开关替代 “禁用功能即 throw”，降低误调用崩溃风险 | `Media3CastManager#prepareCastSession`：改为返回 `CastSessionPrepareResult`（`Ready/Disabled/UnsupportedTarget`），并通过 `castSenderEnabled` 能力开关控制行为，避免底层直接抛异常 | 1) 任何 UI/业务入口都不会因误调用导致崩溃；2) TV 模式下保持“投屏发送不可用”的可理解提示（`Disabled.message`）；3) 覆盖 JVM 单测（`Media3CastManagerTest`）验证禁用/启用/目标缺失分支 | Medium | Small | P2 | AI（Codex） | Done |
| APP-T003 | APP-F003 | 抽取壳层 Fragment 装载/切换逻辑，减少重复实现与差异漂移 | 抽取 `FragmentSwitcher`/delegate（建议落在 `:core_ui_component` 或 `:app` 内部包），统一 `findFragmentByTag/add/show/hide` 与状态处理；TV 专有的导航数据结构保留在 `TvMainActivity` | 1) `MainActivity` 与 `TvMainActivity` 使用同一套切换实现；2) 旋转/进程重建后不出现错乱（当前已 `findAndRemoveFragment` 规避）；3) TV DPAD 焦点可达、可见、可返回；4) 不改变现有路由路径与功能入口 | Medium | Medium | P2 | AI（Codex） | Done |

## 5) 风险与回归关注点

- 行为回退风险（哪些场景最敏感）
- 回归成本（需要的账号/媒体文件/设备）

## 6) 备注（历史背景/待确认点）

- `Media3BackgroundCoordinator` 与 `Media3CastManager` 的“TV 裁剪”属于产品决策还是临时方案需进一步确认；若为全端禁用，应在配置/文档中明确为“全端禁用”，避免以“TV 适配”名义静默 stub。
- 2026-02-04：已将 `Media3BackgroundCoordinator#sync` 的“TV 禁用后台/画中画”策略显式化为 `isTelevisionUiMode()` 分流；TV 仍强制清空，非 TV 恢复按 `PlayerCapabilityContract` 同步能力（见 `Media3BackgroundCoordinatorTest`）。
- 2026-02-06：已完成 `APP-T002`：`Media3CastManager#prepareCastSession` 不再抛 `UnsupportedOperationException`，改为返回显式结果 `CastSessionPrepareResult`，并新增 `Media3CastManagerTest` 覆盖禁用/启用/目标缺失路径。
- 2026-02-06：已完成 `APP-T003`：新增 `ShellFragmentSwitcher` 统一 Fragment 装载/切换流程，`MainActivity` 与 `TvMainActivity` 共用同一实现，保留各自导航与焦点逻辑。
