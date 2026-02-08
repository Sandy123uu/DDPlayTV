## Why

`.sonarcloud-report` 显示当前项目质量门为 `ERROR`，且阻塞项集中在 `new_coverage=0.0`、`new_duplicated_lines_density=3.6`、`new_security_hotspots_reviewed=0.0`，同时仍有 978 个未关闭问题与 3 个漏洞。若不尽快治理，会持续抬高回归与发布风险，并削弱后续增量开发的可维护性。

## What Changes

- 以 `.sonarcloud-report` 为基线，按“质量门阻塞项优先”建立可执行治理范围，并明确完成判定标准。
- 修复 SonarCloud 报告中的安全相关问题（含漏洞与未审查的 Security Hotspot），将处理结果落地到代码与记录。
- 针对高频高严重 Code Smell（如过高认知复杂度、缺失 `switch default`、重复逻辑）进行分批重构，优先覆盖问题密度最高文件。
- 为本次改动补齐必要单元测试，提升 new code 覆盖率，确保变更不会因测试缺失再次触发质量门失败。
- 在不改变既有业务语义的前提下，统一修复风格与代码规范，降低后续同类问题反复出现概率。

## Capabilities

### New Capabilities
- `sonarcloud-quality-gate-remediation`: 定义并落地质量门阻塞指标（新代码覆盖率、重复率、Hotspot 审查率）的修复与验收流程。
- `sonarcloud-priority-issue-fix`: 建立基于报告优先级的批量修复能力，先处理漏洞与高严重度问题，再推进高频 Code Smell 收敛。
- `sonarcloud-governance-traceability`: 为问题修复建立可追踪记录（问题来源、修复状态、验证结果），支持后续持续治理。

### Modified Capabilities
- 无

## Impact

- 受影响模块：`app`、`core_system_component`、`anime_component`、`player_component`、`core_ui_component` 及其相关测试代码。
- 受影响系统：SonarCloud 质量门结果、CI 质量检查与本地代码质量回归流程。
- 外部 API/协议：无新增对外接口；主要为内部代码质量与安全修复，不引入破坏性对外行为变更。
