# 质量快照对比

- 生成时间：2026-02-08T07:37:30.151222+00:00
- 基线分析线：`07c4be1f-f37a-418a-926f-2a13a7a15f86`
- 当前分析线：`07c4be1f-f37a-418a-926f-2a13a7a15f86`
- 门禁结论：`FAIL`

## 指标对比

| 指标 | Baseline | Current | Delta |
|------|----------|---------|-------|
| 总问题数 | 978 | 978 | 0 |
| 高影响问题 | 291 | 291 | 0 |
| 漏洞数 | 3 | 3 | 0 |

## 阈值检查

| Metric | Comparator | Actual | Threshold | Result |
|--------|------------|--------|-----------|--------|
| `new_coverage` | `>=` | `0.0` | `80.0` | `FAIL` |
| `new_duplicated_lines_density` | `<=` | `3.5812895890855936` | `3.0` | `FAIL` |
| `new_security_hotspots_reviewed` | `>=` | `0.0` | `100.0` | `FAIL` |
| `vulnerabilities` | `<=` | `3` | `0` | `FAIL` |

## Top10 文件问题变化

| # | File | Baseline | Current | Delta |
|---|------|----------|---------|-------|
| 1 | `player_component/src/main/java/com/xyoye/player/wrapper/ControlWrapper.kt` | 26 | 26 | 0 |
| 2 | `storage_component/src/main/java/com/xyoye/storage_component/ui/activities/storage_file/StorageFileActivity.kt` | 12 | 12 | 0 |
| 3 | `user_component/src/main/assets/bilibili/geetest_voucher.html` | 11 | 11 | 0 |
| 4 | `player_component/src/main/java/com/xyoye/player/controller/video/PlayerControlView.kt` | 8 | 8 | 0 |
| 5 | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/repository/BilibiliRepositoryCore.kt` | 7 | 7 | 0 |
| 6 | `player_component/src/main/java/com/xyoye/player/controller/video/InterControllerView.kt` | 7 | 7 | 0 |
| 7 | `core_network_component/src/test/java/com/xyoye/common_component/network/open115/Open115ModelsMoshiTest.kt` | 6 | 6 | 0 |
| 8 | `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/Cloud115Storage.kt` | 6 | 6 | 0 |
| 9 | `core_storage_component/src/test/java/com/xyoye/common_component/storage/cloud115/auth/Cloud115TokenParserTest.kt` | 6 | 6 | 0 |
| 10 | `player_component/src/test/java/com/xyoye/player_component/media3/Media3PlayerDelegateTest.kt` | 6 | 6 | 0 |

## US1 本地结论补充

- 本次对比使用同一基线分析线（`baselineAnalysisKey == targetAnalysisKey`），指标尚未反映最新代码提交。
- US1 的代码修复与定向测试已完成，待下一次 SonarCloud 分析后复核 `vulnerabilities` 与 `hotspots reviewed` 指标。
- 复核入口：`/quality/snapshots/compare`（目标分析线应替换为本次提交对应的分支/PR analysis key）。
