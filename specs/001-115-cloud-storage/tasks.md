---
description: "Task list for 115 Cloud 存储库在线播放"
---

# Tasks: 115 Cloud 存储库在线播放

**Input**: 设计文档来自 `specs/001-115-cloud-storage/`（`plan.md`、`spec.md`、`research.md`、`data-model.md`、`contracts/`、`quickstart.md`）  
**Tests**: 建议新增 JVM 单测覆盖稳定逻辑（m115 加解密、字段映射、脱敏工具）；端到端以 `quickstart.md` 的手动用例为准。  
**组织方式**: 任务按“共享基础 → 阻塞性能力 → 用户故事闭环 → 交叉关注/门禁”拆分，确保可阶段性交付与验收。

## Checklist 格式（强制）

每条任务必须严格使用：

```text
- [ ] TaskID [P] [US1] 在 path/to/file 做某事
```

- `[P]`：可并行（不同文件/无未完成依赖）
- `[US1]`：用户故事任务必须标注；Setup/Foundational/Polish 阶段不标注
- 描述必须包含**明确文件路径**（允许 `A.kt + B.xml` 这种多路径写法）

---

## Phase 1: Setup（共享基础配置）

**Purpose**: 为 115 Cloud 接入准备“枚举资源/日志脱敏/基础常量”等通用能力（后续所有任务都依赖）

- [ ] T001 [P] 在 `core_network_component/src/main/java/com/xyoye/common_component/network/config/Api.kt` 增加 115 Cloud baseUrl 常量（`QRCODE_API/MY/WEBAPI/PROAPI/PASSPORTAPI`）
- [ ] T002 [P] 在 `data_component/src/main/java/com/xyoye/data_component/enums/MediaType.kt` 新增 `CLOUD_115_STORAGE`（value 建议 `cloud_115_storage`）并添加图标 `data_component/src/main/res/drawable/ic_cloud_115_storage.xml`（同时补齐 `fromValue` 映射）
- [ ] T003 [P] 在 `data_component/src/main/java/com/xyoye/data_component/entity/MediaLibraryEntity.kt` 增加 `MediaType.CLOUD_115_STORAGE` 的 `disPlayDescribe` 显示（从 `url=115cloud://uid/<userId>` 提取并遮罩展示，不涉及 cookie）
- [ ] T004 [P] 在 `core_network_component/src/main/java/com/xyoye/common_component/network/helper/LoggerInterceptor.kt` 增强脱敏：对 `Cookie` 头（UID/CID/SEID/KID）与表单 `data=` 等敏感字段做遮罩，确保日志/Toast 不输出完整敏感信息（FR-012/宪章 P4）

---

## Phase 2: Foundational（阻塞性前置能力）

**Purpose**: 115 Cloud 的 API/鉴权/加解密/授权态持久化等基础设施；在完成前不应开始任何 UI/浏览/播放开发

- [ ] T005 [P] 新增 115 Cloud API Moshi 模型 `data_component/src/main/java/com/xyoye/data_component/data/cloud115/Cloud115Models.kt`（按 `contracts/115-cloud-openapi.yaml` 覆盖：QRCodeToken/Status/Login、FileList/FileInfo、FileStat(paths)、DownloadResp 等）
- [ ] T006 [P] 新增 Retrofit Service `core_network_component/src/main/java/com/xyoye/common_component/network/service/Cloud115Service.kt` 并在 `core_network_component/src/main/java/com/xyoye/common_component/network/Retrofit.kt` 注册（支持多域名：qrcodeapi/passportapi/my/webapi/proapi）
- [ ] T007 [P] 新增 115 Cloud Header/脱敏工具 `core_storage_component/src/main/java/com/xyoye/common_component/storage/cloud115/net/Cloud115Headers.kt`（UA 常量、Cookie 拼装、cookie 脱敏工具）
- [ ] T008 [P] 新增 m115 加解密实现 `core_storage_component/src/main/java/com/xyoye/common_component/storage/cloud115/crypto/M115Crypto.kt`（算法参考 MIT 实现；禁止引入 AGPL 代码）
- [ ] T009 [P] 新增 JVM 单测 `core_storage_component/src/test/java/.../M115CryptoTest.kt`：覆盖 encode/decode 循环一致性（多轮随机 padding）、异常输入处理
- [ ] T010 [P] 新增授权态持久化 `core_storage_component/src/main/java/com/xyoye/common_component/storage/cloud115/auth/Cloud115AuthStore.kt`（按 storageKey 隔离；读写 cookie/userId/userName/avatar/updatedAt；清理）
- [ ] T011 [P] 新增可识别异常 `core_storage_component/src/main/java/com/xyoye/common_component/storage/cloud115/auth/Cloud115AuthExceptions.kt`（`Cloud115ReAuthRequiredException`/`Cloud115NotConfiguredException` 实现 `PassThroughException`，错误文案不含敏感信息）
- [ ] T012 实现 115 Cloud 仓库层 `core_storage_component/src/main/java/com/xyoye/common_component/network/repository/Cloud115Repository.kt`：封装二维码会话、轮询状态、登录换 cookie、cookie 校验、列表/搜索/stat、downloadUrl（m115 加解密），并统一日志脱敏
- [ ] T013 [P] 新增父路径缓存 `core_storage_component/src/main/java/com/xyoye/common_component/storage/cloud115/path/Cloud115FolderInfoCache.kt`（调用 `category/get` 缓存面包屑 id 链路，用于搜索结果路径构造）
- [ ] T014 [P] 新增直链缓存 `core_storage_component/src/main/java/com/xyoye/common_component/storage/cloud115/play/Cloud115DownUrlCache.kt`（短 TTL；支持强刷一次；必要时允许回退旧值）

