# Tasks: update-tv-focus-style-unification

## 1. Audit（输出不一致清单）

- [x] 1.1 通过 `rg -n \"state_focused\" --glob '!openspec/**' --glob '**/src/main/res/**' .` 汇总全工程含 focused 的 drawable/selector，并按模块归类（详见 `audit.md`）。
- [x] 1.2 对清单逐项标注：是否 `drawable-television` 覆盖、是否使用 `tv_focus_*`、是否 overlay+stroke、是否存在 hardcode（颜色/线宽）、是否覆盖 `selected+focused`（详见 `audit.md`）。
- [x] 1.3 明确最终统一口径：本次 **仅统一 `state_focused`**（`pressed/selected` 不做全工程统一，仅必要收敛 hardcode/遮盖 focused 的场景）。

## 2. `core_ui_component`：通用 selector 的 focused 统一

- [x] 2.1 将 `drawable-television/background_storage_item_selector.xml` 的 focused 分支收敛为“base + overlay+stroke”模板（使用 `tv_focus_*`）。
- [x] 2.2 将 `drawable-television/background_storage_item_icon_selector.xml` 的 focused 分支收敛为 overlay+stroke（使用 `tv_focus_*`），避免使用 `theme_light` 作为 focused 主表达。
- [x] 2.3 将 `drawable-television/background_storage_path_selector.xml` 的 focused 分支收敛为“base + overlay+stroke”模板（使用 `tv_focus_*`）。
- [x] 2.4 本次不做全工程 pressed 统一（保持现状；仅对存在 hardcode 的 pressed 做必要收敛）。

## 3. `app`：TV 主导航 item focused 可见且一致

- [x] 3.1 更新 `app/src/main/res/drawable-television/background_tv_main_nav_item.xml`：
  - focused：使用 overlay+stroke（`tv_focus_*`），确保在透明底上可见
  - selected：保持“选中但未 focused”为弱表达（可选 `tv_unfocused_*`），并避免覆盖 focused

## 4. `player_component`：播放器控件 focused/selected_focused 统一

- [x] 4.1 更新 `player_component/src/main/res/drawable-television/background_player_setting_text.xml`、`background_player_setting_text_transparent.xml`：focused/selected_focused 统一为 overlay+stroke（保持圆角）。
- [x] 4.2 更新 `player_component/src/main/res/drawable-television/background_player_setting_item.xml`：focused/selected_focused 统一为“base + overlay+stroke”（保持胶囊形状）。
- [x] 4.3 更新 `player_component/src/main/res/drawable-television/background_video_source_item.xml`：selected+focused 统一为“selected base + overlay+stroke”，避免仅描边或仅底色导致语义不清。
- [x] 4.4 复核 `player_component/src/main/res/drawable-television/background_send_danmu_bt.xml`：focused/pressed 分支去硬编码（仅保留必要的 normal 底色差异）。

## 5. `storage_component`：按钮/开关类 focused 线宽与 token 化

- [x] 5.1 将 `storage_component/src/main/res/drawable/background_button_corner_*_focused.xml` 的 stroke width 从硬编码（如 `3dp`）收敛为 `@dimen/tv_focus_stroke_width`，并统一 overlay/stroke 使用 `tv_focus_*`。
- [x] 5.2 更新 `storage_component/src/main/res/drawable-television/background_screencast_switch_button.xml`：focused 与 selected+focused 统一为“base + overlay+stroke”。
- [x] 5.3 补齐 `drawable-television` 覆盖（并移除移动端版本的 focused 分支）以隔离 TV focused 逻辑对移动端的潜在影响（详见 `audit.md`）。

## 6. `anime_component`：自定义条目背景对齐

- [x] 6.1 复核 `anime_component/src/main/res/drawable-television/background_c3_stoke_gray.xml`：确认 focused 分支完全遵循 token 化 overlay+stroke；如存在 hardcode 则收敛。

## 7. Validation

- [x] 7.1 编译验证：`./gradlew :app:assembleDebug`（确认尾部为 `BUILD SUCCESSFUL`）
- [x] 7.2 静态检查：`./gradlew lint`（或按仓库约定 `lintDebug`，确认尾部为 `BUILD SUCCESSFUL`）
- [x] 7.3 依赖治理校验：`./gradlew verifyModuleDependencies`（确认尾部为 `BUILD SUCCESSFUL`）
- [x] 7.4 手工验收（TV/遥控器）（需设备，本环境未执行；建议按下列清单回归）：
  - 主导航 item、存储按钮、播放器设置项、播放源条目：focused 反馈强度一致（overlay+stroke + 2dp 白描边）
  - selected+focused 语义清晰：既能看出“选中”，也能看出“当前焦点”
