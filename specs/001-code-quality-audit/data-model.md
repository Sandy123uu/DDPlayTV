# Phase 1：数据模型（代码质量排查与复用治理）

本文档从 `spec.md` 的实体与需求出发，整理“模块排查 → 模块报告 → 全局汇总 → 治理任务”的核心数据结构、字段约束与状态机。其目的不是要求立刻实现一套新系统，而是为“统一口径、可汇总、可追踪”提供清晰的数据边界（便于后续落地到文档模板、Issue、或内部工具）。

- Feature spec：`spec.md`
- 依赖治理口径：`document/architecture/module_dependency_governance.md`

## 1. 实体总览

| 实体 | 类型 | 归属 | 用途 |
|---|---|---|---|
| Module Group（模块分组） | 规范化定义（文档/配置） | 规划层 | 用于分工、排查顺序、汇总口径统一 |
| Module（模块） | 事实数据（来自 Gradle） | 规划层 | 与 `settings.gradle.kts` 对齐的排查对象 |
| Finding（发现） | 可追踪条目 | 模块报告/全局汇总 | 记录重复实现/冗余/复用机会/架构风险/安全隐私风险等问题 |
| Refactor Task（治理任务） | 可执行条目 | 模块报告/全局汇总 | 把治理建议落为可验收、可指派、可跟踪的任务 |
| Evidence（证据） | 结构化引用 | Finding/Task 附属 | 满足“可追溯”的最低要求：文件路径 + 关键符号名 |
| Module Report（模块报告） | 文档/记录 | 模块级产出 | 单模块排查结果容器（发现 + 任务 + 风险 + 负责人） |
| Global Summary（全局汇总） | 文档/记录 | 全局产出 | 跨模块去重合并、优先级排序、路线图与来源映射 |

---

## 2. Module Group（模块分组）

> 目标：让“排查范围/顺序/汇总口径”可复用、可被他人按同一标准继续执行。

### 2.1 字段（建议）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `key` | `String` | 是 | 稳定标识，例如：`APP` / `FEATURE` / `CORE` / `DATA` / `REPOSITORY` |
| `name` | `String` | 是 | 展示名，例如“应用壳层/业务特性模块/基础能力模块” |
| `order` | `Int` | 是 | 排查顺序（小 → 大） |
| `modulePaths` | `List<String>` | 是 | Gradle module path 列表，例如 `:app`、`:core_storage_component` |
| `notes` | `String` | 否 | 边界说明与注意事项 |

### 2.2 约束

- `key` 全局唯一；`modulePaths` 中每个模块只允许出现在一个分组中（避免重复统计/职责不清）。
- `modulePaths` 必须是 `settings.gradle.kts` 中存在的模块（观察项除外，观察项单独清单管理）。

---

## 3. Module（模块）

> 模块对象主要用于把“模块报告/发现/任务”的归属落地到可验证的目录范围。

### 3.1 字段（建议）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `path` | `String` | 是 | Gradle module path，例如 `:player_component` |
| `dir` | `String` | 是 | 仓库目录，例如 `player_component/` |
| `groupKey` | `String` | 是 | 所属模块分组 `ModuleGroup.key` |
| `owner` | `String` | 否 | 负责人（人名/账号/团队） |
| `status` | `Enum` | 是 | `NotStarted / InProgress / Done / Observing` |

---

## 4. Evidence（证据）

> 满足 spec 的最低可追溯要求：**文件路径 + 关键符号名（类/方法/函数）**。

### 4.1 字段（建议最小集合）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `filePath` | `String` | 是 | 相对仓库根目录的路径，例如 `player_component/src/main/java/...` |
| `symbol` | `String` | 是 | 关键符号名，例如 `PlayerViewModel#startPlay` / `class Foo` |
| `kind` | `Enum` | 是 | `Code / Config / Resource / Log / Doc` |
| `notes` | `String` | 否 | 简要说明（为什么这是证据） |

### 4.2 可选增强字段（建议）

| 字段 | 类型 | 说明 |
|---|---|---|
| `excerpt` | `String` | 代码/配置的短摘录（注意不要粘贴敏感信息） |
| `lineHint` | `String` | 行号提示（若团队习惯记录） |
| `command` | `String` | 复现/定位命令（例如 `rg "xxx" -n <path>`） |

---

## 5. Finding（发现）

### 5.1 分类（建议枚举）

| 字段 | 候选值 | 说明 |
|---|---|---|
| `category` | `Duplication` / `Redundancy` / `ReuseOpportunity` / `ArchitectureRisk` / `SecurityPrivacyRisk` | 对齐 FR-002 的维度口径 |
| `multiImplType` | `Intentional` / `Unintentional` / `N/A` | 仅在“同功能多实现”场景必填（FR-005） |
| `recommendation` | `Keep` / `Unify` / `Deprecate` | 必填结论（FR-005） |

