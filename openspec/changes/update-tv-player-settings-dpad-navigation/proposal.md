# Change: TV 端“播放器设置”改为内容左右切页（Tab 仅作指示）

## Why

当前“播放器设置”（视频/弹幕/字幕）在 TV/遥控器（DPAD）场景下的交互体验不佳：焦点容易在 Tab 行与内容列表之间跳转，用户在内容中按方向键时会误触发 Tab 切换或落到非预期区域，造成“焦点不稳定/不可控”的主观体验。

现有 `update-tv-focus-style-and-navigation` 变更提供了通用的“Tab 可聚焦 + 上下进入/返回 Tab”的策略，适用于搜索/详情等 Tab 页面；但对 **设置类（Preference 列表）** 页面来说，“Tab 可聚焦”反而增加了导航层级与误操作概率。

因此需要为“播放器设置”引入更符合 TV 设置页直觉的导航：**Tab 仅作选中指示，不参与焦点；在配置项列表内通过 `DPAD_LEFT/DPAD_RIGHT` 切换 视频/弹幕/字幕 页面。**

## What Changes

- 为 `TabLayout + ViewPager2` 的 TV 焦点协调器补齐一个“设置页模式”：
  - Tab 行在 TV/非触摸模式下**不可聚焦**（仅展示选中态/指示器）。
  - 当焦点位于内容列表（Preference 列表）时，按 `DPAD_LEFT/DPAD_RIGHT` 切换到相邻页面，并在切换后将焦点落到该页的可操作配置项（优先恢复该页上次焦点，否则聚焦首项）。
  - 不干扰触控模式与移动端：触控模式下 Tab 仍可点击/滑动切页，且不新增抢焦点行为。
- 将该“设置页模式”应用到 `user_component` 的“播放器设置”（视频/弹幕/字幕）页面，统一其 DPAD 交互逻辑。

## Capabilities

### Related Capabilities / Dependencies

- `tv-focus-style-and-navigation`：本变更以“新增 Requirement”的方式扩展其在 **设置类 Tab 页** 的导航语义。

## Impact

- 影响模块：
  - `core_ui_component`：扩展 Tab/ViewPager2 的 TV 焦点协调器，增加“设置页模式”。
  - `user_component`：`SettingPlayerActivity` 接入设置页模式，并调整“返回键 → 内容首项”等 DPAD 入口行为。
- 风险：
  - `DPAD_LEFT/DPAD_RIGHT` 可能与部分设置项的“左右调节”（如 SeekBar/步进项）冲突，需要在实现中明确优先级（优先让控件消费；仅在控件未消费时切页）。

## Out of Scope

- 不修改搜索/详情/绑定等非设置类 Tab 页面（这些页面继续使用 Tab 可聚焦的通用策略）。
- 不引入 TV 专属的设置页布局复制；优先在现有结构上通过焦点策略统一实现。

