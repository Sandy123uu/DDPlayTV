# Phase 0：调研与关键决策（115 Cloud 存储源）

本文档用于把实现所需的关键事实、约束与技术决策一次讲清，并尽量标注“证据来源”（OpenList/社区实现/项目内既有实现），以便后续实现时可快速对照。

- Feature spec：`spec.md`
- 参考实现（外部）：OpenList 115 Cloud driver、SheltonZhu/115driver（MIT）等
- 参考实现（项目内）：`specs/001-baidu-pan-storage/*`、`specs/001-115-open-storage/*`、以及对应模块代码

## 关键结论（TL;DR）

1. **授权**：采用“应用内展示二维码 → 轮询状态 → 用户在 115 客户端确认 → 换取 Cookie（UID/CID/SEID/KID）”作为唯一授权方式；Cookie 即后续所有 API 的凭证；Cookie 失效后只能重新扫码（不做自动刷新）。
2. **账号唯一性**：扫码登录响应中可获得 `user_id/user_name`（或可通过用户信息接口获取）；以 `user_id` 作为同账号去重与隔离边界，`MediaLibraryEntity.url` 建议固定为 `115cloud://uid/<user_id>`。
3. **文件浏览**：目录列表使用 `GET https://webapi.115.com/files`（cookie + UA），分页 `offset/limit`；排序映射到 `o=file_name|file_size|user_ptime|file_type` 与 `asc=0/1`，并与现有 UI 排序选项对齐。
4. **搜索**：使用 `GET https://webapi.115.com/files/search`，传 `search_value` + `cid=<当前目录ID>` + `type=4`（视频）实现“当前目录递归搜索视频”；若线上验证发现 `cid` 非递归，则降级为“全盘搜索 + 结果提示（不满足则不交付）”不是可接受方案，必须改为“API 递归能力”或“服务端可递归检索”。
5. **播放直链**：直链获取建议走 `POST https://proapi.115.com/android/2.0/ufile/download`（cookie + UA），请求体为 `application/x-www-form-urlencoded` 的 `data=<m115加密串>`；响应 `data` 需用同一请求 key 解密得到 `url`。直链用短 TTL 缓存（建议 2 分钟）+ Range 不支持/403 时强刷一次。
6. **本地代理策略**：复用项目内 `LocalProxy/HttpPlayServer` 的 Range 处理与节流策略（mpv/VLC 等高风险播放器优先启用），并确保 `Cookie/User-Agent` 等必要 header 通过代理与直连链路一致传递。

---

## 决策 1：授权方式采用“二维码扫码 → Cookie 挂载”

- **Decision**：实现 115 Cloud 存储源的新增/编辑时，仅提供“二维码扫码授权”流程；授权成功后持久化 Cookie（至少包含 `UID/CID/SEID`，建议同时保存 `KID`），不引导账号/密码登录。
- **Rationale**：
  - 与 `spec.md` 的 Clarifications 一致（默认二维码扫码，不输入账号/密码）。
  - OpenList 115 Cloud driver 的主流实现路径即为“扫码 → cookie”。
  - Cookie 作为凭证可覆盖目录浏览、搜索、播放直链等链路，避免在 UI/业务层泄漏过多平台细节。
- **Alternatives considered**：
  - 用户手动填 Cookie：可作为调试/应急入口，但不符合首期交互；且 Cookie 来源复杂、易误用（例如不可用设备来源）。
  - 改走 115 Open（access_token/refresh_token）：更合规但这属于另一个存储类型（项目已实现），不满足本需求“115 Cloud”。

## 决策 2：二维码登录接口与状态机采用 OpenList/社区一致定义

- **Decision**：二维码授权流程按以下端点与状态机实现（端点与状态码以 OpenList 文档与 115driver 为准）：
  - 获取二维码 token：`GET https://qrcodeapi.115.com/api/1.0/web/1.0/token/`
  - 获取二维码图片：`GET https://qrcodeapi.115.com/api/1.0/mac/1.0/qrcode?uid={uid}`（UI 展示用）
  - 轮询状态：`GET https://qrcodeapi.115.com/get/status/?uid=...&time=...&sign=...`
    - `0` 等待、`1` 已扫码、`2` 已确认、`-1` 过期、`-2` 取消（实现时以实际字段为准）
  - 确认登录换取 Cookie：`POST https://passportapi.115.com/app/1.0/{app}/1.0/login/qrcode/`（表单：`account=uid`）
