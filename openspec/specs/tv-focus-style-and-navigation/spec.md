# tv-focus-style-and-navigation Specification

## Purpose
TBD - created by archiving change update-tv-focus-style-and-navigation. Update Purpose after archive.
## Requirements
### Requirement: TV 端 Tab 行 focused 必须可见且样式统一
系统 SHALL 在 TV UI mode 且处于非触摸模式下，确保 TabLayout 的各 TabView 可被 DPAD 聚焦，并提供明确的 `state_focused` 视觉反馈；focused 的视觉样式 SHOULD 统一复用 `core_ui_component` 的 TV 焦点令牌（例如 `tv_focus_*`），不得依赖仅 `state_pressed` 的反馈。

#### Scenario: 播放器设置页 Tab focused 可见
- **GIVEN** 设备处于 TV UI mode 且处于非触摸模式
- **WHEN** 用户在“播放器设置”页面从返回键按 `DPAD_DOWN` 将焦点移动到 Tab 行
- **THEN** 当前 Tab 展示明确的 focused 反馈（可见描边/高亮）
- **AND** 用户可通过 `DPAD_LEFT/DPAD_RIGHT` 在各 Tab 间移动焦点

### Requirement: TabLayout + ViewPager2 页面 DPAD 垂直导航必须可预期且不丢焦点
系统 SHALL 在 TV UI mode 且处于非触摸模式下，为采用 “TabLayout + ViewPager2” 的页面提供一致的垂直 DPAD 导航语义：

- 当焦点位于 Tab 行且用户按 `DPAD_DOWN` 时，系统 MUST 将焦点移动到**当前页**的首要可操作控件；若该页存在页内“上次焦点”，则 SHOULD 恢复到该位置（仅在同一页面生命周期内）。
- 当焦点位于内容页的顶部边界且用户按 `DPAD_UP` 时，系统 MUST 将焦点移交回 Tab 行（或上方首要入口），避免焦点陷阱或不可见焦点。
- 在上述过程中，焦点 MUST 保持可见，不得出现“焦点存在但用户不可见”的状态。

#### Scenario: 播放器设置页从 Tab 进入内容首项
- **GIVEN** 设备处于 TV UI mode 且处于非触摸模式
- **AND** 用户当前焦点位于“播放器设置”页面的 Tab 行
- **WHEN** 用户按 `DPAD_DOWN`
- **THEN** 焦点进入当前 Tab 对应的内容页，并聚焦到首要可操作设置项（例如列表第 0 条）
- **AND** focused 反馈可见且可继续通过 `DPAD_UP/DOWN` 在设置项间移动

#### Scenario: 列表顶部按上返回 Tab 行
- **GIVEN** 设备处于 TV UI mode 且处于非触摸模式
- **AND** 用户在某个 Tab 内容页的列表中已聚焦到第 0 条（顶部）
- **WHEN** 用户按 `DPAD_UP`
- **THEN** 焦点返回到 Tab 行（或上方首要入口），且 focused 反馈可见

### Requirement: 分页内容交互必须与可见页面一致（避免离屏页误触发）
系统 SHALL 在 TV UI mode 且处于非触摸模式下，避免 ViewPager2 离屏页面导致的错页交互：

- **WHEN** 焦点落在非当前页的子 View 上
- **THEN** 系统 MUST 自动切换到该子 View 所在的页面，使“可见页面”与“可交互页面”一致

#### Scenario: 焦点落在离屏页时自动切换页面
- **GIVEN** 设备处于 TV UI mode 且处于非触摸模式
- **AND** 页面使用 ViewPager2 且存在离屏页
- **WHEN** 用户通过 DPAD 导航使焦点落在离屏页的子 View 上
- **THEN** ViewPager2 自动切换到对应页面
- **AND** 用户按 `DPAD_CENTER` 触发的动作与当前可见页面一致

### Requirement: Preference 页在 TV 模式下提供默认焦点与页内焦点恢复
系统 SHALL 在 TV UI mode 且处于非触摸模式下，为 Preference（`PreferenceFragmentCompat`）页面提供一致的焦点策略：

- 进入页面（成为当前页）时，若页面没有任何 focused child，系统 MUST 将焦点移动到首要可操作 Preference（通常为列表第 0 条）。
- 在同一 Activity/Fragment 生命周期内切换 Tab 再返回时，系统 SHOULD 恢复到该页上次聚焦的 Preference 条目（不做跨会话持久化）。

#### Scenario: Tab 切换后恢复到上次 Preference 条目
- **GIVEN** 设备处于 TV UI mode 且处于非触摸模式
- **AND** 用户在“播放器设置-视频”页聚焦过某个 Preference 条目
- **WHEN** 用户切换到其它 Tab 并再次切回“视频”页
- **THEN** 系统恢复到切换前的 Preference 条目（或等价位置）并保持 focused 可见

### Requirement: TV 焦点策略不得影响触控模式
系统 SHALL 将本能力中引入的焦点策略限制在“TV UI mode 且非触摸模式”生效；在触控模式下，系统 MUST 保持现有触控交互语义，不强制 requestFocus 或拦截 DPAD 事件导致异常。

#### Scenario: 触控模式下不抢焦点
- **GIVEN** 设备处于触控模式（`isInTouchMode == true`）
- **WHEN** 用户进入包含 TabLayout + ViewPager2 的页面并进行触控交互
- **THEN** 页面不发生额外的强制聚焦/跳焦点行为，触控交互保持与现状一致

