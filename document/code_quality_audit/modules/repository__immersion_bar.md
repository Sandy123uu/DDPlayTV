# 模块排查报告：:repository:immersion_bar

- 模块：:repository:immersion_bar
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：repository/immersion_bar/

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`REPO_IMMERSION_BAR-F###`  
> - Task：`REPO_IMMERSION_BAR-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 以 Gradle module 形式封装本地预编译 AAR：`repository/immersion_bar/immersionbar.aar`，供工程以 `project(":repository:immersion_bar")` 依赖方式接入。
  - 提供沉浸式状态栏/导航栏能力（`com.gyf.immersionbar.*`），被 `:core_ui_component` 作为基础 UI 能力对外暴露，并被多个 feature 页面直接使用。
- 模块职责（不做什么）
  - 不承载任何业务逻辑；除 `build.gradle.kts` 与 AAR 工件外不应新增 Kotlin/Java 实现。
  - 不应在 wrapper 内引入其它工程模块依赖（避免把 wrapper 变为“功能模块”并扩大耦合面）。
- 关键入口/关键路径（示例）
  - AAR 封装：`repository/immersion_bar/build.gradle.kts` + `artifacts.add("default", file("immersionbar.aar"))`
  - 二进制产物：`repository/immersion_bar/immersionbar.aar`
  - 依赖声明（出口）：`core_ui_component/build.gradle.kts` + `api(project(":repository:immersion_bar"))`
  - 典型使用点（基础类）：`core_ui_component/src/main/java/com/xyoye/common_component/base/BaseActivity.kt` + `BaseActivity#initStatusBar`
  - 典型使用点（App 启动页）：`app/src/main/java/com/okamihoro/ddplaytv/ui/splash/BaseSplashActivity.kt` + `BaseSplashActivity#initStatusBar`
- 依赖边界
  - 对外（被依赖）：主要通过 `:core_ui_component` 的 `api(...)` 对外扩散；实际使用点分布在 `app/*`、`player_component/*`、`anime_component/*`、`storage_component/*` 等模块。
  - 对内（依赖）：无（仅提供 AAR 工件，不应再依赖其它工程模块）。
  - 边界疑点：
    - 由于 `:core_ui_component` 以 `api(...)` 方式导出，第三方类型会扩散到多个模块的编译期 API 面，升级/替换成本会随调用点数量线性增长（见 Findings）。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - `rg`：确认第三方类型扩散范围  
    - `rg "com\\.gyf\\.immersionbar\\." -n`
  - ast-grep：确证关键调用形态与调用点分布（避免字符串误判）  
    - Kotlin：`ImmersionBar.with($X)`（定位典型调用点与入口）

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| REPO_IMMERSION_BAR-F001 | ArchitectureRisk | 预编译 AAR 缺少来源/版本/License 等元信息，升级与合规审计不可追溯 | `repository/immersion_bar/immersionbar.aar`（二进制）；`repository/immersion_bar/build.gradle.kts` + `artifacts.add("default", file("immersionbar.aar"))`；对照 License 线索：`user_component/src/main/assets/license/ImmersionBar.txt`（含上游地址与 Apache 2.0） | N/A | Unify | `repository/immersion_bar/`（补齐元信息并与全仓 wrapper 统一规范） | Medium | Small | P1 | 需要确认 AAR 上游版本号/来源与校验和；否则安全事件或升级时难以评估影响面 |
| REPO_IMMERSION_BAR-F002 | ArchitectureRisk | 第三方类型通过 `core_ui_component` 的 `api(...)` 扩散到多模块，耦合面大且升级成本高 | 出口：`core_ui_component/build.gradle.kts` + `api(project(":repository:immersion_bar"))`；典型调用点：`core_ui_component/src/main/java/com/xyoye/common_component/base/BaseActivity.kt` + `BaseActivity#initStatusBar`；以及多个 feature 直接调用 `ImmersionBar.with(...)`（例如 `app/.../BaseSplashActivity.kt`） | Intentional | Unify | `:core_ui_component`（提供抽象/门面，逐步收敛到统一入口） | Medium | Medium | P2 | 若直接升级/替换库（或 API 变更），需要同时迁移多模块多处调用，回归成本高 |
| REPO_IMMERSION_BAR-F003 | Redundancy | wrapper 模块重复“手写 default artifact”封装方式，构建脚本口径易漂移 | `repository/immersion_bar/build.gradle.kts` + `configurations.maybeCreate("default")`；对比：`repository/danmaku/build.gradle.kts` / `repository/video_cache/build.gradle.kts`（同类写法） | Intentional | Unify | `buildSrc`（统一 prebuilt-aar 约定插件/脚本） | Medium | Medium | P2 | 需要验证对 AGP/Gradle 版本升级、配置缓存、依赖解析行为不产生副作用 |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| REPO_IMMERSION_BAR-T001 | REPO_IMMERSION_BAR-F001 | 为 ImmersionBar AAR 增加可追溯元信息（来源/版本/License/校验和/更新流程） | 新增 `repository/immersion_bar/README.md`（中文）；在 README 中引用/关联 `user_component/src/main/assets/license/ImmersionBar.txt`，并补充 AAR 的 SHA256 与升级步骤 | 1) README 明确：上游项目/下载地址、版本号、License、AAR SHA256、更新步骤；2) 任意人可按文档复现升级；3) 不影响现有依赖解析 | Medium | Small | P1 | 待分配（Repo） | Draft |
| REPO_IMMERSION_BAR-T002 | REPO_IMMERSION_BAR-F002 | 收敛第三方类型扩散：以 `core_ui_component` 提供统一状态栏/沉浸式配置入口 | 在 `:core_ui_component` 增加统一门面（例如 `StatusBarStyleApplier` / `ImmersionBarFacade`），并将 feature 侧直接 `ImmersionBar.with(...)` 的调用逐步迁移；迁移完成后评估是否可将 `api(project(":repository:immersion_bar"))` 降级为 `implementation(...)` | 1) 新增入口可覆盖现有常见用法（透明/fitSystemWindows/字体颜色等）；2) 至少迁移 2 个调用点验证可行（例如 `BaseActivity` 与 `BaseSplashActivity`）；3) 不影响 TV/移动端交互与样式；4) 迁移过程保持渐进（允许双栈共存一段时间） | Medium | Medium | P2 | 待分配（UI） | Draft |

## 5) 风险与回归关注点

- 行为回退风险（哪些场景最敏感）
- 回归成本（需要的账号/媒体文件/设备）

## 6) 备注（历史背景/待确认点）

- 当前仓库内已存在 ImmersionBar 的 License 线索（见 `user_component/src/main/assets/license/ImmersionBar.txt`），但 wrapper 目录缺少“该 AAR 与该 License 的对应关系、版本号、来源”说明；建议优先补齐元信息后再评估升级/替换方案。