- **Rationale**：
  - 该流程具有清晰的 UI 状态推进（可覆盖“取消/刷新二维码/重试”的验收要求 FR-002）。
  - 登录响应中已包含 `cookie` 以及 `user_id/user_name` 等用户信息（可直接用于展示名与账号唯一标识）。
- **Alternatives considered**：
  - 仅使用“二维码 token”不换取 Cookie：无法作为后续 API 的通用凭证，无法完成浏览/播放闭环。
  - 使用 `LoginCheck` 校验 cookie：部分实现说明其可能触发“挤掉其他设备登录”；本项目应优先使用不会影响其他设备的校验方式（见决策 3）。

## 决策 3：凭证校验使用“CookieCheck/状态检查”，避免影响用户其他设备

- **Decision**：cookie 持久化后，连接校验优先使用 `GET https://my.115.com/?ct=guide&ac=status`（仅检查 `state`），不使用可能导致其他设备掉线的登录校验接口作为“常规校验”。
- **Rationale**：
  - 满足 FR-013“重启免重复授权”：cookie 仍有效时可直接通过校验继续使用。
  - 避免引入对用户其他设备登录态的副作用，降低“扫码一次导致其他端掉线”的体验风险。
- **Alternatives considered**：
  - `GET https://passportapi.115.com/app/1.0/web/1.0/check/sso`：可拿到 user_id，但存在“可能影响其他设备”的风险；仅在明确确认无副作用且确有必要时才考虑。
  - 不做校验：会把错误延迟到浏览/播放阶段，导致失败路径不可控。

## 决策 4：同账号去重与隔离 key 采用 `user_id` + 统一 URL Scheme

- **Decision**：以 `user_id` 作为“同一账号不可重复添加”的唯一标识；保存媒体库时固定：
  - `MediaLibraryEntity.mediaType = MediaType.CLOUD_115_STORAGE`（名称待定）
  - `MediaLibraryEntity.url = 115cloud://uid/<user_id>`
  - `MediaLibraryEntity.displayName = user_name`（可编辑，默认值来自登录响应）
- **Rationale**：
  - `media_library` 对 `(url, media_type)` 有唯一索引；用 `user_id` 构造 url 可天然满足 FR-009 的去重与多账号隔离。
  - `storageKey = "${mediaType.value}:${url}"` 可复用项目内 AuthStore/MMKV 的隔离方式，移除存储源时可精准清理对应授权数据（FR-008/FR-012）。
- **Alternatives considered**：
  - 使用自增 `id` 作为隔离 key：新增流程中难以稳定引用，且不利于跨端/跨流程一致性。
  - 把 cookie 写入 `MediaLibraryEntity.account/password/describe` 等通用字段：会污染通用字段语义并提高泄露风险（违背 P4）。

## 决策 5：文件列表使用 `webapi.115.com/files`，分页与排序映射与现有 UI 对齐

- **Decision**：目录浏览使用 `GET https://webapi.115.com/files`（cookie + UA），最小参数集建议：
  - `aid=1`
  - `cid=<目录ID>`（根目录 `0`）
  - `offset/limit`
  - `show_dir=1`（包含目录）
  - `o=<排序字段>`：`file_name` / `file_size` / `user_ptime` / `file_type`
  - `asc=<0|1>`
  - `format=json`
- **Rationale**：
  - 社区实现普遍使用该接口完成目录浏览，并支持分页与排序字段。
  - 与项目现有存储页的“刷新/排序/分页”交互天然匹配，可复用 `PagedStorage`。
- **Alternatives considered**：
  - 一次性拉取全量：大目录会导致首屏延迟与内存压力，不满足 SC-002。
  - 依赖非 webapi 的其他域名/备用域名：可作为降级重试策略，但主路径保持单一，避免复杂度失控。

