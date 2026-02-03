## Context

仓库以 TV 端交互为先，且已在 `core_ui_component` 定义了一组可复用的 TV 焦点视觉令牌：

- `@color/tv_focus_stroke_color`
- `@color/tv_focus_overlay_color`
- `@dimen/tv_focus_stroke_width`
-（可选）`@color/tv_pressed_overlay_color`、`@color/tv_unfocused_*`

同时，工程存在多套“自定义 focused 样式”：

- 线宽不一致（2dp vs 3dp）
- focused 是否叠加 overlay 不一致
- 使用硬编码颜色/尺寸
- 资源是否走 `drawable-television` 覆盖不一致（部分 focused 写在默认 `drawable/` 中）

本变更目标是：**不引入新的视觉体系**，而是把所有 focused 视觉收敛到同一套令牌与同一套结构模板，确保“用户感知一致 + 代码可治理”。

## Standard（本变更的统一标准）

### 1) `state_focused` 的标准结构（推荐：layer-list 叠加）

当控件背景在 focused 时需要既保留原始底色/形状，又统一提供“可见的焦点反馈”，标准结构为：

- **底层（Base）**：保持原背景的 shape（normal 或 selected 的底色/圆角/边框等）
- **上层（Focus Overlay）**：叠加一个与底层相同圆角/形状的 shape：
  - `solid=@color/tv_focus_overlay_color`
  - `strokeColor=@color/tv_focus_stroke_color`
  - `strokeWidth=@dimen/tv_focus_stroke_width`

> 备注：若原控件本身是透明底（例如导航 item），Base 可为透明；此时 Focus Overlay 仍统一使用 overlay+stroke，以保证在深色背景上 focused 可见。

### 2) `selected + focused` 的标准表达

对于同时存在 “selected（业务语义）” 与 “focused（操作语义）” 的控件：

- selected 负责表达“当前选中/正在播放/启用”等业务状态（底色/弱描边等）
- focused 必须仍然可见：采用与普通 focused **同样的 Focus Overlay（overlay+stroke）** 叠加在 selected 底色之上

这样可以避免常见问题：selected 覆盖了 focused，导致“有焦点但不可见”或焦点强度不一致。

### 3) Token 归属与引用方式

- `tv_focus_*` 令牌由 `core_ui_component` 统一提供，其它模块 **不得复制定义**（避免多套 token 漂移）。
- focused 相关尺寸与颜色 **不得硬编码**（例如 `3dp`、`#55FFFFFF` 等），必须通过 token 引用。

### 4) TV 差异隔离策略

focused 视觉的差异优先用 `drawable-television/` 覆盖实现：

- 非 TV：保持现有 ripple/pressed 语义
- TV：在 `drawable-television` 中提供 focused 分支（overlay+stroke），并尽量保证 default/pressed 与非 TV 语义一致

当某个 drawable 在默认 `drawable/` 中已经包含 focused 分支时，需要评估其是否会对移动端造成“意外样式”；若有风险，应将 focused 分支迁移到 TV 覆盖版本。

## Migration Scope（本次统一覆盖范围）

以“工程内所有包含 `state_focused` 的 drawable/selector”为主（排除 `openspec/`），并按以下维度逐个收敛：

1. focused 是否使用 `tv_focus_*` token
2. focused 是否采用 overlay+stroke 结构（含 selected+focused）
3. 是否存在 hardcode（颜色/线宽）
4. 是否通过 `drawable-television` 覆盖隔离 TV 差异

## Validation

- 静态与构建：
  - `./gradlew :app:assembleDebug`（确认尾部为 `BUILD SUCCESSFUL`）
  - `./gradlew lint`
  - `./gradlew verifyModuleDependencies`
- TV 手工回归（遥控器 DPAD）：
  - TvMainActivity 左侧主导航条目：focused 反馈可见且与其它模块一致
  - 播放器设置页：设置项/按钮/Tab（如仍可聚焦）focused 视觉一致
  - 存储页/投屏页按钮：focused 线宽与 overlay 强度一致
  - 播放源列表条目：selected、focused、selected+focused 语义清晰且一致

