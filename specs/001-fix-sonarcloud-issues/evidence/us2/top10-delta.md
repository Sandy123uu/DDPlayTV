# US2 Top10 文件治理前后对比（本地预估）

- 基线来源：`specs/001-fix-sonarcloud-issues/evidence/baseline/quality-baseline.md`
- 基线分析线：`07c4be1f-f37a-418a-926f-2a13a7a15f86`
- 统计口径：Top10 文件问题总量（US2 范围）
- 说明：以下“当前值”为基于已提交代码改动的本地预估，最终以新一轮 SonarCloud 分析线结果为准。

| # | 文件 | Baseline | 本轮治理动作 | 预估 Current | 预估 Delta |
|---|------|----------|--------------|--------------|------------|
| 1 | `player_component/src/main/java/com/xyoye/player/wrapper/ControlWrapper.kt` | 26 | 使用 `by` 委托重构大量直通转发；移除注释死代码；合并链式条件 | 0 | -26 |
| 2 | `storage_component/src/main/java/com/xyoye/storage_component/ui/activities/storage_file/StorageFileActivity.kt` | 12 | 清理注释死代码；合并重复回调分支；抽取可测决策函数 | 0 | -12 |
| 3 | `user_component/src/main/assets/bilibili/geetest_voucher.html` | 11 | 去除禁用缩放 viewport；`window` 访问统一切到 `globalThis` | 0 | -11 |
| 4 | `player_component/src/main/java/com/xyoye/player/controller/video/PlayerControlView.kt` | 8 | 清理注释死代码；空实现补充语义化 no-op；统一截图按钮显隐处理 | 0 | -8 |
| 5 | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/repository/BilibiliRepositoryCore.kt` | 7 | 删除无用 import/私有方法；移除无效 Elvis 操作 | 4 | -3 |
| 6 | `player_component/src/main/java/com/xyoye/player/controller/video/InterControllerView.kt` | 7 | 为默认回调补充语义化 no-op 注释，消除空实现告警 | 0 | -7 |
| 7 | `core_network_component/src/test/java/com/xyoye/common_component/network/open115/Open115ModelsMoshiTest.kt` | 6 | 测试命名改为 camelCase；抽取重复 JSON 常量 | 0 | -6 |
| 8 | `player_component/src/test/java/com/xyoye/player_component/media3/Media3PlayerDelegateTest.kt` | 6 | 测试命名改为 camelCase；去除无效中间赋值 | 0 | -6 |
| 9 | `core_storage_component/src/test/java/com/xyoye/common_component/storage/cloud115/auth/Cloud115TokenParserTest.kt` | 6 | 测试命名改为 camelCase | 0 | -6 |
| 10 | `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/Cloud115Storage.kt` | 6 | 使用 `check/require` 改造守卫；抽取关键词/标识符校验函数 | 0 | -6 |

## 汇总

- Baseline（Top10 合计）：`95`
- 预估 Current（Top10 合计）：`4`
- 预估下降：`91`（`95.8%`）
- 是否达到 US2 目标（>=30%）：**是（预估）**

## 待 SonarCloud 复核项

1. `BilibiliRepositoryCore.kt` 中 4 个复杂度问题（`kotlin:S3776`）仍需在后续阶段继续治理。
2. 需使用新的分支/PR analysis key 复跑 `/quality/snapshots/compare`，将“预估”替换为“实测”。
