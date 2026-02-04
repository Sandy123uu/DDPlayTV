# 模块排查报告：:repository:seven_zip

- 模块：:repository:seven_zip
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：repository/seven_zip/

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`REPO_SEVEN_ZIP-F###`  
> - Task：`REPO_SEVEN_ZIP-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 以 Gradle module 形式封装本地预编译 AAR：`repository/seven_zip/sevenzipjbinding4Android.aar`，供工程以 `project(":repository:seven_zip")` 依赖方式接入。
  - 提供 7z/zip/rar 等多格式解压能力（`net.sf.sevenzipjbinding.*`），AAR 内含多 ABI 的 native `.so`（例如 `jni/arm64-v8a/lib7-Zip-JBinding.so`）。
- 模块职责（不做什么）
  - 不承载任何业务逻辑；除 `build.gradle.kts` 与 AAR 工件外不应新增 Kotlin/Java 实现。
  - 不应在 wrapper 内引入其它工程模块依赖（避免把 wrapper 变为“功能模块”并扩大耦合面）。
- 关键入口/关键路径（示例）
  - AAR 封装：`repository/seven_zip/build.gradle.kts` + `artifacts.add("default", file("sevenzipjbinding4Android.aar"))`
  - 二进制产物：`repository/seven_zip/sevenzipjbinding4Android.aar`（SHA256：`def3f43296b21e148b8bcc4bb7cf40c99352488c73ded7e2faa258fbabf17caa`）
  - 依赖声明（入口）：`core_storage_component/build.gradle.kts` + `implementation(project(":repository:seven_zip"))`
  - 典型使用点（解压工具）：`core_storage_component/src/main/java/com/xyoye/common_component/utils/seven_zip/SevenZipUtils.kt` + `SevenZipUtils#extractFile`
  - 典型使用点（回调/输出流）：`core_storage_component/src/main/java/com/xyoye/common_component/utils/seven_zip/ArchiveExtractCallback.kt` + `ArchiveExtractCallback#getStream`；`core_storage_component/src/main/java/com/xyoye/common_component/utils/seven_zip/SequentialOutStream.kt` + `SequentialOutStream#write`
  - 典型业务链路：`core_storage_component/src/main/java/com/xyoye/common_component/utils/subtitle/SubtitleUtils.kt` + `SubtitleUtils#saveAndUnzipFile`
  - License 线索：`user_component/src/main/assets/license/7-Zip-JBinding.txt`；概览清单：`document/Third_Party_Libraries.md`
