# :repository:panel_switch（PanelSwitchHelper AAR wrapper）

本模块以 **本地预编译 AAR** 的形式封装 PanelSwitchHelper（AndroidX 版本），供工程通过 `implementation(project(":repository:panel_switch"))` 依赖接入。

## 上游 / 来源

- 上游项目：PanelSwitchHelper
  - 仓库：`https://github.com/YummyLau/PanelSwitchHelper`
- 本仓库工件：
  - AAR：`repository/panel_switch/panelSwitchHelper-androidx.aar`

## 版本信息

- AAR `BuildConfig.VERSION_NAME = 1.0`
- 说明：该版本号可能来自封装时的默认值；升级时建议以“上游 tag / Release 版本号”为准补齐并更新本 README。

## License

- License：Apache-2.0
- 本仓库 License 线索：
  - `user_component/src/main/assets/license/PanelSwitchHelper.txt`

## 校验和（SHA256）

- `panelSwitchHelper-androidx.aar`：
  - `ace2e33a70f393388c041ce428f497ce0379b0367a4279fae2dc9f8d9f671c00`
- 计算命令：
  - `sha256sum repository/panel_switch/panelSwitchHelper-androidx.aar`

## 升级流程（可复现）

1. 从上游获取目标版本源码，并按上游构建方式产出 AAR（或从可信渠道获取同版本 AAR）。
2. 用新文件替换 `repository/panel_switch/panelSwitchHelper-androidx.aar`。
3. 更新本 README：
   - 上游版本号（建议记录 tag / Release）
   - SHA256
4. 如上游 License 有变化，同步更新 `user_component/src/main/assets/license/PanelSwitchHelper.txt`（或新增本目录 `LICENSE` 并在 README 中指向）。
5. 运行工程验证（建议至少）：
   - `./gradlew :app:assembleDebug`

