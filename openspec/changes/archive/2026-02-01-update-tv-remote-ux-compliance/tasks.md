## 1. `core_ui_component`：TV focused 视觉基线

- [x] 1.1 新增 `core_ui_component/src/main/res/drawable-television/background_item_press.xml`：补齐 `state_focused`（描边/overlay），默认态与现有一致。
- [x] 1.2 新增 `core_ui_component/src/main/res/drawable-television/background_item_press_corner.xml`：补齐 `state_focused`（圆角与原始保持一致）。
- [x] 1.3 （可选）评估是否需要覆盖 `background_item_press_corner_40dp.xml`：当前未发现实际使用点，暂不覆盖。
- [x] 1.4 focused 样式统一使用 `@color/tv_focus_stroke_color` / `@color/tv_focus_overlay_color` / `@dimen/tv_focus_stroke_width`，避免模块内自定义分叉。

## 2. `local_component`：媒体库首页 DPAD 可达 + 默认焦点

- [x] 2.1 调整 `local_component/src/main/res/layout/item_media_library.xml`：可点击目标 `item_layout` 增加 `focusable/clickable` 与 `@string/focusable_item` tag。
- [x] 2.2 调整 `local_component/src/main/java/com/xyoye/local_component/ui/fragment/media/MediaFragment.kt`：为 `mediaLibRv` 接入 `RecyclerViewFocusDelegate`（onResume/onPause 保存/恢复；首次进入默认焦点策略）。
- [x] 2.3 （如需要）为 `fragment_media.xml` 的 `add_media_storage_bt` 补齐 TV 可达策略：已在列表上支持 `MENU/SETTINGS` 键触发“新增媒体库”入口（避免 FAB 不可达）。

## 3. `user_component`：个人中心 DPAD 可达 + 默认焦点

- [x] 3.1 调整 `user_component/src/main/res/layout/fragment_personal.xml`：所有可点击入口容器增加 `focusable/clickable` 与 `@string/focusable_item` tag。
- [x] 3.2 调整 `user_component/src/main/res/layout-land/fragment_personal.xml`：同上（本次以补齐 focusability 为主；如后续发现 DPAD 死角再补充 `nextFocus*`）。
- [x] 3.3 调整 `user_component/src/main/java/com/xyoye/user_component/ui/fragment/personal/PersonalFragment.kt`：在非触摸模式下进入/返回页面若无焦点，requestFocus 到首要入口；记录/恢复上次焦点。

## 4. `anime_component`：搜索页关键控件与列表项 DPAD 可达

- [x] 4.1 调整 `anime_component/src/main/res/layout/activity_search.xml`：确保返回按钮/搜索按钮可聚焦且有 focused 反馈（避免焦点落在不可操作容器）。
- [x] 4.2 调整 `anime_component/src/main/java/com/xyoye/anime_component/ui/activities/search/SearchActivity.kt`：在 TV/非触摸模式下明确默认焦点策略（避免自动弹键盘导致焦点陷阱）。
- [x] 4.3 调整 `anime_component/src/main/res/layout/item_search_magnet.xml`：将 `content_view` 作为可聚焦点击目标（`focusable/clickable/tag`，并使用 `background_item_press` 获得 focused 反馈）。
- [x] 4.4 调整 `anime_component/src/main/java/com/xyoye/anime_component/ui/fragment/search_magnet/SearchMagnetFragment.kt`：接入 `RecyclerViewFocusDelegate`（焦点保存/恢复、上下键一致性）。

## 5. 验证

- [x] 5.1 编译验证：`./gradlew :app:assembleDebug`（确认输出尾部为 `BUILD SUCCESSFUL`）
- [x] 5.2 静态检查：`./gradlew lint`（或按仓库约定运行 `lintDebug`）
- [x] 5.3 依赖治理校验：`./gradlew verifyModuleDependencies`（确认输出尾部为 `BUILD SUCCESSFUL`）
- [ ] 5.4 手工验收（TV/遥控器）（需设备/遥控器，本环境未执行）：
  - 从 `TvMainActivity` 进入媒体库/个人中心/搜索/设置：均有默认焦点且 focused 反馈可见
  - 列表可 DPAD 上下移动；进入子页面再返回能恢复焦点（至少媒体库与磁力搜索结果）
  - 不存在“焦点陷阱/焦点丢失/隐藏视图抢焦点”
