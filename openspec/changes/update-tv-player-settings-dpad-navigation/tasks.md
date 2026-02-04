# Tasks: update-tv-player-settings-dpad-navigation

## 1. `core_ui_component`：为 Tab/分页协调器增加设置页模式

- [x] 1.1 扩展 `TabLayoutViewPager2DpadFocusCoordinator`：增加“设置页模式（Tab 指示器 + 内容左右切页）”开关，默认行为保持不变。
- [x] 1.2 在设置页模式下：`DPAD_LEFT/DPAD_RIGHT` 仅在事件未被控件消费时触发切页；切页后调用统一的“聚焦当前页首项/恢复上次焦点”逻辑，避免焦点落到容器。
- [x] 1.3 增加/暴露一个公开方法（例如 `requestContentFocus()`），用于页面在 toolbar/返回键处触发“进入内容首项”。

## 2. `user_component`：播放器设置页接入设置页模式

- [x] 2.1 `SettingPlayerActivity`：TV/非触摸模式下启用“设置页模式”，并禁用 Tab 行聚焦（仅显示选中态）。
- [x] 2.2 `SettingPlayerActivity`：在 toolbar 返回键（或 toolbar 容器）处理 `DPAD_DOWN`，调用协调器的 `requestContentFocus()` 将焦点进入当前页配置首项。
- [ ] 2.3 手工回归：视频/弹幕/字幕任意页面内，左右切页后焦点可见且可继续上下浏览；顶部按上可返回到返回键，不出现跳到其它 Tab 的情况。（需 TV 设备）

## 3. Validation

- [x] 3.1 编译验证：`./gradlew :app:assembleDebug`（确认尾部为 `BUILD SUCCESSFUL`）
- [x] 3.2 静态检查：`./gradlew lint`（或按仓库约定 `lintDebug`）
- [x] 3.3 依赖治理校验：`./gradlew verifyModuleDependencies`（确认尾部为 `BUILD SUCCESSFUL`）
- [ ] 3.4 手工验收（TV/遥控器，需设备）：播放器设置页 DPAD 导航符合本提案与 `AGENTS.md` 的 TV 约束（可达/可见/可反馈/可返回）。
