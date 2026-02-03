# Phase 1：Quickstart（代码质量排查与复用治理）

本文档面向“开始按模块执行排查”的维护者，给出最短路径的分工、排查、记录与增量汇总步骤，并内置可直接复制的模板（模块报告/全局汇总）。

## 1. 前置条件（建议）

1. 明确本次排查范围：以 `settings.gradle.kts` 纳入构建的 21 个模块为主；仓库历史目录作为“观察项”单独记录（不阻塞主流程）。
2. 明确负责人：每个模块至少 1 名负责人（Owner），负责产出模块报告与参与复核。
3. 统一门禁口径（用于评估“治理建议是否会破坏架构约束”）：
   - 推荐：`./gradlew verifyArchitectureGovernance`
   - 依赖治理：`./gradlew verifyModuleDependencies`

> 依赖治理门禁要求：必须确认 Gradle 输出末尾为 `BUILD SUCCESSFUL`，不要只看任务执行过程。

## 2. 排查顺序（推荐）

按分层推进（详见 `research.md`）：

1. 基础能力模块（`core_*` / `bilibili_component` / `data_component`）
2. 业务特性模块（`*_component`）
3. 应用壳层（`:app`）
4. repository wrappers（`:repository:*`：仅审视边界/使用方式，不做结构性治理）

## 3. 统一口径（必须遵守）

### 3.1 证据最低要求（强制）

每条 Finding/Task 至少包含：

- 文件路径（相对仓库根目录）
- 关键符号名（类/方法/函数）

建议同时附带：

- 一条可复现的定位命令（例如 `rg "关键字" -n <moduleDir>`）
- 若涉及日志：必须给出 **过滤条件**（tag/pid/关键字），禁止全文倾倒 `adb logcat`

### 3.2 “多实现”分类与结论（强制）

凡是“同一功能存在多个实现路径”的 Finding，必须填：

- 类型：有意（Intentional）/ 无意（Unintentional）
- 结论：保留（Keep）/ 统一（Unify）/ 废弃（Deprecate）
- 理由与边界说明（避免误合并导致行为回退）

### 3.3 优先级评估（强制）

统一采用 `Impact(高/中/低) × Effort(小/中/大)` → `P1/P2/P3` 的固定矩阵（来自 `spec.md` FR-011）：

| Impact / Effort | 小 | 中 | 大 |
|---|---|---|---|
| 高 | P1 | P1 | P2 |
| 中 | P1 | P2 | P2 |
| 低 | P2 | P3 | P3 |

并要求在模块报告中写明“Impact/Effort 的判定依据”，减少主观差异。

## 4. 执行流程（最短路径）

### 4.1 为每个模块创建一份模块报告

建议文件放置（可按团队习惯调整）：

- `document/code_quality_audit/modules/<modulePath>.md`

然后按下方“模块报告模板”填写。

### 4.2 产出 Finding 与 Task（模块内）

1. 用统一维度扫描模块（重复实现/冗余/复用机会/架构一致性风险/安全隐私风险）
2. 对每条发现填写：
   - 证据（至少路径+符号）
   - 影响/工作量 → 优先级
   - 多实现分类与结论（若适用）
3. 把“建议”落为任务（Task），每条 P1 Finding 至少 1 条 Task

### 4.3 增量汇总到全局清单（至少两份模块报告后开始）

建议文件放置（可按团队习惯调整）：

- `document/code_quality_audit/global/summary.md`

按下方“全局汇总模板”进行去重合并、分配全局 ID，并维护来源映射。

## 5. 常用命令（建议）

### 5.1 架构与质量门禁（建议）

- 推荐集合：`./gradlew verifyArchitectureGovernance`
- 依赖治理：`./gradlew verifyModuleDependencies`
- 旧 API 门禁：`./gradlew verifyLegacyPagerApis`
- 风格检查：`./gradlew ktlintCheck`
- Lint：`./gradlew lint`（或 `./gradlew lintDebug`）

### 5.2 文本搜索（探索阶段）

- 在模块内找关键字：`rg "关键词" -n <moduleDir>`
- 查找某接口/路由：`rg "RouteTable" -n`

### 5.3 AST 精确匹配（确证阶段，结构敏感）

当需要“按语法匹配”时，优先使用 ast-grep（参考仓库 `AGENTS.md` 的用法策略）：

- 例：查找某方法调用形态（需按实际语法调整 pattern）

### 5.4 adb logcat（必须过滤）

禁止直接贴全量 log。推荐过滤方式：

- 按 pid：`adb logcat --pid $(adb shell pidof -s com.okamihoro.ddplaytv)`
- 按 tag：`adb logcat -s DDPlayTV Player Subtitle`
- 关键字二次过滤：`adb logcat --pid <pid> | rg "关键字"`

## 6. 模块报告模板（复制即可用）

> 文件：`document/code_quality_audit/modules/<modulePath>.md`（建议）

```md
# 模块排查报告：<modulePath>

- 模块：<modulePath>
- 负责人：<owner>
- 日期：YYYY-MM-DD
- 范围：<moduleDir 或关键包名>

## 1) 背景与职责

- 模块职责（做什么/不做什么）
- 关键入口/关键路径（可列 3~5 个关键符号）
- 依赖边界（与哪些模块交互，是否存在边界疑点）

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：rg 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核

## 3) Findings（发现列表）

> ID 规则：`<MODULE>-F###`（例如 PLAYER-F001）

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| PLAYER-F001 | Duplication | ... | `path/to/File.kt` + `Foo#bar` | Unintentional | Unify | `:core_xxx_component` | High | Small | P1 | ... |

## 4) Refactor Tasks（治理任务）

> ID 规则：`<MODULE>-T###`（例如 PLAYER-T001）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| PLAYER-T001 | PLAYER-F001 | ... | ... | ... | High | Small | P1 | ... | Draft |

## 5) 风险与回归关注点

- 行为回退风险（哪些场景最敏感）
- 回归成本（需要的账号/媒体文件/设备）

## 6) 备注（历史背景/待确认点）
```

## 7. 全局汇总模板（复制即可用）

> 文件：`document/code_quality_audit/global/summary.md`（建议）

```md
# 全局问题清单与治理路线图（增量）

- 日期：YYYY-MM-DD
- 已汇总模块报告：
  - <modulePath>@YYYY-MM-DD
  - ...

## 1) 全局 Findings（去重合并后）

> 全局 ID：`G-F####`（合并后分配），并保留来源映射。

| G-ID | 类别 | 标题 | 涉及模块 | 来源（模块内 ID 列表） | 结论 | 建议落点 | P |
|---|---|---|---|---|---|---|---|
| G-F0001 | Duplication | ... | :player_component,:local_component | PLAYER-F001,LOCAL-F003 | Unify | :core_xxx_component | P1 |

## 2) 全局 Tasks（治理 Backlog）

| G-ID | 目标 | 关联 G-F | 范围 | 验收标准 | P | 负责人 | 状态 | 依赖 |
|---|---|---|---|---|---|---|---|---|
| G-T0001 | ... | G-F0001 | ... | ... | P1 | ... | Planned | ... |

## 3) 来源映射（必须维护）

- G-F0001: [PLAYER-F001, LOCAL-F003]
- ...

## 4) 路线图（批次建议）

- Batch 1（P1 优先 + 低风险）：...
- Batch 2（P2/P3 或高风险）：...
```

