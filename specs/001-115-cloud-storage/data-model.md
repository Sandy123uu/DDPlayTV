# Phase 1：数据模型（115 Cloud 存储源）

本文档从 `spec.md` 的实体与需求出发，整理“需要持久化/需要在内存中流转”的核心数据结构、字段约束与状态机。实现期如需对照接口字段，可结合：

- `spec.md`：`spec.md`
- Phase 0 调研：`research.md`
- API 摘要：`115-cloud-openapi.md`
- OpenAPI 契约：`contracts/115-cloud-openapi.yaml`

## 1. 实体总览

| 实体 | 类型 | 归属层 | 用途 |
|---|---|---|---|
| 115 Cloud 存储源（Library） | 持久化（Room） | `:data_component` | 在“存储源列表”中展示/选择一个 115 账号入口；作为多账号隔离边界 |
| 115 Cloud 授权态（AuthState） | 持久化（MMKV/可迁移至 Room） | `:core_storage_component` | 存储 cookie、用户信息与最近校验时间；支撑重启免授权与失效处理 |
| 扫码授权会话（QRCodeSession） | 运行态（内存） | `:storage_component` | 展示二维码、轮询状态、处理取消/过期/失败分支 |
| 云盘文件项（FileItem） | 内存态（API 模型 + StorageFile 适配） | `:data_component` + `:core_storage_component` | 列表展示、目录导航、播放入口 |
| 目录路径缓存（FolderInfoCache，可选） | 内存态 | `:core_storage_component` | 用 `category/get` 缓存父路径链路，用于搜索结果构造稳定 `filePath` |
| 播放直链缓存（DownUrlCache） | 内存态（可选短期持久化） | `:core_storage_component` | 缓存直链与 UA；失败时可强制刷新并回退 |
| 播放请求（PlayAttempt） | 运行态（日志/可选埋点） | `:core_log_component` | 关键路径可观测：授权/加载列表/启动播放 成功与失败原因（脱敏） |

---

## 2. 115 Cloud 存储源（MediaLibraryEntity 扩展约定）

> `MediaLibraryEntity` 为 Room 实体，且对 `(url, media_type)` 有唯一索引；因此 115 Cloud 必须设计“每账号唯一 url”，以满足 `FR-009` 的去重与多账号隔离。

### 2.1 字段映射（建议）

| 字段 | 值 | 约束/说明 |
|---|---|---|
| `mediaType` | `CLOUD_115_STORAGE`（名称待定） | 需要新增枚举值与图标资源 |
| `displayName` | 默认 `user_name`（可编辑） | 用于列表展示；编辑时允许用户自定义 |
| `url` | `115cloud://uid/<user_id>` | **必须唯一**（同一 mediaType 下）；`user_id` 来自扫码登录响应（或用户信息接口） |
| `account/password/describe` | 不用于存 cookie | 避免“字段滥用”导致跨存储实现耦合与泄露风险（宪章 P4） |
| `playerTypeOverride` | 复用现有机制 | 支撑“媒体库覆盖播放器内核选择”，满足 `FR-014` 验收 |

### 2.2 派生键（storageKey）

沿用项目内偏好存储思路，避免依赖自增 `id`：

`storageKey = "${mediaType.value}:${url.trim().removeSuffix("/")}"`  

该 key 用于：

- MMKV namespacing（AuthState、偏好等）
- 授权流程互斥（同账号不并发换票/校验）
- 清理（移除存储源时精准清理该账号授权数据）

---

## 3. 115 Cloud 授权态（AuthState，持久化）

### 3.1 字段（建议最小集合）

| 字段 | 类型 | 必填 | 来源 | 说明 |
|---|---|---:|---|---|
| `cookie` | `String` | 是（授权后） | 扫码登录响应 | 至少包含 `UID/CID/SEID`，建议同时保存 `KID` |
| `userId` | `String` | 是（授权后） | 扫码登录响应 `user_id` | 用于 `MediaLibraryEntity.url` 与账号去重 |
| `userName` | `String` | 否 | 扫码登录响应 `user_name` | 默认展示名来源 |
| `avatarUrl` | `String` | 否 | 扫码登录响应 `face_*` | UI 头像（可选） |
| `updatedAtMs` | `Long` | 是 | 本地写入 | 最近一次授权/校验成功时间 |

### 3.2 不变式/校验规则

- `cookie` 存在时，`userId` 必须存在（否则无法构造唯一 URL/隔离 key）。
- **脱敏**：任何日志/错误提示不得输出完整 cookie；UI 仅允许遮罩展示（FR-012）。
- **失效处理**：cookie 不存在“官方刷新 token”语义；当校验或业务接口返回未授权时进入 `NeedReAuth`，要求重新扫码（FR-013）。

### 3.3 状态机（授权）

