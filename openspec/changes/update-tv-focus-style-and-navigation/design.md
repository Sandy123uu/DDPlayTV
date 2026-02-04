## Context

工程已具备一定的 TV 焦点基建与规范约束（见根目录 `AGENTS.md`）：

- 视觉差异优先走 `*-television` 资源覆盖
- 行为差异统一通过 `Context.isTelevisionUiMode()` + 非触摸模式分流
- 既有工具：`FocusPolicy`、`RecyclerViewFocusDelegate`、`ViewPager2DpadPageFocusSync`、`@string/focusable_item`

同时，`update-tv-remote-ux-compliance` 变更已为通用 item 背景补齐 TV `state_focused` 反馈，并在部分列表页引入默认焦点与焦点恢复策略。但在“TabLayout + ViewPager2 +（Search/Preference/List）”组合页面中，仍有两类系统性问题：

1) **Tab 行 focused 不可见**：焦点从返回键向下移动可能落在 Tab 上，但由于 Tab 背景未提供 `state_focused`，用户误以为焦点丢失。  
2) **分页焦点跳转不一致**：从 Tab 行进入内容页时，焦点可能落在容器（ViewPager2/RecyclerView）或跳到非预期条目；并且 ViewPager2 离屏页仍在层级中，存在错页误触发风险。

本变更目标是补齐“Tab/分页场景”的统一策略，让焦点在 TV 端始终“可见、可达、可返回”，并复用同一套工具链避免逐页修补。

## Goals / Non-Goals

### Goals

1. **焦点样式统一**：Tab/Preference 等高频控件在 TV 端的 focused 视觉反馈与通用 item 保持一致（统一使用 `tv_focus_*` 令牌）。
2. **焦点移动一致**：TabLayout + ViewPager2 页面满足明确的 DPAD 导航语义：
   - 返回键 → Tab 行 → 当前页内容首项（或恢复上次焦点）
   - 内容顶部 `DPAD_UP` → Tab 行（或上方入口）
3. **避免错页交互**：TV/非触摸模式下，当焦点落在非当前页的子 View 上时，自动同步到对应页面（复用 `ViewPager2DpadPageFocusSync`）。
4. **可复用与可治理**：提供通用 Focus Coordinator，避免各模块各写一套 KeyListener/nextFocus 规则。

### Non-Goals

- 不在本次对所有页面做 TV 专属布局复制；保持“同屏小差异/纯视觉差异”策略优先。
- 不在本次重写 Preference 框架；优先通过主题/资源覆盖与轻量 delegate 解决 focused 可见与默认焦点问题。

## Decisions

### 0) 进入页面的默认焦点：保持现状，不引入额外“自动跳焦点”逻辑

为避免增加复杂度与引入新的“抢焦点”风险，本变更 **不改变** 进入 Tab 页（Activity/Fragment 首次展示）时的默认焦点策略：

- 默认仍停留在页面既有的首要入口（例如 Toolbar 返回键）。
- 仅在用户通过 DPAD 发生显式导航（例如从返回键向下进入 Tab 行/内容区域）时，协调器才介入修复“不可见/跳焦点/错页交互”等问题。

### 1) 视觉：Tab 行 focused 反馈采用 selector + `drawable-television` 覆盖

为 TabLayout 提供统一的 Tab 背景 selector（建议形态）：

- `default`：透明或与现有保持一致（避免影响移动端视觉）
- `state_focused`（TV）：叠加 `tv_focus_overlay_color` + `tv_focus_stroke_color` 描边（与 `background_item_press.xml` 视觉一致）
- `state_selected`：保持 indicator 作为“选中”语义，focused 仍作为主要操作反馈

落地方式优先选择：

- 在布局中统一设置 `app:tabBackground="@drawable/xxx_tab_background"`，并在 `drawable-television/` 提供 TV 变体；避免在业务模块内新增各自的 selector。

### 2) 行为：引入 TabLayout + ViewPager2 的 DPAD 焦点协调器（Focus Coordinator）

目标是把“分页页面的 DPAD 语义”集中收敛到一个可复用工具里（放在 `core_ui_component`），由页面在 TV 模式下按需 attach。

协调器职责建议包含：

