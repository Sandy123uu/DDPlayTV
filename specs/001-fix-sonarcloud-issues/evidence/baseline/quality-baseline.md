# 质量基线快照（Phase 2）

- 导出时间：2026-02-08T06:56:17.681700+00:00
- 报告生成时间：2026-02-08T05:49:19.682235+00:00
- 项目：`okami-horo_DDPlayTV`
- 分支：``
- 基线分析线：`07c4be1f-f37a-418a-926f-2a13a7a15f86`

## 数据完整性

- Issues：500 / 978
- Hotspots：52 / 52
- Issues 是否截断：True
- Hotspots 是否截断：False

## 关键指标概览

- Quality Gate：`ERROR`
- 总问题数：978
- 漏洞总数：3
- 高影响问题总数：291
- Hotspot 总数：52
- new_coverage：0.0
- new_duplicated_lines_density：3.5812895890855936
- new_security_hotspots_reviewed：0.0

## 质量门条件

| Metric | Status | Actual | Threshold |
|--------|--------|--------|-----------|
| `new_reliability_rating` | `OK` | `1` | `1` |
| `new_security_rating` | `OK` | `1` | `1` |
| `new_maintainability_rating` | `OK` | `1` | `1` |
| `new_coverage` | `ERROR` | `0.0` | `80` |
| `new_duplicated_lines_density` | `ERROR` | `3.6` | `3` |
| `new_security_hotspots_reviewed` | `ERROR` | `0.0` | `100` |

## Top 问题文件

