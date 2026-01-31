<!-- OPENSPEC:START -->
# OpenSpec Instructions

These instructions are for AI assistants working in this project.

Always open `@/openspec/AGENTS.md` when the request:
- Mentions planning or proposals (words like proposal, spec, change, plan)
- Introduces new capabilities, breaking changes, architecture shifts, or big performance/security work
- Sounds ambiguous and you need the authoritative spec before coding

Use `@/openspec/AGENTS.md` to learn:
- How to create and apply change proposals
- Spec format and conventions
- Project structure and guidelines

Keep this managed block so 'openspec update' can refresh the instructions.

<!-- OPENSPEC:END -->

# Agent Operating Rules
- Do thinking/planning/coding in English; provide final user-facing replies in Chinese.
- Use the GitHub CLI (`gh`) for GitHub workflows (issues/PRs/checks) whenever possible.
- `adb logcat` can be noisy: do not dump full logs; filter to only what is needed, and note that logs may contain other apps/system output.
- Prefer architectural and logical consistency over “minimal intrusion”; align with existing patterns even if it takes extra work to unify/abstract.
- Write Markdown documents in Chinese unless the task explicitly requests otherwise.

# Environment
- Windows 11 + WSL2.

# Repository Guidelines

## Project Structure & Module Organization
The app follows a modular MVVM layout. The composition root lives in `app/` (launcher shell + global wiring). Feature/business modules live in sibling directories (e.g. `anime_component/`, `local_component/`, `player_component/`, `storage_component/`, `user_component/`, `bilibili_component/`). Shared foundations are provided by `core_*` modules and `data_component/`. Build tooling resides in `buildSrc/`, and custom assets/scripts live under `document/`, `scripts/`, and `repository/`. Prefer keeping large media/prompts in dedicated folders rather than polluting module folders.

## Module Overview (based on `settings.gradle.kts`)
> Scope: only modules included via `include(...)` in `settings.gradle.kts`. The repo may contain similarly named folders that are not part of the main build.

- Total modules: `21` (`15` app/core/data + `6` bundled `repository` dependencies)
- App shell (1)
  - `:app`: app entry + shell (startup/main UI including TV), composes all modules into the final APK; also hosts global capabilities like Media3 sessions/background services.
- Feature/business modules (6)
  - `:anime_component`: anime/resource search, filters, details, follow/history (including magnet-search UI).
  - `:local_component`: local media library + playback entry; playback history; danmaku/subtitle source binding and downloads (e.g. Bilibili danmaku, Shooter subtitles).
  - `:player_component`: player capabilities + UI (Media3/VLC/mpv adapters, subtitles/ASS rendering, danmaku rendering/filtering, TV/gesture controls, caching, etc.).
  - `:storage_component`: storage + “streaming/casting” UI (file browsing, remote storage config, QR/remote scan, casting provider/receiver, etc.).
  - `:user_component`: user center + settings (login/profile, theme, player/app/developer settings, cache/scan management, about/licenses, etc.).
  - `:bilibili_component`: Bilibili integration (auth/cookies, signing, playback links/MPD, danmaku download, live danmaku socket, playback heartbeat + risk-control state), reused across modules.
- Core foundation modules (7)
  - `:core_contract_component`: cross-module contracts + routing (e.g. `RouteTable`), Service interfaces (file sharing/casting), playback extensions / shared Media3 session APIs.
  - `:core_system_component`: runtime/system integration (Application/startup orchestration, permissions/notifications/broadcasts, global config tables/tools, build-time injected keys/flags); may depend on `:core_log_component` to initialize logging/crash reporting early.
  - `:core_log_component`: logging + reporting infrastructure (collection/persistence, Bugly reporting, runtime log policy/sampling, subtitle/playback telemetry); initialized/wired by runtime; does not depend on `:core_system_component`.
  - `:core_network_component`: network foundations (Retrofit + Moshi, request wrappers/interceptors, shared Service/Repository plumbing).
  - `:core_database_component`: database layer (Room database management, DAO, migrations, selected local stores).
  - `:core_storage_component`: storage abstraction + implementations (multi-protocol/multi-source storage, media parsing/play proxy, danmaku/subtitle lookup, 7zip extraction, thunder download management, etc.).
  - `:core_ui_component`: shared UI foundations (BaseActivity/Fragment/ViewModel, adapters/paging, theme/focus policy, shared widgets/dialogs, etc.).
