## Context

工程已引入通用的 `TabLayoutViewPager2DpadFocusCoordinator`，并在 TV/非触摸模式下提供：

- Tab 行可聚焦、focused 可见
- `DPAD_DOWN`：从 Tab 行进入内容（首项/恢复上次焦点）
- `DPAD_UP`：从内容顶部返回 Tab 行
- 离屏页焦点同步（避免错页触发）

该策略对“搜索/详情/列表”等 Tab 页是合理的，但对“播放器设置（Preference 列表）”类页面存在明显 UX 负担：

- Tab 行参与焦点会引入额外的“可聚焦层级”，用户在内容中进行上下浏览时更容易误落到 Tab 行。
- 在 Tab 行上进行左右移动会导致“误切页”，用户主观上感知为“焦点跳来跳去”。
- 设置页的主任务是连续浏览并调整配置项，更符合“内容为主、标题为指示”的导航模型。

## Goals / Non-Goals

### Goals

- TV/非触摸模式下，“播放器设置”页面的 DPAD 交互逻辑统一且可预期：
  - Tab 行仅作“选中指示”，不参与焦点。
  - 在配置项列表内按 `DPAD_LEFT/DPAD_RIGHT` 切换页面（视频/弹幕/字幕）。
  - 切页后焦点落在该页可操作项（恢复上次焦点，否则首项），焦点始终可见。
- 不影响触控模式：移动端与 TV 触控模式下仍保持原有 Tab 点击/滑动语义，不抢焦点。

### Non-Goals

- 不将该模式推广到所有 Tab 页（搜索/详情等仍保持 Tab 可聚焦策略）。
- 不重写 Preference 框架或大规模复制 layout。

## Decisions

### 1) 为 Tab/ViewPager2 焦点协调器增加“设置页模式”

在 `core_ui_component` 的协调器中引入模式开关（示例：`mode = TabDpadMode.Default | TabDpadMode.SettingsIndicatorOnly`）：

- `Default`：保持现有行为（Tab 可聚焦、上下进入/返回 Tab）。
- `SettingsIndicatorOnly`（新增）：
  - TV/非触摸模式下将 TabView 设为不可聚焦（仍保持选中态/指示器展示）。
  - 当焦点位于内容页时，监听 `DPAD_LEFT/DPAD_RIGHT`：
    - 若当前控件未消费（例如不是 SeekBar/步进控件），则切换到相邻页面。
    - 切换后调用“聚焦当前页内容首项/恢复上次焦点”的统一逻辑，避免焦点落到容器或不可见位置。

这样做的好处是：不引入新的页面级临时逻辑，仍沿用现有的“一个 Coordinator 覆盖 Tab+分页”模式，便于治理与复用。

### 2) “返回键 → 内容首项”的入口路径由页面显式触发

由于 Tab 行在设置页模式下不可聚焦，`DPAD_DOWN` 从 Toolbar 返回键向下的默认 focusSearch 可能会落到 ViewPager2 容器而非首项。

因此在 `SettingPlayerActivity` 里增加一个小的 DPAD 入口桥接：当返回键（或 toolbar 容器）收到 `DPAD_DOWN` 时，显式调用协调器的“请求聚焦当前页内容”方法。

## Risks / Trade-offs

- `DPAD_LEFT/DPAD_RIGHT` 与少数设置控件的“左右调节”冲突：
  - 约束：优先让控件本身消费；仅在事件未被消费时切页。
  - 需要以“实际控件类型/可聚焦子 View”做判断，避免影响现有 Preference 交互。
- 该模式会让“切页”从显式 Tab 行变为隐式方向键，需要确保 UI 上仍清晰表达当前页（Tab 选中态/指示器必须明显）。

## Migration Plan

1. `core_ui_component`：为协调器增加模式开关与内容左右切页逻辑（默认不变）。
2. `user_component`：仅在“播放器设置”页启用设置页模式，并补齐 toolbar 的 `DPAD_DOWN` → 内容首项逻辑。
3. TV 回归：验证视频/弹幕/字幕三页的上下浏览、左右切页、返回路径均可达且焦点可见。

## Open Questions

- 是否需要将同样的“设置页模式”扩展到 `ScanManagerActivity` 等其它“设置类 Tab 页”？（本变更默认仅覆盖“播放器设置”。）

