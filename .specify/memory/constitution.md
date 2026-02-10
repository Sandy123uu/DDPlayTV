<!--
Sync Impact Report
- Version change: N/A (template) -> 1.0.0
- Modified principles:
  - Template Principle 1 -> I. 模块边界与契约优先
  - Template Principle 2 -> II. TV 优先与 DPAD 可达性（不可协商）
  - Template Principle 3 -> III. 可验证交付与质量门禁（不可协商）
  - Template Principle 4 -> IV. 可观测性与安全日志
  - Template Principle 5 -> V. 规格驱动与文档同步
- Added sections:
  - 技术与安全基线
  - 研发流程与质量门禁
- Removed sections:
  - 无
- Templates requiring updates:
  - ✅ updated: .specify/templates/plan-template.md
  - ✅ updated: .specify/templates/spec-template.md
  - ✅ updated: .specify/templates/tasks-template.md
  - ⚠ pending: .specify/templates/commands/*.md（目录不存在，无法校验/更新）
  - ✅ checked: README.md（无宪法引用需要同步）
  - ✅ checked: AGENTS.md（规则已与本宪法一致，无需改动）
- Follow-up TODOs:
  - 无
-->

# DDPlayTV Constitution

## Core Principles

### I. 模块边界与契约优先
所有新增或变更依赖 MUST 保持单向分层，禁止业务模块之间直接耦合。跨模块协作
MUST 通过 `:core_contract_component` 契约、路由或服务接口完成。任何 `project(...)`
依赖调整 MUST 同步更新依赖治理文档并执行 `./gradlew verifyModuleDependencies`。

Rationale: 保持模块可替换性与可维护性，避免跨特性耦合导致的回归扩散。

### II. TV 优先与 DPAD 可达性（不可协商）
涉及 UI 或交互的改动 MUST 以 Android TV（Leanback）遥控器路径为第一目标，并保持移动端
可用。TV 分流 MUST 统一使用 `Context.isTelevisionUiMode()`；页面 MUST 提供默认焦点、
可见 focused 反馈与可闭环返回路径；禁止在 TV 模式暴露仅触摸可触发的关键入口。

Rationale: 本项目核心价值是 TV 场景体验，DPAD 可达性缺陷会直接阻断主路径使用。

### III. 可验证交付与质量门禁（不可协商）
每项行为变更 MUST 具备可执行验证方案（至少覆盖受影响测试与一次构建命令）。交付记录
MUST 明确写出 Gradle 结果尾行是 `BUILD SUCCESSFUL` 或 `BUILD FAILED`。失败测试或失败构建
MUST 阻断合并，除非有书面豁免与负责人批准。

Rationale: 可复现的验证证据是控制回归风险与发布质量的最低保障。

### IV. 可观测性与安全日志
关键路径（播放、字幕/弹幕、存储鉴权、网络取流）变更 MUST 提供可诊断信号（日志、状态或
指标）。日志与调试输出 MUST 进行敏感信息脱敏，严禁输出 token、cookie、密钥或用户隐私。
`adb logcat` 使用 MUST 采用过滤策略，仅保留定位问题所需内容。

Rationale: 可观测性决定故障定位效率，安全边界决定版本可分发性。

### V. 规格驱动与文档同步
需求实现 MUST 维持 `spec.md`、`plan.md`、`tasks.md` 的双向追踪关系。任何影响架构、依赖、
TV 交互策略或外部接口的变更 MUST 同步更新对应文档与说明，不得在代码与文档之间留下
冲突状态。

Rationale: 规格与实现一致性是多人协作、评审和长期演进的基础。

## 技术与安全基线

- Android 主线技术栈 MUST 维持 Kotlin 1.9.25、AGP 8.7.2、JVM target 1.8（除非经宪法修订）。
- 模块依赖治理 MUST 以 `document/architecture/module_dependency_governance.md` 为准。
- TV 相关 UI 变更 MUST 在评审中附带 DPAD 焦点路径与回归结论。
- 敏感配置 MUST 存放于 `local.properties` 或 Gradle properties，禁止硬编码到仓库源码。
- `repository/*` 模块仅用于第三方封装，业务逻辑不得下沉到该层。

## 研发流程与质量门禁

- 开发流程 MUST 按 spec -> plan -> tasks -> implementation -> verification 执行。
- Implementation Plan 的 Constitution Check MUST 在 Phase 0 前通过，并在 Phase 1 后复检。
- Tasks MUST 按用户故事分组，且每个故事包含独立验收与必要测试任务。
- 合并前 MUST 提供受影响模块清单、执行命令、测试结果与构建结果。
- 如需违反任一原则，MUST 在 Complexity Tracking 中记录理由、替代方案与批准人。

## Governance

本宪法高于项目内其他流程性约定；若其他文档与本宪法冲突，以本宪法为准。

- Amendment Procedure: 修订 MUST 通过 PR 提交，包含条款变更说明、影响分析、模板联动状态与
  版本号变更理由；至少一名受影响模块维护者批准后生效。
- Versioning Policy: 宪法版本采用语义化版本。MAJOR 用于不兼容治理变更或原则重定义；MINOR
  用于新增原则/章节或实质性扩展；PATCH 用于措辞澄清、错别字修复或非语义调整。
- Compliance Review: 每个 `plan.md` 与 PR MUST 执行 Constitution Check。发布前评审 MUST 抽查
  原则符合性、文档同步性与质量门禁证据。

**Version**: 1.0.0 | **Ratified**: 2026-02-09 | **Last Amended**: 2026-02-09
