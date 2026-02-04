# 模块排查报告：:repository:video_cache

- 模块：:repository:video_cache
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：repository/video_cache/

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`REPO_VIDEO_CACHE-F###`  
> - Task：`REPO_VIDEO_CACHE-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 以 Gradle module 形式封装本地预编译 AAR：`repository/video_cache/library-release.aar`，为播放链路提供“本地代理 + 缓存”能力（典型包名：`com.danikula.videocache.*`）。
  - 由 `:player_component` 的缓存子模块（`com.xyoye.cache.*`）统一使用，支持 `headers` 注入、Range 请求与代理播放 URL 生成。
- 模块职责（不做什么）
  - 不承载业务代码；除 `build.gradle.kts` 与 AAR 产物外不应新增 Kotlin/Java 实现。
  - 不应被其它 feature 模块直接依赖（避免将第三方缓存/proxy 类型扩散到更多模块）。
- 关键入口/关键路径（示例）
  - AAR 封装：`repository/video_cache/build.gradle.kts` + `artifacts.add("default", file("library-release.aar"))`
  - 二进制产物：`repository/video_cache/library-release.aar`
  - 依赖声明（消费方）：`player_component/build.gradle.kts` + `dependencies { implementation(project(":repository:video_cache")) }`
  - 典型入口：`player_component/src/main/java/com/xyoye/cache/CacheManager.kt` + `CacheManager#createCacheServer/getCacheUrl`（`HttpProxyCacheServer.Builder(...)`）
  - 典型网络实现：`player_component/src/main/java/com/xyoye/cache/OkHttpUrlSource.kt` + `OkHttpUrlSource#openConnection/fetchContentInfo`（OkHttp + headers 注入）
- 依赖边界
  - 对外（被依赖）：仅 `:player_component`（见 `player_component/build.gradle.kts`）。
  - 对内（依赖）：无（本模块仅提供 AAR 工件，不应再依赖其它工程模块）。
  - 边界疑点：缓存/proxy 链路经常携带鉴权信息（URL query、Cookie/Authorization headers），需要统一脱敏与日志策略（见 Findings）。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - `rg`：定位 `com.danikula.videocache` 的引用分布，确认依赖扩散范围  
    - `rg "com\\.danikula\\.videocache" -n player_component/src/main/java/com/xyoye/cache`
  - ast-grep：确证关键语法形态（避免纯文本误判）  
    - Kotlin：`HttpProxyCacheServer.Builder($X)`（定位 server 构建入口）  
    - Kotlin：`server.getProxyUrl($X)`（定位代理 URL 生成）

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| REPO_VIDEO_CACHE-F001 | ArchitectureRisk | 预编译 AAR 缺少来源/版本/License 等元信息，升级与合规审计不可追溯 | `repository/video_cache/library-release.aar`（二进制）；`repository/video_cache/build.gradle.kts` + `artifacts.add("default", file("library-release.aar"))` | N/A | Unify | `repository/video_cache/`（补齐元信息）+ 全仓统一规范（建议由 `buildSrc` 或文档约束） | Medium | Small | P1 | 需要确认 AAR 上游来源、版本号、授权协议；否则难以在安全事件/升级时快速定位影响 |
| REPO_VIDEO_CACHE-F002 | SecurityPrivacy | 缓存/代理链路在 debug 日志中直接输出完整 URL，存在 token/签名泄露风险（尤其是网盘/鉴权直链） | `player_component/src/main/java/com/xyoye/cache/OkHttpUrlSource.kt` + `OkHttpUrlSource#fetchContentInfo/openConnection`（`LogFacade.d(..., "…${sourceInfo.url}")`） | N/A | Unify | 复用 `:core_log_component` 的 URL 脱敏策略（与 `PLAYER-F001` 同口径），并在缓存链路统一使用 | High | Small | P1 | 需要先定义“可保留字段”（host/path/hash），避免丢失定位能力；同时确保异常上报/日志均使用脱敏值 |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| REPO_VIDEO_CACHE-T001 | REPO_VIDEO_CACHE-F001 | 为 AAR 增加可追溯元信息（来源/版本/License/校验和/更新流程） | 新增 `repository/video_cache/README.md`（中文）；可选新增 `repository/video_cache/LICENSE` 或在 README 中明确 License 与引用位置 | 1) README 明确：上游项目/下载地址、版本号、License、AAR SHA256、更新步骤；2) 任意人可按文档复现升级；3) 不影响现有依赖解析 | Medium | Small | P1 | 待分配（Repo） | Draft |
| REPO_VIDEO_CACHE-T002 | REPO_VIDEO_CACHE-F002 | 对缓存/代理链路的 URL 日志进行脱敏，默认不输出 query/token 等敏感信息 | `player_component/src/main/java/com/xyoye/cache/OkHttpUrlSource.kt`（替换日志内容）；若已在 `:core_log_component` 提供 URL 脱敏工具则直接复用，否则先补齐该工具并统一替换调用点 | 1) 相关日志不包含 query/fragment/token；2) 仍可定位问题（保留 host/path + 可选 hash）；3) 与 `PLAYER-T001` 使用同一套脱敏策略；4) 全仓编译通过 | High | Small | P1 | 待分配（Player/Log） | Draft |

## 5) 风险与回归关注点

- 行为回退风险：升级/替换 AAR 可能影响 Range 行为、缓存命中/回源策略、headers 注入有效性（尤其是需要 Cookie/Authorization 的站点），进而导致播放失败/卡顿。
- 回归成本：需要覆盖 1) 普通直链；2) 需要 headers 的鉴权直链；3) seek/快进快退（Range）；4) TV/移动端各 1 套；必要时增加对照日志（必须脱敏）。

## 6) 备注（历史背景/待确认点）

- 目前缓存链路的日志口径与播放器其它链路（Media3 诊断等）需要统一，避免出现“某条链路已脱敏、另一条链路仍明文输出”的策略漂移。
