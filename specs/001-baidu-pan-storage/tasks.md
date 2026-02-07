---
description: "Task list for 百度网盘存储库在线播放"
---

# Tasks: 百度网盘存储库在线播放

**Input**: 设计文档来自 `specs/001-baidu-pan-storage/`（`plan.md`、`spec.md`、`research.md`、`data-model.md`、`contracts/`、`quickstart.md`）  
**Tests**: 本次不新增自动化测试任务（`spec.md` 未要求）；以 `specs/001-baidu-pan-storage/quickstart.md` 的手动用例为准。  
**组织方式**: 任务按用户故事拆分（P1 → P2 → P3），确保每个故事都可独立验收；同时避免“过于松散”的碎片化任务。

## Checklist 格式（强制）

每条任务必须严格使用：

```text
- [ ] TaskID [P] [US#] 在 path/to/file 做某事
```

- `[P]`：可并行（不同文件/无未完成依赖）
- `[US1]/[US2]/[US3]`：仅用户故事阶段必须标注；Setup/Foundational/Polish 阶段不标注
- 描述必须包含**明确文件路径**

---

## Phase 1: Setup（共享基础配置）

**Purpose**: 为百度网盘 OpenAPI 接入准备“密钥注入/常量/枚举资源”等基础能力（后续所有故事都依赖）

- [X] T001 在 `core_system_component/build.gradle.kts` 增加 `BAIDU_PAN_CLIENT_ID/BAIDU_PAN_CLIENT_SECRET` 的 ENV/Gradle/local.properties 注入，并更新 `local.properties.template`
- [X] T002 [P] 新增百度网盘密钥读取封装 `core_system_component/src/main/java/com/xyoye/common_component/config/BaiduPanOpenApiConfig.kt`（统一读取 `BuildConfig.BAIDU_PAN_CLIENT_ID/SECRET`，提供 `isConfigured()`）
- [X] T003 [P] 在 `core_network_component/src/main/java/com/xyoye/common_component/network/config/Api.kt` 增加 `BAIDU_OAUTH`/`BAIDU_PAN` baseUrl 常量
- [X] T004 [P] 在 `data_component/src/main/java/com/xyoye/data_component/enums/MediaType.kt` 新增 `BAIDU_PAN_STORAGE`（含 `fromValue` 映射）并添加图标 `data_component/src/main/res/drawable/ic_baidu_pan_storage.xml`

---

## Phase 2: Foundational（阻塞性前置能力）

**Purpose**: 百度网盘 API/鉴权/数据模型的通用基础设施；在完成前不应开始任何用户故事 UI/播放链路开发

- [X] T005 [P] 新增 OAuth 模型到 `data_component/src/main/java/com/xyoye/data_component/data/baidupan/oauth/BaiduPanOAuthModels.kt`（device_code/token/oauth error）
- [X] T006 [P] 新增 XPan 模型到 `data_component/src/main/java/com/xyoye/data_component/data/baidupan/xpan/BaiduPanXpanModels.kt`（uinfo/list/search/filemetas + errno/errmsg）
- [X] T007 [P] 新增 Retrofit Service `core_network_component/src/main/java/com/xyoye/common_component/network/service/BaiduPanService.kt` 并在 `core_network_component/src/main/java/com/xyoye/common_component/network/RetrofitManager.kt` 注册 `baiduPanService`
- [X] T008 实现 Baidu Pan 仓库层 `core_storage_component/src/main/java/com/xyoye/common_component/network/repository/BaiduPanRepository.kt`（封装 baseUrl、参数拼装、errno/OAuth error 映射与重试语义）
- [X] T009 [P] 实现授权态持久化 `core_storage_component/src/main/java/com/xyoye/common_component/storage/baidupan/auth/BaiduPanAuthStore.kt`（storageKey 规则、AuthState 读写/清理）
- [X] T010 实现 token 管理器 `core_storage_component/src/main/java/com/xyoye/common_component/storage/baidupan/auth/BaiduPanTokenManager.kt`（互斥刷新 + refresh_token 旋转 + 原子写入；刷新失败需清理并抛出“需要重新授权”的可识别异常）

**Checkpoint**: Foundation ready（可开始 US1/US2/US3 的并行开发）

---

## Phase 3: User Story 1 - 挂载百度网盘并播放视频 (Priority: P1) 🎯 MVP

**Goal**: 新增“百度网盘”存储源，通过二维码扫码授权后可浏览目录并一键播放视频（兼容 Media3/mpv/VLC）