1. **Tab 可聚焦**：确保每个 TabView 在非触摸模式下可聚焦，并可见 focused 反馈。
2. **Down 入内容**：当焦点在 Tab 行且收到 `DPAD_DOWN`：
   - 优先将焦点移动到当前页内容的“首要可操作控件”
   - 若该页已有“上次焦点”，则优先恢复到该位置（焦点恢复策略见下文第 3 点）
3. **Up 回 Tab**：当焦点位于内容列表顶部且收到 `DPAD_UP`：把焦点移交回 Tab 行（或上方入口）。
4. **页同步**：整合 `ViewPager2DpadPageFocusSync`：如果焦点落在离屏页 child，自动切换到对应页面，避免错页触发。

实现上优先复用现有工具链：

- 列表/RecyclerView 使用（或扩展）`RecyclerViewFocusDelegate` 处理“上下键移动 + 顶部回 Tab”的边界语义。
- 对非列表内容，使用 `FocusFinder` 或统一的“寻找首个可聚焦 child”逻辑，避免焦点落在容器导致不可见。

### 3) Preference：统一默认焦点与焦点恢复策略

SettingPlayerActivity 的各页（视频/弹幕/字幕）使用 `PreferenceFragmentCompat`，其内部为 RecyclerView。为避免“从 Tab 进入内容后焦点落在不可见位置/跳转不可预期”，建议：

- 在 `BasePreferenceFragmentCompat` 或专用 delegate 中，为 TV/非触摸模式补齐：
  - 进入页面（成为当前页）时若无焦点：聚焦到首要可操作 Preference（通常为第 0 条）
  - 焦点恢复策略（**已确认**）：
    - **设置类页面**：默认启用“跨会话焦点恢复”（持久化到本地存储），以便用户下次进入仍回到上次聚焦的设置项/列表条目。
    - **非设置类页面**：默认仅做“生命周期内”恢复（同一 Activity/Fragment 生命周期内切换 Tab/返回时恢复）；如后续确有需要，再按页面逐个加入白名单放开跨会话恢复。
- focused 视觉尽量走 theme/资源覆盖，让 Preference item 背景也使用统一的 TV selector（避免单个 Preference 自定义 layout 分叉）。

**落地约束（减少复杂度）**

- 跨会话恢复仅对“设置类页面”启用，且仅在 TV UI mode + 非触摸模式下读取/写入；触控模式不抢焦点。
- 恢复目标优先使用“稳定标识”：
  - Preference 场景优先用 Preference 的 `key`；
  - 列表场景优先用 `RecyclerViewFocusDelegate.uniqueKeyProvider` 的 uniqueKey（若无则退化为 index）。
- 若持久化记录无法解析（例如条目变更/被移除），则安全回退到首要入口（第 0 条），不得导致焦点丢失或落到不可见 View。

### 4) 统一落地点：覆盖所有 TabLayout + ViewPager2 页面

优先覆盖高频入口，并采用同一协调器：

- `user_component`: `SettingPlayerActivity`、`ScanManagerActivity`
- `anime_component`: `SearchActivity`、`HomeFragment`、`AnimeDetailActivity`
- `local_component`: `BindExtraSourceActivity`

后续如发现新的 TabLayout + ViewPager2 页面，可按同一规则接入（治理一致性优先）。

## Risks / Trade-offs

- TabBackground 的引入会改变 Tab 的触控态表现：通过 `drawable-television` 与非触摸模式 gating，降低对移动端影响。
- Preference focused 样式统一可能涉及主题属性覆盖，需要回归确认不影响对话框/弹窗等使用 Preference 的场景。
- 焦点恢复策略若处理不当可能“抢焦点”：需要明确优先级（仅在页面无任何 focused child 时才做 requestFocus）。

## Validation

- 构建与静态检查：`./gradlew :app:assembleDebug`、`./gradlew lint`、`./gradlew verifyModuleDependencies`
- TV 手工回归（遥控器 DPAD）：
  - 复现并验证修复：播放器设置页从返回键向下，焦点不再不可见且不跳到非预期位置
  - Tab 切换/返回后焦点可恢复；列表顶部可返回 Tab 行；不存在错页触发
