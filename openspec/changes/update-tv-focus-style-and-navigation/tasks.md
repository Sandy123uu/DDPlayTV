# Tasks: update-tv-focus-style-and-navigation

## 1. `core_ui_component`：TV 焦点样式与焦点策略统一

- [ ] 1.1 资源：补齐/新增 Tab 行 focused selector（`drawable` + `drawable-television` 覆盖），focused 视觉统一使用 `tv_focus_*` 令牌（颜色/描边/overlay）。
- [ ] 1.2 资源：为 Preference 列表项（含 `SeekBarPreference`/`SwitchPreference`）补齐 TV focused 视觉策略（优先通过 `*-television` 资源覆盖或 TV 主题覆盖，避免逐个 Preference 自定义 layout）。
- [ ] 1.3 工具：新增 “TabLayout + ViewPager2 DPAD 焦点协调器”（建议放在 `com.xyoye.common_component.focus`）：
  - Tab 可聚焦且 focused 可见（不依赖 pressed）
  - `DPAD_DOWN`：从 Tab 行进入当前页内容时聚焦到首要可操作控件（或恢复该页上次焦点）
  - `DPAD_UP`：从内容顶部返回 Tab 行（避免焦点丢失/陷阱）
  - 内部整合 `ViewPager2DpadPageFocusSync`，避免离屏页触发错页交互
- [ ] 1.4 工具：扩展 `RecyclerViewFocusDelegate` 或新增小型 helper，支持“在列表顶部按 `DPAD_UP` 时把焦点移交给外部目标”（例如 Tab 行），以便在多页面复用同一套边界语义。

## 2. `user_component`：修复设置类 Tab 页焦点移动异常

- [ ] 2.1 `SettingPlayerActivity`：接入焦点协调器；为 Tab 行提供 focused 反馈；修复“从返回键向下焦点不可见/跳到非预期设置项”的问题。
- [ ] 2.2 `ScanManagerActivity`：接入同一焦点协调器，保证 Tab 行与内容列表的 DPAD 上下/左右行为一致。
- [ ] 2.3 相关 Preference Fragment（播放器/弹幕/字幕/扫描等）：在 TV/非触摸模式下补齐默认焦点与焦点恢复策略（与协调器策略一致）。

## 3. `anime_component`：修复 Tab/分页页面的焦点一致性

- [ ] 3.1 `SearchActivity`：Tab 行 focused 可见；从搜索栏/Tab 行进入结果列表时焦点可见且位置可预期。
- [ ] 3.2 `HomeFragment`（含 `layout-land`）：Tab 行与分页内容 DPAD 导航一致，不出现焦点不可见或跨页误触发。
- [ ] 3.3 `AnimeDetailActivity`：Tab 行 focused 可见；从 Tab 行进入内容页（列表/详情模块）时焦点可达且可返回。

## 4. `local_component`：修复绑定类 Tab 页焦点一致性

- [ ] 4.1 `BindExtraSourceActivity`：Tab 行 focused 可见；从搜索栏/Tab 行进入结果列表时焦点位置可预期；列表顶部 `DPAD_UP` 可返回 Tab 行/搜索栏。

## 5. Validation

- [ ] 5.1 编译验证：`./gradlew :app:assembleDebug`（确认尾部为 `BUILD SUCCESSFUL`）
- [ ] 5.2 静态检查：`./gradlew lint`（或按仓库约定 `lintDebug`）
- [ ] 5.3 依赖治理校验：`./gradlew verifyModuleDependencies`（确认尾部为 `BUILD SUCCESSFUL`）
- [ ] 5.4 手工验收（TV/遥控器）（需设备/遥控器，本环境未执行）：
  - “播放器设置”：返回键 → Tab 行 → 内容首项，焦点始终可见且不跳到非预期位置
  - 各 Tab 间切换后，焦点恢复到该页上次条目（或首要入口），不出现错页触发
  - 结果/列表顶部按 `DPAD_UP` 可返回 Tab 行（或上方入口），不存在焦点陷阱/丢失