- Data model module (1)
  - `:data_component`: shared data layer (Room entities/converters, Moshi network models, business enums/parameter objects).
- Bundled repository dependency modules (6)
  - `:repository:danmaku`: wrapper for `DanmakuFlameMaster.aar` (danmaku rendering).
  - `:repository:immersion_bar`: wrapper for `immersionbar.aar` (immersive status bar).
  - `:repository:panel_switch`: wrapper for `panelSwitchHelper-androidx.aar` (panel/keyboard switch helper).
  - `:repository:seven_zip`: wrapper for `sevenzipjbinding4Android.aar` (7z extraction).
  - `:repository:thunder`: wrapper for `thunder.aar` (download-related).
  - `:repository:video_cache`: wrapper for `library-release.aar` (video caching).

## Module Dependency Layering Rules (based on current Gradle dependencies)
> Note: this is a reference for “layer semantics alignment + dependency governance”. The dependency snapshot is `document/architecture/module_dependencies_snapshot.md` (direct Gradle `project(...)` edges). Governance rules live in `document/architecture/module_dependency_governance.md` (includes DR-0001: treat `system` as runtime; allow `system -> log`).

**Design Principles**
- One-way dependencies, no cycles (dependencies only flow from “higher-level features” to “lower-level infra/contracts/data”).
- `core_*` provides reusable capabilities and must not depend on any feature module.
- Feature modules must not depend on each other (cross-feature collaboration must go through `:core_contract_component` contracts/interfaces + routing).
- `repository:*` is only for 2nd/3rd-party wrappers; depend on them directly where needed, avoid pulling them into `:app` without reason.

```mermaid
graph TD
  %% Convention: A --> B means A depends on B

  subgraph Repo["repository/* (bundled AAR dependencies)"]
    repo_danmaku[":repository:danmaku"]
    repo_immersion[":repository:immersion_bar"]
    repo_panel[":repository:panel_switch"]
    repo_seven[":repository:seven_zip"]
    repo_thunder[":repository:thunder"]
    repo_cache[":repository:video_cache"]
  end

  subgraph Base["Base layer (data/contracts only)"]
    data[":data_component"]
    contract[":core_contract_component"]
  end

  subgraph Runtime["Runtime layer"]
    system[":core_system_component"]
    log[":core_log_component"]
  end

  subgraph Infra["Infrastructure layer (replaceable implementations)"]
    network[":core_network_component"]
    db[":core_database_component"]
    bilibili[":bilibili_component"]
    storageCore[":core_storage_component"]
  end

  subgraph UI["UI foundation layer"]
    uiCore[":core_ui_component"]
  end

  subgraph Feature["Feature layer"]
    anime[":anime_component"]
    local[":local_component"]
    user[":user_component"]
    player[":player_component"]
    storageFeature[":storage_component"]
  end

  app[":app (composition root / shell)"]

  contract --> data
  system --> contract
  system --> data

  log --> data
  system --> log

  network --> system
  network --> log
  network --> data

  db --> system
  db --> data

  bilibili --> network
  bilibili --> db
  bilibili --> system
  bilibili --> log
  bilibili --> contract
  bilibili --> data

  storageCore --> contract
  storageCore --> network
  storageCore --> db
  storageCore --> system
  storageCore --> log
  storageCore --> data
  storageCore --> bilibili
  storageCore --> repo_seven
  storageCore --> repo_thunder

  uiCore --> system
  uiCore --> log
  uiCore --> contract
  uiCore --> data
  uiCore --> repo_immersion

  anime --> uiCore
  anime --> system
  anime --> log
  anime --> network
  anime --> db
  anime --> storageCore
  anime --> contract
  anime --> data

  local --> uiCore
  local --> system
  local --> log
  local --> network
  local --> storageCore
  local --> db
  local --> bilibili
  local --> contract
  local --> data

  user --> uiCore
  user --> system
  user --> log
  user --> network
  user --> db
  user --> storageCore
  user --> bilibili
  user --> contract
  user --> data

  player --> uiCore
  player --> system
  player --> log
  player --> storageCore
  player --> network
  player --> db
  player --> contract
  player --> data
  player --> repo_danmaku
  player --> repo_panel
  player --> repo_cache

  storageFeature --> uiCore
  storageFeature --> system
  storageFeature --> log
  storageFeature --> network
  storageFeature --> db
  storageFeature --> storageCore
  storageFeature --> bilibili
  storageFeature --> contract
  storageFeature --> data

  app --> anime
  app --> local
  app --> user
  app --> player
  app --> storageFeature
  app --> system
  app --> log
  app --> network
  app --> db
  app --> uiCore
  app --> contract
  app --> data
```

