# 排查规划（US1 / MVP）

- 日期：2026-02-03
- 适用范围：`settings.gradle.kts` 纳入构建的 21 个模块（见 `document/code_quality_audit/global/module_status.md`）
- 规格来源：`specs/001-code-quality-audit/spec.md`

本文件是“按模块代码质量排查与复用治理”的统一入口，目标是让团队**可以直接分工开干**，且产出物口径一致、可追溯、可增量汇总。

## 1. 范围与边界（强制）

### 1.1 纳入范围（Build Scope）

- 以 `settings.gradle.kts` 的 `include(...)` 为准（共 21 个模块）
- 每个模块对应 1 份模块报告：`document/code_quality_audit/modules/<module>.md`
- 覆盖率/Owner/状态统一维护：`document/code_quality_audit/global/module_status.md`

### 1.2 观察项（Out of Build Scope）

- 不在构建范围内但存在于仓库的目录/文件，统一记录在：`document/code_quality_audit/global/observing.md`
- 观察项**不阻塞** US1/US2 主流程，但可作为风险/噪音源记录（必要时在全局汇总里形成治理任务）

## 2. 分组与排查顺序（强制）

模块分组配置：`document/code_quality_audit/config/module_groups.yaml`

推荐顺序（从“复用面大/依赖多/底层”开始）：

1. Base（data/contract）
2. Runtime（system/log）
3. Infrastructure（network/db/storage/bilibili）
4. UI Foundation（core_ui）
5. Feature（业务组件）
6. App Shell
7. repository wrappers（第三方 AAR 封装）

## 3. 产出物与落点（强制）

- 统一口径配置：`document/code_quality_audit/config/`
  - 分组：`module_groups.yaml`
  - 模块 ID 前缀：`module_id_prefixes.yaml`
  - 排查维度：`audit_dimensions.md`
  - 优先级口径：`priority.md`
- 报告模板：`document/code_quality_audit/templates/`
  - 模块报告：`module_report.md`
  - 全局汇总：`global_summary.md`
- 模块报告：`document/code_quality_audit/modules/`
- 全局产出：`document/code_quality_audit/global/`
- 执行记录：`document/code_quality_audit/runlogs/`（初始化脚本输出/门禁/过滤日志片段）

## 4. ID 规则（强制）

模块前缀映射：`document/code_quality_audit/config/module_id_prefixes.yaml`

- 模块内 Finding：`<PREFIX>-F###`（例：`PLAYER-F001`）
- 模块内 Task：`<PREFIX>-T###`（例：`PLAYER-T001`）
- 全局 Findings/Tasks（去重合并后）：`G-F####` / `G-T####`（US3 使用）

> 约束：任意 Finding/Task 必须至少可追溯到 “文件路径 + 关键符号名（类/方法/函数）”。

## 5. 排查维度与证据标准（强制）

排查维度清单：`document/code_quality_audit/config/audit_dimensions.md`

### 5.1 证据最低要求

每条 Finding/Task 至少包含：

- 文件路径（相对仓库根目录）
- 关键符号名（类/方法/函数）

建议同时包含：

- 一条定位命令（例如 `rg "关键词" -n <moduleDir>`）
- 若涉及日志：必须给出过滤条件（tag/pid/关键字），禁止全文倾倒 `adb logcat`（详见 `document/code_quality_audit/runlogs/README.md`）

### 5.2 定位方法（推荐：rg → ast-grep 确证）

探索阶段（快、容错高）：

- `rg "关键词" -n <moduleDir>`

确证阶段（结构敏感、低误报）：

- 使用 ast-grep 做语法级匹配（按实际语法调整 pattern）

示例（已验证可命中本仓库代码）：

- 查找路由表定义：
  - pattern：`object RouteTable { $$$ }`
- 查找 ARouter 路由构建调用：
  - pattern：`ARouter.getInstance().build($PATH)`
- 查找 TV UI Mode 判断调用：
  - pattern：`$A.isTelevisionUiMode()`
  - pattern：`isTelevisionUiMode()`

## 6. 优先级评估（强制）

统一矩阵与判定依据：`document/code_quality_audit/config/priority.md`

要求：

- 每条 Finding/Task 必填 `Impact` / `Effort`，并写明判定依据
- `P1/P2/P3` 必须可由矩阵推导（禁止拍脑袋）

## 7. 分工与节奏（US1 交付点）

### 7.1 分工入口

分工与推进状态统一维护：

- `document/code_quality_audit/global/module_status.md`

要求：

- 每个模块必须有 Owner（可先填“待分配”，但必须在启动排查前补齐）
- 每次提交模块报告时同步更新 Status（Todo/Doing/Review/Done）

### 7.2 建议节奏（可按实际调整）

建议以“组”为单位分工（Base/Runtime/Infra/UI/Feature/App/Repo），并优先完成被多模块依赖的基础能力层，以降低后续重复劳动。

详细建议排期见：`document/code_quality_audit/global/module_status.md`

## 8. 复核与门禁（强制）

### 8.1 模块报告复核清单

- `document/code_quality_audit/global/review_checklist.md`

### 8.2 基础门禁（必须执行并记录）

执行记录落点：`document/code_quality_audit/runlogs/`

要求：

- 记录命令 + 关键输出尾部
- 必须明确写出最终是 `BUILD SUCCESSFUL` 还是 `BUILD FAILED`

推荐（基础且快速）：

- `./gradlew verifyModuleDependencies`
- `./gradlew verifyLegacyPagerApis`

本期已执行一次并记录：`document/code_quality_audit/runlogs/foundation_gates.md`

## 9. 增量汇总（US3 预告）

当至少完成 2 份模块报告后，开始维护：

- `document/code_quality_audit/global/summary.md`（结构参考 `document/code_quality_audit/templates/global_summary.md`）

并持续维护来源映射：

- `G-F####: [<MODULE>-F###, ...]`

