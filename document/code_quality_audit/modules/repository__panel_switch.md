# 模块排查报告：:repository:panel_switch

- 模块：:repository:panel_switch
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：repository/panel_switch/

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`REPO_PANEL_SWITCH-F###`  
> - Task：`REPO_PANEL_SWITCH-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 以 Gradle module 形式封装本地预编译 AAR：`repository/panel_switch/panelSwitchHelper-androidx.aar`，供工程以 `project(":repository:panel_switch")` 依赖方式接入。
  - 提供面板/键盘切换能力（`com.effective.android.panel.*`），典型用于“输入框 + 扩展面板（表情/样式面板等）”的切换与高度联动。
- 模块职责（不做什么）
  - 不承载任何业务逻辑；除 `build.gradle.kts` 与 AAR 工件外不应新增 Kotlin/Java 实现。
  - 不应在 wrapper 内引入其它工程模块依赖（避免把 wrapper 变为“功能模块”并扩大耦合面）。
- 关键入口/关键路径（示例）
  - AAR 封装：`repository/panel_switch/build.gradle.kts` + `artifacts.add("default", file("panelSwitchHelper-androidx.aar"))`
  - 二进制产物：`repository/panel_switch/panelSwitchHelper-androidx.aar`（SHA256：`ace2e33a70f393388c041ce428f497ce0379b0367a4279fae2dc9f8d9f671c00`）
  - 依赖声明（入口）：`player_component/build.gradle.kts` + `implementation(project(":repository:panel_switch"))`
  - 典型使用点（对话框）：`player_component/src/main/java/com/xyoye/player/controller/video/SendDanmuDialog.kt` + `SendDanmuDialog#init`（`PanelSwitchHelper.Builder(...)`）
  - 典型使用点（布局）：`player_component/src/main/res/layout/layout_send_danmu.xml` + `PanelSwitchLayout/PanelView`（`com.effective.android.panel.view.*`）
  - License 线索：`user_component/src/main/assets/license/PanelSwitchHelper.txt`（含上游地址）；概览清单：`document/Third_Party_Libraries.md`
