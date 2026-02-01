# Implementation Plan: 115 Cloud 存储库在线播放

**Branch**: `001-115-cloud-storage` | **Date**: 2026-02-01 | **Spec**: `specs/001-115-cloud-storage/spec.md`<br>
**Input**: Feature specification from `specs/001-115-cloud-storage/spec.md`

**Note**: 本模板为实现计划模板；执行前先阅读项目宪章：`.specify/memory/constitution.md`。

## Summary

在应用现有“存储源”体系中新增“115 Cloud”类型：用户通过应用内二维码扫码授权完成绑定，不要求输入账号/密码；绑定后可从云盘根目录浏览目录/文件，对受支持的视频文件一键进入播放器开始播放。刷新/排序/搜索等定位能力与现有存储源保持一致（与“百度网盘”“115 Open”的浏览/播放体验对齐），避免引入独立的专用操作路径（除扫码授权页外）。授权态按账号隔离持久化并支持“重启后免重复授权”；授权失效时阻断访问并提示重新授权。播放链路复用现有 `LocalProxy/HttpPlayServer` 的代理策略（对 mpv/VLC 等高风险播放器优先启用）降低 115 风控与 Range 行为差异导致的失败概率，保证多播放内核一致可播。移除存储源时清理对应授权与缓存数据，UI 与日志严格脱敏，异常场景提供可恢复入口（重试/重新授权/返回）。

## Technical Context

**Language/Version**: Kotlin 1.9.25（JVM target 1.8），Android Gradle Plugin 8.7.2<br>
**Primary Dependencies**: AndroidX、Kotlin Coroutines、Retrofit+OkHttp、Moshi、Room、MMKV、Media3、NanoHTTPD（本地代理）、ARouter<br>
**Storage**: Room（`media_library` 等表）+ MMKV（偏好/授权态隔离存储）+ 本地缓存文件（字幕/弹幕/临时清单等）<br>
**Testing**: `./gradlew testDebugUnitTest`（JVM 单测）、`./gradlew connectedDebugAndroidTest`（设备/模拟器）<br>
**Target Platform**: Android（手机 + TV；需考虑遥控器/焦点）<br>
**Project Type**: Android 多模块（MVVM）；`:storage_component` 提供 UI，`:core_storage_component` 提供存储实现<br>
**Performance Goals**: 对齐 `spec.md` 成功指标：添加存储源到可浏览 ≤2min（SC-001）；打开已添加存储源首屏列表 ≤3s（SC-002）；点击视频到开始播放 ≤5s（SC-003）<br>
**External Integration**: 115 Cloud（扫码授权 + 文件列表/搜索 + 播放直链）<br>
**Constraints**: 遵守模块依赖治理（见宪章 P1 + `document/architecture/module_dependency_governance.md`）；授权信息属于敏感数据（宪章 P4/FR-012）；入口与交互必须与现有存储源保持一致（FR-010）；TV 端需考虑焦点与遥控器可达性（宪章 P7）<br>
**Resolved Decisions**（详见 `research.md`）：
- 授权：二维码扫码 → 轮询状态 → 换取 Cookie（UID/CID/SEID/KID）；cookie 失效后重新扫码，不做自动刷新
- 校验：优先使用 `my.115.com/?ct=guide&ac=status` 做 cookie 有效性检查，避免影响用户其他设备登录态
- 列表：`GET https://webapi.115.com/files`（分页 `offset/limit`；排序 `o=file_name|file_size|user_ptime|file_type` + `asc=0/1`）
- 搜索：`GET https://webapi.115.com/files/search`（`search_value` + `cid=<当前目录>` + `type=4` 视频 + `offset/limit`）
- 播放：`POST https://proapi.115.com/android/2.0/ufile/download`（`data=<m115加密>` + cookie + UA）；直链短 TTL 缓存（建议 2 分钟）+ Range 不支持/失败强刷一次

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- 模块职责与归属清晰；避免把不相关能力硬拼到同一模块
- 不新增 feature ↔ feature 的 Gradle 依赖（跨业务交互走 `:core_contract_component` 契约/路由/Service）
- 新增/调整共享类型优先下沉到 `:core_contract_component` 或 `:data_component`
- 不引入“为了拿 transitive 类型”的依赖；默认使用 `implementation(project(...))`
- 若引入 `api(project(...))`，必须在 `build.gradle.kts` 中写明对外 API 暴露理由，并评估影响面
- 若涉及依赖变更：`./gradlew verifyModuleDependencies`（建议 `./gradlew verifyArchitectureGovernance`）通过，且输出末尾为 `BUILD SUCCESSFUL`
- 若更新依赖治理规则/白名单：同步更新 `document/architecture/module_dependency_governance.md` + `buildSrc` + 快照（如需要）
- 若引入授权/密钥：不得硬编码，必须通过 `local.properties`/Gradle properties/CI Secrets 注入（宪章 P4）
- Phase 1 设计完成后复检：PASS（规划内不新增 feature ↔ feature 依赖；敏感信息不落库且统一脱敏；以 contracts/docs 固化调用契约）

## Project Structure

### Documentation (this feature)

```text
specs/001-115-cloud-storage/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output (if needed)
├── quickstart.md        # Phase 1 output
├── 115-cloud-openapi.md # 已整理的 115 Cloud API 资料（实现参考）
├── contracts/           # Phase 1 output (if needed)
└── tasks.md             # Phase 2 output
```

### Source Code (repository root)

```text
app/
core_*_component/
*_component/
buildSrc/
document/
```

**结构说明（必须）**：

- 涉及模块（预计改动点）：
  - `:storage_component`：新增“115 Cloud”存储源的新增/编辑 UI（扫码二维码、状态轮询、取消/重试/刷新、脱敏展示、更新/移除）；入口与百度网盘/115 Open 保持一致
  - `:core_storage_component`：新增 `Cloud115Storage` 与 `Cloud115StorageFile`；新增 115 Cloud Repository + 授权态管理（按 storageKey 隔离、串行刷新/换票、原子持久化）；复用 `LocalProxy/HttpPlayServer` 处理 Range/风控
  - `:core_network_component`：新增 115 Cloud Retrofit Service（webapi/qrcode 等接口声明，不承载业务逻辑）
  - `:data_component`：新增 `MediaType.CLOUD_115_STORAGE`（名称待定）、图标资源；新增 115 Cloud API 模型（data class）
  - `:core_contract_component`：仅当需要新增跨模块契约类型/接口时使用（优先保持现有 Storage 契约不变）
- 依赖关系：不新增 feature ↔ feature Gradle 依赖；仅在既有依赖链条内扩展（`storage_component -> core_storage_component -> core_network_component/data_component`）。

## Complexity Tracking

无（当前设计不引入宪章约束的破例项）。
