# 模块排查报告：:storage_component

- 模块：:storage_component
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：storage_component/src/main/java/com/xyoye/storage_component/（含 `storage_component/src/main/java/com/xyoye/common_component/utils/QrCodeHelper.kt`）

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`STORAGE-F###`  
> - Task：`STORAGE-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - “存储/串流/投屏”相关 UI 与服务：存储配置（WebDAV/SMB/FTP/网盘等）编辑对话框、存储列表页、文件浏览页、远程扫码/导入、投屏接收（TV）服务等。
  - 承接 `:core_storage_component` 的 `StorageFactory/StorageFile` 抽象，提供“选择/浏览/播放入口”的 feature 层编排（路由、权限、UI 交互）。
  - 投屏（Screencast）协议的 feature 侧实现：UDP 发现 + HTTP 接收端（TV）服务，以及历史上“发送端（Phone）”的实现痕迹。
- 模块职责（不做什么）
  - 不应沉淀可跨模块复用的底层网络/协议实现（例如通用 NanoHTTPD server/Range/鉴权/错误上报策略），否则容易与 `:core_storage_component/:core_network_component` 能力重复并产生口径漂移。
  - 不应把“仅 TV 禁用/仅移动端启用”的能力做成全端静默 stub；若是 TV UI mode 特化，需明确使用 `Context.isTelevisionUiMode()` 做分流，避免误伤移动端能力（对齐仓库 TV 规范）。
- 关键入口/关键符号（示例）
  - `storage_component/src/main/java/com/xyoye/storage_component/ui/activities/storage_file/StorageFileActivity.kt` + `StorageFileActivity`
  - `storage_component/src/main/java/com/xyoye/storage_component/ui/fragment/storage_file/StorageFileFragmentViewModel.kt` + `StorageFileFragmentViewModel`
  - `storage_component/src/main/java/com/xyoye/storage_component/ui/activities/storage_plus/StoragePlusActivity.kt` + `StoragePlusActivity`
  - `storage_component/src/main/java/com/xyoye/storage_component/services/ScreencastReceiveService.kt` + `ScreencastReceiveService`
  - `storage_component/src/main/java/com/xyoye/storage_component/utils/screencast/receiver/HttpServer.kt` + `HttpServer#authentication`
