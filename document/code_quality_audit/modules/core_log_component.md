# 模块排查报告：:core_log_component

- 模块：:core_log_component
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：core_log_component/src/main/java/com/xyoye/common_component/（log/ + config/ + utils/）

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`CORE_LOG-F###`  
> - Task：`CORE_LOG-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 提供统一的结构化日志系统：门面（`LogFacade`）、运行时（`LogSystem`）、写入调度（`LogWriter`）、格式化（`LogFormatter`）。
  - 提供可持久化的日志策略与调试开关：`LogPolicyRepository` + `LogConfigTable`（MMKV 注解表）。
  - 管理日志文件（双文件 + MediaStore/内部目录回退）：`LogFileManager` + `LogPaths`。
  - 提供可选的 TCP 日志服务（用于开发/远程 tail）：`TcpLogServerManager` / `TcpLogServer`。
  - Crash/异常上报与相关能力封装：`BuglyReporter`、`ErrorReportHelper`，以及字幕侧高信号日志（`SubtitleTelemetryLogger`/`SubtitleFallbackReporter`）。
- 模块职责（不做什么）
  - 不依赖 `:core_system_component`（初始化与 wiring 由 runtime 层负责；本模块仅提供能力与可调用入口）。
  - 不把业务模块强耦合进日志系统（通过 `LogModule`/结构化 context 表达，而不是引入业务依赖）。
- 关键入口/关键路径（示例）
  - `core_log_component/src/main/java/com/xyoye/common_component/log/LogSystem.kt` + `LogSystem#init`
  - `core_log_component/src/main/java/com/xyoye/common_component/log/LogFacade.kt` + `LogFacade#log`
  - `core_log_component/src/main/java/com/xyoye/common_component/log/LogFileManager.kt` + `LogFileManager#appendLine`
  - `core_log_component/src/main/java/com/xyoye/common_component/log/tcp/TcpLogServerManager.kt` + `TcpLogServerManager#setEnabled`
  - `core_log_component/src/main/java/com/xyoye/common_component/log/BuglyReporter.kt` + `BuglyReporter#init`
