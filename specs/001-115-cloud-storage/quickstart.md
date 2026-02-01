# Phase 1：Quickstart（115 Cloud 存储源）

本文档面向后续实现与自测，给出最短路径的配置、运行与验收步骤，并附带关键资料来源，便于实现时查原文。

## 1. 前置条件

### 1.1 账号与客户端

- 准备一个可用的 115 账号（个人云盘场景）
- 手机上安装 115 客户端（用于扫码确认登录）

本功能的鉴权约束为“应用内展示二维码 → 用户扫码确认 → 获取 Cookie 并挂载”，不提供账号/密码输入（详见 `spec.md` 的 Clarifications 与 Out of Scope）。

### 1.2 关键接口与域名（实现/联调用）

- 二维码 token：`https://qrcodeapi.115.com/api/1.0/web/1.0/token`
- 二维码图片：`https://qrcodeapi.115.com/api/1.0/mac/1.0/qrcode?uid=<uid>`
- 二维码状态：`https://qrcodeapi.115.com/get/status/?uid=...&time=...&sign=...`
- 二维码确认登录：`https://passportapi.115.com/app/1.0/{app}/1.0/login/qrcode`
- Cookie 状态校验：`https://my.115.com/?ct=guide&ac=status`
- 目录列表：`https://webapi.115.com/files`
- 搜索：`https://webapi.115.com/files/search`
- 播放直链：`https://proapi.115.com/android/2.0/ufile/download`

资料：

- `specs/001-115-cloud-storage/research.md`
- `specs/001-115-cloud-storage/115-cloud-openapi.md`
- `specs/001-115-cloud-storage/contracts/115-cloud-openapi.yaml`

## 2. 构建与运行

在仓库根目录执行：

- Debug 构建：`./gradlew assembleDebug`
- 完整校验（实现阶段若涉及依赖调整）：`./gradlew verifyModuleDependencies`（或推荐 `./gradlew verifyArchitectureGovernance`）

> 依赖变更门禁要求：必须确认输出末尾为 `BUILD SUCCESSFUL`。

## 3. 手动验收用例（对应 spec P1）

### 3.1 新增 115 Cloud 并完成扫码授权

1. 打开“新增存储源”，选择“115 Cloud”
2. 页面展示二维码，并提示“使用 115 客户端扫码确认”
3. 扫码后应用展示状态变化（等待 → 已扫码 → 已确认）
4. 确认成功后自动保存并返回存储源列表

验收点：

- 可取消、可刷新二维码、可重试（FR-002）
- 成功/失败/取消均有明确反馈（FR-002）

### 3.2 浏览目录并返回

1. 从存储源列表进入已添加的 115 Cloud
2. 默认显示根目录列表（cid=0）
3. 进入任意子目录并返回上级，重复 2~3 次

验收点：

- 列表可区分“目录/文件”，并在可展示时提供基础信息（FR-003/FR-004）
- 目录导航正常，不丢失上下文（FR-003）

### 3.3 选择视频并开始播放（多内核）

1. 在文件列表中选择一个受支持的视频文件
2. 验证进入播放器并开始播放（出现画面或听到声音）
3. 在设置中分别切换不同播放方式（Media3/Exo、mpv、VLC 等），再次播放同一文件验证均可启动（FR-014）

### 3.4 刷新/排序/搜索定位

1. 在当前目录执行刷新，列表应更新且保持上下文稳定
2. 切换排序（名称/大小/时间等）并验证结果
3. 输入关键词搜索：结果仅返回“匹配的可播放视频文件”，清空关键词后恢复当前目录列表与上下文（FR-011）

### 3.5 授权失效/断网等异常恢复

1. 断网或让 cookie 失效后进入目录/播放
2. 预期：阻止继续访问并提示用户“重试/重新授权/返回”（FR-007/FR-013）
3. 重新扫码授权成功后可继续浏览与播放（FR-013）

### 3.6 移除存储源与清理

1. 在存储源管理中移除该 115 Cloud
2. 预期：存储源从列表消失；授权数据被清理；再次进入需要重新扫码才可恢复（FR-008/FR-012）

## 4. 播放稳定性提示（115 风控 / Range）

项目已有“mpv/VLC 触发上游 Range 风控”经验。建议：

- 对 mpv/VLC 默认走 `LocalProxy/HttpPlayServer`（上游 headers 注入 Cookie/UA，并控制 Range 行为）
- 直链缓存采用短 TTL（建议 2 分钟），失败时强刷一次再提示用户重试/重新授权

资料：`document/support/mpv-115-proxy.md`（如实现复用该策略）

## 5. 已知限制/风险提示

- **非开放平台接口**：115 Cloud 接口可能随时间变化；实现需保留可观测性与降级/提示空间（宪章 P5）。
- **敏感信息保护**：UI/日志不得输出完整 Cookie（FR-012）。

