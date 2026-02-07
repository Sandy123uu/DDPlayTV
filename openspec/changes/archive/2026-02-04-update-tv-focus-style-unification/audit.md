# 审计：TV 端 `state_focused` 资源清单与不一致项

## 统一口径（结论）

- 本次变更 **仅统一 `state_focused`** 的视觉结构为 **overlay + stroke（token 化）**。
- `pressed/selected` 不做全工程统一；仅在 **存在 hardcode** 或 **遮盖 focused 可见性** 时做必要收敛：
  - pressed 如需 overlay，优先使用 `@color/tv_pressed_overlay_color`
  - selected（未 focused）保持弱表达，避免覆盖 focused（必要时可用 `tv_unfocused_*`）

## 清单与分类（来自 `rg -n "state_focused" --glob '!openspec/**' --glob '**/src/main/res/**' .`）

> 字段说明：  
> - **TV 覆盖**：是否位于 `drawable-television/`（或将补齐 TV 覆盖）  
> - **token 化**：focused 是否仅引用 `tv_focus_*` / `tv_pressed_overlay_color` 等令牌（无 `#...`/`3dp`）  
> - **overlay+stroke**：focused 是否为 `tv_focus_overlay_color` + `tv_focus_stroke_color/tv_focus_stroke_width` 叠加结构  

### A. 含 `state_focused` 的资源（selector / layer-list）

| 模块 | 文件 | TV 覆盖 | token 化（focused） | overlay+stroke（focused） | selected+focused | 结论/备注 |
| --- | --- | --- | --- | --- | --- | --- |
| core_ui_component | `core_ui_component/src/main/res/drawable-television/background_storage_item_icon_selector.xml` | 是 | 是 | 是 | N/A | 已收敛为 overlay+stroke |
| core_ui_component | `core_ui_component/src/main/res/drawable-television/background_storage_item_selector.xml` | 是 | 是 | 是 | N/A | 已收敛为 base + overlay+stroke |
| core_ui_component | `core_ui_component/src/main/res/drawable-television/background_storage_path_selector.xml` | 是 | 是 | 是 | N/A | 已收敛为 base + overlay+stroke |
| core_ui_component | `core_ui_component/src/main/res/drawable-television/background_item_press.xml` | 是 | 是 | 是 | N/A | 保持（focused 已为 base + overlay+stroke） |
| core_ui_component | `core_ui_component/src/main/res/drawable-television/background_item_press_corner.xml` | 是 | 是 | 是 | N/A | 保持（focused 已为 base + overlay+stroke） |
| core_ui_component | `core_ui_component/src/main/res/drawable-television/background_focus_overlay.xml` | 是 | 是 | 是 | N/A | 保持 |
| core_ui_component | `core_ui_component/src/main/res/drawable-television/background_tab_press.xml` | 是 | 是 | 是 | N/A | 保持 |
| app | `app/src/main/res/drawable-television/background_tv_main_nav_item.xml` | 是 | 是 | 是 | 是 | 补齐 selected+focused，focused 在透明底上可见 |
| player_component | `player_component/src/main/res/drawable-television/background_video_source_item.xml` | 是 | 是 | 是 | 是 | selected+focused 已收敛为 selected base + overlay+stroke；normal 的 `1dp` 为非 focused 边框（保留） |
| player_component | `player_component/src/main/res/drawable-television/selector_player_setting_item_bg.xml` | 是 | 是（经引用） | 是（经引用） | 是 | 通过 `drawable-television/shape_player_setting_item_bg_*` 统一 focused/selected_focused |
| player_component | `player_component/src/main/res/drawable-television/background_player_setting_text.xml` | 是 | 是（经引用） | 是（经引用） | 是 | 通过 `drawable-television/background_player_setting_text_*` 统一 focused/selected_focused |
| player_component | `player_component/src/main/res/drawable-television/background_player_setting_text_transparent.xml` | 是 | 是（经引用） | 是（经引用） | 是 | 同上（透明底版本） |
| player_component | `player_component/src/main/res/drawable-television/background_player_setting_item.xml` | 是 | 是（经引用） | 是（经引用） | 是 | 通过 `drawable-television/background_player_setting_item_*` 统一 focused/selected_focused |
| player_component | `player_component/src/main/res/drawable-television/background_video_action_item.xml` | 是 | 是（经引用） | 是（经引用） | N/A | focused 使用 `background_video_action_item_focused_color`（overlay+stroke） |
| player_component | `player_component/src/main/res/drawable-television/background_send_danmu_bt.xml` | 是 | 是 | 是 | N/A | 去除 `#...` hardcode；focused/pressed 均 token 化 |
| player_component | `player_component/src/main/res/drawable-television/background_player_action.xml` | 是 | 是（经引用） | 是（经引用） | N/A | `background_tv_focus_circle` 已提供 TV 覆盖为 base + overlay+stroke |
| player_component | `player_component/src/main/res/drawable/selector_player_setting_item_bg.xml` | 否 | 部分 | 否 | 是 | 非 TV 资源，保持现状（TV 端已通过 `drawable-television/` 覆盖统一） |
| player_component | `player_component/src/main/res/drawable/ic_subtitle_setting.xml` | 否 | N/A | N/A | N/A | 仅包含 `state_focused=\"false\"` 的 icon 选择逻辑，无需统一 |
| storage_component | `storage_component/src/main/res/drawable-television/background_screencast_switch_button.xml` | 是 | 是 | 是 | 是 | focused/selected+focused 已收敛为 base + overlay+stroke |
| storage_component | `storage_component/src/main/res/drawable-television/background_button_corner_gray.xml` | 是 | 是（经引用） | 是（经引用） | N/A | 补齐 TV 覆盖；移动端版本移除 focused 分支以隔离差异 |
| storage_component | `storage_component/src/main/res/drawable-television/background_button_corner_state.xml` | 是 | 是（经引用） | 是（经引用） | 是 | 补齐 TV 覆盖；移动端版本移除 focused+selected 分支以隔离差异 |
| storage_component | `storage_component/src/main/res/drawable-television/background_button_corner_switch.xml` | 是 | 是（经引用） | 是（经引用） | 是 | 补齐 TV 覆盖；移动端版本移除 focused 分支以隔离差异 |
| storage_component | `storage_component/src/main/res/drawable-television/selector_player_kernel_bg.xml` | 是 | 是（经引用） | 是（经引用） | 是 | 补齐 TV 覆盖；移动端版本移除 focused 分支以隔离差异 |
| anime_component | `anime_component/src/main/res/drawable-television/background_c3_stoke_gray.xml` | 是 | 是 | 是 | N/A | focused 已满足 overlay+stroke；base 的 `0.8dp` 边框为非 focused 表达（保留） |

