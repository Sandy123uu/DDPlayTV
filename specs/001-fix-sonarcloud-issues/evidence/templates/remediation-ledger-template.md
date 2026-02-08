# 修复台账模板（Issue / Task / Hotspot / Snapshot）

> 用途：统一记录每轮质量修复的追踪信息，支持审计抽样与验收复核。

## 1) Issue 台账

| issueKey | ruleKey | issueType | severity | impactSeverity | sourceScope | modulePath | filePath | line | status | baselineAnalysisKey | currentAnalysisKey | updatedAt |
|----------|---------|-----------|----------|----------------|-------------|------------|----------|------|--------|---------------------|--------------------|-----------|
| ISSUE-EXAMPLE-001 | kotlin:S3776 | CODE_SMELL | MAJOR | HIGH | FIRST_PARTY | player_component | player_component/src/main/.../Foo.kt | 123 | IN_PROGRESS | baseline-20260208 | branch-20260208 | 2026-02-08T10:00:00Z |

## 2) Remediation Task 台账

| taskId | issueKey | priority | targetOutcome | taskStatus | assignee | reviewer | decisionReason | evidenceLinks | createdAt | updatedAt | closedAt |
|--------|----------|----------|---------------|------------|----------|----------|----------------|---------------|-----------|-----------|----------|
| TASK-EXAMPLE-001 | ISSUE-EXAMPLE-001 | P1 | FIX | IN_PROGRESS | @devA | @reviewerB |  | [PR#123](https://example.invalid/pr/123) | 2026-02-08T10:10:00Z | 2026-02-08T10:20:00Z |  |

## 3) Security Hotspot 台账

| hotspotKey | issueKey | riskLevel | reviewStatus | disposition | rationale | reviewer | acceptedBy | reviewedAt |
|------------|----------|-----------|--------------|-------------|-----------|----------|------------|------------|
| HOTSPOT-EXAMPLE-001 | ISSUE-EXAMPLE-001 | MEDIUM | IN_REVIEW | FIX | 需要替换不安全实现 | @securityReviewer |  | 2026-02-08T11:00:00Z |

## 4) Snapshot 台账

| snapshotId | baselineAnalysisKey | targetAnalysisKey | gateStatus | totalIssuesBaseline | totalIssuesCurrent | highImpactBaseline | highImpactCurrent | vulnerabilitiesBaseline | vulnerabilitiesCurrent | hotspotsReviewedRate | newCoverage | newDuplicatedLinesDensity | generatedAt |
|------------|---------------------|-------------------|------------|---------------------|--------------------|--------------------|-------------------|------------------------|-----------------------|----------------------|-------------|---------------------------|-------------|
| SNAPSHOT-EXAMPLE-001 | baseline-20260208 | branch-20260208 | FAIL | 978 | 950 | 120 | 100 | 3 | 1 | 80 | 45.5 | 3.4 | 2026-02-08T12:00:00Z |

## 5) 使用说明

1. 每次 Sonar 分析完成后，先更新 Issue 与 Hotspot，再更新 Task 与 Snapshot。
2. `targetOutcome` 为 `DEFER` / `FALSE_POSITIVE` / `EXEMPT` 时，`decisionReason` 必填。
3. `disposition=ACCEPT_RISK` 时必须填写 `rationale`、`acceptedBy`、`reviewer`。
4. 台账完成后请在对应 Story 目录补充摘要文档并链接至此文件。
