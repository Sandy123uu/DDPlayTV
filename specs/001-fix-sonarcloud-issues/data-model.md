# Phase 1 Data Model: SonarCloud 报告问题修复优化

## 1. 实体总览

本特性的交付对象是“质量治理流程”，不新增线上业务表结构；以下实体用于规范修复过程、验收证据与审计追溯。

1. `QualityIssueItem`：Sonar 报告中的问题项（Issue / Vulnerability / Code Smell）。
2. `SecurityHotspotReview`：热点问题的评审与风险处置记录。
3. `RemediationTaskItem`：问题修复动作与复核流程。
4. `QualityComparisonSnapshot`：基线与当前分析结果对比快照。
5. `ExemptionRecord`：第三方/外部引入源码的豁免说明与复核结论。

## 2. 实体定义

### 2.1 QualityIssueItem

| 字段 | 类型 | 说明 |
|------|------|------|
| issueKey | string | Sonar 问题唯一标识 |
| ruleKey | string | 触发规则（如 `kotlin:S3776`） |
| issueType | enum | `BUG` \| `VULNERABILITY` \| `CODE_SMELL` |
| severity | enum | `BLOCKER` \| `CRITICAL` \| `MAJOR` \| `MINOR` \| `INFO` |
| impactSeverity | enum | `HIGH` \| `MEDIUM` \| `LOW` \| `INFO` |
| modulePath | string | 模块路径（如 `player_component/...`） |
| filePath | string | 文件路径 |
| line | int? | 行号（可空） |
| status | enum | `OPEN` \| `IN_PROGRESS` \| `FIXED_PENDING_ANALYSIS` \| `CLOSED` \| `DEFERRED` \| `ACCEPTED_RISK` \| `FALSE_POSITIVE` \| `EXEMPTED` |
| sourceScope | enum | `FIRST_PARTY` \| `THIRD_PARTY` |
| baselineAnalysisKey | string | 基线分析标识 |
| currentAnalysisKey | string? | 当前分析标识 |
| createdAt | datetime | 创建时间 |
| updatedAt | datetime | 更新时间 |

### 2.2 SecurityHotspotReview

| 字段 | 类型 | 说明 |
|------|------|------|
| hotspotKey | string | Hotspot 唯一标识 |
| issueKey | string | 关联的 `QualityIssueItem.issueKey` |
| riskLevel | enum | `HIGH` \| `MEDIUM` \| `LOW` |
| reviewStatus | enum | `TO_REVIEW` \| `IN_REVIEW` \| `REVIEWED_SAFE` \| `REVIEWED_FIXED` \| `REVIEWED_ACCEPTED` |
| disposition | enum | `FIX` \| `ACCEPT_RISK` \| `DEFER` |
| rationale | string | 评审/处置理由 |
| reviewer | string | 评审人 |
| acceptedBy | string? | 风险接受人（仅接受风险时必填） |
| reviewedAt | datetime | 评审完成时间 |

### 2.3 RemediationTaskItem

| 字段 | 类型 | 说明 |
|------|------|------|
| taskId | string | 修复任务唯一标识 |
| issueKey | string | 关联问题 |
| priority | enum | `P1` \| `P2` \| `P3` |
| targetOutcome | enum | `FIX` \| `ACCEPT_RISK` \| `DEFER` \| `FALSE_POSITIVE` \| `EXEMPT` |
| taskStatus | enum | `NEW` \| `TRIAGED` \| `IN_PROGRESS` \| `READY_FOR_REVIEW` \| `VERIFIED` \| `CLOSED` \| `REJECTED` |
| assignee | string | 执行人 |
| reviewer | string | 复核人 |
| decisionReason | string? | 延期/接受风险/误报理由 |
| evidenceLinks | string[] | 证据链接（Issue URL/PR/Commit/Test Report 等） |
| createdAt | datetime | 创建时间 |
| updatedAt | datetime | 更新时间 |
| closedAt | datetime? | 关闭时间 |

### 2.4 QualityComparisonSnapshot

