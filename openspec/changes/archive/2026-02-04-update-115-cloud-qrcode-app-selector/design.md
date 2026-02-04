# 设计说明：115 Cloud 扫码授权设备来源（app）可选与按账号记忆

## 背景与目标

115 Cloud 的扫码登录确认接口路径包含 `{app}`（设备来源）。不同 `app` 可能对应不同客户端类型的登录态隔离策略；为降低“顶号/互斥登录态”的风险，应用需要允许用户选择该值，并在同一账号维度记住选择。

目标：

- 授权弹窗提供“设备来源（app）”选择，默认 `tv`
- 登录确认请求使用用户选中的 `app`（path + form 同步）
- 选择结果按账号（`115cloud://uid/<user_id>`）持久化，后续再次授权默认回显
- 不污染 `MediaLibraryEntity` 通用字段；敏感信息保护不受影响

非目标：

- 不引入账号密码登录、短信登录等其他授权方式
- 不尝试穷举并硬编码“所有可能的 app 值”（提供自定义输入即可覆盖）

## 数据与持久化

### 存储位置

将 `app` 存储在 `Cloud115AuthStore`（MMKV）中，与 cookie/userId/userName/avatarUrl 同级：

- KEY：`login_app`（命名可调整，但应语义明确）
- 读写：扩展 `AuthState` 增加 `loginApp: String?`
- 回退策略：
  - `loginApp` 为空时默认 `tv`
  - 旧版本存量数据没有该字段时自然回退 `tv`

### 隔离边界

沿用现有隔离策略：`storageKey = "${mediaType.value}:${url.trim().removeSuffix("/")}"`。

当账号确定后，`url` 固定为 `115cloud://uid/<user_id>`，因此 `login_app` 自然按账号隔离。

## UI 与交互

### 控件形式

在 `Cloud115LoginDialog`（扫码授权弹窗）中增加一行：

- 标签：`设备来源（app）`
- 值展示：当前选中的 `app`（例如 `tv`）
- 操作：点击/确认后弹出选择列表（BottomActionDialog 或同类控件）

选择列表提供：

- 预置项：`tv / wechatmini / alipaymini / qandroid`
- `自定义...`：弹出输入框让用户填写 `app` 字符串

### 默认值与回显

- 新增账号：默认 `tv`
- 编辑既有账号并重新授权：
  - 从 `Cloud115AuthStore` 读取该账号上次 `login_app` 作为默认选中值
  - 若不存在则回退 `tv`

### 结果回传

`Cloud115LoginDialog.LoginResult` 增加 `loginApp` 字段，授权成功回传给 `Cloud115StorageEditDialog`，由后者在写入 `Cloud115AuthStore` 时一并保存。

> 说明：扫码前无法确定 `user_id`，因此不在弹窗内直接落库；避免产生“未完成授权但写入了错误账号 key”的风险。

## 网络请求一致性

扫码登录确认请求需要同步使用 `loginApp`：

- path：`/app/1.0/{app}/1.0/login/qrcode`（`{app} = loginApp`）
- form：`app=loginApp`

该值本身不敏感，允许记录在日志的上下文中（但 cookie 等敏感信息仍必须脱敏）。

