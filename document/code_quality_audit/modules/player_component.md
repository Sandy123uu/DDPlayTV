# 模块排查报告：:player_component

- 模块：:player_component
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：player_component/src/main/java/（含 `com/xyoye/player_component/`、`com/xyoye/player/`、`com/xyoye/subtitle/`、`com/xyoye/danmaku/`、`com/xyoye/cache/`）

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`PLAYER-F###`  
> - Task：`PLAYER-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 播放器能力与 UI：播放器页（`PlayerActivity`）+ 控制器（`VideoController`）+ 多种播放内核（Media3/mpv/VLC）统一编排。
  - 伴随能力：字幕解析与渲染（含 libass/GPU 管线与 fallback）、弹幕渲染与过滤、截图、音频焦点、缓存与 headers 注入等。
  - Media3 迁移期的诊断/遥测：解码器选择/回退、HTTP 打开行为、崩溃标签（Bugly）等（`Media3Diagnostics/Media3CrashTagger`）。
- 模块职责（不做什么）
  - 不应在 release 默认输出包含敏感信息的日志（URL/Authorization/Cookie/query token 等），尤其是 Media3 诊断与 HTTP 相关日志；应有明确的开关与脱敏策略。
  - 不应在没有明确产品策略的情况下默认使用“不安全 TLS”（信任所有证书/关闭主机名校验）的 OkHttpClient，除非受用户显式开关控制（Release 允许；debug 可提供快捷入口）。
- 关键入口/关键符号（示例）
  - `player_component/src/main/java/com/xyoye/player_component/ui/activities/player/PlayerActivity.kt` + `PlayerActivity`
  - `player_component/src/main/java/com/xyoye/player_component/ui/activities/player/PlayerViewModel.kt` + `PlayerViewModel`
  - `player_component/src/main/java/com/xyoye/player/kernel/facoty/PlayerFactory.kt` + `PlayerFactory`
  - `player_component/src/main/java/com/xyoye/player/kernel/impl/media3/Media3VideoPlayer.kt` + `Media3VideoPlayer`
  - `player_component/src/main/java/com/xyoye/player/kernel/impl/vlc/VlcVideoPlayer.kt` + `VlcVideoPlayer`
  - `player_component/src/main/java/com/xyoye/player/kernel/impl/mpv/MpvVideoPlayer.kt` + `MpvVideoPlayer`
  - `player_component/src/main/java/com/xyoye/player/utils/VlcProxyServer.kt` + `VlcProxyServer#getProxyResponse`
