# :repository:video_cache（AndroidVideoCache AAR wrapper）

本模块以 **本地预编译 AAR** 的形式封装 AndroidVideoCache，供工程通过 `implementation(project(":repository:video_cache"))` 依赖接入。

## 上游 / 来源

- 上游项目：AndroidVideoCache
  - 仓库：`https://github.com/danikula/AndroidVideoCache`
- 本仓库工件：
  - AAR：`repository/video_cache/library-release.aar`

## 版本信息

- AAR `BuildConfig.VERSION_NAME = 2.7.1`

## License

- License：Apache-2.0（以上游仓库 `LICENSE` 为准）
- 说明：当前仓库的 `user_component/src/main/assets/license/` 未包含该库条目；若需要在 App 内第三方声明页展示，建议新增对应 license 文本并与现有清单保持一致。

## 校验和（SHA256）

- `library-release.aar`：
  - `dfeb8092dd51608c46e443c4ef5d049aa803c5d54e067f3e5ef1add07014cdbc`
- 计算命令：
  - `sha256sum repository/video_cache/library-release.aar`

## 升级流程（可复现）

1. 从上游获取目标版本源码，并按上游构建方式产出 AAR（或从可信渠道获取同版本 AAR）。
2. 用新文件替换 `repository/video_cache/library-release.aar`。
3. 更新本 README：
   - 版本号（上游版本 + `BuildConfig.VERSION_NAME`）
   - SHA256
4. 如上游 License 有变化，同步更新本 README，并视需要补齐 App 内第三方声明文本。
5. 运行工程验证（建议至少）：
   - `./gradlew :app:assembleDebug`