**Independent Test**: 使用包含至少 1 个视频文件的百度网盘账号：新增存储源 → 扫码授权 → 浏览到视频 → 点击播放，验证能进入播放器并成功开始播放（参考 `specs/001-baidu-pan-storage/quickstart.md`）

- [X] T011 [P] [US1] 新增 PanFile 适配 `core_storage_component/src/main/java/com/xyoye/common_component/storage/file/impl/BaiduPanStorageFile.kt`（`path=="/"` 根目录判定、`category==1` 视频判定 + 扩展名回退、payload 挂载）
- [X] T012 [P] [US1] 新增 dlink 缓存 `core_storage_component/src/main/java/com/xyoye/common_component/storage/baidupan/play/BaiduPanDlinkCache.kt`（按 `fsId` 缓存 `dlink/expiry/contentLength`，支持强制刷新回退旧值）
- [X] T013 [US1] 实现 `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/BaiduPanStorage.kt`（extends `AbstractStorage`）：`getRootFile/openDirectory/pathFile/historyFile/openFile/createPlayUrl/getNetworkHeaders`，并在 mpv/VLC 分支用 `LocalProxy.wrapIfNeeded(...)` 注入 `User-Agent: pan.baidu.com`（依赖 T008/T010/T011/T012）
- [X] T014 [US1] 在 `core_storage_component/src/main/java/com/xyoye/common_component/storage/StorageFactory.kt` 注册 `MediaType.BAIDU_PAN_STORAGE -> BaiduPanStorage`
- [X] T015 [P] [US1] 新增扫码授权对话框 `storage_component/src/main/java/com/xyoye/storage_component/ui/dialog/BaiduPanLoginDialog.kt` + `storage_component/src/main/res/layout/dialog_baidu_pan_login.xml`（复用 `storage_component/src/main/java/com/xyoye/common_component/utils/QrCodeHelper.kt`，轮询 `/oauth/2.0/token` 状态并处理过期/取消/拒绝）
- [X] T016 [US1] 新增存储源编辑对话框 `storage_component/src/main/java/com/xyoye/storage_component/ui/dialog/BaiduPanStorageEditDialog.kt` + `storage_component/src/main/res/layout/dialog_baidu_pan_storage.xml`（添加/编辑、展示名可编辑、集成 `storage_component/src/main/java/com/xyoye/storage_component/ui/dialog/PlayerTypeOverrideBinder.kt`、触发扫码授权并写入 `MediaLibraryEntity` 的 `url=baidupan://uk/<uk>`）
- [X] T017 [US1] 在 `storage_component/src/main/java/com/xyoye/storage_component/ui/activities/storage_plus/StoragePlusActivity.kt` 增加 `MediaType.BAIDU_PAN_STORAGE -> BaiduPanStorageEditDialog`
- [X] T018 [P] [US1] 在 `local_component/src/main/java/com/xyoye/local_component/ui/fragment/media/MediaFragment.kt` 的 `launchMediaStorage` 分支加入 `MediaType.BAIDU_PAN_STORAGE` 打开 `RouteTable.Stream.StorageFile`
- [X] T019 [P] [US1] 在 `data_component/src/main/java/com/xyoye/data_component/entity/MediaLibraryEntity.kt` 的 `disPlayDescribe` 为 `BAIDU_PAN_STORAGE` 提供更友好描述，避免列表只展示 `baidupan://...`
- [X] T020 [US1] 在 `storage_component/src/main/java/com/xyoye/storage_component/ui/activities/storage_plus/StoragePlusViewModel.kt` 增加百度网盘保存前校验：未生成 `baidupan://uk/<uk>` 时拒绝保存并提示
- [X] T021 [US1] 在 `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/BaiduPanStorage.kt` 对不可播放文件给出明确失败（抛出带提示的异常或返回 null 并确保提示可见），与 `storage_component/src/main/java/com/xyoye/storage_component/ui/activities/storage_file/StorageFileViewModel.kt` 的错误提示链路对齐
- [X] T022 [US1] 对照实现结果校对并更新 `specs/001-baidu-pan-storage/quickstart.md`（仅在实现与文档有偏差时修改）
- [ ] T023 [US1] 手动走通 P1 用例并记录关键结果（参考 `specs/001-baidu-pan-storage/quickstart.md`）
- [X] T024 [US1] 在仓库根目录运行 `./gradlew assembleDebug` 并确认输出末尾为 `BUILD SUCCESSFUL`（`./gradlew`）

---

## Phase 4: User Story 2 - 在网盘中快速定位内容 (Priority: P2)

**Goal**: 面对大目录时可刷新/排序/分页加载/搜索，以更快定位视频