### 5.2 字段（建议）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `moduleLocalId` | `String` | 是 | 模块内唯一 ID，例如 `PLAYER-F001`（FR-010） |
| `globalId` | `String` | 否 | 全局 ID（去重合并后分配），例如 `G-F0007` |
| `modulePath` | `String` | 是 | 归属模块（Gradle path） |
| `title` | `String` | 是 | 一句话概括问题 |
| `description` | `String` | 是 | 问题说明（包含“为什么是问题”） |
| `category` | `Enum` | 是 | 见 5.1 |
| `multiImplType` | `Enum` | 条件必填 | category=Duplication 时必填 |
| `recommendation` | `Enum` | 是 | Keep/Unify/Deprecate |
| `suggestedLanding` | `String` | 否 | 若建议统一/抽取：建议落点模块（例如 `:core_storage_component`） |
| `evidences` | `List<Evidence>` | 是 | 至少 1 条证据（FR-004） |
| `impact` | `Enum` | 是 | `High / Medium / Low` |
| `effort` | `Enum` | 是 | `Small / Medium / Large` |
| `priority` | `Enum` | 是 | `P1 / P2 / P3`（由矩阵映射得出，FR-011） |
| `risks` | `List<String>` | 否 | 风险点（行为回退/兼容性/回归成本） |
| `dependencies` | `List<String>` | 否 | 依赖（人/模块/外部因素） |
| `owner` | `String` | 否 | 负责人 |
| `status` | `Enum` | 是 | `Draft / Reviewed / Accepted / Rejected / Closed` |

### 5.3 不变式/校验规则

- `moduleLocalId` 在同一模块报告内必须唯一。
- 若 `category == Duplication`：
  - `multiImplType` 必填（Intentional/Unintentional）
  - `recommendation` 必须给出（Keep/Unify/Deprecate），且 `description` 必须包含理由与边界说明（防误合并）。
- `priority` 必须能由 Impact×Effort 矩阵推导得到（禁止“拍脑袋”优先级）。

### 5.4 状态机（建议）

```text
Draft
  ├─(模块负责人复核通过)→ Reviewed
  ├─(证据不足/描述不清)→ Draft（补证据后再次复核）
  └─(确认不成立/属于设计选择且无治理点)→ Rejected

Reviewed
  ├─(纳入治理范围)→ Accepted
  └─(不纳入/延期)→ Rejected

Accepted
  ├─(拆分/关联治理任务)→ Accepted（持续更新 tasks）
  └─(治理完成并验收)→ Closed
```

---

## 6. Refactor Task（治理任务）

> 把“建议”转成可验收的行动项（AC-FR-008）。

### 6.1 字段（建议）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `moduleLocalId` | `String` | 是 | 模块内唯一 ID，例如 `CORE_STORAGE-T012` |
| `globalId` | `String` | 否 | 全局 ID（合并后） |
| `title` | `String` | 是 | 一句话任务目标 |
| `goal` | `String` | 是 | 为什么做（收益/风险降低） |
| `scope` | `String` | 是 | 影响范围（模块/目录/关键路径） |
| `relatedFindings` | `List<String>` | 是 | 关联的 Finding ID（至少 1） |
| `acceptanceCriteria` | `List<String>` | 是 | 验收标准（可测试/可检查） |
| `impact` | `Enum` | 是 | `High/Medium/Low` |
| `effort` | `Enum` | 是 | `Small/Medium/Large` |
| `priority` | `Enum` | 是 | `P1/P2/P3` |
| `owner` | `String` | 否 | 负责人 |
| `status` | `Enum` | 是 | `Draft / Planned / InProgress / Done / Dropped` |
| `risks` | `List<String>` | 否 | 风险与回归点 |
| `dependencies` | `List<String>` | 否 | 依赖与阻塞项 |

### 6.2 状态机（建议）

```text
Draft → Planned → InProgress → Done
   └──────────────→ Dropped（策略调整/不再需要）
```

---

## 7. Module Report（模块报告）

### 7.1 字段（建议）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `modulePath` | `String` | 是 | 归属模块 |
| `owner` | `String` | 是 | 负责人 |
| `date` | `String` | 是 | 报告日期 |
| `background` | `String` | 是 | 模块职责/边界/关键路径 |
| `findings` | `List<Finding>` | 是 | 发现列表 |
| `tasks` | `List<RefactorTask>` | 是 | 治理任务列表 |
| `risks` | `List<String>` | 否 | 模块级风险与回归关注点 |
| `notes` | `String` | 否 | 备注（历史背景/待确认点） |

### 7.2 约束

- 报告必须引用统一的“排查维度清单”与“优先级口径”（AC-FR-002 / AC-FR-011）。
- 每条 Finding/Task 必须具备模块内唯一 ID（AC-FR-010）。

---

## 8. Global Summary（全局汇总）

### 8.1 字段（建议）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `date` | `String` | 是 | 汇总日期 |
| `moduleReports` | `List<String>` | 是 | 纳入汇总的模块报告列表（含版本/日期） |
| `globalFindings` | `List<Finding>` | 是 | 去重合并后的 Finding（分配 globalId） |
| `globalTasks` | `List<RefactorTask>` | 是 | 去重合并后的 Task（分配 globalId） |
| `sourceMapping` | `Map<String, List<String>>` | 是 | `globalId -> [moduleLocalIds...]` |
| `roadmap` | `List<String>` | 否 | 治理批次/路线图（按 P1/P2/P3、依赖与风险排序） |

### 8.2 去重与合并（建议规则）

- 合并的最小单位建议以“能力/机制”定义（而非仅以文件名/类名），例如“同一套字幕解析/同一套 Range 代理/同一套路由注册”。
- 合并后必须保留来源映射（sourceMapping），并在条目内补充“语义差异/保留原因”（避免误判）。

