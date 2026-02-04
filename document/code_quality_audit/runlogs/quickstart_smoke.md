# Quickstart 冒烟流程记录（Phase 6 / Smoke）

- 日期：2026-02-04
- 环境：Windows 11 + WSL2
- 目标：走通 `specs/001-code-quality-audit/quickstart.md` 的最短路径（初始化 → 校验 → 门禁），形成可回放证据

## 1) 初始化模块报告骨架与状态表

命令：

```bash
python3 scripts/code_quality_audit/init_audit_docs.py
```

关键输出：

```text
Init audit docs done.
- Repo root: /home/tzw/workspace/DDPlayTV
- Modules (from settings.gradle.kts): 21
- Module reports: created 0, existing 21
- Module status updated: document/code_quality_audit/global/module_status.md
```

## 2) 文档一致性校验（覆盖率/必填字段/ID 基础校验）

命令：

```bash
python3 scripts/code_quality_audit/validate_audit_docs.py
```

关键输出：

```text
Validate audit docs: PASS
- Repo root: /home/tzw/workspace/DDPlayTV
- Modules: 21
- Issues: errors=0, warns=0
```

## 3) 质量与架构门禁（推荐集合）

命令：

```bash
./gradlew verifyArchitectureGovernance --console=plain
```

执行记录：

- `document/code_quality_audit/runlogs/final_gates.md`

