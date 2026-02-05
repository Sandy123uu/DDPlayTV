# 模块排查报告：:core_ui_component

- 模块：:core_ui_component
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：core_ui_component/src/main/java/com/xyoye/common_component/（`base/`、`adapter/`、`focus/`、`extension/`、`preference/`、`utils/`、`weight/`）

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`CORE_UI-F###`  
> - Task：`CORE_UI-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 作为 UI 基础设施模块，为各 feature 提供“统一的页面基类/绑定/加载态/通用控件/适配器/焦点策略”等能力，减少重复实现与口径漂移。
  - 维护 TV/遥控器 DPAD 相关的焦点策略与协作工具（避免各模块各写一套导致不可控）。
- 模块职责（不做什么）
  - 不承载业务模块（anime/local/player/storage/user/app）的业务逻辑与业务 UI；只提供可复用 UI 基础能力。
  - 不应让 feature 直接绑定第三方 UI 库类型（例如 ImmersionBar），否则会造成边界泄漏与迁移困难（详见 Findings）。
- 关键入口/关键路径（示例）
  - `core_ui_component/src/main/java/com/xyoye/common_component/base/BaseAppCompatActivity.kt` + `BaseAppCompatActivity#onCreate/initToolbar/showLoading`
  - `core_ui_component/src/main/java/com/xyoye/common_component/base/BaseActivity.kt` + `BaseActivity#onCreate/initStatusBar`
  - `core_ui_component/src/main/java/com/xyoye/common_component/base/BaseAppFragment.kt` + `BaseAppFragment#onAttach/showLoading/hideLoading`
  - `core_ui_component/src/main/java/com/xyoye/common_component/adapter/BaseAdapter.kt` + `BaseAdapter#setData/getItemViewType`
  - `core_ui_component/src/main/java/com/xyoye/common_component/focus/RecyclerViewFocusDelegate.kt` + `RecyclerViewFocusDelegate`（列表焦点委托）
  - `core_ui_component/src/main/java/com/xyoye/common_component/extension/ContextExt.kt` + `Context#isTelevisionUiMode`
