# tv-remote-ux-baseline Specification

## Purpose
TBD - created by archiving change update-tv-remote-ux-compliance. Update Purpose after archive.
## Requirements
### Requirement: TV 模式下可操作控件必须可聚焦且 focused 可见

系统 SHALL 在 TV UI mode（且为非触摸模式）下确保所有可操作控件可被 DPAD 聚焦，并提供明确的 `state_focused` 视觉反馈（例如描边/高亮），不得仅依赖 `state_pressed`。

#### Scenario: 媒体库列表项聚焦时有明确 focused 反馈

- **GIVEN** 设备处于 TV UI mode 且处于非触摸模式
- **WHEN** 用户在“媒体库首页”通过 DPAD 导航到任一媒体库条目
- **THEN** 条目展示明确的 focused 反馈（可见描边/高亮）
- **AND** 用户按 DPAD_CENTER 可触发进入该媒体库

### Requirement: TV 模式进入页面必须存在默认焦点

系统 SHALL 在 TV UI mode（且为非触摸模式）下进入页面时保证存在明确的默认焦点；若当前页面根视图无 focused child，系统应将焦点移动到该页面的首要入口，避免出现“无焦点/焦点落在不可见 View”。

#### Scenario: 进入个人中心页面后默认聚焦到首要入口

- **GIVEN** 设备处于 TV UI mode 且处于非触摸模式
- **WHEN** 用户从 `TvMainActivity` 进入“个人中心”页面
- **THEN** 页面存在明确的默认焦点（例如账号卡片或第一个功能入口）
- **AND** focused 反馈可见且 DPAD 可继续导航

### Requirement: 列表页在 TV 模式下支持焦点保存与恢复

系统 SHALL 在 TV UI mode（且为非触摸模式）下对关键列表页提供焦点保存与恢复能力：当用户进入子页面并返回或页面短暂切到后台再恢复时，列表应尽量恢复到上次聚焦条目，避免焦点丢失或回到不可预期位置。

#### Scenario: 磁力搜索结果返回后焦点恢复到上次条目

- **GIVEN** 设备处于 TV UI mode 且处于非触摸模式
- **AND** 用户在“搜资源（磁力搜索）”结果列表中已聚焦某个条目
- **WHEN** 用户进入子页面（或触发页面切换）后返回结果列表
- **THEN** 系统恢复到返回前的聚焦条目（或等价位置）并保持 focused 可见

### Requirement: TV 端 focused 视觉样式必须统一使用 `tv_focus_*`（overlay + stroke）

系统 SHALL 在 TV UI mode 且处于非触摸模式下，将所有可操作控件的 `state_focused` 视觉反馈统一为“overlay + stroke”结构，并且仅使用 `core_ui_component` 提供的 TV 焦点令牌：

- `@color/tv_focus_overlay_color`
- `@color/tv_focus_stroke_color`
- `@dimen/tv_focus_stroke_width`

系统 MUST 避免在 focused 反馈中使用硬编码颜色/线宽（例如 `#...`、`3dp`）；TV focused 的视觉差异 SHOULD 优先通过 `drawable-television/` 资源覆盖实现，以避免影响移动端触控视觉。

同时，当控件处于 `selected + focused` 组合态时，系统 MUST 保证 focused 反馈仍然可见：应在 selected 底色之上叠加同样的 focused overlay+stroke，以确保“选中语义”与“焦点语义”同时可读。

#### Scenario: 不同模块的 focused 反馈强度一致

- **GIVEN** 设备处于 TV UI mode 且处于非触摸模式
- **WHEN** 用户分别在 `TvMainActivity` 主导航条目、播放器设置项、存储页按钮上移动焦点
- **THEN** 三者均展示一致的 focused 反馈：半透明 overlay + 白色描边
- **AND** 白色描边线宽一致（统一使用 `tv_focus_stroke_width`）

#### Scenario: selected+focused 同时表达选中与焦点

- **GIVEN** 设备处于 TV UI mode 且处于非触摸模式
- **AND** 某列表条目存在“selected（业务选中）”状态
- **WHEN** 用户将焦点移动到该 selected 条目
- **THEN** 条目既保持 selected 的底色/语义
- **AND** 同时叠加 focused 的 overlay+stroke，使焦点反馈可见且与未 selected 的 focused 强度一致

