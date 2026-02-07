# Phase 0：调研与关键决策（代码质量排查与复用治理）

本文档用于把“排查规划与复用治理”落地所需的关键事实、约束与技术决策一次讲清，并尽量给出项目内可追溯的依据，便于后续按模块分工执行与增量汇总。

- Feature spec：`spec.md`
- 依赖治理与门禁：
  - `document/architecture/module_dependency_governance.md`
  - `document/architecture/module_dependencies_snapshot.md`
  - `document/architecture/architecture_governance_guardrails.md`

## 关键结论（TL;DR）

1. **范围口径**：以 `settings.gradle.kts` 纳入构建的 21 个模块为主范围；仓库中存在但未纳入构建的目录统一标记为“观察项”（不阻塞主流程）。
2. **分组与顺序**：按“基础能力/核心层 → 业务特性层 → 应用壳层 → repository wrappers”的顺序推进，优先治理“被多处复用/被多模块依赖”的共性能力。
3. **证据标准**：每条 Finding/Task 至少包含“文件路径 + 关键符号名（类/方法/函数）”；若属于“多实现”，必须说明“有意/无意”与“保留/统一/废弃”的结论及理由。
4. **优先级口径**：统一使用 `Impact(高/中/低) × Effort(小/中/大)` → `P1/P2/P3` 的 3×3 固定映射（见 `spec.md` FR-011），高影响低成本优先。
5. **治理不引入新耦合**：任何“统一/抽取”建议不得新增 feature ↔ feature 依赖；跨模块协作通过既有契约/路由机制表达（`core_contract_component`），并以 `./gradlew verifyModuleDependencies` 作为约束校验。

---

## 决策 1：排查范围以“纳入构建模块”为主，非构建目录统一作为“观察项”

- **Decision**：排查范围与分工以 `settings.gradle.kts` 的 `include(...)` 为准；对仓库中存在但未纳入构建的目录，统一放入“观察项（Out of Build Scope）”清单记录其风险与处理建议，但不阻塞主流程交付。
- **Rationale**：
  - 依赖治理与门禁（`verifyModuleDependencies`）仅能覆盖纳入构建的模块集合；用同一口径可保证跨模块汇总的一致性与可执行性。
  - 大仓库历史目录容易带来噪音与误判；先以“可被验证的模块集合”构建治理基线，再逐步扩展更可控。
- **Alternatives considered**：
  - 全仓库全目录扫描：覆盖更全，但会把“未编译/未使用”的历史代码与主线混在一起，导致结论不可行动、成本不可控。
- **Sources（依据）**：
  - `specs/001-code-quality-audit/spec.md`（Assumption 1）
  - `document/architecture/module_dependency_governance.md`（0.1/0.2 覆盖范围口径）

## 决策 2：按分层推进排查顺序：Core/Infra → Feature → App → Wrappers

- **Decision**：排查与治理建议输出采用固定顺序：
  1) 基础能力模块（`core_*` / `bilibili_component` / `data_component`）
  2) 业务特性模块（`*_component`）
  3) 应用壳层（`:app`）
  4) repository wrappers（`:repository:*` 仅做边界与使用方式审视）
- **Rationale**：
  - 基础能力层被多模块依赖，重复实现/职责漂移的修复往往能带来更高的复用收益与更低的迁移成本（Impact 高、Effort 相对可控）。
  - 先统一底层口径，可以减少后续 feature 模块报告里的“同问题多次出现但根因一致”的重复劳动。
- **Alternatives considered**：
  - 先从 feature 开始：短期容易产出大量发现，但很多会被归因到 core/infra 的边界不清或重复能力，后续仍需回头治理底层，汇总成本更高。
- **Sources（依据）**：
  - `specs/001-code-quality-audit/spec.md`（Audit Scope & Module Partition 的建议顺序）
  - `document/architecture/module_dependencies_snapshot.md`（可见 core 模块被多个 feature 依赖）

## 决策 3：统一门禁与“可复用的排查方法”以仓库现有治理任务为主

- **Decision**：在“排查执行阶段”，优先使用仓库已有的治理任务作为事实基线与回归门禁：
  - 推荐：`./gradlew verifyArchitectureGovernance`
  - 依赖治理：`./gradlew verifyModuleDependencies`
  - 旧 API 门禁：`./gradlew verifyLegacyPagerApis`
  - 风格一致性：`./gradlew ktlintCheck`
  - 质量检查：`./gradlew lint` / `./gradlew lintDebug`
- **Rationale**：
  - 这些任务已被仓库治理文档固化，且可在本地/CI 一致执行，适合作为“治理建议是否破坏既有架构约束”的快速校验。
  - 对“依赖环/跨层依赖”的问题定位与防回归，自动化门禁比人工 review 稳定。
- **Alternatives considered**：
  - 引入新的全量静态分析工具链（例如 detekt 全量规则集、CPD/重复代码检测器）：可能更强，但需要额外配置/规则基线/误报治理，作为后续增强更合适；本期先把“流程与口径”打通。
- **Sources（依据）**：
  - `document/architecture/architecture_governance_guardrails.md`

## 决策 4：搜索与定位工具采用“rg 先行 + ast-grep 精确匹配”的两段式策略

- **Decision**：排查执行时采用两段式工具策略：
  1) **探索阶段**：用 `rg`（文本搜索）快速定位候选点（字符串、类名、路径、错误信息、配置 key）
  2) **确证阶段**：对需要“按语法结构匹配/跨文件重写”场景，使用 `ast-grep` 做 AST 级确认与统计（例如：特定 API 调用、特定注解/继承链、特定路由注册形式）
