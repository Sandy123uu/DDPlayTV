# 网络 TLS 安全策略（OkHttp）

> 目的：在不牺牲“默认安全”的前提下，给 WebDAV/本地代理等对 TLS 更敏感的链路提供**可控**的兼容方案，并避免 `trust-all`（信任所有证书）在 Release 默认路径中扩散。

## 1. 结论（最重要的几条）

1) **默认严格 TLS**：默认使用系统信任链 + 主机名校验（推荐）。
2) **优先选择更安全的兼容方案**：
   - **证书导入（自签 / 私有 CA）**：只额外信任指定 CA（仍保留系统默认信任链）。
   - **Pin（公钥固定）**：对指定域名启用 `sha256/` pin，提升抗 MITM 能力。
   - **仅 HTTP**：如果服务端允许且安全模型可接受，使用 `http://` 规避 TLS 问题。
3) **`trust-all` 仅作为最后手段**：必须由**用户显式开启**（Release 允许），并明确风险提示；代码层需要显式 opt-in，避免误用。

## 2. 代码落点

- TLS 策略模型：`core_network_component/src/main/java/com/xyoye/common_component/network/helper/OkHttpTlsPolicy.kt`
- TLS 策略应用器：`core_network_component/src/main/java/com/xyoye/common_component/network/helper/OkHttpTlsConfigurer.kt`
- WebDAV 专用 Client 工厂：`core_network_component/src/main/java/com/xyoye/common_component/network/helper/WebDavOkHttpClientFactory.kt`
- `trust-all` 旧入口（已标记 deprecated + requires opt-in）：`core_network_component/src/main/java/com/xyoye/common_component/network/helper/UnsafeOkHttpClient.kt`

## 3. 使用约束（Guardrails）

- `OkHttpTlsPolicy.UnsafeTrustAll` 与 `UnsafeOkHttpClient` 受 `@UnsafeTlsApi` 保护：
  - 任何调用点必须显式 `@OptIn(UnsafeTlsApi::class)`。
  - 新增调用点必须能说明“为何不能用严格 TLS / 证书导入 / Pin / HTTP”，并确保由用户开关控制（Release 默认关闭）。

## 4. 推荐使用方式（示例）

### 4.1 证书导入（自签 / 私有 CA）

- 使用 `OkHttpTlsPolicy.CustomCaCertificates.fromInputStream(...)` 构建策略，并通过 `OkHttpTlsConfigurer.apply(builder, policy)` 应用到 `OkHttpClient.Builder`。
- 该策略会在**保留系统默认信任链**的前提下，额外信任导入的 CA。

### 4.2 Pin（公钥固定）

- 使用 `OkHttpTlsPolicy.PinnedPublicKeys(hostname, sha256Pins)`。
- Pin 值使用 OkHttp 标准格式：`sha256/<base64>`。

### 4.3 最后手段：trust-all

- 仅用于“用户明确理解风险且确实需要”的场景。
- 必须通过 UI/配置开关启用，并在代码中显式 `@OptIn(UnsafeTlsApi::class)`。

