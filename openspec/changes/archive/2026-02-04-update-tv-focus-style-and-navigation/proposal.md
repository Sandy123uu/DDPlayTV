# Change: 统一 TV 端焦点样式并修复 Tab/分页场景焦点移动异常

## Why

仓库以 TV 端（遥控器 DPAD）交互为先，但目前仍存在大量“焦点样式不统一、焦点移动异常/不可见”的问题，影响可达性与可用性。

典型问题（示例）：

- **焦点样式不统一**：同为可操作控件，在不同页面/控件类型下 `state_focused` 的反馈表现不一致（有的有描边/高亮，有的只有 pressed ripple，甚至完全无反馈）。
- **焦点移动异常**：在“TabLayout + ViewPager2”类页面中，焦点从工具栏（返回）向下移动时可能出现：
  - 焦点落在 Tab 上但**不可见**（用户误以为焦点丢失）
  - 继续向下后焦点直接跳到列表/设置项的**非预期位置**（例如跳到当前页的靠后条目）
  - 极端情况下出现“看见的是 A 页，按确定却触发 B 页”的错页交互风险（与 ViewPager2 离屏页面仍在层级有关）

这些问题与 `AGENTS.md` 中 TV/Remote UX 强约束（焦点可达、可见、可反馈、可返回）不一致，需要以“统一样式 + 统一焦点策略”的方式系统性修复，而非逐页打补丁。

## What Changes

- **视觉统一**：在 `core_ui_component` 定义/补齐 TV 端通用的焦点视觉“设计令牌”（`tv_focus_*`）与可复用 selector（尤其覆盖 Tab/Preference 等常见控件），确保 focused 反馈在全工程一致。
- **行为统一**：在 `core_ui_component` 增加可复用的“TabLayout + ViewPager2 DPAD 焦点协调器”（Focus Coordinator）：
  - Tab 行可聚焦且 focused 可见
  - `DPAD_DOWN` 从 Tab 行进入当前页内容时，聚焦到**当前页**的首要可操作控件（或恢复到该页上次聚焦位置）
  - `DPAD_UP` 从内容顶部返回 Tab 行（避免焦点陷阱/不可见焦点）
  - 复用/整合现有 `ViewPager2DpadPageFocusSync`，避免离屏页触发错页操作
- **落地修复**：将上述统一策略应用到工程中所有“TabLayout + ViewPager2”页面（优先覆盖设置/搜索/绑定/详情等高频入口），并补齐相关 Preference 页的默认焦点与焦点恢复策略。

## Capabilities

### New Capabilities

- `tv-focus-style-and-navigation`：TV 端焦点样式与 Tab/分页场景的 DPAD 焦点导航一致性规范。

### Related Capabilities / Dependencies

- `update-tv-remote-ux-compliance` 变更中已建立的 `tv-remote-ux-baseline`（focused 视觉基线、默认焦点、列表焦点恢复）作为前置与基础；本变更在其基础上覆盖“Tab/分页 + Preference”等复杂组合场景。

## Impact

- 影响模块（预期）：
  - `core_ui_component`：TV focused 资源与 Focus Coordinator 工具链
  - `user_component`：播放器设置/扫描管理等 Tab 页
  - `anime_component`：搜索页/首页/详情页等 Tab 页
  - `local_component`：绑定弹幕/字幕等 Tab 页
- 风险与代价：
  - Tab/Preference 的 focused 样式覆盖范围广，需要一次 TV 端回归确认视觉与可达性。
  - 焦点协调器会改变部分页面在 TV 端的默认焦点/焦点恢复行为，需要明确并统一策略以避免“抢焦点/跳焦点”。

## Out of Scope

- 不在本次将所有页面重构为 Leanback 或 TV 专用布局；优先以统一策略修复现有页面的可用性问题。
- 不在本次引入全新的复杂 UI 框架；复用并扩展现有 `FocusPolicy`/`RecyclerViewFocusDelegate` 等基建。

## Open Questions（需确认）

1) “进入 Tab 页”时的默认焦点策略：默认停留在“返回键”，还是默认进入“Tab 行/内容首项”？（本提案默认：进入页面焦点在返回键，向下依次到 Tab 行 → 内容首项）
2) “再次进入同一页面”是否需要跨进程/跨会话恢复上次焦点位置？（本提案默认：仅在同一 Activity/Fragment 生命周期内恢复；不做跨进程持久化）

