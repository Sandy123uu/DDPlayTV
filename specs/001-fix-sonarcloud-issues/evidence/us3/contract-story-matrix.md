# 契约端点与 User Story 映射矩阵

- 基准契约：`specs/001-fix-sonarcloud-issues/contracts/quality-remediation.openapi.yaml`
- 映射时间：2026-02-08
- 目标：覆盖 contracts 中全部端点，明确每个端点由哪个 Story 产物承接与验证

| # | Contract Endpoint | OperationId | 主要 Story | 对应任务 | 证据文件 |
|---|-------------------|-------------|------------|----------|----------|
| 1 | `GET /quality/issues` | `listQualityIssues` | US1 / US2 / US3 | T021, T039, T046 | `specs/001-fix-sonarcloud-issues/evidence/us1/high-risk-task-map.md`<br>`specs/001-fix-sonarcloud-issues/evidence/us2/top10-delta.md`<br>`specs/001-fix-sonarcloud-issues/evidence/us3/remediation-ledger.md` |
| 2 | `POST /quality/issues/{issueKey}/tasks` | `createRemediationTask` | US1 / US2 / US3 | T021, T046 | `specs/001-fix-sonarcloud-issues/evidence/us1/high-risk-task-map.md`<br>`specs/001-fix-sonarcloud-issues/evidence/tracking/remediation_tasks.csv` |
| 3 | `POST /quality/tasks/{taskId}/transition` | `transitionTask` | US3 | T044, T046, T050 | `specs/001-fix-sonarcloud-issues/evidence/us3/remediation-ledger-validation.json`<br>`specs/001-fix-sonarcloud-issues/evidence/us3/remediation-ledger.md`<br>`specs/001-fix-sonarcloud-issues/evidence/us3/final-acceptance-log.md` |
| 4 | `POST /quality/hotspots/{hotspotKey}/review` | `reviewHotspot` | US1 / US3 | T022, T047 | `specs/001-fix-sonarcloud-issues/evidence/us1/hotspot-review-log.md`<br>`specs/001-fix-sonarcloud-issues/evidence/us3/high-risk-dispositions.md` |
| 5 | `POST /quality/issues/{issueKey}/disposition` | `setIssueDisposition` | US3 | T045, T047 | `specs/001-fix-sonarcloud-issues/evidence/us3/issue-dispositions.json`<br>`specs/001-fix-sonarcloud-issues/evidence/us3/high-risk-dispositions.md` |
| 6 | `POST /quality/snapshots/compare` | `createComparisonSnapshot` | US1 / US3 | T023, T048 | `specs/001-fix-sonarcloud-issues/evidence/us1/us1-snapshot.md`<br>`specs/001-fix-sonarcloud-issues/evidence/us3/quality-gate-compare.md` |
| 7 | `GET /quality/snapshots/{snapshotId}` | `getComparisonSnapshot` | US1 / US3 | T023, T048 | `specs/001-fix-sonarcloud-issues/evidence/us1/us1-compare.json`<br>`specs/001-fix-sonarcloud-issues/evidence/us3/quality-gate-compare.json` |

## 覆盖性结论

- contracts 中 7 个端点均已映射到至少一个 User Story 与已落地证据。
- US3 负责承接“流程状态流转 + 处置导出 + 校验”三类端点闭环。
- 对于需依赖 SonarCloud 新分析线的状态（例如最终 `CLOSED/REVIEWED_FIXED`），当前已在证据中记录待复核动作。
