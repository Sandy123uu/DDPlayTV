## Why

仓库以 TV 端交互为先，但当前部分页面仍存在“遥控器不可达 / 焦点不可见 / 默认无焦点”的问题，违背了 `AGENTS.md` 中 TV/Remote UX 规范（焦点可达、可见、可反馈、可返回）。

已发现的典型问题（示例）：

- **焦点反馈缺失**：通用背景（如 `core_ui_component/src/main/res/drawable/background_item_press.xml`、`core_ui_component/src/main/res/drawable/background_item_press_corner.xml`）仅提供 `state_pressed` 的 ripple，不提供 `state_focused`，导致 TV 端“有焦点但看不见”。
- **可操作控件不可聚焦**：多个列表/条目容器仅设置了点击事件但未设置 `focusable`，例如：
  - `local_component/src/main/res/layout/item_media_library.xml` 的 `item_layout`
  - `user_component/src/main/res/layout/item_cache_type.xml` 的根布局
  - `user_component/src/main/res/layout(-land)/fragment_personal.xml` 中多处可点击入口容器
- **进入页面默认焦点不明确**：部分页面缺少“非触摸模式进入时的默认焦点”，易出现“无焦点/焦点落在不可见 View”。

这些问题会直接影响 TV 端核心路径（进入媒体库/个人中心/搜索/设置），造成 DPAD 导航困难、误以为不可操作、以及焦点陷阱等体验问题。

## What Changes

- 在 `core_ui_component` 以 `drawable-television` 资源覆盖的方式，为通用 item 背景补齐 `state_focused` 视觉反馈（描边/overlay），使“focused”成为 TV 端主反馈而不是仅依赖“pressed”。
- 以“可操作控件必须可聚焦”为基线，补齐关键页面/列表条目的 DPAD focusability：
  - 列表 item 的可点击目标视图增加 `focusable/clickable`，并优先使用 `@string/focusable_item` tag 统一焦点目标。
  - 关键列表页面优先接入 `RecyclerViewFocusDelegate`，统一处理：上下移动、焦点保存/恢复、菜单键语义扩展（按页面决定）。
- 为关键页面补齐“非触摸模式默认焦点”逻辑：进入页面时如果未持有焦点，自动聚焦到首要入口，避免“无焦点”。
- 所有行为差异严格限定在 TV/非触摸模式生效（通过 `drawable-television` 覆盖 + `isInTouchMode`/`isTelevisionUiMode()` 分流），避免误伤移动端。

## Capabilities

### New Capabilities

- `tv-remote-ux-baseline`: TV/遥控器交互的焦点可达性与可见性基线（focused 反馈 + 默认焦点 + 列表焦点管理建议）。

### Modified Capabilities

- （无）

## Impact

- 影响模块：
  - `core_ui_component`：新增/调整 TV 专用 drawable（focused selector）
  - `local_component`：媒体库首页列表 item 的可聚焦与默认焦点
  - `user_component`：个人中心页面可聚焦入口与默认焦点
  - `anime_component`：搜索页关键按钮与磁力搜索列表 item 可聚焦
- 风险与代价：
  - 通用背景在 TV 端的 focused 样式会影响多个页面，需要做一次 TV 遥控器回归（视觉与 DPAD 路径）。
  - 为列表补齐 focusability 可能暴露此前被“无焦点”掩盖的导航顺序问题，必要时需要补充 `nextFocus*` 或 `RecyclerViewFocusDelegate`。
  - 部分页面可能需要细化“首要入口”的默认焦点策略（例如避免默认落在 `EditText` 导致键盘弹出）。