```text
NoAuth
  └─(开始扫码)→ Authorizing

Authorizing
  ├─(已扫码/等待确认)→ Authorizing
  ├─(确认成功，拿到 cookie + userId)→ Authorized
  ├─(取消/过期/失败)→ NoAuth

Authorized
  ├─(cookie 校验失败/接口未授权)→ NeedReAuth
  ├─(用户移除存储源)→ Cleared(NoAuth)
  └─(应用重启)→ Authorized（读取持久化 cookie；首次访问时做校验）

NeedReAuth
  ├─(用户重新扫码成功)→ Authorized
  └─(用户移除存储源)→ Cleared(NoAuth)
```

---

## 4. 扫码授权会话（QRCodeSession，运行态）

| 字段 | 类型 | 说明 |
|---|---|---|
| `uid/time/sign` | `String/Long/String` | token 接口返回；用于轮询状态 |
| `qrcodeContent` | `String` | 可用本地二维码库生成；也可直接取图片接口 |
| `status` | `Int` | 0 等待 / 1 已扫码 / 2 已确认 / -1 过期 / -2 取消（以实际字段为准） |
| `createdAtMs` | `Long` | 本地创建时间 |

关键 UI 行为（对齐 FR-002）：

- 取消：停止轮询、关闭页面，不写入 AuthState
- 刷新二维码：清理旧 session，重新请求 token + 重建轮询
- 重试：网络失败/接口失败后可再次发起轮询或重新获取 token

---

## 5. 云盘文件项（API 模型 → StorageFile）

### 5.1 列表/搜索返回的关键字段（`/files` & `/files/search`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `cid` | `String` | 对文件：父目录 ID；对目录：目录自身 ID |
| `fid` | `String` | 文件 ID；目录通常为空字符串 |
| `pid` | `String` | 目录的父目录 ID（文件通常为空） |
| `n` | `String` | 名称 |
| `s` | `Long` | 大小（字节） |
| `t` | `String` | 更新时间（字符串；对目录可能为时间戳字符串） |
| `pc` | `String` | pickcode（用于获取直链） |
| `ico` | `String` | 后缀/类型标识（可用于 UI 图标与兜底判断） |

### 5.2 目录/文件判定（建议）

- 若 `fid` 非空：视为文件，`id = fid`
- 若 `fid` 为空：视为目录，`id = cid`

> 该规则便于统一 `filePath` 的“ID 链路”拼接；实际以接口返回为准。

---

## 6. StorageFile 适配约定（建议）

### 6.1 `filePath/storagePath`（ID 链路，避免重命名导致定位失败）

- 根目录：`"/"`
- 目录/文件：`"<parentPath>/<id>"`（示例：`/12345/67890`）
  - `id` 对目录为 `cid`，对文件为 `fid`

### 6.2 `fileUrl()`（稳定唯一值）

建议：`115cloud://file/<id>`，用于 `uniqueKey` 与历史定位。

### 6.3 搜索结果的路径构造（可选增强但建议做）

搜索结果通常只有“父目录 ID”，不一定能直接得出根到父目录的完整链路。为保证：

- 播放历史可回放（`history.storagePath` 可解析）
- 搜索结果可稳定定位到所在目录（后续可扩展“打开所在目录”）

建议用 `category/get` 的 `paths` 构建面包屑，并按目录维度缓存：

`dirId -> breadcrumbIds`  

然后把文件 `filePath` 构造为：

`"/<cid1>/<cid2>/.../<dirId>/<fid>"`。

---

## 7. 播放直链缓存（DownUrlCache，内存态）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `String` | key（建议 fid；也可 pickcode） |
| `pickCode` | `String` | 直链接口参数（用于强刷） |
| `url` | `String` | 直链（解密后的 url） |
| `userAgent` | `String` | 直链访问需要的 UA |
| `fileSize` | `Long` | 用于 `LocalProxy/HttpPlayServer` 的 contentLength |
| `updatedAtMs` | `Long` | 最近一次获取时间 |

缓存策略建议：

- TTL：2 分钟（保守策略，降低直链失效概率）
- 失败强刷：直链失效、403、Range 不支持时强制刷新一次再失败则抛错并提示用户重试/重新授权

---

## 8. 目录信息缓存（FolderInfoCache，可选）

若要为“搜索结果/历史定位”提供稳定 `filePath`，可对目录调用：

- `GET https://webapi.115.com/category/get?cid=<dirId>`

并缓存：

- `dirId -> breadcrumbIds`（从根到当前目录的 id 列表）

---

## 9. 敏感信息与脱敏

必须脱敏的字段：

- `cookie`（UID/CID/SEID/KID…）
- 任何可能包含 token/cookie 的 Header

建议脱敏规则（可参考 Open115 的 token redact 思路）：

- 仅显示前 3~6 位 + 后 3~6 位，其余使用 `*` 替换
- 日志中避免输出完整请求 URL（若包含敏感 query），必要时拆分字段并脱敏后输出