**Checkpoint**: Foundation ready（可开始 US1 的 UI/浏览/播放并行开发）

---

## Phase 3: User Story 1 - 挂载 115 Cloud 并播放视频 (Priority: P1) 🎯

**Goal**: 用户扫码授权后可浏览目录并播放视频；刷新/排序/搜索一致；异常可恢复；移除可清理

### 3.1 Storage 实现（浏览/分页/搜索/播放）

- [ ] T015 [US1] 在 `core_storage_component/src/main/java/com/xyoye/common_component/storage/file/impl/Cloud115StorageFile.kt` 实现 StorageFile 适配（filePath 采用 ID 链路；fileUrl 使用 `115cloud://file/<id>`；区分目录/文件；暴露 pickcode/size/time 等）
- [ ] T016 [US1] 在 `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/Cloud115Storage.kt` 实现 `AbstractStorage + PagedStorage + AuthStorage`：getRootFile/openDirectory/loadMore/supportSearch/search/createPlayUrl/getNetworkHeaders/openFile
- [ ] T017 [US1] 在 `core_storage_component/src/main/java/com/xyoye/common_component/storage/StorageFactory.kt` 注册 `MediaType.CLOUD_115_STORAGE -> Cloud115Storage(library)`

### 3.2 StoragePlus UI（扫码授权/保存/去重/更新/移除）

- [ ] T018 [US1] 新增扫码弹窗 `storage_component/src/main/java/com/xyoye/storage_component/ui/dialog/Cloud115LoginDialog.kt` + `storage_component/src/main/res/layout/dialog_cloud115_login.xml`（二维码展示、状态轮询、取消/刷新/重试；TV 焦点可达）
- [ ] T019 [US1] 新增编辑弹窗 `storage_component/src/main/java/com/xyoye/storage_component/ui/dialog/Cloud115StorageEditDialog.kt` + `storage_component/src/main/res/layout/dialog_cloud115_storage.xml`：包含展示名、授权状态、进入扫码、清除授权/移除入口（对齐现有存储源交互）
- [ ] T020 [US1] 在 `storage_component/src/main/java/com/xyoye/storage_component/ui/activities/storage_plus/StoragePlusActivity.kt` 增加 `MediaType.CLOUD_115_STORAGE` 的弹窗分发（新增/编辑）
- [ ] T021 [US1] 在 `storage_component/src/main/java/com/xyoye/storage_component/ui/activities/storage_plus/StoragePlusViewModel.kt` 为 115 Cloud 增加“按 userId 构造唯一 url + 去重校验 + 保存”逻辑（同类型禁止重复添加，满足 FR-009）

### 3.3 文件浏览页登录引导与异常恢复

- [ ] T022 [US1] 在 `storage_component/src/main/java/com/xyoye/storage_component/ui/fragment/storage_file/StorageFileFragmentViewModel.kt` 处理 `Cloud115ReAuthRequiredException`：提示“授权失效需要重新扫码”，并触发 loginRequired 流程（不泄露 cookie）
- [ ] T023 [US1] 在 `storage_component/src/main/java/com/xyoye/storage_component/ui/fragment/storage_file/StorageFileFragment.kt` 的 `showLoginDialog()` 增加 `MediaType.CLOUD_115_STORAGE`：导航到 `RouteTable.Stream.StoragePlus`（携带 `mediaType` + `editData`），保存成功后触发刷新

### 3.4 清理链路

- [ ] T024 [US1] 在 `local_component/src/main/java/com/xyoye/local_component/ui/fragment/media/MediaViewModel.kt` 删除媒体库时清理 115 Cloud 授权数据（调用 `Cloud115AuthStore.clear(Cloud115AuthStore.storageKey(data))`，满足 FR-008/FR-012）

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: 稳定性、可观测性与发布前门禁

- [ ] T025 为关键链路补充日志与错误上下文（且必须脱敏 cookie）：`Cloud115Repository.kt` + `Cloud115Storage.kt` + `Cloud115StorageEditDialog.kt`（使用 `LogFacade`/`ErrorReportHelper`，并统一走 `Cloud115Headers` 脱敏工具）
- [ ] T026 在仓库根目录运行 `./gradlew verifyModuleDependencies` 并确认输出末尾为 `BUILD SUCCESSFUL`（`./gradlew`）
- [ ] T027 在仓库根目录运行 `./gradlew lint` 并确认输出末尾为 `BUILD SUCCESSFUL`（`./gradlew`）
- [ ] T028 在仓库根目录运行 `./gradlew assembleDebug` 并确认输出末尾为 `BUILD SUCCESSFUL`（`./gradlew`）
- [ ] T029 最终对照 `specs/001-115-cloud-storage/quickstart.md` 走通 P1 用例并在 `specs/001-115-cloud-storage/checklists/acceptance.md` 留存验收记录（文字/截图/关键日志均可）

