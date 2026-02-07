# 模块报告复核清单（Reviewer Checklist）

用于复核 `document/code_quality_audit/modules/*.md` 的一致性与可追溯性，避免“口径漂移/证据不足/结论不可执行”。

## 1) 基础信息

- [ ] 标题/元信息齐全：模块、负责人、日期、范围已填写
- [ ] 范围合理：范围描述与 `settings.gradle.kts` 的模块目录/关键包名一致（未把观察项当作主证据）
- [ ] 状态表同步：`document/code_quality_audit/global/module_status.md` 的 Owner/Status 与报告一致

## 2) 口径一致性

- [ ] 引用统一维度：对齐 `document/code_quality_audit/config/audit_dimensions.md`
- [ ] 引用统一优先级：对齐 `document/code_quality_audit/config/priority.md`，P 值可由矩阵推导
- [ ] ID 规则正确：Finding/Task 使用本模块 `<PREFIX>-F### / <PREFIX>-T###`（PREFIX 来自 `module_id_prefixes.yaml`）

## 3) 证据与可追溯性（强制）

- [ ] 每条 Finding/Task 至少包含：文件路径 + 关键符号名（类/方法/函数）
- [ ] 如引用日志：提供过滤条件（pid/tag/关键字），且仅保留必要片段（禁止全文 logcat）
- [ ] 如引用搜索：建议附定位命令（`rg` / ast-grep pattern）

## 4) 多实现（Duplication）条目（强制）

- [ ] 已分类：有意（Intentional）/ 无意（Unintentional）
- [ ] 已给结论：保留（Keep）/ 统一（Unify）/ 废弃（Deprecate）
- [ ] 已说明边界与理由：避免误合并造成行为回退

## 5) 结论可执行性

- [ ] P1 Finding 均至少对应 1 条可验收的治理 Task（含验收标准）
- [ ] Task 的建议落点不破坏依赖治理：禁止 feature ↔ feature 直接依赖；跨模块协作通过 `:core_contract_component` 契约/路由/Service
- [ ] 风险与回归关注点明确：写清敏感场景、回归成本（账号/媒体文件/设备）

## 6) 格式与可读性

- [ ] 报告结构遵循模板：`document/code_quality_audit/templates/module_report.md`
- [ ] 表格字段不缺失（尤其是 Impact/Effort/P、证据、多实现字段）
- [ ] 文字描述可读、可复核：避免“只有结论没有依据”

