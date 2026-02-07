# 统一排查维度与证据标准

本文件用于固化“排查维度清单”与“证据最低要求”，确保不同负责人输出的模块报告具备可比性与可追溯性。

## 1. 证据最低要求（强制）

每条 Finding/Task 至少包含：

- 文件路径（相对仓库根目录）
- 关键符号名（类/方法/函数）

建议同时包含：

- 1 条可复现定位命令（例如 `rg "关键字" -n <moduleDir>`）
- 若涉及日志：必须给出过滤条件（tag / pid / 关键字），禁止全文倾倒 `adb logcat`

## 2. 排查维度（统一口径）

### 2.1 Duplication（重复实现 / 多版本实现）

关注点：

- 同一功能存在多版本/多路径实现（模块内或跨模块）
- API/工具类/数据转换/播放器适配等存在并行实现

证据建议：

- 对比两处（或多处）实现的文件路径 + 关键符号
- 必要时使用 ast-grep 做“语法级确证”（例如同一方法调用形态、同名不同参等）

补充要求（强制）：

- 若属于“多实现”，必须填写分类与结论（见 2.5）

### 2.2 Redundancy（冗余 / 低收益代码）

关注点：

- 未使用/基本不可达的代码（历史遗留、废弃入口）
- 重复封装但收益低（仅转调、无语义增量）
- 过度分层导致维护成本上升

证据建议：

- 关键符号定位 + 调用链/引用点数量（可用 `rg` / IDE 查引用）
- 若涉及 Gradle 依赖或模块边界：引用 `verifyModuleDependencies` 的约束说明

### 2.3 Reuse Opportunity（复用机会 / 可抽取能力）

关注点：

- 多个模块都需要的共性能力（解析、缓存、协议适配、UI 控件、错误处理等）
- 可以下沉到更合适模块的能力（遵循模块分层语义）

证据建议：

- 给出“当前散落点”列表（路径+符号）与“建议统一落点”
- 明确迁移风险与回归关注点（调用点/数据格式/行为差异）

### 2.4 Architecture Consistency（架构一致性风险）

关注点：

- 违反模块分层语义（例如 feature ↔ feature 直接依赖倾向）
- 职责漂移：模块边界不清导致能力堆积/重复
- API 形态不一致（同类能力在不同模块暴露方式不同）

证据建议：

- 引用仓库治理文档（例如 `document/architecture/module_dependency_governance.md`）
- 用门禁任务验证约束：`./gradlew verifyModuleDependencies`

### 2.5 Security & Privacy（安全与隐私：仅明显高风险项）

范围（本期仅覆盖明显风险，避免扩大范围）：

- 硬编码密钥/Token（字符串常量、资源、BuildConfig 等）
- 敏感信息日志输出（token/cookie/deviceId/手机号/定位等）
- 明文存储敏感数据（SharedPreferences/MMKV/文件未加密）

证据建议：

- 关键字定位命令（`rg`）+ 文件路径 + 关键符号
- 若需要运行期证据：使用过滤后的 `adb logcat` 片段（禁止全量）

## 3. 多实现分类与结论（强制）

当某条 Finding 认为“同一功能存在多个实现路径”时，必须补充：

- 类型：有意（Intentional）/ 无意（Unintentional）
- 结论：保留（Keep）/ 统一（Unify）/ 废弃（Deprecate）
- 理由与边界说明（避免误合并导致行为回退）

## 4. 推荐工具链（方法口径）

- 探索阶段：`rg`（先快后准）
- 确证阶段：ast-grep（按语法匹配，减少误判）
- 约束校验：`./gradlew verifyModuleDependencies` / `./gradlew verifyArchitectureGovernance`
- 日志：`adb logcat` 必须过滤（见 `document/code_quality_audit/runlogs/README.md`）

## 5. DATA-T003 包结构约定（Media3/字幕遥测）

为提升可发现性并减少同类类型散落，`data_component` 中 media3/字幕遥测相关类型采用以下统一命名空间：

- `com.xyoye.data_component.media3.dto`：跨模块传输 DTO（请求/响应）。
- `com.xyoye.data_component.media3.entity`：Media3 领域实体/枚举（含 Room Entity 与契约模型）。
- `com.xyoye.data_component.media3.telemetry.subtitle`：字幕遥测数据模型（sample/snapshot/fallback/state/target）。

迁移策略：优先以“单批次可编译通过”为门槛逐步迁移，必要时先保留过渡兼容层，避免一次性大改造成回归风险扩大。
