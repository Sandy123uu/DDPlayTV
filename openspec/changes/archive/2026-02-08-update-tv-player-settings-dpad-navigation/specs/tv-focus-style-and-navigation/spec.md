## ADDED Requirements

### Requirement: TV 设置类 Tab 页支持“Tab 指示器 + 内容左右切页”模式

系统 SHALL 在 TV UI mode 且处于非触摸模式下，为“设置类（Preference 列表）”的 `TabLayout + ViewPager2` 页面提供一种可选导航模式：

- Tab 行仅用于展示“当前页指示”（选中态/指示器），MUST NOT 成为 DPAD 的可聚焦目标；
- 当焦点位于内容列表时，用户按 `DPAD_LEFT/DPAD_RIGHT` MUST 切换到相邻页面；
- 切换页面后，系统 MUST 将焦点落到该页的可操作配置项（优先恢复该页上次焦点，否则聚焦首项），且 focused 反馈 MUST 可见；
- 该模式 MUST 仅在 TV UI mode 且非触摸模式下生效；触控模式下 MUST 保持现有触控语义（Tab 可点击/可滑动）且不新增抢焦点行为。

#### Scenario: 播放器设置页的 Tab 行不参与焦点
- **GIVEN** 设备处于 TV UI mode 且处于非触摸模式
- **AND** 用户在“播放器设置”页面的配置项列表中聚焦到任意条目
- **WHEN** 用户按 `DPAD_UP` 返回到页面顶部入口
- **THEN** 焦点返回到工具栏返回键（或上方首要入口）
- **AND** 焦点不会落在“视频/弹幕/字幕”Tab 行上

#### Scenario: 在配置项中按左右切换页面并保持焦点可见
- **GIVEN** 设备处于 TV UI mode 且处于非触摸模式
- **AND** 用户在“播放器设置-弹幕”页的配置项列表中已聚焦到某个条目
- **WHEN** 用户按 `DPAD_LEFT`
- **THEN** 页面切换到“播放器设置-视频”（相邻页）
- **AND** 焦点落在“视频”页的可操作配置项（恢复上次焦点或首项），且 focused 反馈可见

#### Scenario: 触控模式下不启用该 DPAD 切页策略
- **GIVEN** 页面处于触控模式（`isInTouchMode == true`）
- **WHEN** 用户点击 Tab 或滑动 ViewPager2 切换页面
- **THEN** 页面切换行为与现状一致
- **AND** 系统不会强制 requestFocus 或拦截方向键导致异常交互