| Rank | File | Issues |
|------|------|--------|
| 1 | `player_component/src/main/java/com/xyoye/player/wrapper/ControlWrapper.kt` | 26 |
| 2 | `storage_component/src/main/java/com/xyoye/storage_component/ui/activities/storage_file/StorageFileActivity.kt` | 12 |
| 3 | `user_component/src/main/assets/bilibili/geetest_voucher.html` | 11 |
| 4 | `player_component/src/main/java/com/xyoye/player/controller/video/PlayerControlView.kt` | 8 |
| 5 | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/repository/BilibiliRepositoryCore.kt` | 7 |
| 6 | `player_component/src/main/java/com/xyoye/player/controller/video/InterControllerView.kt` | 7 |
| 7 | `core_network_component/src/test/java/com/xyoye/common_component/network/open115/Open115ModelsMoshiTest.kt` | 6 |
| 8 | `player_component/src/test/java/com/xyoye/player_component/media3/Media3PlayerDelegateTest.kt` | 6 |
| 9 | `core_storage_component/src/test/java/com/xyoye/common_component/storage/cloud115/auth/Cloud115TokenParserTest.kt` | 6 |
| 10 | `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/Cloud115Storage.kt` | 6 |

## 高风险清单

### 漏洞问题（0）

| # | Key | Rule | Severity | Path:Line |
|---|-----|------|----------|-----------|
| 1 | (none) | - | - | - |

### 高影响问题（抽样 154）

| # | Key | Rule | Severity | Path:Line |
|---|-----|------|----------|-----------|
| 1 | `AZwnn9Cfsj5rjNHRFbU8` | `java:S3776` | HIGH | `anime_component/src/main/java/com/xyoye/anime_component/ui/dialog/date_picker/NumberPicker.java:937` |
| 2 | `AZwnn9Cfsj5rjNHRFbVW` | `java:S3776` | HIGH | `anime_component/src/main/java/com/xyoye/anime_component/ui/dialog/date_picker/NumberPicker.java:2303` |
| 3 | `AZwnn9CAsj5rjNHRFbUk` | `java:S115` | HIGH | `anime_component/src/main/java/com/xyoye/anime_component/ui/dialog/date_picker/Scroller.java:99` |
| 4 | `AZw7WyrSsxwkjR-r7sSf` | `kotlin:S3776` | HIGH | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/danmaku/BilibiliDanmakuDownloader.kt:12` |
| 5 | `AZwnn84jsj5rjNHRFbR4` | `kotlin:S3776` | HIGH | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/live/danmaku/LiveDanmakuSocketClient.kt:81` |
| 6 | `AZwnn85osj5rjNHRFbSH` | `kotlin:S3776` | HIGH | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/playback/BilibiliPlaybackAddon.kt:128` |
| 7 | `AZw4PUrvpBg_nGQ6xCml` | `kotlin:S3776` | HIGH | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/repository/BilibiliRepositoryCore.kt:650` |
| 8 | `AZw4PUrvpBg_nGQ6xCmm` | `kotlin:S3776` | HIGH | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/repository/BilibiliRepositoryCore.kt:914` |
| 9 | `AZw4PUrvpBg_nGQ6xCmn` | `kotlin:S3776` | HIGH | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/repository/BilibiliRepositoryCore.kt:1016` |
| 10 | `AZw4PUrvpBg_nGQ6xCmo` | `kotlin:S3776` | HIGH | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/repository/BilibiliRepositoryCore.kt:1409` |
| 11 | `AZwoFAepY8ZohlEMpmY3` | `kotlin:S1192` | HIGH | `bilibili_component/src/test/java/com/xyoye/common_component/bilibili/cdn/BilibiliCdnStrategyTest.kt:12` |
| 12 | `AZwoFAecY8ZohlEMpmY1` | `kotlin:S1192` | HIGH | `bilibili_component/src/test/java/com/xyoye/common_component/bilibili/mpd/BilibiliMpdGeneratorTest.kt:27` |
| 13 | `AZwoFAecY8ZohlEMpmY2` | `kotlin:S1192` | HIGH | `bilibili_component/src/test/java/com/xyoye/common_component/bilibili/mpd/BilibiliMpdGeneratorTest.kt:28` |
| 14 | `AZw4PU0opBg_nGQ6xCmy` | `kotlin:S3776` | HIGH | `core_database_component/src/test/java/com/xyoye/common_component/database/migration/RoomSchemaMigrationGateTest.kt:81` |
| 15 | `AZwnn86esj5rjNHRFbSY` | `kotlin:S3776` | HIGH | `core_log_component/src/main/java/com/xyoye/common_component/log/LogFileManager.kt:347` |
| 16 | `AZw4PUxlpBg_nGQ6xCmu` | `kotlin:S1192` | HIGH | `core_log_component/src/main/java/com/xyoye/common_component/log/privacy/SensitiveDataSanitizer.kt:122` |
| 17 | `AZw4PUyopBg_nGQ6xCmv` | `kotlin:S3776` | HIGH | `core_log_component/src/main/java/com/xyoye/common_component/utils/ErrorReportHelper.kt:29` |
| 18 | `AZw4PUv9pBg_nGQ6xCmt` | `kotlin:S1192` | HIGH | `core_log_component/src/test/java/com/xyoye/common_component/log/privacy/SensitiveDataSanitizerTest.kt:43` |
| 19 | `AZwoFAZOY8ZohlEMpmYo` | `kotlin:S1192` | HIGH | `core_log_component/src/test/java/com/xyoye/common_component/log/tcp/TcpLogServerTest.kt:20` |
| 20 | `AZwnn8uksj5rjNHRFbPY` | `kotlin:S3776` | HIGH | `core_network_component/src/main/java/com/xyoye/common_component/network/helper/LoggerInterceptor.kt:81` |
| 21 | `AZwnn8vJsj5rjNHRFbPh` | `kotlin:S1192` | HIGH | `core_network_component/src/main/java/com/xyoye/common_component/network/request/NetworkException.kt:50` |
| 22 | `AZwnn8vpsj5rjNHRFbPo` | `kotlin:S3776` | HIGH | `core_network_component/src/main/java/com/xyoye/common_component/utils/AuthenticationHelper.kt:50` |
| 23 | `AZwoFACRY8ZohlEMpmXy` | `kotlin:S1192` | HIGH | `core_network_component/src/test/java/com/xyoye/common_component/network/open115/Open115ModelsMoshiTest.kt:17` |
| 24 | `AZwnn80rsj5rjNHRFbRY` | `kotlin:S1186` | HIGH | `core_storage_component/src/main/java/com/xyoye/common_component/source/base/BaseVideoSource.kt:21` |
| 25 | `AZwnn80rsj5rjNHRFbRZ` | `kotlin:S1186` | HIGH | `core_storage_component/src/main/java/com/xyoye/common_component/source/base/BaseVideoSource.kt:26` |
| 26 | `AZwnn80rsj5rjNHRFbRa` | `kotlin:S1186` | HIGH | `core_storage_component/src/main/java/com/xyoye/common_component/source/base/BaseVideoSource.kt:31` |
| 27 | `AZw4PUcEpBg_nGQ6xCmK` | `kotlin:S3776` | HIGH | `core_storage_component/src/main/java/com/xyoye/common_component/storage/baidupan/auth/BaiduPanTokenManager.kt:40` |
| 28 | `AZwnn8yLsj5rjNHRFbQw` | `kotlin:S1192` | HIGH | `core_storage_component/src/main/java/com/xyoye/common_component/storage/baidupan/auth/BaiduPanTokenManager.kt:53` |
| 29 | `AZwoFALuY8ZohlEMpmYB` | `kotlin:S3776` | HIGH | `core_storage_component/src/main/java/com/xyoye/common_component/storage/cloud115/auth/Cloud115TokenParser.kt:11` |
| 30 | `AZwnn8xzsj5rjNHRFbQr` | `kotlin:S3776` | HIGH | `core_storage_component/src/main/java/com/xyoye/common_component/storage/file/helper/HttpPlayServer.kt:134` |

### 高风险热点（2）

| # | Key | Rule | Probability | Status | Path:Line |
|---|-----|------|-------------|--------|-----------|
| 1 | `AZw4PUuMpBg_nGQ6xCmp` | `kotlin:S6418` | HIGH | TO_REVIEW | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/app/BilibiliTvClient.kt:13` |
| 2 | `AZwnn8vhsj5rjNHRFbPn` | `kotlin:S6418` | HIGH | TO_REVIEW | `core_network_component/src/main/java/com/xyoye/common_component/network/config/Api.kt:21` |

> 注：若 `issuesFetched < issuesTotal`，高影响问题清单仅代表已抓取样本，最终验收以分析线完整数据为准。