- 依赖边界
  - 对内（依赖）：`core_ui/system/log/network/database/storage/bilibili/contract/data`（见 `storage_component/build.gradle.kts`）。
  - 对外（被依赖）：作为 feature 模块通常不被其它 feature 依赖，入口通过 `RouteTable.Stream.*`/`RouteTable.Storage.*` 由 `:app` 发起导航或服务获取。
  - 边界疑点：
    - 投屏能力分布在 `:core_storage_component`（`ScreencastStorage/ScreencastConstants`）与 `:storage_component`（UDP/HTTP server 与服务）之间，容易出现“协议/安全/日志策略”不一致。
    - `ScreencastProvideService` 在本模块内被整体 stub 掉且保留大量注释掉的旧实现，存在“产品策略不清 + 误伤非 TV 场景 + 技术债堆积”的风险。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - ast-grep：定位 `printStackTrace()` 分布、`NanoHTTPD` 派生类、`DatabaseManager.instance` 直连 DAO 的调用点，避免纯文本误报。

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| STORAGE-F001 | Redundancy | 投屏发送端（Sender）被“全端静默 stub + 大段注释旧实现”处理，策略不清且可能误伤非 TV 场景 | `storage_component/src/main/java/com/xyoye/storage_component/services/ScreencastProvideService.kt` + `ScreencastProvideService#start/isRunning/onProvideVideo`（无条件禁用并抛异常）；`storage_component/src/main/java/com/xyoye/storage_component/services/ScreencastProvideServiceImpl.kt` + `ScreencastProvideServiceImpl#startService`（对外仍暴露入口） | N/A | Unify | 以 `Context.isTelevisionUiMode()` 或 build flavor 明确分流；保留可维护的实现（移动端）与明确禁用策略（TV） | High | Small | P1 | 需要确认产品策略：DDPlayTV 是否存在移动端形态；若仅 TV 形态，则应删除旧注释实现并保留明确“不可用”说明，避免误用 |
| STORAGE-F002 | SecurityPrivacy | 模块内 `printStackTrace()` 分布广且错误上报上下文拼接分散，存在隐私泄露与可观测性口径漂移风险 | `storage_component/src/main/java/com/xyoye/storage_component/services/ScreencastReceiveService.kt` + `ScreencastReceiveService#createHttpServer`；`storage_component/src/main/java/com/xyoye/storage_component/utils/screencast/receiver/HttpServer.kt` + `HttpServer#authentication`；`storage_component/src/main/java/com/xyoye/common_component/utils/QrCodeHelper.kt` + `QrCodeHelper#createQrCode`；以及 `storage_component/...` 多处 `e.printStackTrace()`（可用 ast-grep 精确统计） | N/A | Unify | `:core_log_component`（统一异常上报入口 + 脱敏策略）+ 本模块逐步迁移替换 | Medium | Small | P1 | 脱敏策略需覆盖：URL/headers/token/password/文件路径；替换时避免吞异常导致行为变化 |
| STORAGE-F003 | Duplication | 投屏协议/服务器实现分散：本模块自建 NanoHTTPD/UDP 发现，与 core 层的投屏存储与本地代理能力存在重叠，安全/Range/鉴权策略难统一 | `storage_component/src/main/java/com/xyoye/storage_component/utils/screencast/provider/HttpServer.kt` + `HttpServer#serve`；`storage_component/src/main/java/com/xyoye/storage_component/utils/screencast/receiver/HttpServer.kt` + `HttpServer#serve`；`storage_component/src/main/java/com/xyoye/storage_component/utils/screencast/receiver/UdpServer.kt` + `UdpServer#sendMulticast`；对照 core 层：`core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/ScreencastStorage.kt` + `ScreencastStorage#test/cacheSubtitle` | Unintentional | Unify | 以 `:core_storage_component` 作为投屏 domain 的统一落点（协议/常量/鉴权/错误口径），feature 仅负责 UI 与服务编排 | Medium | Medium | P2 | 涉及跨模块重构；需回归 TV 端“投屏接收→播放”链路与历史进度回写；注意 `adb logcat` 必须过滤 |
| STORAGE-F004 | ArchitectureRisk | DAO 访问散落在 ViewModel/Dialog/Controller 中，数据写入口径分散（MediaLibrary/PlayHistory），迁移与一致性维护成本高 | `storage_component/src/main/java/com/xyoye/storage_component/ui/activities/storage_plus/StoragePlusViewModel.kt` + `StoragePlusViewModel`（MediaLibrary upsert）；`storage_component/src/main/java/com/xyoye/storage_component/ui/fragment/storage_file/StorageFileFragmentViewModel.kt` + `StorageFileFragmentViewModel`（PlayHistory 更新/绑定）；`storage_component/src/main/java/com/xyoye/storage_component/utils/screencast/provider/ServerController.kt` + `ServerController#handleScreencastCallback`（历史回写） | N/A | Unify | `:core_database_component` 提供 repository/usecase（PlayHistory/MediaLibrary），feature 侧只依赖接口 | Medium | Medium | P2 | 需要统一线程调度（IO/Main）与事务边界；迁移要避免破坏“续播/弹幕字幕绑定”行为 |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| STORAGE-T001 | STORAGE-F001 | 明确投屏 Sender 的产品策略并落地成“可维护的分流实现”，清理注释掉的旧实现与全端 stub | 方案 A：在 `:storage_component` 内按 `Context.isTelevisionUiMode()` 分流启用/禁用 Sender；方案 B：用 build flavor/sourceSet 区分 TV/Phone 并移除注释块；同步调整 `ScreencastProvideServiceImpl` 对外能力暴露 | 1) TV 端不出现“可见但不可用”的 Sender 入口；2) 若移动端存在 Sender，确保可运行且不引入 feature↔feature 依赖；3) 不保留大段注释实现；4) 全仓编译通过 | High | Medium | P1 | 待分配（Storage/TV） | Draft |
| STORAGE-T002 | STORAGE-F002 | 移除 `printStackTrace()` 并统一异常上报上下文与脱敏策略（URL/token/password/路径） | 在 `:core_log_component` 增加 `safeReport(...)`/`LogContext`（若已有则复用）；本模块替换 `e.printStackTrace()` 与分散的 try/catch 上报逻辑 | 1) `storage_component` 内无 `printStackTrace()`；2) 上报上下文满足脱敏策略；3) 关键失败仍可定位（模块/动作/关键 ID） | Medium | Small | P1 | 待分配（Log/Feature） | Draft |
| STORAGE-T003 | STORAGE-F003 | 抽取投屏协议与 server 公共能力到 core 层，减少 NanoHTTPD/UDP 重复与策略漂移 | 将投屏协议（常量/鉴权/header/range/错误响应）统一到 `:core_storage_component`；`storage_component` 的 UDP/HTTP server 仅保留编排或改为调用 core 的实现 | 1) core 层定义稳定 API；2) feature 不再重复实现同一协议细节；3) 投屏接收链路可回归（发现→鉴权→播放→进度回写）；4) 全仓编译通过 | Medium | Large | P2 | 待分配（Storage/Infra） | Draft |
| STORAGE-T004 | STORAGE-F004 | 收敛 MediaLibrary/PlayHistory 的写入口径：提供 repository/usecase 并替换 feature 直连 DAO | `:core_database_component` 新增 `MediaLibraryRepository/PlayHistoryRepository`；迁移 `StoragePlusViewModel/StorageFileFragmentViewModel/ServerController` 等调用点 | 1) 目标类不再直接调用 `DatabaseManager.instance.get*Dao()`；2) 行为一致（存储增删改/续播/绑定字段更新）；3) 线程与事务边界一致；4) 全仓编译通过 | Medium | Medium | P2 | 待分配（DB/Feature） | Draft |

## 5) 风险与回归关注点

- TV 端焦点/可达性：存储配置对话框、文件浏览列表、投屏接收页面必须验证 DPAD 路径与默认焦点（避免“无焦点/隐藏 View 抢焦点”）。
- 投屏接收链路：`ScreencastReceiveService` 依赖前台通知、UDP 组播、HTTP 鉴权与路由到播放器；任何抽取/收敛都需要回归“发现→连接→播放→续播/进度回写”。
- 隐私与日志：投屏/存储配置涉及 IP/端口/Authorization/密码/URL/文件路径；上报与日志统一后需确保脱敏仍可定位问题（建议保留 host 与长度/哈希）。

## 6) 备注（历史背景/待确认点）

- 本报告为 AI 辅助生成的初稿，建议按 `document/code_quality_audit/global/review_checklist.md` 做一次人工复核后再标记为 Done。
