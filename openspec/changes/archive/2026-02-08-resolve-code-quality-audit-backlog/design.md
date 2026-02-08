## Context

`document/code_quality_audit` 已沉淀 21 个模块的排查报告与全局 Backlog（68 项），覆盖安全/隐私、稳定性、性能与架构一致性问题。其中特别需要优先治理的横切点包括：

- TLS/网络安全：默认路径存在 `UnsafeOkHttpClient`、禁用 hostname 校验等实现，风险外溢到 WebDAV/代理播放等链路。
- 可观测性与隐私：日志与异常上报缺少统一脱敏，且存在 `printStackTrace()` 等无序输出点。
- 凭证安全：Cookie/Token/远程存储密码等在本地落盘策略不一致，存在明文风险与迁移缺口。
- 协议与原生链路：投屏 UDP 加密实现不安全；解压链路存在线程/资源释放与压缩炸弹风险。
- 复用治理：多处重复实现（OkHttpClient 构建、Result 失败处理、PreferenceDataStore 映射、本地代理等）导致口径漂移。

本设计文档的目标：在不引入依赖环的前提下，为 Backlog 的落地提供“统一口径 + 渐进迁移”的实现路径，避免治理过程中产生新的漂移点。

## Goals / Non-Goals

**Goals**

1. 以 Backlog（G-T0001~G-T0068）为准，按批次闭环：实现 → 回归/门禁 → 文档状态更新。
2. 默认安全：Release 默认不降低 TLS/加密/存储安全；任何降级必须是**用户显式开关**（Release 允许，但必须强警告；debug 可提供快捷入口），并有风险提示。
3. 脱敏优先：日志（文件/TCP/logcat）与异常上报均默认脱敏，避免在“治理阶段”扩大泄露面。
4. 统一基础设施：网络客户端构建、脱敏、异常上报上下文、Result 失败处理等横切能力集中在 `:core_*` 层，调用方只做迁移。
5. 渐进式重构：对 repository/usecase 下沉与大型拆分（如 `BilibiliRepository`）采用 facade + 分批迁移，保持对外 API 尽量稳定。

**Non-Goals**

- 不引入 feature ↔ feature 的直接依赖；跨模块协作必须通过既有契约/路由机制表达。
- 不在本变更中引入不必要的新基础设施（例如全量 DI 框架）；以项目既有模式（Provider/Factory/ServiceLocator）渐进落地。

## Key Decisions

### 1) 统一 OkHttpClientFactory + TLS Policy（对应 G-T0001/G-T0002/G-T0008/G-T0024/G-T0046）

在 `:core_network_component` 建立统一的 OkHttpClientFactory：

- 以“默认安全”为基线：使用系统 TrustManager + HostnameVerifier（不再默认信任所有证书/忽略主机名校验）。
- TLS 放宽能力采用显式策略对象（例如 `TlsPolicy.Strict` / `TlsPolicy.UserAllowedInsecure` / `TlsPolicy.DebugInsecure`），并在 Release 侧强制 gating（仅用户显式）。
- 调用方（WebDAV、VLC Proxy、Bilibili 等）不再自行拼装 OkHttpClient，统一从 Factory 获取“可带域差异的 client”（CookieJar、headers、签名拦截器等以配置项表达）。

> 备注：`UnsafeOkHttpClient` 作为遗留能力可短期保留但必须下沉为“显式声明用途 + 受开关约束”的实现，并逐步迁移调用点直至可删除。

### 2) 统一脱敏工具与日志/上报管道（对应 G-T0012/G-T0013/G-T0016/G-T0018/G-T0023/G-T0026/G-T0061）

在 `:core_log_component` 提供单一脱敏工具（例如 `SensitiveDataSanitizer` / `RedactionRules`）并满足：

- 覆盖日志 message/context、URL query、header、JSON/key-value 参数等常见载体。
- 默认规则：对 `token/cookie/authorization/password/secret/access_key/appsec` 等 key 做遮蔽；必要时输出 hash/fingerprint 以便排障。
- 统一接入点：
  - `LogFormatter`：所有落盘/TCP 输出进入 formatter 前先做脱敏。
  - `ErrorReportHelper`/`BuglyReporter`：异常上报额外信息与上下文构建复用同一套脱敏策略。
