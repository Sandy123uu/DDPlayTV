# 模块排查报告：:core_system_component

- 模块：:core_system_component
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：core_system_component/src/main/java/com/xyoye/common_component/

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`CORE_SYSTEM-F###`  
> - Task：`CORE_SYSTEM-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 作为 runtime/system 层基础能力：应用启动与初始化支撑（`BaseApplication`、AndroidX Startup Initializer）、全局配置表（MMKV）、通知渠道、权限请求、通用工具与扩展方法。
  - 提供“跨模块共享但强依赖 Android runtime”的能力落点（Context/Handler、Activity 生命周期、屏幕/磁盘/媒体工具等）。
  - 提供密钥/凭证的本地存取与（尽可能）加密能力（`DeveloperCredentialStore`、`SecurityHelperConfig`）。
- 模块职责（不做什么）
  - 不承载具体业务特性逻辑（Anime/Local/Player/Storage/User 等）。
  - 不直接依赖任何 feature 模块（避免依赖环）；跨模块协作应通过 `:core_contract_component` 契约表达。
- 关键入口/关键路径（示例）
  - `core_system_component/src/main/java/com/xyoye/common_component/base/app/BaseApplication.kt` + `BaseApplication#onCreate`
  - `core_system_component/src/main/java/com/xyoye/common_component/base/app/BuglyInitializer.kt` + `BuglyInitializer#create`
  - `core_system_component/src/main/java/com/xyoye/common_component/config/AppConfigTable.kt` + `AppConfigTable`（生成 `AppConfig`）
  - `core_system_component/src/main/java/com/xyoye/common_component/application/permission/PermissionManager.kt` + `PermissionManager#requestPermissions`
  - `core_system_component/src/main/java/com/xyoye/common_component/config/DeveloperCredentialStore.kt` + `DeveloperCredentialStore#getAppSecret/putAppSecret`
