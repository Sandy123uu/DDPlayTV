# Change: 统一 TV 端焦点视觉样式（以 overlay + stroke 为标准）

## Why

当前工程的 TV 端（DPAD）存在明显的“焦点视觉不统一”问题：不同模块/控件对 `state_focused` 的表达方式不一致（线宽不一致、是否叠加 overlay 不一致、部分使用硬编码颜色/尺寸），导致用户的焦点反馈强度与语义在页面间跳变，也增加了后续维护成本（每个模块各写一套 selector/shape）。

仓库已有 TV 端焦点基线与令牌（`tv_focus_*`）以及若干在途变更（例如 `update-tv-focus-style-and-navigation` 聚焦 Tab/分页场景）。但目前在“自定义背景/按钮/播放器控件”等场景仍存在残留差异，需要一次“以同一套标准模板收敛”的统一化修复。

## What Changes

- **统一标准**：以 `core_ui_component` 的 `tv_focus_*` 令牌为唯一来源，统一 `state_focused` 的视觉结构为：
  - **overlay**：`@color/tv_focus_overlay_color`
  - **stroke**：`@color/tv_focus_stroke_color` + `@dimen/tv_focus_stroke_width`
- **统一组合态**：对 `selected + focused` 的控件，统一采用“selected 底色 + focused overlay+stroke 叠加”，保证“选中语义”和“焦点语义”同时可读。
- **消除硬编码**：将 focused 相关的硬编码颜色/线宽（例如 `3dp`、`#...`）替换为 `tv_focus_*` 令牌引用；半透明按压态（如需）优先复用 `tv_pressed_overlay_color`。
- **TV 差异隔离**：focused 视觉差异优先通过 `drawable-television/` 资源覆盖实现，避免影响移动端触控视觉与 ripple 语义。
- **审计与治理**：在落地前先产出“当前焦点资源清单与分类”，并在落地后作为后续新增 UI 的对齐基线。

## Capabilities

### Modified Capabilities

- `tv-remote-ux-baseline`：补齐“TV 端 focused 视觉样式统一（overlay+stroke + token 化）”的规范要求。

### Related Capabilities / Dependencies

- `update-tv-focus-style-and-navigation`：该变更已覆盖 Tab/分页场景的 focused 可见与导航一致性；本变更聚焦更底层的“视觉令牌一致性”，作为其补全与延伸。

## Impact

- 影响模块（预期）：
  - `core_ui_component`：通用 selector 的 focused 分支与 TV token 的使用方式
  - `app`：TV 主导航条目 focused 可见性与一致性
  - `player_component`：播放器设置/动作按钮/播放源条目等 focused 样式一致性
  - `storage_component`：若干按钮/开关类 focused 样式（含线宽）一致性
  - `anime_component`：少量自定义条目背景 focused 一致性
- 风险与代价：
  - 视觉会发生细微变化，需要一次 TV 遥控器回归确认“可见且不过度抢眼”。
  - 若历史控件把 `pressed` 当作主要反馈（不规范），统一后必须确保 `focused` 仍为主反馈。

## Out of Scope

- 不在本变更中系统性补齐“缺少 focused 反馈/不可聚焦”的控件（这属于可达性补齐专项）；本变更聚焦“已有 focused/自定义背景但表现不一致”的统一。
- 不改变 DPAD 导航行为与焦点策略（由 `update-tv-focus-style-and-navigation`、`update-tv-player-settings-dpad-navigation` 等变更负责）。
- 不引入新的 UI 框架或大规模布局复制；优先使用资源覆盖与既有基础设施完成统一。

## Open Questions（需确认）

1) 是否需要同时统一 **TV 端的 pressed 视觉**（例如统一使用 `tv_pressed_overlay_color`），还是仅统一 `state_focused`？
2) 对“selected 但未 focused”的控件，是否需要统一采用 `tv_unfocused_*`（弱描边/弱 overlay）来表达“选中但当前不在焦点”，还是保持各模块现状？

