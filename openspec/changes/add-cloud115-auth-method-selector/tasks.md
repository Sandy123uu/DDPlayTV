## 1. Cloud115 Token 解析与校验能力

- [x] 1.1 在 `core_storage_component` 增加/抽取 Cloud115 token 解析与规范化工具：
  - 从用户输入解析 `UID/CID/SEID/KID`
  - 生成标准化 `cookieHeader`（仅包含上述字段）
  - 输出 `userId=UID`
- [x] 1.2 单元测试覆盖常见输入与失败场景（缺字段、包含 `Cookie:` 前缀、大小写/空格差异）。

## 2. 115 Cloud 授权方式选择 UI

- [x] 2.1 调整 `Cloud115StorageEditDialog`：授权按钮点击后弹出方式选择（`扫码授权` / `手动输入 token`），并将按钮文案改为更中性表述（例如“授权/重新授权”）。
- [x] 2.2 扫码流程保持不变：继续使用 `Cloud115LoginDialog`（包含 `loginApp` 选择与记忆）。

## 3. 手动 Token 授权流程

- [x] 3.1 增加手动 token 输入弹窗（`CommonEditDialog` 或 `Cloud115TokenLoginDialog`）。
- [x] 3.2 确认后执行：
  - 解析并校验 token
  - 通过后写入 `Cloud115AuthStore` 并触发自动保存/刷新授权状态
  - 失败则提示原因且不落库（或回滚临时写入）
- [x] 3.3 确保新增路径与扫码路径一样进行日志脱敏（`Cloud115Headers.redactCookie`）。

## 4. 文案一致性

- [x] 4.1 将 Cloud115 相关提示从“扫码授权/重新扫码”调整为“完成授权/重新授权”（例如 `StoragePlusViewModel`、Cloud115 授权失效异常提示）。

## 5. 验证

- [x] 5.1 编译验证：`./gradlew :storage_component:assembleDebug`（确认输出尾部为 `BUILD SUCCESSFUL`）
- [x] 5.2 静态检查：`./gradlew lint`（确认输出尾部为 `BUILD SUCCESSFUL`）
- [x] 5.3 依赖治理校验：`./gradlew verifyModuleDependencies`（确认输出尾部为 `BUILD SUCCESSFUL`）
- [x] 5.4 单元测试：`./gradlew :core_storage_component:testDebugUnitTest`（确认输出尾部为 `BUILD SUCCESSFUL`）
- [ ] 5.5 手工验收（TV/手机均可）：
  - 扫码授权仍可用（含 `loginApp` 选择/记忆）
  - 手动 token：校验通过后授权成功并可浏览/播放
  - token 无效/缺字段：提示清晰且不会保存
