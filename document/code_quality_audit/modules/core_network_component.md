# 模块排查报告：:core_network_component

- 模块：:core_network_component
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：core_network_component/src/main/java/com/xyoye/common_component/

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`CORE_NETWORK-F###`  
> - Task：`CORE_NETWORK-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 提供网络基础设施：Retrofit/Moshi/OkHttpClient 统一封装、拦截器链、服务接口与基础 Repository。
  - 提供跨模块共享的网络通用能力：请求包装（`Request`）、统一异常（`NetworkException`）、动态 BaseUrl、解压、认证与日志拦截等。
- 模块职责（不做什么）
  - 不承载 UI 与具体页面逻辑；不应引入 feature ↔ feature 的直接耦合。
  - 不应在默认路径中降低 TLS 安全（证书/主机名校验），除非有明确的用户显式开关策略（Release 允许；debug 可提供快捷入口）。
- 关键入口/关键路径（示例）
  - `core_network_component/src/main/java/com/xyoye/common_component/network/Retrofit.kt` + `Retrofit`（`danDanClient/commonClient`、`createService`）
  - `core_network_component/src/main/java/com/xyoye/common_component/network/request/Request.kt` + `Request#doGet/doPost`
  - `core_network_component/src/main/java/com/xyoye/common_component/network/helper/LoggerInterceptor.kt` + `LoggerInterceptor#intercept/sanitizeHeader`
  - `core_network_component/src/main/java/com/xyoye/common_component/network/helper/DynamicBaseUrlInterceptor.kt` + `DynamicBaseUrlInterceptor#intercept`
  - `core_network_component/src/main/java/com/xyoye/common_component/network/helper/UnsafeOkHttpClient.kt` + `UnsafeOkHttpClient#client`
