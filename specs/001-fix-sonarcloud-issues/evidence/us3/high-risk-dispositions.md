# 遗留高风险问题处置登记

- 生成时间：2026-02-08T08:41:21.218455+00:00
- 分析线标识：`07c4be1f-f37a-418a-926f-2a13a7a15f86`
- 处置总数：5
- 高风险条目：3

## 处置分布

| Outcome | Count |
|---------|-------|
| `ACCEPT_RISK` | 1 |
| `DEFER` | 2 |
| `EXEMPT` | 1 |
| `FALSE_POSITIVE` | 1 |

## 条目明细

| # | issueKey | Outcome | Priority | 高风险 | 理由 | 接受人 | 复核结论 | 证据 |
|---|----------|---------|----------|--------|------|--------|----------|------|
| 1 | `AZw4PUrvpBg_nGQ6xCmc` | `ACCEPT_RISK` | `P3` | 是 | 当前语义转换仅影响内部日志格式，风险可控并已有人工审计兜底。 | security-owner | APPROVED | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZw4PUrvpBg_nGQ6xCmc`<br>`commit:us3-risk-accept` |
| 2 | `AZw4PUrvpBg_nGQ6xCml` | `DEFER` | `P3` | 是 | 复杂度拆分涉及多接口返回结构，计划在下一轮专项重构统一处理。 | - | - | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZw4PUrvpBg_nGQ6xCml` |
| 3 | `AZw4PUrvpBg_nGQ6xCmm` | `DEFER` | `P3` | 是 | 与 AAR 依赖解包流程耦合，当前迭代内优先保证播放链路稳定。 | - | - | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZw4PUrvpBg_nGQ6xCmm` |
| 4 | `TP-ISSUE-001` | `EXEMPT` | `P3` | 否 | 第三方 AAR 封装目录，按治理边界仅登记豁免不做深改。 | - | - | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=TP-ISSUE-001`<br>`commit:us3-exempt-001` |
| 5 | `AZwoFACRY8ZohlEMpmXt` | `FALSE_POSITIVE` | `P3` | 否 | 测试命名已切换 camelCase，待新分析线同步刷新旧 issue 状态。 | - | - | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZwoFACRY8ZohlEMpmXt` |

> 说明：`DEFER/ACCEPT_RISK/FALSE_POSITIVE/EXEMPT` 均由台账驱动导出；其中 `ACCEPT_RISK` 必须包含接受人与复核结论。