- “禁用/降级/门禁”：TCP 日志仅在调试会话/显式授权下输出（与已有 `tcp-log-server` 能力对齐）。

### 3) 去硬编码密钥 + 安全存储（对应 G-T0007/G-T0027/G-T0028/G-T0031/G-T0064）

密钥/凭证治理拆成两类：

1. **构建期注入类**（例如 B 站 TV `APP_KEY/APP_SEC`）：通过 Gradle properties/CI 注入到 BuildConfig 或资源中；源码不包含明文。缺省时必须有明确降级策略（禁用相关功能/提示配置），避免 silent failure。
2. **运行期用户凭证类**（Cookie/Token/远程存储密码）：统一落到“加密存储层”，要求：
   - 使用 Android Keystore 派生/保护密钥；对称加密采用 AEAD（建议 AES-GCM）+ 随机 IV；
   - 提供迁移：从旧的明文 MMKV/DB 字段迁移到密文；迁移失败必须可观测且不崩溃；
   - 避免在日志与上报中泄露明文（依赖脱敏工具兜底）。

### 4) 投屏 UDP 加密升级与兼容（对应 G-T0022/G-T0045）

- 新协议载荷包含：版本号 + IV/nonce + ciphertext + tag（AEAD）。
- Receiver 侧支持多版本解析：
  - 优先支持新版本；
  - 对旧版本提供兼容解密或给出明确错误并触发降级策略（由产品结论决定）。
- 与投屏协议公共能力抽取（G-T0045）合并考虑，避免 UDP/HTTP server 在各模块重复实现导致协议漂移。

### 5) 解压链路：线程/取消/资源回收 + 压缩炸弹防护（对应 G-T0005/G-T0006/G-T0067）

- 所有阻塞式解压必须在 `Dispatchers.IO` 执行；支持取消/超时并保证资源释放（文件句柄/临时文件/线程）。
- 增加防护限额：最大文件数、最大总解压体积、路径穿越与非法字符清洗；失败必须回收已写入内容。
- 补齐 instrumentation 用例覆盖关键格式与异常包（更贴近 native so 行为）。

### 6) 大型收敛/下沉类任务：Facade + 渐进迁移（对应多项 P2）

对 `BilibiliRepository` 拆分、ViewModel→repository/usecase 下沉、代理能力收敛等任务采用统一策略：

- 先引入 facade 接口维持对外 API（或提供 Deprecated wrapper），再逐步迁移内部实现与调用点。
- 优先把“跨模块复用面大”的能力下沉到 `:core_*`（例如 OkHttpClientFactory、Result 失败处理助手、PreferenceDataStore 映射），避免在 feature 内形成新单点。
- 每个批次必须通过 `verifyModuleDependencies`，并在必要时补齐单元测试/仪器测试。

### 7) 已确认废弃的不可达链路：直接移除整条链路（对应 G-T0020/G-T0054）

- **投屏 Sender（G-T0020）**：删除 `ScreencastProvideService*` 相关实现与对外暴露入口，清理大段注释旧实现与全端 stub；仅保留投屏接收端（Receiver）链路并确保其回归不受影响。
- **发送弹幕（G-T0054）**：删除 `SendDanmuDialog`、`layout_send_danmu.xml` 与相关入口代码，并移除 `player_component` 对 `:repository:panel_switch` 的依赖；确保全仓无残留第三方类型引用。

## Validation Strategy

- 门禁（必须）：`./gradlew verifyModuleDependencies`、`./gradlew ktlintCheck`、`./gradlew lint`、`./gradlew testDebugUnitTest`、`./gradlew :app:assembleDebug`
- 涉及 DB/存储/解压/投屏等链路：补齐或执行 `connectedDebugAndroidTest`（至少覆盖解压与关键协议）。
- 每次合入需在 `document/code_quality_audit/global/backlog.md` 与对应模块报告中同步任务状态（Done）与必要备注（迁移策略/降级开关）。
