## 1. 按账号存储“设备来源（app）”

- [x] 1.1 扩展 `Cloud115AuthStore`：新增 `login_app` 字段（读/写/clear），`AuthState` 增加 `loginApp`，缺省回退 `tv`。
- [x] 1.2 调整仓库层扫码登录写入逻辑：在 `Cloud115Repository.qrcodeLogin(...)` 写入授权态时同步保存 `loginApp`（确保未来复用仓库层时行为一致）。

## 2. 扫码授权弹窗增加 app 选择与回显

- [x] 2.1 调整扫码授权弹窗布局：新增“设备来源（app）”展示与选择入口（预置 `tv/wechatmini/alipaymini/qandroid` + `自定义`）。
- [x] 2.2 调整 `Cloud115LoginDialog`：
  - 增加 `selectedApp` 状态并绑定 UI
  - 调用 `qrcodeLogin` 时使用 `selectedApp`（path + form）
  - `LoginResult` 增加 `loginApp`
- [x] 2.3 调整 `Cloud115StorageEditDialog`：
  - 打开扫码弹窗时，从该账号的 `Cloud115AuthStore` 读取上次 `loginApp` 作为默认值（无则 `tv`）
  - 授权成功后将 `loginApp` 与 cookie/user 信息一起写入 `Cloud115AuthStore`

## 3. 验证

- [x] 3.1 编译验证：`./gradlew :storage_component:assembleDebug`（确认输出尾部为 `BUILD SUCCESSFUL`）
- [x] 3.2 静态检查：`./gradlew lint`（确认输出尾部为 `BUILD SUCCESSFUL`）
- [x] 3.3 依赖治理校验：`./gradlew verifyModuleDependencies`（确认输出尾部为 `BUILD SUCCESSFUL`）
- [x] 3.4 手工验收（TV/手机均可）：
  - 新增 115 Cloud：默认 `tv`，可切换到其他 app 并成功授权
  - 对同一账号再次扫码授权：默认回显上次选择的 app
  - 清除授权后：app 记忆随授权态一并清理