- 依赖边界（与哪些模块交互，是否存在边界疑点）
  - 依赖：`:data_component`（配置枚举/数据结构）、`:core_contract_component`（契约类型）、`:core_log_component`（日志/上报基础设施）。
  - 被依赖：几乎所有模块（配置表、工具、启动与运行时能力常被共享）。
  - 边界疑点：模块内包含大量 `utils/extension/config/base` 等“通用能力”，容易演化为无边界的“大杂烩”，增加未来拆分成本。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - 示例：用 ast-grep 定位潜在风险调用（如 `MMKV.initialize(...)`、`printStackTrace()`）；用 `rg` 补充定位调用方与跨模块影响面。

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| CORE_SYSTEM-F001 | SecurityPrivacy | `EntropyUtils` 提供不安全的加密/摘要实现（固定 IV + 默认 Key + MD5），且被投屏 UDP 组播直接使用 | `core_system_component/src/main/java/com/xyoye/common_component/utils/EntropyUtils.kt` + `EntropyUtils#aesEncode/aesDecode`（legacy CBC + v2 AES-GCM）；`storage_component/src/main/java/com/xyoye/storage_component/utils/screencast/receiver/UdpServer.kt` + `UdpServer#sendMulticast`；`storage_component/src/main/java/com/xyoye/storage_component/utils/screencast/receiver/HttpServer.kt` + `HttpServer#authentication`；`core_storage_component/src/main/java/com/xyoye/common_component/network/repository/ScreencastRepository.kt` + `ScreencastRepository#init` | N/A | Deprecate | `:core_system_component`（提供安全实现）+ `:storage_component`（迁移调用） | High | Medium | P1 | 需要设计消息版本/兼容策略，避免升级后“旧端/新端”互不兼容；投屏链路回归成本中等 |
| CORE_SYSTEM-F002 | StabilityRisk | `ActivityHelper#getTopActivity` 使用 `first {}` 可能在无存活 Activity 时抛异常，后台服务调用存在崩溃风险 | `core_system_component/src/main/java/com/xyoye/common_component/utils/ActivityHelper.kt` + `ActivityHelper#getTopActivity`；`storage_component/src/main/java/com/xyoye/storage_component/services/ScreencastReceiveService.kt` + `ScreencastReceiveService#considerAcceptScreencast` / `ScreencastReceiveService#onReceiveScreencast` | N/A | Unify | `:core_system_component` | High | Small | P1 | 修改需确保“无前台界面/后台态”下行为可预期；回归需覆盖投屏接收流程 |
| CORE_SYSTEM-F003 | Redundancy | MMKV 初始化入口重复（Startup `BaseInitializer` 与 `BaseApplication#onCreate` 均调用 `MMKV.initialize`） | `core_system_component/src/main/java/com/xyoye/common_component/base/app/BaseInitializer.kt` + `BaseInitializer#create`；`core_system_component/src/main/java/com/xyoye/common_component/base/app/BaseApplication.kt` + `BaseApplication#onCreate` | N/A | Unify | `:core_system_component` | Low | Small | P2 | 需确认多进程/Startup 时序；避免某些场景下初始化过晚影响配置读取 |
| CORE_SYSTEM-F004 | SecurityPrivacy | `DeveloperCredentialStore` 在加密失败时会回退保存明文凭证，存在凭证泄露风险 | `core_system_component/src/main/java/com/xyoye/common_component/config/DeveloperCredentialStore.kt` + `DeveloperCredentialStore#putCredential`（注释“加密失败：兜底仍然保存明文”） | N/A | Unify | `:core_system_component`（存储策略）+ `:user_component`（提示/开关） | Medium | Medium | P2 | 若完全禁用明文兜底可能导致“开发者凭证”功能不可用；需要明确产品策略与迁移方案 |
| CORE_SYSTEM-F005 | Redundancy | 多处异常处理仍 `printStackTrace()`，与 `ErrorReportHelper`/`LogFacade` 重复且易污染日志 | `core_system_component/src/main/java/com/xyoye/common_component/utils/EntropyUtils.kt` + `EntropyUtils#file2Md5/aesEncode/aesDecode`；`core_system_component/src/main/java/com/xyoye/common_component/application/permission/PermissionManager.kt` + `PermissionManager#onRequestComplete`；`core_system_component/src/main/java/com/xyoye/common_component/notification/Notifications.kt` + `Notifications#setupNotificationChannels`；`core_system_component/src/main/java/com/xyoye/common_component/utils/MediaUtils.kt` + `MediaUtils#saveImage` | N/A | Unify | `:core_system_component` | Medium | Small | P1 | 需要统一“捕获异常后的处理口径”（上报/打点/日志级别）；避免在 release 产生无序堆栈输出 |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| CORE_SYSTEM-T001 | CORE_SYSTEM-F001 | 用安全方案替换 `EntropyUtils` 的对称加密（至少支持随机 IV/带认证），并为投屏 UDP 消息引入版本/兼容策略 | `core_system_component/src/main/java/com/xyoye/common_component/utils/EntropyUtils.kt`（新增/替换实现）；`storage_component/src/main/java/com/xyoye/storage_component/utils/screencast/receiver/UdpServer.kt`；`storage_component/src/main/java/com/xyoye/storage_component/utils/screencast/receiver/HttpServer.kt`；`core_storage_component/src/main/java/com/xyoye/common_component/network/repository/ScreencastRepository.kt` | 1) 不再使用固定 `IvParameterSpec(ByteArray(16))` 与默认 Key；2) UDP 加密载荷包含必要的 IV/版本信息；3) 新旧版本至少具备可控的兼容/降级策略；4) 投屏收发链路可回归验证 | High | Medium | P1 | AI（Codex） | Done |
| CORE_SYSTEM-T002 | CORE_SYSTEM-F002 | 修复 `ActivityHelper#getTopActivity` 的潜在崩溃：返回 `firstOrNull` 并清理已销毁 Activity | `core_system_component/src/main/java/com/xyoye/common_component/utils/ActivityHelper.kt` + `ActivityHelper#getTopActivity`；相关调用方（如 `storage_component/.../ScreencastReceiveService.kt`） | 1) `getTopActivity()` 在无可用 Activity 时返回 `null` 不抛异常；2) 保持现有调用方语义（已做 null 判断的逻辑不变）；3) 覆盖投屏接收“前台/后台/无界面”场景回归 | High | Small | P1 | AI（Codex） | Done |
| CORE_SYSTEM-T003 | CORE_SYSTEM-F005 | 收敛异常处理口径：移除 `printStackTrace()`，统一使用 `ErrorReportHelper` + `LogFacade` 记录必要信息 | `core_system_component/src/main/java/com/xyoye/common_component/**`（见 Finding 证据点） | 1) `core_system_component` 内不再出现 `printStackTrace()`；2) 保留必要的异常上报与可定位信息（路径+符号+上下文）；3) 不引入新的敏感信息日志输出 | Medium | Small | P1 | AI（Codex） | Done |
| CORE_SYSTEM-T004 | CORE_SYSTEM-F003 | 统一 MMKV 初始化策略：明确“Startup 初始化”与“BaseApplication 初始化”的职责边界，避免重复与时序不一致 | `core_system_component/src/main/java/com/xyoye/common_component/base/app/BaseInitializer.kt`；`core_system_component/src/main/java/com/xyoye/common_component/base/app/BaseApplication.kt`；`app/src/main/java/com/okamihoro/ddplaytv/app/IApplication.kt` | 1) 明确唯一初始化入口（或保证幂等且只记录一次）；2) 不破坏 MultiDex/Startup 时序；3) 全仓编译通过 | Low | Small | P2 | AI（Codex） | Done |
| CORE_SYSTEM-T005 | CORE_SYSTEM-F004 | 对“加密失败保存明文”制定策略：默认安全优先（禁用/提示/二次确认），并提供可追踪的迁移/清理机制 | `core_system_component/src/main/java/com/xyoye/common_component/config/DeveloperCredentialStore.kt`；相关设置项 UI（`user_component`） | 1) 明确明文兜底的启用条件（例如仅 debug / 显式开关）；2) 存量明文可被识别并提示迁移；3) 不在日志/上报中泄露凭证 | Medium | Medium | P2 | 待分配（Runtime/User） | Draft |

## 5) 风险与回归关注点

- 行为回退风险（哪些场景最敏感）
  - 启动链路（Startup Initializer + MultiDex + BaseApplication）对时序敏感。
  - 投屏接收链路依赖 `ActivityHelper` 与 UDP 消息解密，改动需覆盖前台/后台/无界面场景。
- 回归成本（需要的账号/媒体文件/设备）
  - TV/移动端各至少 1 台；投屏发送端/接收端各 1 台（或同机双进程不可行）。
  - 若涉及开发者凭证：需要可用的 DanDan 凭证或模拟服务端。

## 6) 备注（历史背景/待确认点）

- 本报告为初稿，建议按 `document/code_quality_audit/global/review_checklist.md` 复核后再将 `module_status` 标记为 Done。
