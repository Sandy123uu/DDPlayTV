## 1. `core_log_component`：TCP 日志 Server 基建

- [x] 1.1 扩展 `LogConfigTable`：新增 TCP 日志服务的持久化字段（启用开关、端口等）。
- [x] 1.2 新增 TCP server 组件：监听 `17010`、接受多客户端连接、维护连接列表。
- [x] 1.3 在 `LogSystem` 中接入开关能力：提供启用/关闭 API，并在启动阶段按持久化状态自动恢复 server。
- [x] 1.4 在 `LogWriter` 写入链路中增加广播钩子：将已通过策略过滤的日志行投递到 TCP server（不阻塞主线程与日志写入线程）。

## 2. `user_component`：开发者设置入口与说明

- [x] 2.1 更新 `preference_developer_setting.xml`：新增“TCP 日志服务”开关项（默认关闭）。
- [x] 2.2 更新 `DeveloperSettingFragment`：绑定开关逻辑，展示端口/连接方式与安全提示（必要时提示当前启用状态）。

## 3. 测试

- [x] 3.1 `core_log_component` 单测：TCP server 启停与广播（至少覆盖单客户端与多客户端）。
- [ ] 3.2 （可选）`app` 仪器测试：开发者设置变更能正确更新 `LogSystem` 的 TCP server 状态。

## 4. 验证

- [x] 4.1 编译验证：`./gradlew :app:assembleDebug`（确认输出尾部为 `BUILD SUCCESSFUL`）
- [x] 4.2 静态检查：`./gradlew lint`
- [x] 4.3 依赖治理校验：`./gradlew verifyModuleDependencies`（确认输出尾部为 `BUILD SUCCESSFUL`）
- [ ] 4.4 手工验收（需设备/局域网，本环境未执行）：`nc <device-ip> 17010` 可收到实时日志
