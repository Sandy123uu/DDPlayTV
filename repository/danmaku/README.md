# :repository:danmaku（DanmakuFlameMaster AAR wrapper）

本模块以 **本地预编译 AAR** 的形式封装 DanmakuFlameMaster，供工程通过 `implementation(project(":repository:danmaku"))` 依赖接入。

## 上游 / 来源

- 上游项目：DanmakuFlameMaster（Bilibili）
  - 仓库：`https://github.com/bilibili/DanmakuFlameMaster`
- 本仓库工件：
  - AAR：`repository/danmaku/DanmakuFlameMaster.aar`

## 版本信息

- AAR `AndroidManifest.xml` 声明：
  - `android:versionName = 0.9.25`
  - `android:versionCode = 9025`

## License

- License：Apache-2.0
- 本仓库 License 线索：
  - `user_component/src/main/assets/license/DanmakuFlameMaster.txt`（包含上游地址与 Apache-2.0 提示）

## 校验和（SHA256）

- `DanmakuFlameMaster.aar`：
  - `07ddd7b95b20ec3b3e0bc61717524e2996a1c444fe8d49927b545707beb7d34e`
- 计算命令：
  - `sha256sum repository/danmaku/DanmakuFlameMaster.aar`

## 升级流程（可复现）

1. 从上游获取目标版本的 AAR（建议优先选择上游 Release / tag 对应产物，或自行按上游工程构建生成 AAR）。
2. 用新文件替换 `repository/danmaku/DanmakuFlameMaster.aar`。
3. 更新本 README：
   - 版本号（优先记录上游 tag / Release 版本；其次记录 AAR `AndroidManifest.xml` 的 `versionName`）
   - SHA256
4. 如上游 License 有变化，同步更新 `user_component/src/main/assets/license/DanmakuFlameMaster.txt`（或新增本目录 `LICENSE` 并在 README 中指向）。
5. 运行工程验证（建议至少）：
   - `./gradlew :app:assembleDebug`

