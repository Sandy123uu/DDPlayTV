# 模块排查报告：:bilibili_component

- 模块：:bilibili_component
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：`bilibili_component/src/main/java/com/xyoye/common_component/bilibili/` + `bilibili_component/src/main/java/com/xyoye/common_component/network/service/BilibiliService.kt`

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`BILIBILI-F###`  
> - Task：`BILIBILI-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 提供 B 站域能力：扫码登录（Web/TV）、Cookie/Token 持久化、风控状态与预热、播放链路（PlayUrl/MPD 生成/心跳）、弹幕下载（XML）与直播弹幕 Socket 等。
  - 封装 Bilibili 的请求签名/鉴权机制（WBI/APP/Ticket 等），并将风控/异常上报尽量结构化（避免直接输出敏感信息）。
- 模块职责（不做什么）
  - 不应把 Bilibili 的鉴权细节（Cookie/Token/签名字段）泄漏到上层业务模块；上层应通过仓库/用例获得“可播放 URL/必要 Header/可复现错误信息”。
  - 不应把“可被滥用的固定凭证/密钥”硬编码在源码中（即使无法完全隐藏，也应具备可配置与隔离策略，避免与发行物强绑定）。
- 关键入口/关键路径（示例）
  - `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/repository/BilibiliRepository.kt` + `class BilibiliRepository`（登录/风控/播放/历史等聚合入口，约 1653 行）
  - `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/auth/BilibiliCookieJarStore.kt` + `BilibiliCookieJarStore#saveFromResponse/loadForRequest/exportCookieHeader`
  - `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/auth/BilibiliAuthStore.kt` + `BilibiliAuthStore#read/write/updateFromCookies/updateAppTokens`
  - `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/app/BilibiliTvClient.kt` + `BilibiliTvClient#sign/requireAppCredential`
  - `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/error/BilibiliPlaybackErrorReporter.kt` + `BilibiliPlaybackErrorReporter#reportPlaybackError`（URL/Header 脱敏）
  - `bilibili_component/src/main/java/com/xyoye/common_component/network/service/BilibiliService.kt` + `interface BilibiliService`（Retrofit API 定义）
