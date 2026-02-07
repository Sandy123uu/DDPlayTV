# :repository:seven_zip（sevenzipjbinding4Android AAR wrapper）

本模块以 **本地预编译 AAR** 的形式封装 7-Zip-JBinding for Android，供工程通过 `implementation(project(":repository:seven_zip"))` 依赖接入。

## 上游 / 来源

- 上游项目：7-Zip-JBinding
  - 站点：`http://sevenzipjbind.sourceforge.net`
- 本仓库工件：
  - AAR：`repository/seven_zip/sevenzipjbinding4Android.aar`

## 版本信息

- AAR `BuildConfig.VERSION_NAME = 16.02-2.01`
- AAR `AndroidManifest.xml` 声明：
  - `android:versionName = 16.02-2.01`
  - `android:minSdkVersion = 15`

## ABI / 平台支持

- AAR 内包含的 `jni/*` 目录（实际用于 Android 打包）：
  - `arm64-v8a`
  - `armeabi-v7a`
  - `x86`
  - `x86_64`
- 备注：AAR 内还包含若干非 Android 打包所需的目录（例如 `Linux-arm/*`），升级时建议确认其必要性。

## License

- License：LGPL-2.1-or-later（以本仓库 License 线索与上游为准）
- 本仓库 License 线索：
  - `user_component/src/main/assets/license/7-Zip-JBinding.txt`

## 校验和（SHA256）

- `sevenzipjbinding4Android.aar`：
  - `def3f43296b21e148b8bcc4bb7cf40c99352488c73ded7e2faa258fbabf17caa`
- 计算命令：
  - `sha256sum repository/seven_zip/sevenzipjbinding4Android.aar`

## 升级流程（可复现）

1. 从上游获取目标版本的 Android AAR（或按上游方式构建生成）。
2. 用新文件替换 `repository/seven_zip/sevenzipjbinding4Android.aar`。
3. 更新本 README：
   - 版本号（上游版本 + `BuildConfig/AndroidManifest.xml` 版本号）
   - SHA256
   - ABI 列表（以 AAR 内 `jni/*` 为准）
   - `minSdkVersion`（以 AAR `AndroidManifest.xml` 为准）
4. 如上游 License 有变化，同步更新 `user_component/src/main/assets/license/7-Zip-JBinding.txt`（或新增本目录 `LICENSE` 并在 README 中指向）。
5. 运行工程验证（建议至少）：
   - `./gradlew :app:assembleDebug`

