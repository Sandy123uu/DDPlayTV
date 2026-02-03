# 实施计划：代码质量排查与复用治理（分模块）

**Branch**: `[001-code-quality-audit]` | **Date**: 2026-02-03 | **Spec**: `specs/001-code-quality-audit/spec.md`  
**Input**: Feature specification from `specs/001-code-quality-audit/spec.md`

**Note**：本模板由 `/speckit.plan` 生成与维护；如需校验前置条件，可参考 `.specify/scripts/bash/check-prerequisites.sh`。

## Summary

对当前 DDPlayTV 代码库做“分模块代码质量排查与复用治理”的规划：明确排查范围与模块分组、统一的排查维度与报告模板、Finding/Task 的 ID 规则、以及 Impact×Effort → P1/P2/P3 的统一分级口径；并以“逐步汇总”的方式把模块报告增量合并为全局问题清单与治理路线图，支持跨模块去重、统一落点与风险/依赖追踪。

本次 `/speckit.plan` 的交付物聚焦**可执行的规划与规范化产出物**（research/data-model/contracts/quickstart），不包含实际重构代码落地（作为后续独立阶段推进）。

## Technical Context

**Language/Version**: Kotlin 1.9.25（JVM target 1.8），Android Gradle Plugin 8.7.2，Gradle 8.9  
**Primary Dependencies**: AndroidX、Kotlin Coroutines、Retrofit+OkHttp、Moshi、Room、MMKV、Media3（可开关）、NanoHTTPD（本地代理）、ARouter、ktlint（`org.jlleitschuh.gradle.ktlint`）  
**Storage**: Room（SQLite）+ MMKV（Key-Value）+ 本地缓存文件（字幕/弹幕/临时清单/图片等）  
**Testing**: `./gradlew testDebugUnitTest`（JVM 单测）、`./gradlew connectedDebugAndroidTest`（设备/模拟器）；主要使用 JUnit4/AndroidX Test  
**Target Platform**: Android（minSdk 21 / targetSdk 35；手机 + TV；TV 端交互优先）  
**Project Type**: Android 多模块（MVVM）；`:app` 为组合根（composition root），业务特性与基础能力分离  
**Performance Goals**: 对齐 `spec.md` 成功指标：1 个工作日内完成分工（SC-001）；纳入范围内模块 100% 有结论（SC-002）；P1 100% 具备治理结论与可执行任务（SC-003）；抽查 10 条 Finding ≥9 条可追溯且影响清晰（SC-004）  
**Constraints**:
- 遵守模块依赖治理：禁止 feature ↔ feature 直接依赖；跨模块协作通过 `:core_contract_component` 的契约/路由/Service（FR-006 / AC-FR-006）
- Finding “证据”最低颗粒度：文件路径 + 关键符号名（类/方法/函数）（Clarifications）
- 对“多实现”必须分类：有意 vs 无意，并给出保留/统一/废弃结论与理由（FR-005 / AC-FR-005）
- 优先级分级口径固定：Impact×Effort（3×3）映射到 P1/P2/P3（FR-011）
- `adb logcat` 不可全文倾倒，必须按 tag/pid/关键字过滤（日志可能包含其他 app/系统 log）
- `.specify/scripts/bash/setup-plan.sh` 当前会提示 numeric prefix `001` 存在多个 spec 目录；本计划以 `specs/001-code-quality-audit` 为准，后续需决定是否调整 prefix 规则/工具校验逻辑（不阻塞本次文档产出）
**Scale/Scope**: 约 21 个 Gradle module（含 repository wrappers）；本期范围仅做“规划 + 模板 + 规范化产出物”，不做实质性重构改动

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

`.specify/memory/constitution.md` 目前为占位模板，未给出可执行的强约束条款。本计划将以“仓库 AGENTS 规则 + 本 spec 的验收标准”作为门禁（GATE）：

- 产出物完整：生成 `research.md` / `data-model.md` / `quickstart.md` / `contracts/*`，且无 `NEEDS CLARIFICATION` 遗留
- 口径统一：模块分组/排查维度/报告结构/ID 规则/优先级矩阵在所有产出物中一致
- 可追溯：任意 Finding/Task 均可追溯到文件路径 + 关键符号名（并可归属模块/目录范围）
- 依赖治理不被破坏：任何治理建议不得引入 feature ↔ feature 依赖或依赖环（FR-006）
- 可执行：每条 P1 Finding 至少对应 1 条可验收的 Refactor Task（AC-FR-008 / SC-003）

**Phase 1 复核结果**：PASS（已生成 Phase 0/1 产出物，且未遗留 `NEEDS CLARIFICATION`）。

## Project Structure

### Documentation (this feature)

```text
specs/001-code-quality-audit/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/
anime_component/
local_component/
player_component/
storage_component/
user_component/

core_contract_component/
core_log_component/
core_system_component/
core_network_component/
core_database_component/
core_storage_component/
core_ui_component/

bilibili_component/
data_component/
repository/
buildSrc/

document/                # 架构与依赖治理文档
scripts/                 # 开发/排查脚本（如有）
.specify/                # 规格与计划工具链
```

**结构说明（必须）**：

- 本特性是“治理规划与产出物”而非代码功能开发；计划文档位于 `specs/001-code-quality-audit/`。
- 后续“按模块排查”执行阶段，将以 `settings.gradle.kts` 的模块清单为准，并遵循 `document/architecture/module_dependency_governance.md` 的依赖治理口径。

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

无（当前计划不引入需要破例的复杂度项）。