- 依赖边界
  - 对内（依赖）：`core_ui/system/log/network/database/storage/contract/data` + `repository:danmaku/panel_switch/video_cache`（见 `player_component/build.gradle.kts`）。
  - 对外（被依赖）：作为 feature 模块不应被其它 feature 直接依赖；入口由 `RouteTable.Player.*` 暴露给 `:app`。同时承担“播放器是全局关键路径”的责任（回归成本高）。
  - 边界疑点：
    - 网络诊断/遥测代码（`Media3Diagnostics`、`Media3TelemetryRepository`）在 player 内实现，但与 `:core_log_component/:core_network_component` 的策略高度耦合；若不统一口径，容易出现“日志/遥测包含敏感数据”的隐私风险。
    - `VlcProxyServer` 作为本地代理承担“把 headers 注入到 VLC 播放链路”的角色，但当前使用 `UnsafeOkHttpClient`，安全策略与 core 层不一致。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - ast-grep：定位 `Media3Diagnostics.logHttpOpen(...)` 调用点、`UnsafeOkHttpClient` 使用点、`printStackTrace()` 分布，避免纯文本误报。

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| PLAYER-F001 | SecurityPrivacy | Media3 诊断日志/遥测直接记录完整 URL（包含 query 的可能），存在 token/签名泄露风险 | `player_component/src/main/java/com/xyoye/player/kernel/impl/media3/Media3Diagnostics.kt` + `Media3Diagnostics#logHttpOpen`（log + emit）；`player_component/src/main/java/com/xyoye/player/kernel/impl/media3/LoggingHttpDataSource.kt` + `LoggingHttpDataSource#open/logOpen`（调用 `logHttpOpen(uri.toString(), ...)`） | N/A | Unify | `:core_log_component` 提供 URL 脱敏工具/策略（保留 host+path，去 query/fragment）+ player 侧统一使用 | High | Small | P1 | 需确认“哪些场景必须保留 query 才能定位问题”；建议仅保留 hash/长度，避免明文 |
| PLAYER-F002 | SecurityPrivacy | `VlcProxyServer` 使用 `UnsafeOkHttpClient` 代理拉流，默认信任所有证书存在 MITM 风险 | `player_component/src/main/java/com/xyoye/player/utils/VlcProxyServer.kt` + `VlcProxyServer#getProxyResponse`（`UnsafeOkHttpClient.client.newCall(request)`） | N/A | Unify | `:core_network_component`（统一 OkHttpClientFactory/TLS 策略）+ `:player_component`（迁移使用） | High | Medium | P1 | 收紧 TLS 可能影响自签证书/特殊站点；需要明确“用户显式不安全开关（Release 允许）”与回归清单 |
| PLAYER-F003 | Duplication | 本地代理/HTTP server 能力存在多实现且策略不一致：VLC 代理（player）与 core/storage 的本地代理/投屏 server 存在重叠 | `player_component/src/main/java/com/xyoye/player/utils/VlcProxyServer.kt` + `VlcProxyServer`；对照 `core_storage_component/src/main/java/com/xyoye/common_component/storage/file/helper/HttpPlayServer.kt` + `HttpPlayServer`；以及 `storage_component/.../utils/screencast/*` 的 NanoHTTPD server | Unintentional | Unify | 以 `:core_storage_component`（或明确的 core 模块）收敛“代理/headers/range/鉴权/错误口径”，player 侧只做引擎适配 | Medium | Medium | P2 | 涉及多播放器回归（VLC/mpv/Media3）；注意 Range/headers 行为差异 |
| PLAYER-F004 | ArchitectureRisk | `Media3TelemetryRepository` 放在 player 模块但使用 `com.xyoye.common_component.network.repository`（core_network 命名空间），导致模块语义与包归属错位 | `player_component/src/main/java/com/xyoye/common_component/network/repository/Media3TelemetryRepository.kt` + `Media3TelemetryRepository`；其 emitter 调用 `core_network_component/src/main/java/com/xyoye/common_component/network/repository/Media3Repository.kt` + `Media3Repository#emitTelemetry` | N/A | Unify | 将 Telemetry repository 下沉到 `:core_network_component` 或调整包名为 `com.xyoye.player_component.*`，明确 ownership | Medium | Small | P1 | 移动类需注意二进制兼容与 import；若外部引用该类需提供过渡（typealias/Deprecated） |
| PLAYER-F005 | ArchitectureRisk | 模块内仍存在 `printStackTrace()`，与统一异常上报/脱敏口径不一致，且容易在崩溃场景遗漏上下文 | `player_component/src/main/java/com/xyoye/player_component/providers/PlayerFoundationInitializer.kt` + `PlayerFoundationInitializer#create`；`player_component/src/main/java/com/xyoye/player_component/utils/PlayRecorder.kt` + `PlayRecorder#recordImage`；`player_component/src/main/java/com/xyoye/player/kernel/impl/vlc/VlcVideoPlayer.kt` + `VlcVideoPlayer`（异常处理分支） | N/A | Unify | `:core_log_component` 统一 `safeReport(...)`；player 侧替换并补齐上下文（引擎/源类型/会话） | Medium | Small | P1 | 替换需避免吞异常导致行为变化；同时确保不把 URL/header 明文上报 |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| PLAYER-T001 | PLAYER-F001 | 对 Media3 HTTP 诊断日志/遥测进行 URL 脱敏，默认不输出 query/token 等敏感信息 | 增加 URL 脱敏工具（建议落 `:core_log_component` 或 `:core_system_component`）；改造 `Media3Diagnostics#logHttpOpen` 与 `LoggingHttpDataSource` 调用链，确保 log/emit 均使用脱敏后的 URL | 1) 日志与遥测不包含 query/fragment；2) 仍可定位问题（保留 host/path + 可选 hash）；3) 可通过开关控制详细程度；4) 全仓编译通过 | High | Small | P1 | 待分配（Player/Log） | Draft |
| PLAYER-T002 | PLAYER-F002 | 将 VLC 代理拉流从 `UnsafeOkHttpClient` 迁移到“默认安全”的 OkHttpClient，并提供可控降级策略 | 在 `:core_network_component` 提供可复用的 OkHttpClientFactory（若已有则复用）；`VlcProxyServer` 使用安全 client；如需信任自签证书，必须受用户显式配置控制（Release 允许） | 1) release 默认不再使用 trust-all client；2) VLC 播放带 headers 的直链可回归；3) 若开启降级策略，入口明确且可审计；4) 全仓编译通过 | High | Medium | P1 | 待分配（Network/Player） | Draft |
| PLAYER-T003 | PLAYER-F003 | 收敛本地代理/HTTP server 能力，减少多实现与策略漂移 | 抽取 “proxy + headers + range + 鉴权 + 错误口径” 到 `:core_storage_component`（或新 core 模块）；player/storage/screencast 复用统一实现 | 1) 同类 server 代码显著减少；2) 多引擎（VLC/mpv/Media3）可播放且 Range/headers 行为一致；3) 默认 TLS 与日志策略统一；4) 全仓编译通过 | Medium | Large | P2 | 待分配（Infra/Player/Storage） | Draft |
| PLAYER-T004 | PLAYER-F004 | 统一 Telemetry repository 的模块归属与包命名，避免“core_network 包名却在 feature 模块” | 方案 A：移动 `Media3TelemetryRepository` 到 `:core_network_component`；方案 B：保留在 player，但改包名为 `com.xyoye.player_component.*` 并通过接口在 core 层暴露；必要时提供 `@Deprecated` 过渡层 | 1) 包与模块语义一致；2) 外部引用不受影响或有清晰迁移路径；3) 不引入依赖环；4) 全仓编译通过 | Medium | Medium | P2 | 待分配（Network/Player） | Draft |
| PLAYER-T005 | PLAYER-F005 | 清理 `printStackTrace()` 并补齐异常上报上下文（引擎/会话/源类型） | 替换 `PlayerFoundationInitializer/PlayRecorder/VlcVideoPlayer/ExternalSubtitleManager/...` 的 `printStackTrace` 为统一上报；对可能包含 URL/header 的内容做脱敏 | 1) player_component 内无 `printStackTrace()`；2) 异常上报有足够上下文但不泄露敏感信息；3) 关键路径行为不变；4) 全仓编译通过 | Medium | Small | P1 | 待分配（Player/Log） | Draft |

## 5) 风险与回归关注点

- 播放内核回归：Media3/mpv/VLC 三条链路需分别覆盖（直链/本地/投屏/网盘），尤其是 Range/headers/鉴权与 seek/续播。
- 字幕/弹幕回归：ASS/GPU 管线、embedded subtitle、fallback 触发条件与 UI 提示；TV/移动端均需验证显示与性能。
- 后台会话：Media3 session bind、通知、前台/后台切换、音频焦点、投屏接收转播等。
- 隐私与日志：播放器链路最容易携带 cookie/authorization/token；任何诊断/遥测改造都必须先定义脱敏策略并做抽查验证。

## 6) 备注（历史背景/待确认点）

- 本报告为 AI 辅助生成的初稿，建议按 `document/code_quality_audit/global/review_checklist.md` 做一次人工复核后再标记为 Done。
