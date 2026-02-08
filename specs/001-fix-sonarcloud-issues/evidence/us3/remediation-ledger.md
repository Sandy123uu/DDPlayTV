# US3 审计总台账（问题 -> 任务 -> 证据）

- 生成时间：2026-02-08T08:41:21.177439+00:00
- 基线分析线：`07c4be1f-f37a-418a-926f-2a13a7a15f86`
- 台账文件：`specs/001-fix-sonarcloud-issues/evidence/tracking/remediation_tasks.csv`
- 校验结果：`specs/001-fix-sonarcloud-issues/evidence/us3/remediation-ledger-validation.json`（errors=0，warnings=0）
- 处置导出：`specs/001-fix-sonarcloud-issues/evidence/us3/issue-dispositions.json`（count=5）

## 汇总

- 任务总数：18
- FIX：13
- DEFER：2
- ACCEPT_RISK：1
- FALSE_POSITIVE：1
- EXEMPT：1

## Issue -> Task -> Evidence

| # | issueKey | taskId | Outcome | TaskStatus | sourceScope | filePath | 证据 |
|---|----------|--------|---------|------------|-------------|----------|------|
| 1 | `AZw4PUuMpBg_nGQ6xCmp` | `US1-T018` | `FIX` | `CLOSED` | `FIRST_PARTY` | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/app/BilibiliTvClient.kt` | `https://sonarcloud.io/project/security_hotspots?id=okami-horo_DDPlayTV&hotspots=AZw4PUuMpBg_nGQ6xCmp`<br>`commit:us1-t018` |
| 2 | `AZwnn8vhsj5rjNHRFbPn` | `US1-T019` | `FIX` | `CLOSED` | `FIRST_PARTY` | `core_network_component/src/main/java/com/xyoye/common_component/network/config/Api.kt` | `https://sonarcloud.io/project/security_hotspots?id=okami-horo_DDPlayTV&hotspots=AZwnn8vhsj5rjNHRFbPn`<br>`commit:us1-t019` |
| 3 | `AZwnn9Cfsj5rjNHRFbU8` | `US1-T020-A` | `FIX` | `CLOSED` | `FIRST_PARTY` | `anime_component/src/main/java/com/xyoye/anime_component/ui/dialog/date_picker/NumberPicker.java` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZwnn9Cfsj5rjNHRFbU8`<br>`commit:us1-t020-a` |
| 4 | `AZwnn9Cfsj5rjNHRFbVW` | `US1-T020-B` | `FIX` | `CLOSED` | `FIRST_PARTY` | `anime_component/src/main/java/com/xyoye/anime_component/ui/dialog/date_picker/NumberPicker.java` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZwnn9Cfsj5rjNHRFbVW`<br>`commit:us1-t020-b` |
| 5 | `AZwnn8h2sj5rjNHRFbFi` | `US2-T029` | `FIX` | `CLOSED` | `FIRST_PARTY` | `player_component/src/main/java/com/xyoye/player/wrapper/ControlWrapper.kt` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZwnn8h2sj5rjNHRFbFi`<br>`commit:us2-t029` |
| 6 | `AZwoFAi8Y8ZohlEMpmY6` | `US2-T030` | `FIX` | `CLOSED` | `FIRST_PARTY` | `storage_component/src/main/java/com/xyoye/storage_component/ui/activities/storage_file/StorageFileActivity.kt` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZwoFAi8Y8ZohlEMpmY6`<br>`commit:us2-t030` |
| 7 | `AZwnn8dssj5rjNHRFbD7` | `US2-T031` | `FIX` | `CLOSED` | `FIRST_PARTY` | `player_component/src/main/java/com/xyoye/player/controller/video/PlayerControlView.kt` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZwnn8dssj5rjNHRFbD7`<br>`commit:us2-t031` |
| 8 | `AZwnn8eksj5rjNHRFbEb` | `US2-T032` | `FIX` | `CLOSED` | `FIRST_PARTY` | `player_component/src/main/java/com/xyoye/player/controller/video/InterControllerView.kt` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZwnn8eksj5rjNHRFbEb`<br>`commit:us2-t032` |
| 9 | `AZwoFACRY8ZohlEMpmXy` | `US2-T034` | `FIX` | `CLOSED` | `FIRST_PARTY` | `core_network_component/src/test/java/com/xyoye/common_component/network/open115/Open115ModelsMoshiTest.kt` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZwoFACRY8ZohlEMpmXy`<br>`commit:us2-t034` |
| 10 | `AZwoE_p3Y8ZohlEMpmW2` | `US2-T035` | `FIX` | `CLOSED` | `FIRST_PARTY` | `player_component/src/test/java/com/xyoye/player_component/media3/Media3PlayerDelegateTest.kt` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZwoE_p3Y8ZohlEMpmW2`<br>`commit:us2-t035` |
| 11 | `AZwoFARUY8ZohlEMpmYS` | `US2-T036` | `FIX` | `CLOSED` | `FIRST_PARTY` | `core_storage_component/src/test/java/com/xyoye/common_component/storage/cloud115/auth/Cloud115TokenParserTest.kt` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZwoFARUY8ZohlEMpmYS`<br>`commit:us2-t036` |
| 12 | `AZwoFANLY8ZohlEMpmYK` | `US2-T037` | `FIX` | `CLOSED` | `FIRST_PARTY` | `core_storage_component/src/main/java/com/xyoye/common_component/storage/impl/Cloud115Storage.kt` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZwoFANLY8ZohlEMpmYK`<br>`commit:us2-t037` |
| 13 | `AZwnn8qKsj5rjNHRFbOf` | `US2-T038` | `FIX` | `CLOSED` | `FIRST_PARTY` | `user_component/src/main/assets/bilibili/geetest_voucher.html` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZwnn8qKsj5rjNHRFbOf`<br>`commit:us2-t038` |
| 14 | `AZw4PUrvpBg_nGQ6xCml` | `US3-T046-A` | `DEFER` | `TRIAGED` | `FIRST_PARTY` | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/repository/BilibiliRepositoryCore.kt` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZw4PUrvpBg_nGQ6xCml` |
| 15 | `AZw4PUrvpBg_nGQ6xCmm` | `US3-T046-B` | `DEFER` | `TRIAGED` | `FIRST_PARTY` | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/repository/BilibiliRepositoryCore.kt` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZw4PUrvpBg_nGQ6xCmm` |
| 16 | `AZw4PUrvpBg_nGQ6xCmc` | `US3-T047-A` | `ACCEPT_RISK` | `CLOSED` | `FIRST_PARTY` | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/repository/BilibiliRepositoryCore.kt` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZw4PUrvpBg_nGQ6xCmc`<br>`commit:us3-risk-accept` |
| 17 | `AZwoFACRY8ZohlEMpmXt` | `US3-T047-B` | `FALSE_POSITIVE` | `READY_FOR_REVIEW` | `FIRST_PARTY` | `core_network_component/src/test/java/com/xyoye/common_component/network/open115/Open115ModelsMoshiTest.kt` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZwoFACRY8ZohlEMpmXt` |
| 18 | `TP-ISSUE-001` | `US3-T047-C` | `EXEMPT` | `CLOSED` | `THIRD_PARTY` | `repository/danmaku/DanmakuFlameMaster.aar` | `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=TP-ISSUE-001`<br>`commit:us3-exempt-001` |

## 抽样反查（按 issueKey）

- `AZw4PUuMpBg_nGQ6xCmp` -> `US1-T018` / `FIX` / `CLOSED` / `https://sonarcloud.io/project/security_hotspots?id=okami-horo_DDPlayTV&hotspots=AZw4PUuMpBg_nGQ6xCmp|commit:us1-t018`
- `AZw4PUrvpBg_nGQ6xCml` -> `US3-T046-A` / `DEFER` / `TRIAGED` / `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=AZw4PUrvpBg_nGQ6xCml`
- `TP-ISSUE-001` -> `US3-T047-C` / `EXEMPT` / `CLOSED` / `https://sonarcloud.io/project/issues?id=okami-horo_DDPlayTV&issues=TP-ISSUE-001|commit:us3-exempt-001`

> 结论：可按 issueKey 反查到目标处置、任务状态与证据链接；满足 US3 独立验收的可追踪性要求。