- 依赖边界
  - 对外（被依赖）：几乎所有模块都会依赖日志门面（尤其是 `LogFacade`/`ErrorReportHelper`）。
  - 对内（依赖）：MMKV、Bugly（CrashReport）；并通过 `implementation(project(":data_component"))` 使用部分数据类型（避免对外泄露 transitive 依赖）。
  - 边界疑点：日志内容可能携带敏感字段（token/cookie 等）；TCP server/落盘属于“可外泄渠道”，需要明确的脱敏与开关策略。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - `rg`：检索 `BuglyReporter`/`ErrorReportHelper`/`TcpLogServer` 的调用面与风险点。
  - ast-grep：按语法定位重复实现（例如 `levelPriority`）、`CrashReport.*` 调用点、`println(...)` 等非结构化输出点。

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE_LOG-F001 | Duplication | `LogLevel` 优先级映射重复实现（两处 `levelPriority`），存在口径漂移风险 | `core_log_component/src/main/java/com/xyoye/common_component/log/LogWriter.kt` + `LogWriter#levelPriority`；`core_log_component/src/main/java/com/xyoye/common_component/log/LogSampler.kt` + `LogSampler#levelPriority` | Unintentional | Unify | `:core_log_component`（建议收敛到 `LogLevel`） | Low | Small | P2 | 纯内部重构，风险低；需补齐单测覆盖映射一致性 |
| CORE_LOG-F002 | ArchitectureRisk | 磁盘写入失败后状态机与持久化/对外状态不同步：Writer 内部熔断但 `LogSystem` 未必反映 | `core_log_component/src/main/java/com/xyoye/common_component/log/LogWriter.kt` + `LogWriter#handleFileError`；`core_log_component/src/main/java/com/xyoye/common_component/log/LogSystem.kt` + `LogSystem#markDiskError` | N/A | Unify | `:core_log_component`（以 `LogSystem` 为单一真源） | Medium | Medium | P2 | 需要设计“写入层错误→全局状态”的回传，避免死锁/递归日志；并同步 UI 展示 |
| CORE_LOG-F003 | Redundancy | Bugly 上报入口分散且约定不一致：`BuglyReporter` + `ErrorReportHelper` 并存，且存在引用不存在文档的注释 | `core_log_component/src/main/java/com/xyoye/common_component/log/BuglyReporter.kt` + `BuglyReporter`；`core_log_component/src/main/java/com/xyoye/common_component/utils/ErrorReportHelper.kt` + `ErrorReportHelper#postCatchedException` | N/A | Unify | `:core_log_component`（收敛为单一门面） | High | Medium | P1 | `ErrorReportHelper` 已被多模块大量调用，迁移需提供平滑兼容（保持 API 或提供 Deprecated wrapper） |
| CORE_LOG-F004 | SecurityPrivacyRisk | 结构化 context 默认“原样写入”文件/TCP，缺少敏感字段脱敏策略，存在泄露风险 | `core_log_component/src/main/java/com/xyoye/common_component/log/LogFormatter.kt` + `LogFormatter.FieldFilter#filterContext`；`core_log_component/src/main/java/com/xyoye/common_component/log/tcp/TcpLogServer.kt` + `TcpLogServer` | N/A | Unify | `:core_log_component` | High | Medium | P1 | 需要统一脱敏口径（token/cookie/authorization 等），并明确 TCP 日志仅限调试会话/显式授权 |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| CORE_LOG-T001 | CORE_LOG-F001 | 抽取 `LogLevel` 优先级/比较逻辑为单一实现，移除重复 `levelPriority` | `core_log_component/src/main/java/com/xyoye/common_component/log/LogWriter.kt`；`core_log_component/src/main/java/com/xyoye/common_component/log/LogSampler.kt`；`core_log_component/src/main/java/com/xyoye/common_component/log/model/LogLevel.kt` | 1) 全仓仅保留一个优先级实现；2) 单测覆盖 DEBUG/INFO/WARN/ERROR 映射；3) 现有策略筛选行为不变 | Low | Small | P2 | AI（Codex） | Draft |
| CORE_LOG-T002 | CORE_LOG-F002 | 打通“写入层磁盘错误→全局状态”链路，确保 `LogSystem`/持久化/Writer 三者一致 | `core_log_component/src/main/java/com/xyoye/common_component/log/LogWriter.kt`；`core_log_component/src/main/java/com/xyoye/common_component/log/LogSystem.kt`；`core_log_component/src/androidTest/java/com/xyoye/common_component/log/LogDiskErrorInstrumentedTest.kt`；`core_log_component/src/test/java/com/xyoye/common_component/log/LogSystemDiskErrorPropagationTest.kt` | 1) 写入失败后 `LogSystem.getRuntimeState().debugToggleState==DISABLED_DUE_TO_ERROR`；2) 配置表持久化更新；3) 既有 instrumented test 与新增用例通过 | Medium | Medium | P2 | AI（Codex） | Done |
| CORE_LOG-T003 | CORE_LOG-F003 | 收敛 Bugly 上报门面：统一由 `BuglyReporter` 触达 `CrashReport`，并规范 `ErrorReportHelper` 职责与注释 | `core_log_component/src/main/java/com/xyoye/common_component/log/BuglyReporter.kt`；`core_log_component/src/main/java/com/xyoye/common_component/utils/ErrorReportHelper.kt` + 调用方（全仓） | 1) `CrashReport.*` 访问集中（或有明确分层）；2) 删除/替换不存在文档引用；3) `extraInfo` 能进入可追踪上下文（userData 或封装异常 message） | High | Medium | P1 | AI（Codex） | Done |
| CORE_LOG-T004 | CORE_LOG-F004 | 默认脱敏：对日志 message/context 做敏感字段过滤，并收紧 TCP 日志输出条件（仅调试会话/显式授权） | `core_log_component/src/main/java/com/xyoye/common_component/log/LogFormatter.kt`；`core_log_component/src/main/java/com/xyoye/common_component/log/tcp/*`；`core_log_component/src/main/java/com/xyoye/common_component/log/LogSystem.kt` | 1) 常见敏感 key（token/cookie/authorization 等）自动脱敏；2) TCP server 仅在调试会话启用且策略允许时输出；3) 补齐单测/文档说明 | High | Medium | P1 | AI（Codex） | Done |

## 5) 风险与回归关注点

- 日志是全局横切关注点：任何格式/过滤/开关改动都可能影响排障效率与性能，需要在 Debug/Release 场景分别验证。
- 文件与 TCP 属于“外部可见”渠道：脱敏与开关策略必须清晰，避免误把敏感信息写入可导出的介质。
- 本模块存在 JVM 单测与 Android Instrumented 测试：涉及文件/MediaStore/TCP 的改动应补齐对应覆盖并在模拟器/真机验证。

## 6) 备注（历史背景/待确认点）

- 本报告为 AI 辅助生成的初稿，建议按 `document/code_quality_audit/global/review_checklist.md` 做一次人工复核后再标记为 Done。