**Independent Test**: 使用包含多级目录和多文件类型的网盘：验证能正常浏览层级、刷新列表、排序、分页加载和搜索，且返回结果稳定（参考 `specs/001-baidu-pan-storage/quickstart.md`）

- [X] T025 [P] [US2] 在 `core_storage_component/src/main/java/com/xyoye/common_component/storage/PagedStorage.kt` 增加默认方法 `shouldShowPagingItem(directory: StorageFile?): Boolean = true`（为非 Bilibili 的分页 UI 做准备）
- [X] T026 [US2] 在 `storage_component/src/main/java/com/xyoye/storage_component/ui/fragment/storage_file/StorageFileFragmentViewModel.kt` 的 `buildDisplayItems` 改为基于 `PagedStorage.shouldShowPagingItem(...)` 展示分页条（移除仅 Bilibili 的限定）
- [X] T027 [US2] 在 `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/BilibiliStorage.kt` 覆盖 `shouldShowPagingItem(...)`，保持仅在历史/关注目录显示分页条
- [X] T028 [US2] 在 `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/BaiduPanStorage.kt` 实现 `PagedStorage`：按当前 `dir` 维护 `start/limit/hasMore/state`，并在 `openDirectory(refresh=true)` 时重置分页
- [X] T029 [US2] 在 `storage_component/src/main/java/com/xyoye/storage_component/ui/fragment/storage_file/StorageFileFragmentViewModel.kt` 的 `loadMore` 合并后，对 `MediaType.BAIDU_PAN_STORAGE` 重新按 `StorageSortOption.comparator()` 排序（保证排序/目录优先在分页场景下生效）
- [X] T030 [US2] 在 `core_storage_component/src/main/java/com/xyoye/common_component/network/repository/BaiduPanRepository.kt` 增加 `search` 封装，并在 `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/BaiduPanStorage.kt` 实现 `supportSearch/search(keyword)`（method=search，处理 recursion/分类过滤/错误码）
- [ ] T031 [US2] 手动验证 P2 用例：刷新/排序/分页加载/搜索（参考 `specs/001-baidu-pan-storage/quickstart.md`）

---

## Phase 5: User Story 3 - 存储源与授权状态可管理 (Priority: P3)

**Goal**: 可查看授权状态并支持断开/重连；授权失效时引导恢复而不是“未知错误”

**Independent Test**: 模拟授权失效/撤销：验证能检测并引导重新授权；验证移除存储源后授权信息被清理（参考 `specs/001-baidu-pan-storage/quickstart.md`）

- [X] T032 [P] [US3] 新增通用接口 `core_storage_component/src/main/java/com/xyoye/common_component/storage/AuthStorage.kt`（`isConnected()`/`requiresLogin(directory)`/`loginActionText(directory)`）
- [X] T033 [P] [US3] 让 `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/BilibiliStorage.kt` 实现 `AuthStorage`（用现有 `isConnected()` + `isBilibiliPagedDirectoryPath(...)` 实现 `requiresLogin`）
- [X] T034 [US3] 让 `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/BaiduPanStorage.kt` 实现 `AuthStorage`（基于 `core_storage_component/src/main/java/com/xyoye/common_component/storage/baidupan/auth/BaiduPanAuthStore.kt` 判定连接；失效后 `requiresLogin=true`）
- [X] T035 [US3] 在 `storage_component/src/main/java/com/xyoye/storage_component/ui/fragment/storage_file/StorageFileFragmentViewModel.kt` 用 `AuthStorage` 替换 bilibili 特判，并在捕获到“需要重新授权”异常时触发通用 loginRequired LiveData
- [X] T036 [US3] 在 `storage_component/src/main/java/com/xyoye/storage_component/ui/fragment/storage_file/StorageFileFragment.kt` 将 `bilibiliLoginRequiredLiveData` 改为通用 loginRequired 监听，并按 `mediaType` 分发到 `storage_component/src/main/java/com/xyoye/storage_component/ui/dialog/BilibiliLoginDialog.kt` 或 `storage_component/src/main/java/com/xyoye/storage_component/ui/dialog/BaiduPanLoginDialog.kt`
- [X] T037 [US3] 在 `storage_component/src/main/java/com/xyoye/storage_component/ui/fragment/storage_file/StorageFileAdapter.kt` 的空列表提示逻辑改为基于 `AuthStorage.requiresLogin/isConnected` 展示“扫码登录/授权”按钮
- [X] T038 [US3] 在 `local_component/src/main/java/com/xyoye/local_component/ui/fragment/media/MediaViewModel.kt` 删除媒体库时清理百度网盘授权数据（调用 `core_storage_component/src/main/java/com/xyoye/common_component/storage/baidupan/auth/BaiduPanAuthStore.kt`）
- [ ] T039 [US3] 在 `storage_component/src/main/java/com/xyoye/storage_component/ui/dialog/BaiduPanStorageEditDialog.kt` 增加“断开连接/清除授权”入口（清理 AuthState + 退出播放器 + 可选删除媒体库），并手动验证 P3 用例（参考 `specs/001-baidu-pan-storage/quickstart.md`）

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: 跨用户故事的稳定性、可观测性与发布前门禁