- 依赖边界
  - 对外（被依赖）：`REPO_SEVEN_ZIP` 当前由 `:core_storage_component` 直接依赖并通过内部工具类使用（未以 `api(...)` 大范围导出，扩散面相对可控）。
  - 对内（依赖）：无（仅提供 AAR 工件，不应再依赖其它工程模块）。
  - 边界疑点：
    - 虽然 wrapper 边界清晰，但“使用方式（解压实现）”位于 `:core_storage_component` 的 `common_component/utils/seven_zip/*`，当前实现存在资源释放/流式写入语义等风险（见 Findings），这会把风险向上游业务链路放大（字幕下载/解压）。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - `rg`：确认 `net.sf.sevenzipjbinding` 的使用点与调用链  
    - `rg "net\\.sf\\.sevenzipjbinding" -n`
  - ast-grep：确证关键 API 调用形态  
    - Kotlin：`SevenZip.openInArchive($FORMAT, $STREAM)`  
    - Kotlin：`SevenZipUtils.extractFile($FILE)`

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| REPO_SEVEN_ZIP-F001 | ArchitectureRisk | 预编译 AAR 缺少 wrapper 侧可追溯元信息（来源/版本/校验和/更新流程），且含 native so，安全升级/合规审计成本更高 | `repository/seven_zip/sevenzipjbinding4Android.aar`（二进制，含 `jni/*/lib7-Zip-JBinding.so`）；`repository/seven_zip/build.gradle.kts` + `artifacts.add(...)`；License 线索：`user_component/src/main/assets/license/7-Zip-JBinding.txt`；概览：`document/Third_Party_Libraries.md` | N/A | Unify | `repository/seven_zip/`（补齐 README 元信息并与其它 wrapper 统一规范） | Medium | Small | P1 | 需要确认 AAR 对应上游版本号、支持的 ABI/最低系统要求；否则安全事件/升级时难以评估影响面 |
| REPO_SEVEN_ZIP-F002 | BugRisk | 解压实现存在资源释放缺口：`RandomAccessFile`/`IInArchive` 未关闭，协程取消亦不释放（潜在 FD 泄漏/崩溃） | `core_storage_component/src/main/java/com/xyoye/common_component/utils/seven_zip/SevenZipUtils.kt` + `SevenZipUtils#extractFile`（创建 `RandomAccessFile(...)`、`SevenZip.openInArchive(...)` 后无 `close()`/`use`/`try-finally`） | N/A | Unify | `:core_storage_component`（统一资源管理：关闭 archive/stream，并在取消时回收） | High | Small | P1 | 字幕下载/解压是用户高频路径；FD 泄漏可能导致“Too many open files”并影响全局 IO |
| REPO_SEVEN_ZIP-F003 | BugRisk | `ISequentialOutStream` 的写入语义可疑：每次 `write()` 都新建 `FileOutputStream` 且不追加，存在文件被覆盖/性能差的风险 | `core_storage_component/src/main/java/com/xyoye/common_component/utils/seven_zip/SequentialOutStream.kt` + `SequentialOutStream#write`（`FileOutputStream(outFile).use { write(...) }`，未 append） | N/A | Unify | `:core_storage_component`（按“流式输出”语义实现：持有同一输出流并追加写入，或改用更合适的 out stream 实现） | High | Medium | P1 | 若上游库多次回调 `write()`（常见），可能导致解压文件损坏；需要准备样例压缩包做回归验证 |
| REPO_SEVEN_ZIP-F004 | PerformanceRisk | `suspend` 解压函数内部执行同步阻塞式解压，且未显式切到 IO 线程；调用方若在主线程调用会卡 UI | `core_storage_component/src/main/java/com/xyoye/common_component/utils/seven_zip/SevenZipUtils.kt` + `SevenZipUtils#extractFile`（`suspendCancellableCoroutine { inArchive.extract(...) }` 未 `withContext(Dispatchers.IO)`） | N/A | Unify | `:core_storage_component`（统一通过 `Dispatchers.IO` 执行阻塞解压，并补齐取消/超时策略） | Medium | Medium | P2 | 需要梳理解压调用链是否已有 `Dispatchers.IO` 保障；否则卡顿风险会出现在字幕下载/解析流程 |
| REPO_SEVEN_ZIP-F005 | SecurityRisk | 解压缺少“体积/数量/路径”防护策略，可能被压缩炸弹/超大文件拖垮存储与性能 | 调用链：`core_storage_component/src/main/java/com/xyoye/common_component/utils/subtitle/SubtitleUtils.kt` + `SubtitleUtils#saveAndUnzipFile` → `SevenZipUtils#extractFile`；输出：`ArchiveExtractCallback#getStream`（按 `PropID.PATH` 生成文件名） | N/A | Unify | `:core_storage_component`（增加限额与校验：最大文件数/最大总解压体积/失败回收；并更严格清洗路径字符） | Medium | Medium | P2 | 需权衡：字幕压缩包通常不大，但来源可能来自网络；限额策略应可配置并有明确错误提示 |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| REPO_SEVEN_ZIP-T001 | REPO_SEVEN_ZIP-F001 | 为 SevenZip AAR 增加可追溯元信息（来源/版本/License/校验和/更新流程） | 新增 `repository/seven_zip/README.md`（中文）；在 README 中引用/关联 `user_component/src/main/assets/license/7-Zip-JBinding.txt`，并记录 AAR SHA256（当前：`def3f43296b21e148b8bcc4bb7cf40c99352488c73ded7e2faa258fbabf17caa`）、支持 ABI、升级步骤 | 1) README 明确：上游项目/下载地址、版本号、License、AAR SHA256、ABI/最低要求、更新步骤；2) 任意人可按文档复现升级；3) 不影响现有依赖解析 | Medium | Small | P1 | AI（Codex） | Done |
| REPO_SEVEN_ZIP-T002 | REPO_SEVEN_ZIP-F002,REPO_SEVEN_ZIP-F004 | 修复资源释放与线程/取消策略：解压过程在 IO 线程执行，且无论成功/失败/取消都能回收 | 重构 `core_storage_component/.../seven_zip/SevenZipUtils.kt`：使用 `withContext(Dispatchers.IO)` 包裹阻塞解压；对 `RandomAccessFile`、`RandomAccessFileInStream`、`IInArchive` 做 `try/finally` 关闭；在 `suspendCancellableCoroutine` 中 `invokeOnCancellation { ... }` 回收资源并中断 | 1) 压缩包不存在/解压失败/协程取消时不泄漏文件句柄；2) 主线程调用不会发生长时间卡顿（至少能明确切线程）；3) 失败时返回值与异常口径一致并有日志 | High | Medium | P1 | AI（Codex） | Done |
| REPO_SEVEN_ZIP-T003 | REPO_SEVEN_ZIP-F003 | 修复 `ISequentialOutStream` 实现：按“流式追加写入”语义输出文件，避免覆盖与性能劣化 | 重构 `core_storage_component/.../seven_zip/SequentialOutStream.kt`：在同一条流上追加写入（可通过 `FileOutputStream(outFile, true)` 或缓存输出流到对象生命周期）；同时补齐“写入完成/失败”后的关闭策略（必要时配合 `ArchiveExtractCallback` 提供 close 钩子） | 1) 解压后的文件内容完整可用；2) 大文件解压无明显性能退化；3) 失败时能回收临时文件/输出流 | High | Medium | P1 | AI（Codex） | Done |
| REPO_SEVEN_ZIP-T004 | REPO_SEVEN_ZIP-F003,REPO_SEVEN_ZIP-F005 | 增加可复现的回归用例：覆盖多格式/多文件/大文件/异常压缩包的解压行为 | 在 `core_storage_component/src/androidTest/...` 增加 instrumentation 用例（更适配 native so）；准备最小化样例压缩包（可放 `core_storage_component/src/androidTest/assets/` 或 `document/code_quality_audit/fixtures/` 并在 README 说明来源/生成方式） | 1) 覆盖：zip/7z（至少 2 种），以及“多次 write”场景；2) 验证：输出文件 hash/大小正确；3) 验证：异常压缩包/取消时资源回收 | Medium | Large | P3 | 待分配（QA/Storage） | Draft |

## 5) 风险与回归关注点

- 行为回退风险（哪些场景最敏感）
  - 字幕下载后自动解压链路：`SubtitleUtils#saveAndUnzipFile` 的返回值影响后续字幕发现/解析；任何“返回 null / 输出路径变化 / 解压目录命名变化”都可能引发字幕不可用。
  - 不同压缩格式与编码：zip/7z/rar 可能携带不同文件名编码与路径分隔符；需确认路径清洗策略不会误伤合法文件名。
- 回归成本（需要的账号/媒体文件/设备）
  - 建议准备最小样例：包含 1) 小文件多次写入；2) 多文件；3) 带子目录/奇怪路径的条目；4) 较大文件（验证性能）。
  - instrumentation 用例需要设备/模拟器（native so），并确保覆盖常见 ABI（至少 arm64-v8a）。

## 6) 备注（历史背景/待确认点）

- 当前仓库内已存在 7-Zip-JBinding 的 License 线索（见 `user_component/src/main/assets/license/7-Zip-JBinding.txt`），但 wrapper 目录缺少“该 AAR 与该 License/版本的对应关系、校验和、更新方式”说明；建议优先补齐元信息后再推进解压实现的优化与安全加固。
