# Change: 落地 `document/code_quality_audit` 治理 Backlog（G-T0001~G-T0068）

## Why

当前仓库已经完成了“分模块代码质量排查”，并在 `document/code_quality_audit/global/summary.md` 与 `document/code_quality_audit/global/backlog.md` 中沉淀了 **68 条可执行治理任务**（P1=26、P2=40、P3=2）。  
但这些问题目前仍停留在文档阶段：安全与隐私风险（TLS/凭证/日志）、稳定性隐患（潜在崩溃点）、架构一致性漂移（多处重复实现/依赖泄漏）仍会持续影响可维护性与发行风险。

本变更的目标是：以 Backlog 为唯一事实来源，把治理任务逐条落地到代码与门禁，完成后在文档中把对应任务状态同步为 Done，形成“排查→治理→可回归”的闭环。

## What Changes

- **按 Backlog 落地**：以 `document/code_quality_audit/global/summary.md` 的 G-T0001~G-T0068 为唯一清单，逐条实现并验证。
- **安全/隐私基线**（优先 P1）：
  - 默认启用严格 TLS，收敛/下线 `UnsafeOkHttpClient`，并为特殊场景提供“用户显式开关（Release 允许）”的可控降级。
  - 去除源码硬编码敏感凭证（如 B 站 TV `APP_KEY/APP_SEC`），改为构建期注入/本地配置；Release 未配置时**禁用能力并提示用户配置**。
  - 落地统一日志/异常上报脱敏与上下文构建，消除 `printStackTrace()` 等非结构化输出。
  - 用更安全的对称加密替换投屏 UDP 旧实现，引入版本/兼容策略。
  - 解压链路补齐线程、取消、资源释放、以及压缩炸弹防护策略。
- **架构收敛与复用治理**（P2/P3）：
  - 抽取与迁移：OkHttpClientFactory、Result/失败处理助手、PreferenceDataStore 映射、投屏协议公共能力、本地代理能力等。
  - 逐步下沉：将多处 ViewModel 持久化/扫描/下载编排下沉到 repository/usecase，收敛 DAO 访问口径。
  - 依赖治理：减少/消除 `api(...)` 依赖透传与第三方类型扩散；统一 wrapper AAR 的 Gradle 封装方式。
  - 数据库治理：开启 Room schema 导出与迁移校验门禁；完善手动迁移异常可观测性。
  - 回归用例：补齐解压 instrumentation 用例；收敛 media3/字幕遥测相关包结构。

## Capabilities

### Added Capabilities

- `code-quality-security-privacy-baseline`
- `code-quality-observability-privacy-baseline`
- `code-quality-maintainability-governance`

## Impact

- **影响模块**：几乎覆盖所有构建内模块（21 个）；其中对 `:core_network_component`、`:core_log_component`、`:core_system_component`、`:core_storage_component`、`:player_component`、`:bilibili_component`、`:data_component`、`:core_database_component` 影响最大（横切能力与迁移调用面广）。
- **行为变化风险**：
  - TLS 默认更严格可能影响自签证书/不规范站点访问（WebDAV/代理播放等）。
  - 去硬编码凭证会影响“开箱即用”的能力（例如 B 站 TV 登录/签名）。
  - 加密/协议升级可能导致旧端互通问题（投屏 UDP）。
- **风险缓解**：
  - 所有降级能力必须为**用户显式开关**（Release 允许，但必须强警告），并配套 UI 文案/风险提示。
  - 引入版本字段与兼容解析，优先做到“新端兼容旧端”或“可控降级/明确报错”。
  - 全链路使用统一脱敏工具，避免治理过程中扩大敏感信息泄露面。

## Out of Scope

- 不新增与治理无关的业务功能；不引入跨 feature 的直接依赖（必须遵守依赖治理规则）。
- 不处理 `settings.gradle.kts` 之外的“观察项”目录（仅在 Backlog 明确标注为范围外时考虑）。

## Decisions（已确认）

> 确认时间：2026-02-04

1) **B 站 TV `APP_KEY/APP_SEC`**：Release 允许“未配置则禁用并提示用户配置”（不强制构建期注入）。
2) **不安全 TLS**：Release 允许提供“允许不安全证书/忽略主机名校验”的**用户显式**开关（强警告，默认关闭）。
3) **直接移除整条链路**：
   - `G-T0020`：移除投屏 Sender 整条链路（删除 stub/注释旧实现与对外暴露入口）。
   - `G-T0054`：移除“发送弹幕”整条链路（删除相关 UI/代码并移除依赖）。
