## Context

仓库以 TV 端（遥控器 DPAD）为主要交互形态，规范要求：焦点可达、可见、可反馈、可返回，并优先使用 `Context.isTelevisionUiMode()` 做 TV 分流；视觉差异优先使用 `*-television` 资源覆盖。

当前工程已具备部分 TV 基建：

- `core_ui_component`：`FocusPolicy`、`RecyclerViewFocusDelegate`、`@string/focusable_item` 约定
- `player_component`：`TvVideoController` 的 DPAD 分发、部分 item 使用 focused selector
- `storage_component`：列表页已接入 `RecyclerViewFocusDelegate` 并处理 TV 刷新键语义

但在 `local_component/user_component/anime_component` 的部分页面中，仍存在“可点击但不可聚焦”、“focused 无视觉反馈”、“默认无焦点”等问题，需要统一补齐。

## Goals / Non-Goals

**Goals**

1. TV UI mode 下，关键页面的可操作控件均可被 DPAD 聚焦，并具备明确的 `state_focused` 视觉反馈。
2. TV UI mode 下进入页面时，保证存在明确的默认焦点（避免无焦点/不可见焦点）。
3. 列表页在 TV UI mode 下优先复用 `RecyclerViewFocusDelegate`：上下键移动、焦点保存/恢复、可选的 MENU/SETTINGS 语义扩展。
4. 变更保持对移动端的影响最小：视觉差异走 `drawable-television`，行为差异通过 `isInTouchMode`/`isTelevisionUiMode()` 分流。

**Non-Goals**

- 不在本变更中“重做所有页面的 TV 专属布局/Leanback 重构”；仅以“焦点可用”为底线修正。
- 不把所有输入交互都替换为 TV 步进控件（例如必须的文本输入）；数值/枚举类输入的系统性改造可作为后续专项变更。

## Decisions

### 1) 视觉：通用 item 背景补齐 TV focused selector

对被多模块复用的 `background_item_press*`（目前为 ripple）新增 `drawable-television` 覆盖，提供：

- `state_focused`：使用 `@color/tv_focus_stroke_color` + `@dimen/tv_focus_stroke_width` 进行描边，必要时叠加 `@color/tv_focus_overlay_color`
- `state_pressed`：保持轻量按压反馈（与现有语义一致）
- `default`：与当前默认背景一致（不改变布局含义）

该策略符合“纯视觉差异优先用 `*-television` 覆盖”的规范，并能让多个模块自动获得一致的 focused 反馈。

### 2) 行为：可操作控件必须可聚焦（以 focusable/tag 为统一约定）

对“可点击入口/列表条目”采取一致策略：

- 可点击目标视图必须 `android:focusable="true"` 且 `android:clickable="true"`（或在代码中使用 `FocusPolicy.applyDpadFocusable(...)` 达成等价行为）
- 列表 item/条目优先在可点击目标上标注 `android:tag="@string/focusable_item"`，以便复用 `RecyclerView` 的焦点工具链（`requestIndexChildFocus` / `RecyclerViewFocusDelegate`）

### 3) 列表页：优先使用 RecyclerViewFocusDelegate 统一 DPAD 导航与焦点恢复

对关键列表页（媒体库首页、磁力搜索结果等）优先接入 `RecyclerViewFocusDelegate`：

- `onResume/onPause` 保存与恢复焦点
- 统一处理 `DPAD_UP/DOWN` 的移动与边界行为
- 按页面需要配置 `MENU/SETTINGS` 语义（例如刷新/打开设置）

### 4) 默认焦点：进入页面若无焦点，则聚焦“首要入口”

进入页面（非触摸模式）时，若根视图无任何 focused child：

- 列表页优先聚焦到第一个可操作条目（`RecyclerView.requestIndexChildFocus(0)` 或 delegate 的 `requestFocus()`）
- 非列表页聚焦到首要入口（例如个人中心的账号卡片/第一个功能入口）

必要时避免默认落在 `EditText`（防止 TV 端键盘弹出与焦点陷阱），可改为聚焦到容器或按钮。

## Risks / Trade-offs

- 通用背景的 TV focused 样式会影响多个页面，需要 TV 端回归确认“可见且不过度抢眼”。
- 将 view 设为 focusable 后，DPAD 的自然焦点搜索可能暴露“导航顺序不可预期”；必要时补充 `nextFocus*` 或改用 delegate 控制。
- 若页面同时存在多个候选默认焦点，需要明确“首要入口”优先级，避免在返回时抢焦点。

## Validation

- 编译/静态检查：`./gradlew :app:assembleDebug`、`./gradlew lint`、`./gradlew verifyModuleDependencies`
- 手工 TV 回归：仅用遥控器完成 TvMainActivity 入口的核心路径（媒体库/个人中心/搜索/设置），验证焦点可达、可见、可返回

