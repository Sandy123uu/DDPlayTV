# 115 Cloud API 摘要（DDPlayTV 子集）

> 目的：把实现“115 Cloud 存储源：扫码授权 + 浏览 + 搜索 + 在线播放”所需的关键接口、字段与约束用中文整理出来，便于实现期对照抓包与排障。

- OpenAPI 契约：`contracts/115-cloud-openapi.yaml`
- 调研决策：`research.md`

## 1. 通用约定

### 1.1 必要请求头

- `Cookie`：扫码登录后获取，至少包含 `UID/CID/SEID`，建议同时保存 `KID`
- `User-Agent`：建议使用 115Browser/115disk 风格 UA（例如 `Mozilla/5.0 115Browser/<ver>`）

> 注意：Cookie/UA 属于敏感信息与稳定性关键参数，日志与 UI 必须脱敏展示，禁止输出完整值（FR-012）。

### 1.2 根目录与 ID 规则

- 根目录 `cid = "0"`
- 列表返回的“目录项”通常没有 `fid`（为空字符串），目录自身 ID 在 `cid` 字段中；目录的父目录在 `pid`
- 列表返回的“文件项”有 `fid`，其父目录 ID 通常等于请求参数 `cid`

## 2. 二维码扫码授权

### 2.1 获取二维码会话

- `GET https://qrcodeapi.115.com/api/1.0/web/1.0/token`
- 返回：`uid/time/sign/qrcode`

二维码展示有两种选择：

1. 使用 `qrcode` 文本在客户端生成二维码（依赖本地二维码生成库）
2. 直接拉取二维码图片（推荐，减少依赖）：
   - `GET https://qrcodeapi.115.com/api/1.0/mac/1.0/qrcode?uid=<uid>`

### 2.2 轮询授权状态

- `GET https://qrcodeapi.115.com/get/status/?uid=...&time=...&sign=...`
- 常见状态码（以实际字段为准）：
  - `0` 等待扫码
  - `1` 已扫码未确认
  - `2` 已确认（允许换取 cookie）
  - `-1` 过期
  - `-2` 取消

### 2.3 换取 Cookie（登录确认）

- `POST https://passportapi.115.com/app/1.0/{app}/1.0/login/qrcode`
- 表单：
  - `account=<uid>`
  - `app=<app>`（部分实现会同时传）
- 成功响应包含：
  - `cookie`：`UID/CID/SEID/KID`
  - `user_id`、`user_name`、头像等信息

> 设备来源 `app` 建议选择 `tv/wechatmini/alipaymini/qandroid` 等，避免挤掉用户常用客户端登录态。

## 3. Cookie 有效性检查（重启免重复授权）

- `GET https://my.115.com/?ct=guide&ac=status`
- 返回 `state`：true 表示 cookie 有效

> 说明：应优先使用该类“状态检查”接口做常规校验，避免使用可能影响其他设备登录态的校验方式。

## 4. 文件浏览（列表 / 排序 / 分页）

- `GET https://webapi.115.com/files`
- 最小参数集（建议）：
  - `aid=1`
  - `cid=<目录ID>`（根目录 0）
  - `offset=<偏移>`、`limit=<分页大小>`
  - `show_dir=1`（包含目录）
  - `format=json`
  - `o=<排序字段>`：`file_name|file_size|user_ptime|file_type`
  - `asc=<0|1>`

## 5. 搜索（当前目录递归搜索视频）

- `GET https://webapi.115.com/files/search`
- 建议参数：
  - `search_value=<keyword>`
  - `cid=<当前目录ID>`
  - `type=4`（视频）
  - `count_folders=0`（若接口强制为 1，则在客户端过滤目录项）
  - `offset/limit`

## 6. 父路径（面包屑）与路径构造

为让“搜索结果/播放历史”具备稳定的 `storagePath`，建议用以下接口获取父路径：

- `GET https://webapi.115.com/category/get?cid=<fileId-or-dirId>`
- 返回 `paths[]`：父目录链路（`file_id/file_name`）

可按目录维度缓存 `dirId -> breadcrumbIds`，用于把任意文件构造为：

`"/<root>/<...>/<dirId>/<fileId>"`（仅 ID 链路；不依赖文件名，避免重命名导致定位失败）

## 7. 播放直链（m115 加解密）

推荐使用 Android 下载接口获取直链：

- `POST https://proapi.115.com/android/2.0/ufile/download?t=<timestamp>`
- `Content-Type: application/x-www-form-urlencoded`
- 表单：`data=<m115加密串>`
  - 明文 payload 参考：`{"pick_code":"<pickcode>"}`
  - 响应中的 `data` 需要用同一请求 key 解密，得到 `{"url":"https://..."}` 或等价结构

缓存与回退建议：

- 同一文件直链缓存 2 分钟
- 直链失效、403、Range 不支持等失败时强制刷新一次；对 mpv/VLC 等播放器优先走 `LocalProxy/HttpPlayServer` 降低 Range 风控风险

