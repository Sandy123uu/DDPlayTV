# :repository:thunder（thunder.aar wrapper）

本模块以 **本地预编译 AAR** 的形式封装迅雷相关 SDK（典型包名 `com.xunlei.downloadlib.*`），供工程通过 `implementation(project(":repository:thunder"))` 依赖接入。

## 上游 / 来源

- 上游/供应商：迅雷下载 SDK（闭源）
  - 说明：当前仓库未提供公开可访问的下载地址；升级通常需要从供应商/原作者处获取新版 AAR。
- 本仓库工件：
  - AAR：`repository/thunder/thunder.aar`

## 版本信息

- AAR `BuildConfig.VERSION_NAME = 1.0`
- 说明：该版本号可能来自封装时的默认值；升级时建议以供应商版本号/SDK 发行说明为准补齐并更新本 README。

## ABI / 平台支持（AAR 内 `jni/*`）

- `arm64-v8a`
- `armeabi-v7a`

## License

- 说明：闭源 SDK 的 License/授权条款需要以供应商提供的协议为准；当前仓库未附带可公开引用的 License 文本。

## 校验和（SHA256）

- `thunder.aar`：
  - `17577f32ee1c0261eaff912bbc31ef5e5e2a39448eae30b4c9eaa16a16950e82`
- 计算命令：
  - `sha256sum repository/thunder/thunder.aar`

## 升级流程（可复现）

1. 从供应商/原作者获取目标版本 `thunder.aar`（建议同时获取版本号、ABI 说明、变更说明与授权协议）。
2. 用新文件替换 `repository/thunder/thunder.aar`。
3. 更新本 README：
   - 供应商版本号（**必须补齐**）
   - ABI 列表（以 AAR 内 `jni/*` 为准）
   - SHA256
   - 授权/License 获取方式（至少给出内部路径或获取联系人/渠道）
4. 运行工程验证（建议至少）：
   - `./gradlew :app:assembleDebug`