- [X] T040 [P] 为关键链路补充日志与错误上下文（优先在 `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/BaiduPanStorage.kt` 与 `core_storage_component/src/main/java/com/xyoye/common_component/network/repository/BaiduPanRepository.kt` 使用 `LogFacade`/`ErrorReportHelper`）
- [X] T041 在仓库根目录运行 `./gradlew verifyModuleDependencies` 并确认输出末尾为 `BUILD SUCCESSFUL`（`./gradlew`）
- [X] T042 在仓库根目录运行 `./gradlew lint` 并确认输出末尾为 `BUILD SUCCESSFUL`（`./gradlew`）
- [X] T043 在仓库根目录运行 `./gradlew assembleRelease` 并确认输出末尾为 `BUILD SUCCESSFUL`（`./gradlew`）
- [ ] T044 最终对照 `specs/001-baidu-pan-storage/quickstart.md` 走通 P1-P3 用例并在 `specs/001-baidu-pan-storage/checklists/` 留存验收记录（文字/截图均可）

---

## Dependencies & Execution Order

### User Story 依赖图（建议）

```text
Setup (Phase 1)
  ↓
Foundational (Phase 2)
  ↓
US1 (P1, MVP)  →  US2 (P2)  →  US3 (P3)
```

- US2/US3 在实现上依赖 US1 的“可用授权态 + 可浏览目录”闭环，但可以在 Foundation ready 后并行推进（不同人分工时）
- 依赖治理提醒：禁止通过新增 feature ↔ feature 依赖来打通；如需共享类型优先下沉到 `:data_component`/`core_*`（见 `document/architecture/module_dependency_governance.md`）

### Parallel Opportunities（示例）

- Setup：`T002/T003/T004` 可并行（在 `T001` 完成后）
- Foundation：`T005/T006/T007/T009` 可并行（Repository/TokenManager 依赖其结果）
- US1：`T011/T012/T015/T018/T019` 可并行（在 Foundation ready 后）
- US2：`T025` 与 `T030` 可并行（paging 与 search 互不阻塞）

---

## Parallel Example: User Story 1

```text
并行组 A（数据/存储侧）：
- T011 [US1] `core_storage_component/.../BaiduPanStorageFile.kt`
- T012 [US1] `core_storage_component/.../BaiduPanDlinkCache.kt`

并行组 B（UI 侧）：
- T015 [US1] `storage_component/.../BaiduPanLoginDialog.kt` + layout
- T018 [US1] `local_component/.../MediaFragment.kt`
```

---

## Parallel Example: User Story 2

```text
并行组 A（分页 UI 与能力准备）：
- T025 [US2] `core_storage_component/.../PagedStorage.kt`
- T026 [US2] `storage_component/.../StorageFileFragmentViewModel.kt`

并行组 B（搜索能力）：
- T030 [US2] `core_storage_component/.../BaiduPanRepository.kt` + `core_storage_component/.../BaiduPanStorage.kt`
```

---

## Parallel Example: User Story 3

```text
并行组 A（通用授权抽象）：
- T032 [US3] `core_storage_component/.../AuthStorage.kt`
- T033 [US3] `core_storage_component/.../BilibiliStorage.kt`

并行组 B（清理与 UI 引导）：
- T038 [US3] `local_component/.../MediaViewModel.kt`
- T036 [US3] `storage_component/.../StorageFileFragment.kt`
```

---

## Implementation Strategy

### MVP First（只做 US1）

1. Phase 1 → Phase 2 → Phase 3（US1）
2. 以 `specs/001-baidu-pan-storage/quickstart.md` 验收 P1
3. 构建门禁至少通过：`./gradlew assembleDebug`

### Incremental Delivery

1. US1：可挂载 + 可播放（MVP）
2. US2：大目录可用性（分页/搜索/排序稳定）
3. US3：授权失效可恢复 + 可清理/重连