- 依赖边界（与哪些模块交互，是否存在边界疑点）
  - 依赖：`:core_network_component`（Retrofit/OkHttp）、`:core_database_component`（部分持久化）、`:core_system_component`（系统/配置能力）、`:core_log_component`（日志/上报）、`:data_component`（网络/DB 模型）、`MMKV`（轻量 KV 存储）。
  - 被依赖：`:core_storage_component`、`local_component`、`user_component`、`storage_component` 等（用于播放、弹幕、登录态与历史等）。
  - 边界疑点：
    - 认证信息（Cookie/Token）在本地持久化链路缺少统一的“加密/迁移/清理”策略（安全与隐私风险）。
    - `BilibiliRepository` 单类聚合过多职责，长期维护与测试成本高。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - 本轮使用 ast-grep 确证：`MMKV.mmkvWithID(...)` 的使用分布、`BilibiliTvClient` 中硬编码 `APP_KEY/APP_SEC` 的证据点与迁移调用面。
  - 本轮使用 `rg` 检查：敏感字段（cookie/token/access_key 等）在日志输出链路中的脱敏策略是否一致（`BilibiliPlaybackErrorReporter` 已做 URL/字段脱敏）。

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| BILIBILI-F001 | SecurityPrivacy | TV 客户端 `APP_KEY/APP_SEC` 在源码硬编码（等同固定凭证），存在泄露/封禁风险，且不利于多渠道/多策略切换 | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/app/BilibiliTvClient.kt` + `BilibiliTvClient#requireAppCredential/sign`（读取 `BilibiliTvCredentialStore` 配置）；调用示例：`bilibili_component/src/main/java/com/xyoye/common_component/bilibili/repository/BilibiliRepository.kt` + `BilibiliRepository#loginQrCodeGenerate`（TV 分支调用 `BilibiliTvClient.sign(...)`） | N/A | Unify | `:core_system_component`（构建期注入/本地配置）+ `:bilibili_component`（消费端改造） | High | Medium | P1 | Release 未配置时禁用 TV 登录/签名能力并提示用户配置；配置到位后需回归 TV 登录/签名链路 |
| BILIBILI-F002 | SecurityPrivacy | 登录态（Cookie/Token）使用 MMKV 明文持久化，缺少加密/密钥管理与迁移策略 | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/auth/BilibiliCookieJarStore.kt` + `MMKV.mmkvWithID("bilibili_cookie_jar_...")`；`bilibili_component/src/main/java/com/xyoye/common_component/bilibili/auth/BilibiliAuthStore.kt` + `MMKV.mmkvWithID(MMKV_ID)` | N/A | Unify | `:core_system_component`（通用加密存储能力，参考 `DeveloperCredentialStore`）+ `:bilibili_component`（迁移与落地） | High | Large | P2 | 需要兼容旧数据迁移与回滚；加密实现需考虑多进程/性能/密钥丢失场景 |
| BILIBILI-F003 | ArchitectureRisk | `BilibiliRepository` 单类过大且多职责（登录/风控/播放/历史/直播弹幕等），演进与测试成本高 | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/repository/BilibiliRepository.kt` + `class BilibiliRepository` | N/A | Unify | `:bilibili_component`（按子域拆分：Auth/Risk/Playback/History/Live 等） | Medium | Large | P2 | 拆分需要保证调用侧 API 稳定；建议先引入 facade + 渐进迁移，避免一次性大改 |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| BILIBILI-T001 | BILIBILI-F001 | 去硬编码：将 `APP_KEY/APP_SEC` 改为“构建期注入/本地配置”，并提供可控回退策略（避免与发行物强绑定） | `core_system_component`（扩展 BuildConfig 注入 + 本地存储读取入口）；`bilibili_component`（`BilibiliTvClient` 改造为配置读取 + 校验） | 1) 仓库源码中不再出现明文 `APP_SEC`；2) 未配置时给出明确错误与降级策略（例如禁用 TV 登录/提示）；3) 配置到位时登录/播放签名链路可回归 | High | Medium | P1 | AI（Codex） | Done |
| BILIBILI-T002 | BILIBILI-F002 | 为 Cookie/Token 引入加密存储：落地统一的密钥管理与数据迁移，避免明文落盘 | `core_system_component`（抽取通用 `EncryptedStore`/`Crypto`，可参考 `DeveloperCredentialStore`）；`bilibili_component`（替换 `MMKV.mmkvWithID` 明文存储、迁移旧数据） | 1) Cookie/Token 落盘为密文（不可直接 grep 出明文）；2) 旧数据可迁移且不强制用户重新登录（可接受可控场景下的回退）；3) 登录/播放/心跳/直播弹幕等关键流程不回退 | High | Large | P2 | 待分配（Security/System/Bilibili） | Draft |
| BILIBILI-T003 | BILIBILI-F003 | 拆分 `BilibiliRepository`：按子域抽取组件并引入单测/契约化接口，提高可维护性 | `bilibili_component`（新增 `AuthRepository/RiskRepository/PlaybackRepository/HistoryRepository/LiveRepository` 等）；对外保留 facade 以降低调用方改动 | 1) 对外 API 基本不变（或提供 deprecate 迁移期）；2) 关键签名/存储/URL 脱敏具备单测覆盖；3) 全仓编译通过 | Medium | Large | P2 | 待分配（Bilibili） | Draft |

## 5) 风险与回归关注点

- 行为回退风险（哪些场景最敏感）
  - 任何登录态/签名参数变更都可能触发风控（-412/-403/-352 等）；需要准备可观测性与回退开关（避免“越修越封”）。
  - 加密存储/迁移最容易导致“登录丢失/Token 失效/跨 storageKey 串号”等问题，需重点回归。
- 回归成本（需要的账号/媒体文件/设备）
  - 至少需要：可用 B 站账号（扫码登录）、稳定网络环境、1 个普通视频（archive）、1 个番剧（pgc）、1 个直播间（live）用于回归播放/弹幕/心跳。

## 6) 备注（历史背景/待确认点）

- `BilibiliPlaybackErrorReporter` 已对 URL/query 与 header key 做了脱敏与结构化上报（值得在其他含 Cookie/Token 的模块复用同类策略）。
- 本报告为初稿，建议按 `document/code_quality_audit/global/review_checklist.md` 复核后再将 `module_status` 标记为 Done。
