# US2 回归测试记录

## 执行环境

- 分支：`001-fix-sonarcloud-issues`
- 日期：2026-02-08
- 目标：验证 US2（Top10 文件治理）相关改动的单测与 AndroidTest 编译链路

## 执行记录

### 1) Player 相关回归（T025 + T035）

```bash
./gradlew :player_component:testDebugUnitTest \
  --tests com.xyoye.player_component.wrapper.ControlWrapperQualityRegressionTest \
  --tests com.xyoye.player_component.media3.Media3PlayerDelegateTest
```

- 第 1 次执行：`BUILD FAILED`
  - 失败原因：`ControlWrapperQualityRegressionTest` 中枚举常量命名错误、Fake 基类可覆写性与属性/JVM 签名冲突。
- 修复后第 2 次执行：`BUILD SUCCESSFUL`

### 2) Storage / Network 相关回归（T026 + T027 + T034 + T036 + T037）

```bash
./gradlew \
  :core_network_component:testDebugUnitTest \
    --tests com.xyoye.common_component.network.open115.Open115ModelsMoshiTest \
  :core_storage_component:testDebugUnitTest \
    --tests com.xyoye.common_component.storage.cloud115.auth.Cloud115TokenParserTest \
    --tests com.xyoye.common_component.storage.cloud115.Cloud115StorageQualityTest \
  :storage_component:testDebugUnitTest \
    --tests com.xyoye.storage_component.ui.activities.storage_file.StorageFileActivityQualityTest
```

- 结果：`BUILD SUCCESSFUL`

### 3) AndroidTest 编译校验（T028）

```bash
./gradlew :app:compileDebugAndroidTestKotlin --console=plain
```

- 结果：`BUILD SUCCESSFUL`
- 说明：当前记录仅覆盖 AndroidTest 编译通过；未连接设备/模拟器执行 `connectedDebugAndroidTest`。

## 结论

- US2 新增/修改测试文件均已通过本地编译与目标单测执行。
- 本地构建日志尾部均已确认包含 `BUILD SUCCESSFUL`（失败重试场景除外，已修复后复验通过）。

### 4) Windows AVD 设备执行 AndroidTest（补充）

```bash
/mnt/c/Users/Administrator.DESKTOP-1KCKBJ1/AppData/Local/Android/Sdk/platform-tools/adb.exe \
  -s emulator-5554 shell am instrument -w -r \
  -e class com.xyoye.app.quality.CorePathSmokeTest \
  com.okamihoro.ddplaytv.debug.test/androidx.test.runner.AndroidJUnitRunner
```

- 首次执行：`FAILURES!!! Tests run: 2, Failures: 1`
  - 失败原因：`corePlaybackEntryHandlesMissingSourceGracefully` 在 Activity 已销毁场景仍调用 `scenario.onActivity`，触发 `Cannot run onActivity since Activity has been destroyed already`。
- 修复后复验：`OK (2 tests)`
  - 修复文件：`app/src/androidTest/java/com/xyoye/app/quality/CorePathSmokeTest.kt`
  - 修复策略：改为轮询 `scenario.state == Lifecycle.State.DESTROYED`，避免在已销毁 Activity 上执行 `onActivity`。

