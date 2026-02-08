# 第三方目录豁免台账

本台账用于记录 `repository/*` 或外部引入源码中的 Sonar 问题豁免申请与复核结论。

## 适用规则

1. 仅允许登记 `THIRD_PARTY_SOURCE` / `UPSTREAM_CONFLICT` / `NON_ACTIONABLE`。
2. `repository/*` 问题不进入常规修复流，必须在此台账保留证据。
3. 每条记录需要给出问题链接、豁免理由、复核人与结论。

## 豁免记录

| exemption_id | issue_key | module_path | file_path | reason_type | rationale | reviewer | review_conclusion | evidence_links | created_at |
|--------------|-----------|-------------|-----------|-------------|-----------|----------|-------------------|----------------|------------|
| EX-US3-001 | TP-ISSUE-001 | repository/danmaku | repository/danmaku/DanmakuFlameMaster.aar | THIRD_PARTY_SOURCE | 三方 AAR 封装目录，源码不可编辑；按 FR-012 仅登记豁免并保留审计证据。 | quality-owner | APPROVED | https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=TP-ISSUE-001 ; commit:us3-exempt-001 | 2026-02-08 |
| EX-INIT-002 | (pending) | repository/immersion_bar | (pending) | THIRD_PARTY_SOURCE | 三方 AAR 封装目录，按规格仅做豁免登记，等待具体问题映射。 | (pending) | (pending) | (pending) | 2026-02-08 |
| EX-INIT-003 | (pending) | repository/panel_switch | (pending) | THIRD_PARTY_SOURCE | 三方 AAR 封装目录，按规格仅做豁免登记，等待具体问题映射。 | (pending) | (pending) | (pending) | 2026-02-08 |
| EX-INIT-004 | (pending) | repository/seven_zip | (pending) | THIRD_PARTY_SOURCE | 三方 AAR 封装目录，按规格仅做豁免登记，等待具体问题映射。 | (pending) | (pending) | (pending) | 2026-02-08 |
| EX-INIT-005 | (pending) | repository/thunder | (pending) | THIRD_PARTY_SOURCE | 三方 AAR 封装目录，按规格仅做豁免登记，等待具体问题映射。 | (pending) | (pending) | (pending) | 2026-02-08 |
| EX-INIT-006 | (pending) | repository/video_cache | (pending) | THIRD_PARTY_SOURCE | 三方 AAR 封装目录，按规格仅做豁免登记，等待具体问题映射。 | (pending) | (pending) | (pending) | 2026-02-08 |

## 复核记录

- 最近更新时间：2026-02-08
- 备注：后续任务按 `issue_key` 补全后，`review_conclusion` 只能取 `APPROVED` 或 `REJECTED`。