- 依赖边界（与哪些模块交互，是否存在边界疑点）
  - 依赖：`:core_system_component`（配置/凭证/用户态）、`:core_log_component`（日志与异常上报）、`:data_component`（数据模型）。
  - 被依赖：`bilibili_component`、`core_storage_component` 与各业务特性模块（网络访问统一入口）。
  - 边界疑点：当前模块同时包含“infra（OkHttp/Retrofit）”与“业务域 service/repository”，可能导致“低层模块被迫承载业务耦合”，影响可测试性与迁移成本。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - 本轮重点使用 ast-grep 确证 TLS/日志等高风险调用形态（例如 `hostnameVerifier { _, _ -> true }`、`postCatchedExceptionWithContext(...)`）。

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE_NETWORK-F001 | SecurityPrivacy | 多处 OkHttpClient 显式禁用主机名校验（`hostnameVerifier { _, _ -> true }`），存在 MITM 风险 | `core_network_component/src/main/java/com/xyoye/common_component/network/Retrofit.kt` + `Retrofit#danDanClient/commonClient`；`core_network_component/src/main/java/com/xyoye/common_component/network/helper/UnsafeOkHttpClient.kt` + `UnsafeOkHttpClient#client`；`bilibili_component/src/main/java/com/xyoye/common_component/bilibili/net/BilibiliOkHttpClientFactory.kt` + `BilibiliOkHttpClientFactory#create` | Unintentional | Unify | `:core_network_component`（统一 OkHttp 构建口径）+ `:bilibili_component`（迁移） | High | Medium | P1 | 修复可能导致部分“证书/域名不规范”的服务连接失败；需要明确“用户显式不安全开关（Release 允许）”与回归清单 |
| CORE_NETWORK-F002 | SecurityPrivacy | `UnsafeOkHttpClient` 信任所有证书 + 关闭主机名校验，被 WebDAV/代理等链路直接复用，风险外溢 | `core_network_component/src/main/java/com/xyoye/common_component/network/helper/UnsafeOkHttpClient.kt` + `UnsafeOkHttpClient`；`core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/WebDavStorage.kt` + `WebDavStorage`（`OkHttpSardine(UnsafeOkHttpClient.client)`）；`player_component/src/main/java/com/xyoye/player/utils/VlcProxyServer.kt` + `VlcProxyServer#executeRequest` | N/A | Deprecate | `:core_network_component`（安全策略/替代实现）+ 调用方模块 | High | Medium | P1 | 若历史上依赖“自签证书/不可信证书”才能访问，需要提供替代方案（导入证书/Pin/仅 HTTP/用户显式不安全开关（Release 允许）） |
| CORE_NETWORK-F003 | SecurityPrivacy | 异常上报/日志上下文可能包含敏感信息（请求参数/JSON/凭证 Header），存在泄露风险 | `core_network_component/src/main/java/com/xyoye/common_component/network/request/Request.kt` + `Request#doGet/doPost/requestBody`（`postCatchedExceptionWithContext` 直接拼接 `$requestParams/$requestJson`）；`core_network_component/src/main/java/com/xyoye/common_component/utils/JsonHelper.kt` + `JsonHelper#parseJson/parseJsonList/parseJsonMap`（上报中包含原始 `jsonStr`）；`core_network_component/src/main/java/com/xyoye/common_component/network/helper/LoggerInterceptor.kt` + `LoggerInterceptor#sanitizeHeader`；`core_network_component/src/main/java/com/xyoye/common_component/network/helper/DeveloperCertificateInterceptor.kt` + `DeveloperCertificateInterceptor#HEADER_APP_SECRET` | N/A | Unify | `:core_network_component`（脱敏）+ `:core_log_component`（统一脱敏工具，如需要） | High | Medium | P1 | 需要定义“敏感键/敏感头”的口径与脱敏策略，避免排障信息丢失；同时防止新增明文泄露 |
| CORE_NETWORK-F004 | ArchitectureRisk | `Retrofit` 以全局单例形式暴露 Service（静态入口），可测试性与可替换性较弱 | `core_network_component/src/main/java/com/xyoye/common_component/network/Retrofit.kt` + `Retrofit.Companion#danDanService/...`、`Retrofit.Holder` | N/A | Unify | `:core_network_component`（引入 Factory/Provider）+ `:core_contract_component`（如需抽象契约） | Medium | Medium | P2 | 迁移需要批量替换调用点；需避免引入依赖环并跑 `verifyModuleDependencies` |
| CORE_NETWORK-F005 | Duplication | OkHttpClient 构建参数/拦截器链在多个模块重复（timeout、解压/动态域名/日志等），缺少统一工厂导致“安全口径漂移” | `core_network_component/src/main/java/com/xyoye/common_component/network/Retrofit.kt` + `Retrofit#danDanClient/commonClient`；`bilibili_component/src/main/java/com/xyoye/common_component/bilibili/net/BilibiliOkHttpClientFactory.kt` + `BilibiliOkHttpClientFactory#create` | Unintentional | Unify | `:core_network_component`（提取 OkHttpClientFactory） | Medium | Medium | P2 | 抽取工厂时要兼顾各域差异（CookieJar/headers/signature），避免“过度统一”导致能力退化 |
| CORE_NETWORK-F006 | Redundancy | 模块内自定义类名 `Retrofit` 与 `retrofit2.Retrofit` 同名，阅读/检索时易混淆 | `core_network_component/src/main/java/com/xyoye/common_component/network/Retrofit.kt` + `class Retrofit`（同文件存在 `retrofit2.Retrofit.Builder()`） | N/A | Unify | `:core_network_component` | Low | Small | P2 | 需要批量改引用与文件名/类名；注意保持对外 API 稳定（或分阶段迁移） |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| CORE_NETWORK-T001 | CORE_NETWORK-F001 | 统一 TLS 安全默认：移除默认路径中的 `hostnameVerifier { _, _ -> true }`，并为“特殊场景”提供可控开关（用户显式；Release 允许） | `core_network_component/src/main/java/com/xyoye/common_component/network/Retrofit.kt`；`bilibili_component/src/main/java/com/xyoye/common_component/bilibili/net/BilibiliOkHttpClientFactory.kt`；相关 OkHttp 构建点 | 1) 默认 OkHttpClient 使用系统 TLS 校验；2) 若需要“放宽校验”，必须是用户显式开关（Release 允许）；3) 全仓编译通过且核心网络功能可回归 | High | Medium | P1 | 待分配（Infra/Network） | Draft |
| CORE_NETWORK-T002 | CORE_NETWORK-F002 | 下线/收敛 `UnsafeOkHttpClient`：仅允许在明确场景使用，并提供更安全替代（证书导入/Pin/仅 HTTP） | `core_network_component/.../OkHttpTlsPolicy.kt`、`core_network_component/.../OkHttpTlsConfigurer.kt`、`core_network_component/.../WebDavOkHttpClientFactory.kt`、`document/architecture/network_tls_policy.md`；调用方：`core_storage_component/.../WebDavStorage.kt`、`player_component/.../VlcProxyServer.kt` | 1) release 默认不再使用“信任所有证书”的客户端；2) WebDAV/代理仍可用（或提供替代配置路径）；3) 文档化风险与使用方式 | High | Medium | P1 | AI（Codex） | Done |
| CORE_NETWORK-T003 | CORE_NETWORK-F003 | 引入统一脱敏工具：请求参数/JSON/敏感 Header 在日志与异常上报中默认脱敏（含 `X-AppSecret` 等） | `core_network_component/src/main/java/com/xyoye/common_component/network/request/Request.kt`；`core_network_component/src/main/java/com/xyoye/common_component/utils/JsonHelper.kt`；`core_network_component/src/main/java/com/xyoye/common_component/network/helper/LoggerInterceptor.kt`；（必要时）`core_log_component` | 1) `Request` 上报不再包含完整 `$requestParams/$requestJson`（至少按 key 脱敏）；2) `LoggerInterceptor#sanitizeHeader` 覆盖 `X-AppId/X-AppSecret`；3) 抽查关键链路不泄露 token/cookie/secret | High | Medium | P1 | AI（Codex） | Done |
| CORE_NETWORK-T004 | CORE_NETWORK-F004 | 将 `Retrofit` 单例改为可注入 Provider/Factory，提升可测试性并降低全局静态耦合 | `core_network_component/src/main/java/com/xyoye/common_component/network/Retrofit.kt` + 调用方 | 1) 调用方可通过注入/服务定位获取网络 Service；2) 支持在测试中替换实现；3) 不引入依赖环且 `verifyModuleDependencies` 通过 | Medium | Medium | P2 | 待分配（Infra/Network） | Draft |
| CORE_NETWORK-T005 | CORE_NETWORK-F005 | 抽取统一 OkHttpClientFactory：集中维护 timeout/拦截器链/安全策略，减少跨模块漂移 | 新增 `core_network_component` 工厂类；迁移 `Retrofit.kt` 与 `BilibiliOkHttpClientFactory.kt` | 1) 至少收敛公共参数（timeout、解压、动态域名、日志）；2) 允许按域扩展（CookieJar/Headers/签名）；3) 全仓编译通过 | Medium | Medium | P2 | 待分配（Infra/Network） | Draft |
| CORE_NETWORK-T006 | CORE_NETWORK-F006 | 重命名自定义 `Retrofit` 包装类（或对 `retrofit2.Retrofit` 使用别名 import），降低阅读歧义 | `core_network_component/src/main/java/com/xyoye/common_component/network/Retrofit.kt`（类名与引用点） | 1) 对外 API 命名清晰；2) IDE/grep 检索不再混淆；3) 全仓编译通过 | Low | Small | P2 | 待分配（Infra/Network） | Draft |

## 5) 风险与回归关注点

- 行为回退风险（哪些场景最敏感）
  - TLS 校验恢复后，历史上依赖“不规范证书/域名”的服务可能不可用（需要明确支持策略）。
  - WebDAV/本地代理（VLC）链路对网络栈改动敏感，需重点回归。
- 回归成本（需要的账号/媒体文件/设备）
  - 需要至少 1 个 WebDAV 配置、1 个投屏/代理播放用例、以及（如涉及）Bilibili 登录态/播放链路回归。

## 6) 备注（历史背景/待确认点）

- 本报告为初稿，建议按 `document/code_quality_audit/global/review_checklist.md` 复核后再将 `module_status` 标记为 Done。