## Build, Test, and Development Commands
Use Gradle from repo root:
- `./gradlew assembleDebug` – fast developer build with logging enabled.
- `./gradlew assembleRelease` – optimized, signed release artifacts.
- `./gradlew clean build` – full rebuild to validate cross-module wiring.
- `./gradlew dependencyUpdates` – report outdated libraries defined in `build.gradle.kts`.
- `./gradlew verifyModuleDependencies` – module dependency governance check (v2), verifies direct `project(...)` dependencies against the allowed matrix/whitelist.
- `./gradlew testDebugUnitTest` and `./gradlew connectedDebugAndroidTest` – run JVM unit tests and device/emulator instrumentation respectively.

### Build Verification Requirement
- Always read the tail of Gradle output and confirm whether it ends with `BUILD SUCCESSFUL` or `BUILD FAILED` before reporting status to the user. Do **not** assume success just because tasks ran; explicitly mention failures when they occur.

## Coding Style & Naming Conventions
Stick to the Kotlin version configured by the repo (currently 1.9.25), with 4-space indentation, explicit visibility, and trailing commas disabled. View models live under `.../presentation` or `.../viewmodel` packages; fragments/activities use DataBinding layouts named `fragment_<feature>.xml` or `activity_<feature>.xml`. ARouter paths follow `/module/Feature`. Prefer extension functions for shared logic and keep shared helpers in appropriate `core_*` modules (often under `com.xyoye.common_component.*` packages), instead of duplicating them in feature modules. Lint via `./gradlew lint` before sending patches and let ktlint/Detekt settings inside `buildSrc` drive formatting rather than ad-hoc style tweaks.

## Testing Guidelines
Place JVM tests in `*/src/test/java` and instrumentation suites in `*/src/androidTest/java`; name files `<Class>Test.kt` or `<Feature>InstrumentedTest.kt` so Gradle discovers them. Cover parsing, player helpers, and data-layer conversions with unit tests, and reserve playback/integration flows for instrumentation backed by an emulator with media files in `storage_component`. Failing tests should block the PR, so run `testDebugUnitTest` locally and attach emulator logs when `connectedDebugAndroidTest` fails.

## Commit & Pull Request Guidelines
Recent history uses the `<type>: <summary>` pattern (`fix: ...`, `refactor: ...`); keep summaries under ~60 characters and describe scope (e.g., `player_component`). Squash noisy WIP commits before pushing. PRs must include: purpose, affected modules, test evidence (command + result), and UI screenshots when touching layouts. Link GitHub issues and note any required configuration toggles (`IS_APPLICATION_RUN`, `IS_DEBUG_MODE`).

## Security & Configuration Tips
Sensitive tokens belong in `local.properties` or Gradle properties; never hard-code keys. Toggle `IS_DEBUG_MODE` and `IS_APPLICATION_RUN` in `gradle.properties` when enabling verbose logs or single-module runs, then rebuild so the flags propagate. Follow `BUGLY_CONFIG.md` for crash reporting credentials, and remember the `user_component` ships with remote APIs disabled—avoid re-enabling interfaces without coordinator approval to keep builds distributable.

## Recent Changes
- 001-115-open-storage: Added Kotlin 1.9.25 (JVM target 1.8), Android Gradle Plugin 8.7.2 + AndroidX, Kotlin Coroutines, Retrofit + OkHttp, Moshi, Room, MMKV, Media3, NanoHTTPD (local proxy), ARouter.
- 001-baidu-pan-storage: Added Kotlin 1.9.25 (JVM target 1.8), Android Gradle Plugin 8.7.2 + AndroidX, Kotlin Coroutines, Retrofit + OkHttp, Moshi, Room, MMKV, Media3, NanoHTTPD (local proxy), ARouter.

## TV/Remote UX
本仓库以 TV 端交互为先（Leanback + 遥控器 DPAD），移动端保持可用。涉及 UI 与交互时，请以“可达性/一致性/可维护性”为优先级，而不是最小侵入。