- 依赖边界（与哪些模块交互，是否存在边界疑点）
  - 依赖：`:core_system_component`（系统/应用环境能力）、`:core_log_component`（日志上报）、`:core_contract_component`（契约/路由类型）、`:data_component`（通用数据结构）、`:repository:immersion_bar`（沉浸式状态栏）。
  - 被依赖：所有 feature + `:app`（作为 UI 基础层）。
  - 边界疑点：
    - Gradle 使用大量 `api(...)` 透传依赖（含第三方 UI 库），上层可能形成“隐式依赖”，削弱依赖治理与可替换性。
    - `BaseAppFragment` 强制要求宿主 Activity 是 `BaseAppCompatActivity`，降低复用弹性（容易引发隐藏崩溃点）。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - 本轮使用 ast-grep 确证：`Context.isTelevisionUiMode()` 定义、`BaseAppFragment` 的强制类型转换、`BaseAdapter` 的 DiffUtil 调用与异常上报入口。
  - 本轮使用 `rg` 扫描：跨模块 `ImmersionBar` 的直接引用分布（用于评估边界泄漏与迁移成本）。

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE_UI-F001 | ArchitectureRisk | `:core_ui_component` 通过 `api(...)` 透传大量 UI/第三方依赖（含 ImmersionBar），导致上层模块形成隐式依赖与边界泄漏 | `core_ui_component/build.gradle.kts` + `api(project(":repository:immersion_bar"))`；调用示例：`anime_component/src/main/java/com/xyoye/anime_component/ui/activities/anime_detail/AnimeDetailActivity.kt` + `import com.gyf.immersionbar.ImmersionBar` | N/A | Unify | `:core_ui_component`（封装状态栏/主题 API，减少第三方类型外泄） | Medium | Medium | P2 | 迁移需要批量改造 feature/app 代码；需验证 TV/移动端沉浸式样式一致性 |
| CORE_UI-F002 | ArchitectureRisk | `BaseAdapter#setData` 对 DiffUtil 异常使用 try/catch 回退 `notifyDataSetChanged()`（带 TODO），根因未定位可能造成性能抖动与上报噪音 | `core_ui_component/src/main/java/com/xyoye/common_component/adapter/BaseAdapter.kt` + `BaseAdapter#setData/setDiffData`（`DiffUtil.calculateDiff` + `ErrorReportHelper.postCatchedException`） | N/A | Unify | `:core_ui_component`（收敛为稳定的 diff 策略/数据约束） | Medium | Medium | P2 | 需要复现触发条件（数据结构/equals/多线程更新）；修复需回归列表刷新动画与焦点恢复 |
| CORE_UI-F003 | ArchitectureRisk | `BaseAppFragment#onAttach` 强制将宿主 Activity cast 为 `BaseAppCompatActivity`，复用边界不清且存在 `ClassCastException` 风险 | `core_ui_component/src/main/java/com/xyoye/common_component/base/BaseAppFragment.kt` + `BaseAppFragment#onAttach (context as BaseAppCompatActivity<*>)` | N/A | Unify | `:core_ui_component`（引入 `LoadingHost` 等契约/安全校验） | Medium | Small | P1 | 变更可能影响现有 Fragment 宿主；需要统一迁移与明确约束（文档/编译期约束） |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| CORE_UI-T001 | CORE_UI-F001 | 封装状态栏/沉浸式能力：上层不再直接 import `ImmersionBar`，并尽量减少 `api(...)` 依赖透传 | `core_ui_component`（新增 `StatusBarStyle`/扩展 API、调整 `build.gradle.kts`）；迁移 `anime_component/player_component/storage_component/app` 等直接引用 `ImmersionBar` 的页面 | 1) 上层模块不再直接引用 `com.gyf.immersionbar.ImmersionBar`；2) `:core_ui_component` 对外暴露的 API 清晰且可替换；3) 全仓编译通过，关键页面状态栏样式在 TV/移动端一致 | Medium | Medium | P2 | AI（Codex） | Done |
| CORE_UI-T002 | CORE_UI-F002 | 定位并修复 DiffUtil 异常根因：约束数据模型或改造 diff 机制，降低回退刷新与上报噪音 | `BaseAdapter`/`AdapterDiffCreator`/`AdapterDiffCallBack`；必要时为常见列表数据引入稳定 `id` 或不可变数据约束 | 1) `BaseAdapter#setData` 不再依赖 try/catch“吞异常”；2) 列表更新动画与焦点恢复稳定；3) Diff 失败率显著下降（可通过线上/日志统计或回归验证） | Medium | Medium | P2 | 待分配（UI） | Draft |
| CORE_UI-T003 | CORE_UI-F003 | 明确 Fragment 宿主契约：引入 `LoadingHost`（或等价接口）并替换强制 cast，避免隐藏崩溃点 | `BaseAppFragment` + 所有继承链（`BaseFragment/BasePreferenceFragmentCompat` 等）及其宿主 Activity | 1) 不再出现 `context as BaseAppCompatActivity<*>` 的强制转换；2) 宿主不满足契约时能明确报错（Fail Fast）；3) 全仓编译通过，页面 loading 行为不变 | Medium | Small | P1 | AI（Codex） | Done |

## 5) 风险与回归关注点

- 行为回退风险（哪些场景最敏感）
  - 状态栏/沉浸式封装迁移涉及大量页面：需要重点回归“沉浸式 + Toolbar + 横竖屏/TV 焦点”的组合表现。
  - 列表 diff 策略修复会影响动画、焦点保存/恢复与分页加载体验（TV 场景更敏感）。
- 回归成本（需要的账号/媒体文件/设备）
  - 建议回归：TV 遥控器 DPAD（焦点可达/可见/可反馈），以及常见列表页（媒体库、搜索结果、文件列表）滚动与返回焦点恢复。

## 6) 备注（历史背景/待确认点）

- 本报告为初稿，建议按 `document/code_quality_audit/global/review_checklist.md` 复核后再将 `module_status` 标记为 Done。