- 依赖边界
  - 对外（被依赖）：当前仅发现 `:player_component` 直接依赖并引用第三方类型（见上方“依赖声明/典型使用点”）。
  - 对内（依赖）：无（仅提供 AAR 工件，不应再依赖其它工程模块）。
  - 边界疑点：
    - 当前“发送弹幕”入口在 UI 层被显式禁用，导致该依赖呈现“编译期存在、运行期不可达”的状态（见 Findings）。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - `rg`：确认第三方类型引用点与布局使用点  
    - `rg "com\\.effective\\.android\\.panel" -n`
  - ast-grep：确证关键调用形态（避免字符串误判）  
    - Kotlin：`PanelSwitchHelper.Builder($WINDOW, $ROOT)`

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| REPO_PANEL_SWITCH-F001 | ArchitectureRisk | 预编译 AAR 缺少 wrapper 侧可追溯元信息（来源/版本/校验和/更新流程），升级与合规审计成本高 | `repository/panel_switch/panelSwitchHelper-androidx.aar`（二进制）；`repository/panel_switch/build.gradle.kts` + `artifacts.add(...)`；License 线索：`user_component/src/main/assets/license/PanelSwitchHelper.txt`；概览：`document/Third_Party_Libraries.md` | N/A | Unify | `repository/panel_switch/`（补齐 README 元信息，并与其它 wrapper 统一规范） | Medium | Small | P1 | 需要确认 AAR 对应的上游版本号；否则安全事件/升级时难以评估影响面 |
| REPO_PANEL_SWITCH-F002 | Redundancy | 依赖处于“编译期存在、运行期不可达”：发送弹幕入口被禁用，相关 UI/代码疑似废弃 | 禁用入口：`player_component/src/main/java/com/xyoye/player/controller/video/PlayerBottomView.kt` + `PlayerBottomView#init`（`sendDanmuTv.isVisible=false` 且原逻辑被注释）；依赖/调用：`player_component/src/main/java/com/xyoye/player/controller/video/SendDanmuDialog.kt` + `SendDanmuDialog#init`；布局：`player_component/src/main/res/layout/layout_send_danmu.xml` | N/A | Deprecate | `:player_component`（直接移除整条链路：删除相关 UI/代码并移除依赖） | Medium | Medium | P2 | 产品决策已确认：直接移除整条链路；无需为 TV/移动端补齐交互策略 |
| REPO_PANEL_SWITCH-F003 | ArchitectureRisk | 调用侧未持有 `PanelSwitchHelper` 实例，生命周期/资源释放不可控（潜在泄漏/窗口监听残留） | `player_component/src/main/java/com/xyoye/player/controller/video/SendDanmuDialog.kt` + `SendDanmuDialog#init`（`PanelSwitchHelper.Builder(...).build(true)` 返回值未保存） | N/A | Unify | `:player_component`（若继续保留该能力，封装为可释放的成员并与 Dialog 生命周期绑定） | Medium | Small | P2 | 需要查阅上游库的释放方式（如 `reset()/onDestroy()` 等）；否则可能出现 window listener 残留或内存泄漏 |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| REPO_PANEL_SWITCH-T001 | REPO_PANEL_SWITCH-F001 | 为 PanelSwitchHelper AAR 增加可追溯元信息（来源/版本/License/校验和/更新流程） | 新增 `repository/panel_switch/README.md`（中文）；在 README 中引用/关联 `user_component/src/main/assets/license/PanelSwitchHelper.txt`，并记录 AAR SHA256（当前：`ace2e33a70f393388c041ce428f497ce0379b0367a4279fae2dc9f8d9f671c00`）与升级步骤 | 1) README 明确：上游项目/下载地址、版本号、License、AAR SHA256、更新步骤；2) 任意人可按文档复现升级；3) 不影响现有依赖解析 | Medium | Small | P1 | 待分配（Repo） | Draft |
| REPO_PANEL_SWITCH-T002 | REPO_PANEL_SWITCH-F002 | 移除“发送弹幕”整条链路：删除相关 UI/代码并移除 `:repository:panel_switch` 依赖 | 删除 `player_component/.../SendDanmuDialog.kt` 与 `player_component/src/main/res/layout/layout_send_danmu.xml`；移除 `player_component/build.gradle.kts` 对 `:repository:panel_switch` 的依赖，并清理入口/引用点 | 1) 依赖与调用点一致（不再存在“依赖存在但入口不可达”）；2) `rg "com\\.effective\\.android\\.panel" -n` 无命中；3) 全仓编译通过 | Medium | Medium | P2 | 待分配（Player） | Draft |
| REPO_PANEL_SWITCH-T003 | REPO_PANEL_SWITCH-F003 | 若继续使用 PanelSwitchHelper：补齐生命周期释放/资源回收，避免潜在泄漏 | 调整 `SendDanmuDialog` 持有 helper 实例，并在 `dismiss()`/`onStop()` 中按上游 API 做释放；必要时封装为 `PanelSwitchController`（内部持有 helper）以统一管理 | 1) Dialog 关闭后无残留监听（LeakCanary/手动验证）；2) Back 键与沉浸式状态栏恢复行为不回退；3) 不影响现有 UI | Medium | Small | P2 | 待分配（Player） | Draft |

## 5) 风险与回归关注点

- 行为回退风险（哪些场景最敏感）
  - 若恢复发送弹幕入口：输入法弹出/收起、面板切换、返回键关闭顺序（优先关闭面板/键盘，再关闭 Dialog），以及沉浸式状态栏恢复（`ImmersionBar`）。
  - TV 场景：遥控器 DPAD 下无软键盘输入时的可用性（需明确“TV 禁用/替代交互”策略，避免暴露不可触发入口）。
- 回归成本（需要的账号/媒体文件/设备）
  - 需要可播放视频源 + 弹幕可发送的后端/本地弹幕模拟（若功能恢复）。
  - 建议至少覆盖：手机（有软键盘）、TV（无软键盘/DPAD）两类设备。

## 6) 备注（历史背景/待确认点）

- 当前仓库内已存在 PanelSwitchHelper 的 License 线索（见 `user_component/src/main/assets/license/PanelSwitchHelper.txt`），但 wrapper 目录缺少“该 AAR 与该 License/版本的对应关系、校验和、更新方式”说明；建议优先补齐元信息后再评估升级/替换方案。