### 1) 分流与判定
- 统一使用 `Context.isTelevisionUiMode()` 判定 TV UI mode（定义在 `core_ui_component`），不要在业务模块重复实现判断逻辑。
- 分流优先级（从大到小）：
  1. **结构差异大**：使用独立入口或独立页面（例如 `MainActivity` vs `TvMainActivity`）。
  2. **同屏小差异**：用运行时 `isTelevisionUiMode()` 做显隐/交互分流，避免大规模复制 layout。
  3. **纯视觉差异**：优先用 `*-television` 资源覆盖（尤其是 `state_focused` 的 selector/描边/overlay），避免复制整套布局。
- “TV 默认裁剪/关闭”的能力必须明确属于哪一种策略：
  - **仅 TV 禁用**：必须基于 `isTelevisionUiMode()` 分流，避免误伤移动端。
  - **全端禁用**：必须在代码/文档中明确为产品决策，不要以“TV 适配”为名做静默 stub。

### 2) DPAD / 焦点规范（强约束）
- TV 端必须保证“焦点可达、可见、可反馈、可返回”。任何控件的新增/显隐/顺序调整都需要验证 DPAD 导航路径无死角。
- 列表/网格（RecyclerView）建议规范：
  - Item 的默认焦点目标使用 `android:tag="@string/focusable_item"` 标注（必要时也可用 `FocusTarget` 指定子 View）。
  - 列表页优先接入 `RecyclerViewFocusDelegate` 统一处理：DPAD 上下移动、焦点保存/恢复、MENU/SETTINGS 键动作。
  - 进入页面时，在非触摸模式下必须有明确的默认焦点（避免“无焦点/焦点落在不可见 View”）。
- 可点击控件建议规范：
  - DPAD 模式下以 `state_focused` 作为主反馈（selector + `tv_focus_*` 颜色/描边），不要只依赖 `state_pressed`。
  - 同一行左右导航尽量显式配置 `nextFocusLeftId/nextFocusRightId`，避免焦点跳转到不可预期区域。
  - 需要动态启用/禁用焦点时，优先使用 `FocusPolicy.applyDpadFocusable(...)` / `View.applyDpadFocusable(...)`。

### 3) 按键语义（建议）
- `BACK`：优先关闭弹窗/设置面板/控制条；仅当栈空时才执行“二次返回退出”等全局逻辑。
- `MENU/SETTINGS`：在 TV 列表场景可作为“刷新/设置入口”的快捷键（按页面语义决定，并保持同类页面一致）。

### 4) 输入与确认策略（TV 友好）
- TV 端避免依赖 `EditText + 软键盘 + IME_ACTION_DONE` 完成关键配置；对数字/枚举类设置优先使用 DPAD 左右步进/切换控件。
- TV 端优先“修改即生效 + 自动持久化”，减少额外“确定/取消”的确认成本；若必须显式动作（扫码登录/测试连接/投屏连接等），动作成功后自动保存并返回一致的结果语义。

### 5) 触摸/手势
- 依赖触摸手势的交互在 TV UI mode 下应禁用或替换为纯 DPAD 逻辑；不要向 TV 用户暴露无法触发的入口。

### 6) 提交前自检（TV 回归）
- 全程仅用遥控器 DPAD 可完成核心路径（进入/选择/配置/返回/播放/退出）。
- 不存在“焦点陷阱/焦点丢失/隐藏视图抢焦点”；列表滚动与页面返回后焦点可恢复。
- 所有可操作控件均有清晰 focused 反馈（视觉高亮一致、可辨识）。

## Active Technologies
- Kotlin 1.9.25 (JVM target 1.8), Android Gradle Plugin 8.7.2 + AndroidX (Lifecycle/ViewModel/Room, etc.), Kotlin Coroutines, Retrofit/OkHttp, Moshi, Media3, ARouter, MMKV (003-add-bilibili-history)
- Room (SQLite) + MMKV (Key-Value) + local cache files (for temporary MPD/QR images, etc.) (003-add-bilibili-history)
- Kotlin 1.9.25 (JVM target 1.8), Android Gradle Plugin 8.7.2 + AndroidX, Kotlin Coroutines, Retrofit + OkHttp, Moshi, Room, MMKV, Media3, NanoHTTPD (local proxy), ARouter (001-baidu-pan-storage)
- Room (tables like `media_library`) + MMKV (preferences/login state storage) + local cache files (subtitles/danmaku/temporary manifests, etc.) (001-baidu-pan-storage)
- Room (tables like `media_library`) + MMKV (preferences/authorization-isolated storage) + local cache files (subtitles/danmaku/temporary manifests, etc.) (001-115-open-storage)
