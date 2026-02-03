# 代码质量排查与复用治理（分模块）

本目录用于沉淀“按模块进行代码质量排查/复用治理”的**规范化产出物**：统一口径、统一模板、统一 ID 规则、以及可追踪的模块覆盖率/状态表。  
目标是让团队可以按模块分工推进，并能逐步汇总为全局治理清单与路线图。

> 规格与计划来源：`specs/001-code-quality-audit/`

## 目录结构

```text
document/code_quality_audit/
├── README.md                       # 本入口
├── config/                         # 统一口径配置（分组/ID 前缀/维度/优先级）
├── templates/                      # 报告模板（模块/全局）
├── modules/                        # 各模块排查报告（每个模块一个文件）
├── global/                         # 全局汇总（增量合并 + Backlog + Roadmap）
└── runlogs/                        # 运行记录（脚本输出、门禁执行记录、过滤后的日志片段）
```

## 使用方式（最短路径）

1. **初始化**：运行 `scripts/code_quality_audit/init_audit_docs.py` 生成/更新模块报告骨架与状态表，并把输出记录到 `runlogs/`。
2. **分工**：按 `global/module_status.md` 为每个模块指定负责人（Owner），各自维护对应的 `modules/*.md`。
3. **排查**：遵循 `config/` 的统一口径（维度/证据标准/优先级矩阵/ID 规则），在模块报告中填写 Findings 与 Tasks。
4. **增量汇总**：当至少完成 2 份模块报告后，使用 `templates/global_summary.md` 的结构维护 `global/summary.md`，去重合并并分配全局 ID。

## 关键约束（必须遵守）

- **范围口径**：以 `settings.gradle.kts` 纳入构建的模块为主；非构建目录统一作为“观察项”记录，不阻塞主流程。
- **证据最低要求**：每条 Finding/Task 至少包含“文件路径 + 关键符号名（类/方法/函数）”。
- **多实现必须分类**：有意/无意，并给出保留/统一/废弃结论与理由。
- **优先级口径固定**：统一采用 `Impact(高/中/低) × Effort(小/中/大)` → `P1/P2/P3` 的固定矩阵。
- **日志必须过滤**：`adb logcat` 禁止全文倾倒，详见 `runlogs/README.md`。