### B. Focus 相关 shape（被引用，决定 focused 视觉；无 `state_focused`）

| 模块 | 文件 | token 化 | overlay+stroke | 结论/备注 |
| --- | --- | --- | --- | --- |
| storage_component | `storage_component/src/main/res/drawable/background_button_corner_blue_focused.xml` | 是 | 是 | focused stroke width 收敛为 `@dimen/tv_focus_stroke_width` |
| storage_component | `storage_component/src/main/res/drawable/background_button_corner_gray_focused.xml` | 是 | 是 | 同上 |
| storage_component | `storage_component/src/main/res/drawable/background_button_corner_red_focused.xml` | 是 | 是 | 同上 |
| player_component | `player_component/src/main/res/drawable-television/background_player_setting_text_focused.xml` | 是 | 是 | TV 专用 focused shape（圆角保持） |
| player_component | `player_component/src/main/res/drawable-television/background_player_setting_text_selected_focused.xml` | 是 | 是 | TV 专用 selected base + overlay+stroke |
| player_component | `player_component/src/main/res/drawable-television/background_player_setting_item_focused.xml` | 是 | 是 | TV 专用 focused（胶囊形状保持） |
| player_component | `player_component/src/main/res/drawable-television/background_player_setting_item_selected_focused.xml` | 是 | 是 | TV 专用 selected base + overlay+stroke |
| player_component | `player_component/src/main/res/drawable-television/shape_player_setting_item_bg_focused.xml` | 是 | 是 | TV 专用 focused（16dp 圆角保持） |
| player_component | `player_component/src/main/res/drawable-television/shape_player_setting_item_bg_selected_focused.xml` | 是 | 是 | TV 专用 selected base + overlay+stroke |
| player_component | `player_component/src/main/res/drawable-television/background_tv_focus_circle.xml` | 是 | 是 | TV 专用圆形 focused：base + overlay+stroke |
