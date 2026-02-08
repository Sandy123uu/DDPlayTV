# Implementation Plan: SonarCloud 报告问题修复优化

**Branch**: `001-fix-sonarcloud-issues` | **Date**: 2026-02-08 | **Spec**: `/specs/001-fix-sonarcloud-issues/spec.md`
**Input**: Feature specification from `/specs/001-fix-sonarcloud-issues/spec.md`

## Summary

本次实现以 `.sonarcloud-report` 的 2026-02-08 基线为起点，围绕“质量门整体通过”开展分阶段治理：
1) 优先清零漏洞并完成 100% 热点评审；
2) 集中治理问题最多的前 10 个文件并压降高影响问题；
3) 为所有改动补充自动化测试与可追溯证据，保证核心路径（浏览/播放/搜索/设置）零阻断回归；
4) 输出分支/PR 分析线的前后对比快照用于验收。

## Technical Context

**Language/Version**: Kotlin 1.9.25 (JVM target 1.8), Java 8  
**Primary Dependencies**: Android Gradle Plugin 8.7.2, AndroidX Test/JUnit4, Robolectric 4.12.2, Kotlin Coroutines Test, SonarCloud Scan Action v6  
**Storage**: N/A（不新增业务持久化）；使用 `.sonarcloud-report/*.json|*.md` 作为分析输入与 `specs/001-fix-sonarcloud-issues/` 文档产出  
**Testing**: `./gradlew testDebugUnitTest` + 模块级 Robolectric/JUnit；必要时补 `./gradlew connectedDebugAndroidTest`；新增 JaCoCo XML 供 Sonar 覆盖率统计  
**Target Platform**: Android (Mobile + TV) 多模块工程；GitHub Actions `ubuntu-latest` Sonar 分析环境  
**Project Type**: Android multi-module application (mobile + TV)  
**Performance Goals**: 分支或 PR 质量门通过；`new_coverage >= 80`、`new_duplicated_lines_density <= 3`、`new_security_hotspots_reviewed = 100`；高影响问题较基线下降 >= 50%  
**Constraints**: 仅修复自有业务与基础模块；`repository/*` 外部封装仅做豁免说明；不得引入新的漏洞/高影响问题；保持 TV DPAD 可达性与核心路径可用  
**Scale/Scope**: 基线 978 issues + 52 hotspots；治理重点覆盖前 10 高问题文件与所有漏洞/高风险热点，涉及 15 个一方模块 + `app` 组合层

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Phase 0 前 Gate 评估

| Gate | Status | 说明 |
|------|--------|------|
| 宪章约束可执行性 | PASS | `.specify/memory/constitution.md` 当前为占位模板，暂无可判定的强制条款；本计划改用仓库既有治理规则（AGENTS/Spec/Gradle 校验）执行。 |
| 范围边界合规 | PASS | 仅纳入 `settings.gradle.kts` 中一方模块进行实质性修复；`repository:*` 采用豁免+复核记录，不做深度改造。 |
| 可验证性交付 | PASS | 计划要求质量门、测试、热点、对比快照四类证据齐备；任何未闭环项不得宣告验收通过。 |

### Phase 1 后 Gate 复评（设计完成后）

| Gate | Status | 说明 |
|------|--------|------|
| 数据模型可审计性 | PASS | `data-model.md` 已定义问题项、修复任务、热点评审、快照与豁免记录，并包含字段校验与状态流转。 |
| 契约完备性 | PASS | `contracts/quality-remediation.openapi.yaml` 覆盖问题检索、任务创建/流转、热点评审、豁免处置、快照对比。 |
| 执行可落地性 | PASS | `quickstart.md` 提供从基线确认到分支验收的端到端步骤与命令，含失败回退与证据归档要求。 |

## Project Structure

### Documentation (this feature)

```text
specs/001-fix-sonarcloud-issues/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── quality-remediation.openapi.yaml
└── tasks.md              # Phase 2 由 /speckit.tasks 生成
```

### Source Code (repository root)

```text
.github/workflows/
└── sonarcloud.yml

.sonarcloud-report/
├── sonarcloud-report.json
└── sonarcloud-report.md

app/
anime_component/
local_component/
player_component/
storage_component/
user_component/
bilibili_component/
core_contract_component/
core_database_component/
core_log_component/
core_network_component/
core_storage_component/
core_system_component/
core_ui_component/
data_component/

repository/
├── danmaku/
├── immersion_bar/
├── panel_switch/
├── seven_zip/
├── thunder/
└── video_cache/
```

**Structure Decision**: 保持既有 Android 多模块结构，不新增生产模块；修复代码按现有模块内聚落地，质量治理文档和验收产物统一沉淀到 `specs/001-fix-sonarcloud-issues/`。

## Complexity Tracking

> 本次设计未引入违反治理规则的额外复杂度。

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| N/A | N/A | N/A |
