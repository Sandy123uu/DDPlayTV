## ADDED Requirements

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