| 字段 | 类型 | 说明 |
|------|------|------|
| snapshotId | string | 对比快照唯一标识 |
| baselineAnalysisKey | string | 基线分析标识 |
| targetAnalysisKey | string | 当前分支/PR 分析标识 |
| gateStatus | enum | `PASS` \| `FAIL` |
| totalIssuesBaseline | int | 基线问题总数 |
| totalIssuesCurrent | int | 当前问题总数 |
| highImpactBaseline | int | 基线高影响问题数 |
| highImpactCurrent | int | 当前高影响问题数 |
| vulnerabilitiesBaseline | int | 基线漏洞数 |
| vulnerabilitiesCurrent | int | 当前漏洞数 |
| hotspotsReviewedRate | decimal | 热点评审完成率（0-100） |
| newCoverage | decimal | 新代码覆盖率 |
| newDuplicatedLinesDensity | decimal | 新代码重复率 |
| top10FilesDelta | object | 前 10 文件问题变化详情 |
| generatedAt | datetime | 生成时间 |

### 2.5 ExemptionRecord

| 字段 | 类型 | 说明 |
|------|------|------|
| exemptionId | string | 豁免记录标识 |
| issueKey | string | 关联问题 |
| modulePath | string | 模块路径（应位于 `repository/*` 或外部引入目录） |
| reasonType | enum | `THIRD_PARTY_SOURCE` \| `UPSTREAM_CONFLICT` \| `NON_ACTIONABLE` |
| rationale | string | 豁免理由 |
| reviewer | string | 复核人 |
| reviewConclusion | enum | `APPROVED` \| `REJECTED` |
| createdAt | datetime | 记录时间 |

## 3. 实体关系

- `QualityIssueItem (1) -> (0..n) RemediationTaskItem`
- `QualityIssueItem (1) -> (0..1) SecurityHotspotReview`（仅热点）
- `QualityIssueItem (1) -> (0..1) ExemptionRecord`（仅第三方/外部引入范围）
- `QualityComparisonSnapshot` 聚合多条 `QualityIssueItem` 与 `SecurityHotspotReview` 统计结果，用于验收对比，不直接反向持有外键。

## 4. 校验规则（来自需求）

1. 所有 `issueType=VULNERABILITY` 的问题最终必须进入 `CLOSED`，不得 `ACCEPTED_RISK`。
2. `SecurityHotspotReview.riskLevel=HIGH` 时，`disposition` 只能是 `FIX`。
3. `disposition=ACCEPT_RISK` 时必须填写 `rationale`、`acceptedBy`、`reviewer`。
4. `sourceScope=THIRD_PARTY` 的问题不得进入常规实质修复流，必须关联 `ExemptionRecord`。
5. `RemediationTaskItem.targetOutcome` 为 `DEFER`/`FALSE_POSITIVE`/`EXEMPT` 时，`decisionReason` 必填。
6. 任何任务进入 `CLOSED` 前，`evidenceLinks` 至少包含 Sonar 问题链接与对应代码变更证据之一（PR/Commit）。
7. 验收快照必须来自当前功能分支或对应 PR 分析线（`targetAnalysisKey` 不可为空）。

## 5. 状态流转

### 5.1 问题状态（QualityIssueItem.status）

- `OPEN -> IN_PROGRESS -> FIXED_PENDING_ANALYSIS -> CLOSED`
- `OPEN/IN_PROGRESS -> DEFERRED`
- `OPEN/IN_PROGRESS -> ACCEPTED_RISK`（仅中低风险热点）
- `OPEN -> FALSE_POSITIVE`
- `OPEN -> EXEMPTED`（仅 `THIRD_PARTY`）

### 5.2 修复任务状态（RemediationTaskItem.taskStatus）

- `NEW -> TRIAGED -> IN_PROGRESS -> READY_FOR_REVIEW -> VERIFIED -> CLOSED`
- `READY_FOR_REVIEW -> REJECTED -> IN_PROGRESS`

### 5.3 热点评审状态（SecurityHotspotReview.reviewStatus）

- `TO_REVIEW -> IN_REVIEW -> REVIEWED_FIXED`
- `TO_REVIEW -> IN_REVIEW -> REVIEWED_SAFE`
- `TO_REVIEW -> IN_REVIEW -> REVIEWED_ACCEPTED`（仅中低风险）

