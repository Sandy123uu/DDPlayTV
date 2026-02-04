# :repository:immersion_bar（ImmersionBar AAR wrapper）

本模块以 **本地预编译 AAR** 的形式封装 ImmersionBar，供工程通过 `project(":repository:immersion_bar")` 依赖接入。

## 上游 / 来源

- 上游项目：ImmersionBar
  - 仓库：`https://github.com/gyf-dev/ImmersionBar`
- 本仓库工件：
  - AAR：`repository/immersion_bar/immersionbar.aar`

## 版本信息

- 说明：当前 `immersionbar.aar` 未在 AAR 的 `AndroidManifest.xml` 中声明 `android:versionName`，且 `BuildConfig.VERSION_NAME` 为空字符串。
- 建议记录口径：
  - **当前版本号（上游版本）**：待补充（建议在首次升级时以“上游 tag / Release / 构建产物来源”为准补齐）
  - **当前二进制标识**：以 SHA256 作为唯一可追溯标识（见下）

## License

- License：Apache-2.0
- 本仓库 License 线索：
  - `user_component/src/main/assets/license/ImmersionBar.txt`（包含上游地址与 Apache License 文本片段）

## 校验和（SHA256）

- `immersionbar.aar`：
  - `731f2fcf301ff9f1ee0c5fb7c1938911b5977eca65e774ebb895003fb2f7c192`
- 计算命令：
  - `sha256sum repository/immersion_bar/immersionbar.aar`

## 升级流程（可复现）

1. 从上游获取目标版本源码，并按上游构建方式产出 AAR（或从可信渠道获取同版本 AAR）。
2. 用新文件替换 `repository/immersion_bar/immersionbar.aar`。
3. 更新本 README：
   - 上游版本号（**必须补齐**：tag / Release / 构建来源）
   - SHA256
4. 如上游 License 有变化，同步更新 `user_component/src/main/assets/license/ImmersionBar.txt`（或新增本目录 `LICENSE` 并在 README 中指向）。
5. 运行工程验证（建议至少）：
   - `./gradlew :app:assembleDebug`