- **Rationale**：
  - 大仓库里仅靠文本搜索容易出现误报；但一上来就用 AST 规则又会降低探索效率。两段式兼顾速度与准确性。
  - 该策略与仓库的 `AGENTS.md` 指引一致（先 `rg`，需要结构准确时再切到 ast-grep）。
- **Alternatives considered**：
  - 全程只用 `rg`：误报较多，难以在“同名不同义/不同参数行为”场景给出可信结论。
  - 全程只用 AST 工具：初期不熟悉目标模式时成本高、迭代慢。

## 决策 5：Finding/Task 的 ID 规则采用“模块内 ID + 全局 ID（增量分配）”

- **Decision**：
  - **模块内 ID**：`<MODULE>-F###`（Finding）与 `<MODULE>-T###`（Task），例如：`PLAYER-F001`、`CORE_STORAGE-T012`
  - **全局 ID**：`G-F####` 与 `G-T####`（去重合并后分配），例如：`G-F0007`
  - 全局汇总必须保留 `globalId -> [moduleLocalIds...]` 映射，支持追踪来源与合并关系。
- **Rationale**：
  - 模块报告需要稳定引用（便于讨论/复核/修改）；全局汇总又需要跨模块去重后的统一编号，两者分离最清晰。
  - 采用可读前缀有利于会议沟通与 issue/PR 关联。
- **Alternatives considered**：
  - 仅全局 ID：模块负责人与模块报告内引用会变得不直观，且在“未纳入全局汇总前”的增量阶段不方便使用。
  - 仅模块内 ID：跨模块合并后缺少统一索引，不利于路线图与 Backlog 管理。
- **Sources（依据）**：
  - `specs/001-code-quality-audit/spec.md`（Clarifications + FR-010 / AC-FR-010）

## 决策 6：优先级评估严格使用 3×3 固定矩阵，并补充判定指南以减少主观差异

- **Decision**：严格采用 `spec.md` 的 3×3 固定映射（FR-011），并在模块报告模板中强制填写 Impact/Effort 的“判定依据”：
  - Impact 判定参考：影响复用面（跨模块/跨功能）、缺陷风险（稳定性/崩溃/数据一致性）、安全/隐私风险、维护成本（改动频率 + 修改困难）
  - Effort 判定参考：影响面大小（调用点数量/需要迁移的模块数）、回归成本（是否需要设备/媒体文件/账号）、不确定性（外部依赖/历史语义不清）
- **Rationale**：同一问题在不同模块可能被不同人评估；补充“打分依据”可以显著降低口径漂移，提升全局汇总的可比性。
- **Alternatives considered**：
  - 用 story points/工时：细但成本高，且不同维护者标尺不一致；不利于快速建立统一口径。
  - 用单一维度优先级：无法在“高风险但成本大”与“低风险但成本小”之间做有效权衡。
- **Sources（依据）**：
  - `specs/001-code-quality-audit/spec.md`（Clarifications + FR-011）

## 决策 7：安全/隐私维度仅覆盖“明显高风险项”，并以可自动化扫描为主

- **Decision**：安全/隐私排查只覆盖明显高风险项，优先用可自动化的规则扫描作为“候选发现入口”，再由负责人复核确认：
  - 硬编码密钥/Token（字符串常量、BuildConfig、资源）
  - 敏感信息日志输出（含 accessToken、cookie、deviceId、手机号、定位等）
  - 明文存储敏感数据（SharedPreferences/MMKV/文件未加密）
- **Rationale**：本特性目标是“代码质量与复用治理”；安全/隐私仅作为附加维度，避免扩大范围导致无法交付。
- **Alternatives considered**：
  - 全面安全审计：需要系统化威胁建模与渗透测试配合，不符合本期范围与资源。
- **Sources（依据）**：
  - `specs/001-code-quality-audit/spec.md`（Clarifications + FR-002）

## 决策 8：治理建议的“统一落点”遵循现有模块语义与依赖治理（优先一致性）

- **Decision**：对“可复用/可抽取”的建议，统一落点遵循：
  - 共享类型/契约/路由：优先 `:core_contract_component` 或 `:data_component`
  - 可替换的基础设施实现：优先 `:core_*`（network/db/storage/system/log/ui）
  - 业务特性专属能力：留在对应 feature 模块
  - 禁止为了共享而让 feature 之间相互依赖
- **Rationale**：仓库的依赖治理目标与当前快照已提供清晰分层语义；统一落点按该语义执行，才能避免“治理建议本身引入新耦合”。
- **Alternatives considered**：
  - 新建“common”大杂烩模块：短期看似复用，长期会造成职责漂移与依赖膨胀，反而降低可维护性。
- **Sources（依据）**：
  - `document/architecture/module_dependency_governance.md`
  - `AGENTS.md`（跨模块协作与分层约束）

## 决策 9：`.specify` 的 numeric prefix 冲突暂不阻塞本特性文档产出

- **Decision**：本计划仍以 `specs/001-code-quality-audit/` 为唯一执行目标目录；`setup-plan.sh` 提示的 `001-*` 多目录问题记录为后续“工具链改进任务”，不阻塞当前规划交付。
- **Rationale**：本期交付物是规划与模板；修改历史 spec 目录命名或调整工具逻辑属于工具链演进，风险与影响面较大，应单独立项。
- **Alternatives considered**：
  - 立即重命名/合并历史 spec：可能破坏已有引用与历史记录，且需要统一迁移策略；不适合在本期规划任务中隐式处理。

