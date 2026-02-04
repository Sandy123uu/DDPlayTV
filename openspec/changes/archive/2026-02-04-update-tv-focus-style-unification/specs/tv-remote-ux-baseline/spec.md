## ADDED Requirements

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

