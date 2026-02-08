# Phase 0 Research: SonarCloud 报告问题修复优化

## 研究范围

围绕计划阶段识别的关键澄清项与技术选型展开：
- 覆盖率门槛达标路径（`new_coverage`）
- 新代码重复率达标路径（`new_duplicated_lines_density`）
- Security Hotspot 100% 评审闭环（`new_security_hotspots_reviewed`）
- 修复范围边界（自有模块 vs 第三方封装）
- 自动化测试策略与回归成本控制
- 问题治理优先级编排

## 结论清单

### 1) 覆盖率门槛达标策略

- Decision: 在现有 Sonar 扫描流程中补齐“测试执行 + JaCoCo XML 上传”，并将覆盖率统计聚焦到本轮改动模块与新代码窗口。
- Rationale: 当前 CI 仅执行 `assembleDebug` 并注入 `sonar.java.binaries`，未生成覆盖率报告，导致 `new_coverage=0.0`。要满足质量门 `>=80`，必须让 Sonar 接收到 XML 覆盖率数据。
- Alternatives considered:
  - 仅增加测试不上传覆盖率：无法改变 Sonar 覆盖率结果，拒绝。
  - 通过大范围 `sonar.coverage.exclusions` 降低门槛压力：可短期过门但审计风险高，仅允许对明确非业务代码做最小排除。

### 2) 新代码重复率控制策略

- Decision: 以“重构去重优先、配置排除兜底”为原则，将重复逻辑抽取为复用函数/组件；仅对模板或生成产物使用受控 CPD 排除。
- Rationale: 当前 `new_duplicated_lines_density=3.6`，超过阈值 3。直接重构可保持规则一致性与可维护性，避免长期依赖排除策略。
- Alternatives considered:
  - 纯排除法（大面积 `sonar.cpd.exclusions`）：容易掩盖真实维护问题，拒绝。
  - 仅修高严重级别问题不处理重复：质量门仍失败，拒绝。

### 3) Security Hotspot 评审闭环

- Decision: 所有热点必须在 SonarCloud 分支/PR 分析线上完成评审；高风险必须修复，中低风险可在具备理由、接受人、复核结论后标记接受风险。
- Rationale: 质量门要求 `new_security_hotspots_reviewed=100`，该指标依赖 SonarCloud UI 的评审状态而非代码提交本身。
- Alternatives considered:
  - 延后热点评审到发布后：不满足本轮验收条件，拒绝。
  - 统一接受风险：违背“高风险必须修复”的规格要求，拒绝。

### 4) 修复范围边界

- Decision: 实质修复仅覆盖 `settings.gradle.kts` 纳入的自有业务/核心模块；`repository:*`（AAR 封装模块）仅允许豁免说明与复核，不做深度改造。
- Rationale: 规格明确第三方或外部引入源码不属于本轮改造对象，同时需要留存可追溯豁免记录。
- Alternatives considered:
  - 将 `repository/*` 一并深度修复：回归风险高且维护成本不可控，拒绝。
  - 仅修 feature 模块忽略 core 模块：会遗漏高复用底层问题，拒绝。

### 5) 自动化测试策略

- Decision: 采用“JVM 单测优先 + 必要 Android 仪表测试补位”的双层验证；每个改动点至少新增/更新 1 个自动化测试。
- Rationale: 工程已具备 JUnit4、Robolectric、Coroutines Test、Media3 Test Utils。单测执行快且可扩展；TV/DPAD 与播放链路再由少量 instrumentation 覆盖关键路径。
- Alternatives considered:
  - 仅 instrumentation：成本高、反馈慢，拒绝。
  - 仅单测：对 TV 焦点与播放端到端行为覆盖不足，拒绝。

### 6) 问题治理优先级

- Decision: 按三阶段执行：P0 安全与高风险闭环（漏洞 + 热点）→ P1 前 10 高问题文件集中治理 → P2 模块化批量清理与验收收口。
- Rationale: 与 Spec 的 P1/P2/P3 优先级一致，且能最大化“单位改动收益”，同时降低回归爆炸风险。
- Alternatives considered:
  - 全仓规则类型一次性清扫：改动面过大、回归风险高，拒绝。
  - 按模块全量扫而不分风险：不满足高风险优先原则，拒绝。

### 7) 可追溯性数据结构

- Decision: 使用“问题项 + 修复任务 + 热点评审 + 对比快照 + 豁免记录”五类实体，强制记录证据链接（Issue URL/PR/Commit/测试报告/审批记录）。
- Rationale: 该组合能完整覆盖 FR-005/006/007/011/013 的审计需求，并支持复核者按问题标识逆向追踪。
- Alternatives considered:
  - 单实体记录全部信息：状态语义混杂，不利于审计，拒绝。
  - 仅保留人工台账：缺少状态机约束与一致性校验，拒绝。

## NEEDS CLARIFICATION 关闭结果

- 覆盖率如何进入 Sonar 统计：已明确（测试 + JaCoCo XML + Sonar 路径配置）。
- 热点评审闭环如何验收：已明确（分支/PR 分析线 100% reviewed）。
- 第三方目录如何处理：已明确（豁免说明 + 复核，不做深度改造）。
- 重复率超阈值如何达标：已明确（重构去重优先，排除兜底且受控）。

以上澄清项已全部收敛，无未决项残留。
