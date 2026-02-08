# SonarCloud 质量修复证据目录

本目录用于沉淀 `001-fix-sonarcloud-issues` 的全过程证据，确保“问题 -> 修复任务 -> 测试/评审 -> 快照验收”可追溯。

## 目录结构

```text
specs/001-fix-sonarcloud-issues/evidence/
├── README.md                          # 本索引
├── baseline/                          # 基线导出与初始风险清单
├── us1/                               # User Story 1 证据
├── us2/                               # User Story 2 证据
├── us3/                               # User Story 3 证据
├── runlogs/                           # 命令执行日志（Gradle/Sonar/回归）
├── tracking/                          # 任务台账、豁免登记、最终快照等
└── templates/                         # 证据模板
```

## 证据维护约定

1. 所有新增证据文件命名使用小写短横线风格（`kebab-case`）。
2. 每次关键命令执行后，在 `runlogs/` 记录命令、结果和日志结尾。
3. 每个修复任务都要在 `tracking/remediation_tasks.csv` 维护状态、负责人、证据链接。
4. `repository/*` 范围的问题仅记录豁免，不纳入实质代码改造统计。
5. 阶段验收文件必须指向功能分支或 PR 的分析线结果，避免使用无关分析数据。

## 与任务的对应关系（Phase 1）

- T001: 本索引与目录初始化。
- T003: `templates/remediation-ledger-template.md`。
- T004: `runlogs/command-log-template.md`。
- T005: `tracking/remediation_tasks.csv`。

后续阶段产物请按 `tasks.md` 的任务编号持续补齐。
