# code-quality-security-privacy-baseline Specification

## Purpose
TBD - created by archiving change resolve-code-quality-audit-backlog. Update Purpose after archive.
## Requirements
### Requirement: Release 默认 TLS 必须安全，任何放宽必须显式可控

系统 MUST 在 Release 默认路径中使用系统 TLS 校验（证书链校验 + 主机名校验），不得默认信任所有证书或禁用主机名校验。

系统 MAY 提供“特殊场景”的 TLS 放宽能力（例如自签证书/内网环境），但 MUST 满足：

- 仅在用户显式开关启用（Release 允许；debug 可提供快捷入口）
- UI/文档明确风险提示
- 能够按配置粒度精确生效（例如仅对某个存储配置/域名）

#### Scenario: 默认严格 TLS 下可正常访问合规站点

- **GIVEN** 用户在 Release 中配置了使用受信任证书的 WebDAV
- **WHEN** 用户执行连接/列表/播放
- **THEN** 系统成功完成请求且不使用“不安全 TLS”路径

#### Scenario: 仅在显式开关时允许不安全 TLS

- **GIVEN** 用户配置的 WebDAV 使用自签证书导致连接失败
- **WHEN** 用户未启用“不安全 TLS”开关再次连接
- **THEN** 系统仍失败并提示证书/校验问题
- **WHEN** 用户显式启用“不安全 TLS”开关并确认风险
- **THEN** 系统允许继续连接（或提供更安全替代路径，例如导入证书）

### Requirement: 源码不得硬编码可滥用的固定凭证/密钥

系统 MUST 不在仓库源码中硬编码固定凭证/密钥（例如第三方平台 `APP_KEY/APP_SEC`、固定 token/secret）。  
系统 SHALL 提供构建期注入或本地配置方式，并在缺省时提供可控降级策略（禁用相关功能并给出明确提示）。

#### Scenario: 缺少密钥时功能显式降级而非静默失败

- **GIVEN** Release 构建未注入 B 站 TV `APP_KEY/APP_SEC`
- **WHEN** 用户进入 TV 登录/签名相关流程
- **THEN** 系统提示需要配置并禁用该能力（不崩溃、不静默重试）

### Requirement: 投屏 UDP 加密必须使用带认证的方案并支持版本兼容

系统 MUST 使用带认证的对称加密（AEAD，例如 AES-GCM）保护投屏 UDP 载荷，且每条消息 MUST 使用随机 IV/nonce。  
系统 SHALL 为 UDP 载荷引入版本字段，并支持明确的兼容/降级策略，避免升级后新旧端不可控地互不兼容。

#### Scenario: 新旧端互通具备可控策略

- **GIVEN** 新版本 Sender 与新版本 Receiver
- **WHEN** 发送投屏发现/握手消息
- **THEN** Receiver 能正确解密并解析消息
- **GIVEN** 新版本 Receiver 与旧版本 Sender
- **WHEN** 接收到旧版本消息
- **THEN** Receiver 要么兼容解析，要么给出明确错误并触发可控降级（与产品策略一致）

### Requirement: 运行期凭证必须加密落盘并具备迁移机制

系统 MUST 将运行期敏感凭证（Cookie/Token/远程存储密码/secret）以密文形式落盘，并提供从旧明文存储迁移到密文存储的机制。  
迁移失败 MUST 可观测（结构化日志/上报），且不得导致崩溃或泄露明文。

#### Scenario: 旧用户升级后无需重复登录且凭证不以明文落盘

- **GIVEN** 旧版本已保存登录态与远程存储配置
- **WHEN** 用户升级到新版本并首次启动
- **THEN** 系统完成迁移且用户关键功能可继续使用（不强制重新登录/重配）
- **AND** 设备存储中不应出现可直接检索到的明文 token/password（以抽样检查为准）

