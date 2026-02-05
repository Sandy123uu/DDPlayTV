# 模块排查报告：:repository:thunder

- 模块：:repository:thunder
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：repository/thunder/

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`REPO_THUNDER-F###`  
> - Task：`REPO_THUNDER-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 以 Gradle module 形式封装本地预编译 AAR：`repository/thunder/thunder.aar`，提供迅雷下载 SDK（典型包名：`com.xunlei.downloadlib.*`，含 `jni/*/*.so`）。
  - 为“磁力链/BT 种子播放”提供底层能力：下载 `.torrent`、解析种子文件列表、按选定文件索引启动任务并生成本地播放 URL（P2P/边下边播）。
- 模块职责（不做什么）
  - 不承载业务代码；除 `build.gradle.kts` 与 AAR 产物外不应新增 Kotlin/Java 实现。
  - 不应被 feature 模块直接依赖并传播第三方类型（应由 `:core_storage_component` 统一封装对外能力）。
- 关键入口/关键路径（示例）
  - AAR 封装：`repository/thunder/build.gradle.kts` + `artifacts.add("default", file("thunder.aar"))`
  - 二进制产物：`repository/thunder/thunder.aar`（含 `jni/arm64-v8a/*.so`、`jni/armeabi-v7a/*.so`、`classes.jar`）
  - 依赖声明（消费方）：`core_storage_component/build.gradle.kts` + `implementation(project(":repository:thunder"))`
  - 统一封装入口：`core_storage_component/src/main/java/com/xyoye/common_component/utils/thunder/ThunderManager.kt`
    - `ThunderManager#ensureInitialized`（按需初始化、幂等、可降级）
    - `ThunderManager#downloadTorrentFile`（`XLTaskHelper.addMagnetTask` → `getTaskInfo/stopTask`）
    - `ThunderManager#generatePlayUrl`（`startTask` → `XLDownloadManager.getLocalUrl`）
  - 存储/播放接入：`core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/TorrentStorage.kt`
    - `TorrentStorage#listFiles/createPlayUrl`（`TorrentBean.formInfo` + `ThunderManager.generatePlayUrl`）
  - 按需初始化入口：首次使用“磁力链/BT”能力时由 `ThunderManager` 触发初始化（不再在进程启动期初始化）
- 依赖边界
  - 对外（被依赖）：`repository:thunder` 仅被 `:core_storage_component` 依赖（见 `core_storage_component/build.gradle.kts`）。
  - 对内（依赖）：无（wrapper 模块仅提供 AAR 工件）。
  - 边界确认（AST）：全仓 `com.xunlei.downloadlib.*` 的 `import` 仅出现在 `core_storage_component`（见下方方法与证据）；其余模块通过 `ThunderManager` 间接使用，不直接触达第三方 SDK 类型。
  - 边界疑点：`core_storage_component` 内部存在对 `com.xunlei.*` 类型的继承/暴露（例如 `TorrentBean : TorrentInfo`），未来可能导致“第三方类型泄漏到上层模块”（见 Findings）。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - `rg`：确认依赖链路与使用分布  
    - `rg "project\\(\\\":repository:thunder\\\"\\)" -n`（定位消费方）  
    - `rg "com\\.xunlei\\.downloadlib" -n core_storage_component`（定位封装入口）
  - ast-grep：确证第三方 import 的实际分布（避免纯文本误判）  
    - Kotlin：`import com.xunlei.downloadlib.$X`（全仓扫描，确认仅 `core_storage_component` 命中）

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| REPO_THUNDER-F001 | ArchitectureRisk | 预编译 AAR 缺少来源/版本/License 等元信息，升级与合规审计不可追溯 | `repository/thunder/thunder.aar`（二进制）；`repository/thunder/build.gradle.kts` + `artifacts.add("default", file("thunder.aar"))` | N/A | Unify | `repository/thunder/`（补齐元信息）+ 全仓统一规范（建议由 `buildSrc` 或文档约束） | Medium | Small | P1 | 需要确认 AAR 上游来源、版本号、授权协议；否则难以在安全事件/升级时快速定位影响 |
| REPO_THUNDER-F002 | ArchitectureRisk | 第三方 SDK 类型在 `:core_storage_component` 中被继承/暴露，存在“第三方类型泄漏到上层模块”的边界风险 | `core_storage_component/src/main/java/com/xyoye/common_component/storage/file/helper/TorrentBean.kt` + `class TorrentBean : TorrentInfo`；`core_storage_component/src/main/java/com/xyoye/common_component/storage/file/impl/TorrentStorageFile.kt` + `TorrentStorageFile#getRealFile(): TorrentFileInfo` | N/A | Unify | 将 `com.xunlei.*` 类型收敛在 `utils/thunder` 与内部实现层：对外使用自定义 model/interface（例如 `TorrentMeta`/`TorrentEntry`），避免上层模块被迫感知第三方类型 | Medium | Medium | P2 | 需要评估现有调用方是否存在对 `TorrentBean/TorrentStorageFile` 的强依赖（目前全仓命中点仅在 `core_storage_component`） |
| REPO_THUNDER-F003 | PerformanceRisk | 进程启动期初始化迅雷 SDK 会引入启动期开销与崩溃面 | `core_storage_component/src/main/java/com/xyoye/common_component/utils/thunder/ThunderManager.kt` + `ThunderManager#ensureInitialized`（按需初始化、缓存结果、失败可降级）；（已移除）`storage_component` 内 AndroidX Startup 初始化入口 | N/A | Unify | 改为“按需初始化”（首次使用磁力/BT 能力时初始化），并把初始化结果缓存；移除 Startup 初始化入口（见 `REPO_THUNDER-T003`） | Medium | Small | P1 | 需关注 `XLTaskHelper.init` 的线程/时序要求；若必须早期 init，需要量化耗时并制定降级策略 |
| REPO_THUNDER-F004 | SecurityPrivacy | 错误上报上下文包含原始磁力链/URL 与本地路径，可能导致敏感信息泄露（日志/崩溃平台/上报渠道） | `core_storage_component/src/main/java/com/xyoye/common_component/utils/thunder/ThunderManager.kt` + `downloadTorrentFile`（`"磁链: $magnet"`）；`core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/TorrentStorage.kt` + `torrentPath/getRootFile`（`library.url`） | N/A | Unify | 统一脱敏策略：磁力链仅保留 hash；URL 默认去掉 query/fragment；本地路径仅保留文件名或 hash（与全仓日志策略对齐） | High | Small | P1 | 需与 `:core_log_component`/错误上报策略对齐，避免“部分链路脱敏、部分链路明文” |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| REPO_THUNDER-T001 | REPO_THUNDER-F001 | 为 AAR 增加可追溯元信息（来源/版本/License/校验和/更新流程） | 新增 `repository/thunder/README.md`（中文）；可选新增 `repository/thunder/LICENSE` 或在 README 中明确 License 与引用位置 | 1) README 明确：上游项目/下载地址、版本号、License、AAR SHA256、更新步骤；2) 任意人可按文档复现升级；3) 不影响现有依赖解析 | Medium | Small | P1 | AI（Codex） | Done |
| REPO_THUNDER-T002 | REPO_THUNDER-F002 | 封装/隔离迅雷 SDK 类型，避免第三方类型在上层模块显式出现 | `:core_storage_component` 内部新增自定义 model；将 `TorrentBean/TorrentStorageFile` 迁移为 `internal` 或改为仅暴露 `StorageFile` 抽象；封装 `TorrentInfo/TorrentFileInfo/XLTaskInfo` 的读取逻辑到 `utils/thunder` | 1) 上层模块无需 `import com.xunlei.*` 也无需依赖其类型；2) 相关能力仍可完成：列种子文件/生成播放 URL/任务状态查询；3) `./gradlew :core_storage_component:assembleDebug` 通过 | Medium | Medium | P2 | 待分配（Storage） | Draft |
| REPO_THUNDER-T003 | REPO_THUNDER-F003 | 将迅雷 SDK 初始化策略改为按需 + 可降级，降低启动期开销与崩溃面 | 在 `ThunderManager` 内实现“只初始化一次”的幂等保护与按需初始化，并移除进程启动期的 AndroidX Startup 初始化 | 1) 无迅雷 SDK 支持的设备不崩溃；2) 支持设备上磁力/BT 功能首次使用可正常工作；3) 若初始化失败可明确降级（提示/禁用入口），不影响其它功能 | Medium | Small | P1 | AI（Codex） | Done |
| REPO_THUNDER-T004 | REPO_THUNDER-F004 | 对磁力链/URL/本地路径的日志与异常上报进行脱敏，并统一口径 | 修改 `ThunderManager/TorrentStorage/PlayTaskManager` 的错误上下文字符串：磁力链 → 仅 hash；URL → 去 query；路径 → 仅文件名或 hash；必要时在 `:core_log_component` 提供统一脱敏工具供复用 | 1) 上报/日志中不出现原始 magnet/URL query/token/完整本地路径；2) 仍可定位问题（保留 hash/host/path）；3) 全仓日志策略一致 | High | Small | P1 | AI（Codex） | Done |

## 5) 风险与回归关注点

- 行为回退风险：初始化时序变更、任务创建/停止策略变更可能导致“磁力链 → torrent 下载失败”“选定文件索引播放失败”“边下边播卡顿/无法 seek”等回退。
- 回归成本：需要覆盖 1) `magnet:` 链接（不同 tracker）→ 下载 torrent；2) `.torrent` 本地文件；3) 多文件种子选择正确文件索引；4) arm64 与 armeabi-v7a（至少其一）设备/模拟器；5) TV/移动端各 1 套（主要关注交互入口与异常提示）。

## 6) 备注（历史背景/待确认点）

- 当前仓库内未见 `thunder.aar` 的上游来源与版本信息，建议先补齐元信息再做升级/替换评估。
- `thunder.aar` 含 native `.so`，需要明确 ABI 覆盖范围与不支持设备的降级策略；避免“仅少数机型可用但默认入口对所有人可见”的体验问题。
