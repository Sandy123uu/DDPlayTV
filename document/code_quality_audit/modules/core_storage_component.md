# 模块排查报告：:core_storage_component

- 模块：:core_storage_component
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：core_storage_component/src/main/java/com/xyoye/common_component/storage/ + `utils/` + `source/` + `resolver/` + `network/repository/`

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`CORE_STORAGE-F###`  
> - Task：`CORE_STORAGE-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 提供多种“媒体来源/存储协议”的统一抽象与实现：本地媒体库、WebDAV、SMB、FTP、远程串流、磁链、投屏、Alist、百度网盘、115 Open/Cloud、Bilibili 等（`StorageFactory` + 多个 `*Storage`）。
  - 提供播放相关的“取流/代理”能力：本地 HTTP Proxy（NanoHTTPD）、Range 处理、部分协议的按需转发与 headers 透传（`HttpPlayServer` + `LocalProxy` + `*PlayServer`）。
  - 提供与“存储侧”强关联的辅助能力：字幕/弹幕发现与缓存、扫描与部分下载/解压（7zip、thunder）等。
- 模块职责（不做什么）
  - 不应向上层 UI/feature 暴露底层协议库（SMBJ/commons-net/sardine/NanoHTTPD 等）的直接依赖与类型，否则会导致 feature 直接绑定实现细节，难以替换/升级。
  - 不应在默认路径中降低网络安全（例如“信任所有证书/关闭主机名校验”），除非有明确的 debug 或用户显式开关策略。
- 关键入口/关键路径（示例）
  - `core_storage_component/src/main/java/com/xyoye/common_component/storage/StorageFactory.kt` + `StorageFactory#createStorage`
  - `core_storage_component/src/main/java/com/xyoye/common_component/storage/AbstractStorage.kt` + `AbstractStorage#cacheDanmu/cacheSubtitle`
  - `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/VideoStorage.kt` + `VideoStorage`（本地媒体库扫描/DB 同步）
  - `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/WebDavStorage.kt` + `WebDavStorage#createPlayUrl/getNetworkHeaders`
  - `core_storage_component/src/main/java/com/xyoye/common_component/storage/file/helper/HttpPlayServer.kt` + `HttpPlayServer#serve`