## 决策 6：搜索使用 `webapi.115.com/files/search`，以 `cid` 实现“当前目录递归搜索视频”

- **Decision**：实现 `supportSearch=true`，搜索接口使用 `GET https://webapi.115.com/files/search`，参数建议：
  - `search_value=<keyword>`
  - `cid=<当前目录ID>`（根目录 `0`）
  - `type=4`（视频）
  - `count_folders=0`（仅关注文件；若接口必须为 1 则改为结果过滤）
  - `offset/limit`
  - `o/asc` 与列表排序保持一致（必要时固定为名称升序以保证稳定）
- **Rationale**：
  - 满足 FR-011“在当前目录递归范围内按关键字搜索内容（仅返回匹配的可播放视频文件）”的唯一可扩展路径是依赖服务端搜索能力，而非客户端递归全量遍历。
  - `StorageFileFragmentViewModel` 已支持 Storage 侧 search 与 UI 搜索入口，新增存储类型只需实现 Storage.search 并保持行为一致。
- **Alternatives considered**：
  - 客户端递归遍历目录并本地过滤：网络与时间不可控，无法满足 SC-002/SC-003，且大目录风险极高。
  - 全盘搜索后再本地过滤成“当前目录递归”：需要完整父链信息才能判定归属，成本与正确性风险更高；不作为首选。

## 决策 7：播放直链使用 `proapi.115.com` 下载接口 + `m115` 加解密，短 TTL 缓存并支持强刷

- **Decision**：播放直链获取优先使用 Android 下载接口：
  - `POST https://proapi.115.com/android/2.0/ufile/download?t=<timestamp>`
  - `Content-Type: application/x-www-form-urlencoded`
  - 表单：`data=<m115加密串>`，明文 payload：`{"pick_code":"<pickcode>"}`（具体 key 以接口要求为准）
  - Header：`Cookie: <uid/cid/seid/kid...>` + `User-Agent: Mozilla/5.0 115Browser/<ver>`
  - 响应：JSON 包含 `data=<encoded>`，需用同一请求 key 解密得到 `{"url":"https://..."}` 或同等结构
  - 缓存策略：对同一文件（fid/pickcode）缓存 2 分钟；播放失败或 Range 不支持时强刷一次
- **Rationale**：
  - 该路径在 OpenList/115driver 等实现中更常见，且适合“在线播放”场景。
  - 项目内已有 `DownUrlCache + LocalProxy rangeUnsupported 刷新` 的成熟模式，可直接复用以控制风控风险。
- **Alternatives considered**：
  - `POST https://proapi.115.com/app/chrome/downurl`：同样可行但请求明文字段与响应结构不同；若 Android 口不稳定再切换。
  - `GET https://webapi.115.com/files/download?pickcode=...`：部分工具使用，但可观测性与错误码一致性较差；不作为首选。

## 决策 8：UI 与模块落点完全复用既有模式（StoragePlus + PagedStorage + LocalProxy）

- **Decision**：新增存储类型的落点与触点严格对齐现有“百度网盘/115 Open”模式：
  - UI：在 `StoragePlusActivity` 中新增 `Cloud115StorageEditDialog`（或等价页面），复用“二维码 + 轮询 + 保存”模式（可参考百度网盘登录对话框）
  - Storage：`Cloud115Storage : AbstractStorage, PagedStorage, AuthStorage`；实现 `getRootFile/openDirectory/loadMore/supportSearch/search/createPlayUrl/getNetworkHeaders`
  - 网络：`core_network_component` 中新增 Retrofit Service（声明接口）；业务逻辑在 `core_storage_component` 的 Repository 内
  - 数据：`data_component` 承载 `MediaType`、图标、以及 API response model
- **Rationale**：
  - 遵守宪章 P1/P2：不引入 feature ↔ feature 依赖，跨层职责清晰，避免“实现层模块成为类型出口”。
  - 复用既有模式可显著降低集成成本与 UI 行为差异，满足 FR-010。
- **Alternatives considered**：
  - 新建独立 `:cloud115_component`：边界更清晰但会引入新的模块治理与依赖矩阵；首期不做。