- 依赖边界（与哪些模块交互，是否存在边界疑点）
  - 依赖：`:core_contract_component`（`Storage` 契约）、`:core_network_component`（Retrofit/OkHttp）、`:core_database_component`（媒体库/播放历史等）、`:bilibili_component`（B站域能力）、`:repository:seven_zip/:repository:thunder`（第三方 AAR 包装）。
  - 被依赖：`local_component/player_component/storage_component/user_component` 等（“媒体来源管理/播放入口/下载解压”等能力都会触达）。
  - 边界疑点：
    - `core_storage_component` 通过 Gradle `api(...)` 向上游暴露多种第三方协议库，导致上层直接 import 这些类型（边界泄漏）。
    - 模块内存在大量 `printStackTrace()` 与分散的异常上报策略，既影响可观测性一致性，也可能带来敏感信息泄露风险（尤其是 URL/Headers/账号相关）。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - 本轮使用 ast-grep 确证：`api(...)` 依赖泄漏点、`UnsafeOkHttpClient.client` 使用点、`printStackTrace()` 分布。

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE_STORAGE-F001 | ArchitectureRisk | Gradle `api(...)` 暴露底层协议库，导致上层模块直接依赖实现细节（边界泄漏/升级困难） | `core_storage_component/build.gradle.kts` + `dependencies { api(...) }`；调用示例：`storage_component/src/main/java/com/xyoye/storage_component/ui/dialog/SmbStorageEditDialog.kt` + `SmbStorageEditDialog`（直接使用 `SMBClient`） | N/A | Unify | `:core_storage_component`（隐藏第三方类型、提供抽象/封装）+ 上层模块迁移 | Medium | Medium | P2 | 需要梳理哪些第三方类型“真的在公共 API 暴露”；迁移期可能需要过渡层与 deprecate |
| CORE_STORAGE-F002 | SecurityPrivacy | WebDAV 链路曾使用 `trust-all` TLS（信任所有证书 + 关闭主机名校验），存在 MITM 风险 | `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/WebDavStorage.kt` + `WebDavStorage`（默认 `WebDavOkHttpClient` 严格 TLS；仅在用户开启“允许不安全 TLS”时使用 `WebDavOkHttpClientFactory.create(OkHttpTlsPolicy.UnsafeTrustAll)`） | N/A | Unify | `:core_network_component`（安全策略/替代方案）+ `:core_storage_component`（迁移 WebDAV） | High | Medium | P1 | 若历史上依赖自签证书/不规范证书，需要明确可控开关或证书导入方案；回归需覆盖 WebDAV 连接/列表/播放 |
| CORE_STORAGE-F003 | SecurityPrivacy | 媒体库配置包含明文账号/密码字段（Room 表 `media_library`），协议实现直接读取使用，存在本地泄露风险 | `data_component/src/main/java/com/xyoye/data_component/entity/MediaLibraryEntity.kt` + `MediaLibraryEntity#account/password`；调用示例：`core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/WebDavStorage.kt` + `WebDavStorage#getAccountInfo` | N/A | Unify | `:core_system_component`（安全存储/KeyStore）+ `:data_component`（数据模型迁移）+ `:core_storage_component`（读写路径改造） | High | Large | P2 | 需要数据迁移（旧数据解密/搬迁/回写）；需避免日志打印敏感字段；涉及多个协议（FTP/SMB/WebDAV/Remote） |
| CORE_STORAGE-F004 | ArchitectureRisk | 模块内 `printStackTrace()` 广泛存在（含网络/文件/解析路径），错误可观测性与脱敏策略不一致 | 代表性证据：`core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/WebDavStorage.kt` + `WebDavStorage#openFile/listFiles/test`；`core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/FtpStorage.kt` + `FtpStorage`；`core_storage_component/src/main/java/com/xyoye/common_component/utils/subtitle/SubtitleUtils.kt` + `SubtitleUtils` | N/A | Unify | `:core_storage_component`（统一异常处理/日志）+ `:core_log_component`（上报接口） | Medium | Medium | P2 | 批量替换需保证不吞异常导致行为变化；日志脱敏需要覆盖 URL/Headers/账号字段 |
| CORE_STORAGE-F005 | Duplication | 本地代理能力存在多实现：`HttpPlayServer`（mpv 等）与 `VlcProxyServer`（VLC）均基于 NanoHTTPD 且均涉及不安全 OkHttpClient，策略不一致 | `core_storage_component/src/main/java/com/xyoye/common_component/storage/file/helper/HttpPlayServer.kt` + `HttpPlayServer`；`player_component/src/main/java/com/xyoye/player/utils/VlcProxyServer.kt` + `VlcProxyServer` | Unintentional | Unify | `:core_storage_component`（统一 proxy 能力）或 `:player_component`（按播放器收敛），并统一 TLS 策略落点到 `:core_network_component` | Medium | Medium | P2 | 需要兼容 VLC/mpv 对 Range/headers 的差异；统一后需做多播放器回归（TV/移动端） |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| CORE_STORAGE-T001 | CORE_STORAGE-F001 | 收敛第三方协议库依赖泄漏：减少/消除 `api(...)`，上层模块不再直接 import 协议库类型 | `core_storage_component/build.gradle.kts`（调整 `api`→`implementation` 或仅保留“确实出现在公共 API 的类型”）；迁移 `storage_component/...` 等直接引用协议库的代码 | 1) 业务模块不再直接依赖 `SMBClient/FTPClient/NanoHTTPD/...` 等第三方类型；2) 依赖声明清晰（需要时显式依赖）；3) 全仓编译通过且关键存储配置/测试连接可用 | Medium | Medium | P2 | AI（Codex） | Done |
| CORE_STORAGE-T002 | CORE_STORAGE-F002 | WebDAV 默认启用严格 TLS：替换 `UnsafeOkHttpClient`，并为“特殊场景”提供可控开关（用户显式；Release 允许） | `core_storage_component/.../WebDavStorage.kt`；`core_network_component/...`（OkHttpClientFactory/安全策略） | 1) release 默认不再使用“信任所有证书”的客户端；2) 若需要放宽校验，必须是用户显式开关（Release 允许）；3) WebDAV 连接/列表/播放用例可回归 | High | Medium | P1 | AI（Codex） | Done |
| CORE_STORAGE-T003 | CORE_STORAGE-F003 | 迁移存储凭证到安全存储：DB 中不再保存明文 `password/remoteSecret`（改为引用/加密存储） | `data_component`（`MediaLibraryEntity` 字段策略 + migration）；`core_storage_component`（读取/保存逻辑）；相关 UI（新增/编辑存储） | 1) DB 不再持久化明文密码/secret；2) 旧用户数据可迁移且不丢失；3) 任一协议（WebDAV/FTP/SMB/Remote）连接/播放可回归 | High | Large | P2 | AI（Codex） | Done |
| CORE_STORAGE-T004 | CORE_STORAGE-F004 | 统一异常处理与脱敏：移除 `printStackTrace()`，统一到 `LogFacade`/`ErrorReportHelper` 并建立敏感字段脱敏规则 | `core_storage_component` 全模块（storage impl + utils）；必要时抽取 `StorageErrorReporter` | 1) `core_storage_component` 内不再出现 `printStackTrace()`；2) 错误日志包含足够上下文（模块/协议/关键参数 hash），但不包含敏感字段明文；3) 关键路径行为不变（失败仍返回 null/empty 等约定） | Medium | Medium | P2 | AI（Codex） | Done |
| CORE_STORAGE-T005 | CORE_STORAGE-F005 | 统一本地代理能力：抽取通用 Proxy 服务（headers/range/tls 策略一致），减少 `HttpPlayServer`/`VlcProxyServer` 逻辑重复 | `core_storage_component/.../HttpPlayServer.kt`；`player_component/.../VlcProxyServer.kt`；`core_network_component`（OkHttp 安全策略） | 1) mpv/VLC 均可通过统一入口获得可播放 URL；2) Range/headers 行为在主要站点/协议下稳定；3) 默认 TLS 策略安全且可配置；4) 全仓编译通过 | Medium | Medium | P2 | 待分配（Player/Storage/Network） | Draft |

## 5) 风险与回归关注点

- 行为回退风险（哪些场景最敏感）
  - TLS 策略收紧会直接影响 WebDAV/代理链路；需要明确“支持自签证书”的产品策略与开关位置。
  - 代理服务统一/重构会影响播放器（VLC/mpv）对 Range 的兼容性，需重点回归拖动/续播/倍速等。
  - 凭证存储改造涉及迁移与兼容，最容易出现“旧账号失效/需要重新登录/连接测试失败”等回退。
- 回归成本（需要的账号/媒体文件/设备）
  - 至少准备：1 个 WebDAV、1 个 SMB、1 个 FTP（或可替代）配置；以及 1 份可播放媒体文件用于 mpv/VLC 验证。
  - TV/移动端均需回归“焦点/输入/配置保存”（尤其是存储配置对话框与自动保存逻辑）。

## 6) 备注（历史背景/待确认点）

- 本报告为初稿，建议按 `document/code_quality_audit/global/review_checklist.md` 复核后再将 `module_status` 标记为 Done。
