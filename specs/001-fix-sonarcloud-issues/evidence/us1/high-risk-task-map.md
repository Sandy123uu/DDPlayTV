# US1 高风险问题映射台账

- 记录时间：2026-02-08
- 基线分析线：`07c4be1f-f37a-418a-926f-2a13a7a15f86`
- 映射来源：`/quality/issues?analysisKey=07c4be1f-f37a-418a-926f-2a13a7a15f86&impactSeverity=HIGH` 与基线热点清单

## Issue -> Task 映射

| Sonar Key | 类型 | 基线状态 | 目标处置 | 对应任务 | 代码改动 | 测试证据 | 当前状态 |
|---|---|---|---|---|---|---|---|
| `AZwnn9Cfsj5rjNHRFbU8` | HIGH Impact（复杂度） | OPEN | FIX | `US1-T020-A` | `anime_component/src/main/java/com/xyoye/anime_component/ui/dialog/date_picker/NumberPicker.java` | `anime_component/src/test/java/com/xyoye/anime_component/ui/dialog/date_picker/NumberPickerBehaviorTest.kt` | FIXED_PENDING_ANALYSIS |
| `AZwnn9Cfsj5rjNHRFbVW` | HIGH Impact（复杂度） | OPEN | FIX | `US1-T020-B` | `anime_component/src/main/java/com/xyoye/anime_component/ui/dialog/date_picker/NumberPicker.java` | `anime_component/src/test/java/com/xyoye/anime_component/ui/dialog/date_picker/NumberPickerBehaviorTest.kt` | FIXED_PENDING_ANALYSIS |
| `AZw4PUuMpBg_nGQ6xCmp` | HIGH Hotspot（CREDENTIAL） | TO_REVIEW | FIX | `US1-T018` | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/app/BilibiliTvClient.kt` | `bilibili_component/src/test/java/com/xyoye/common_component/bilibili/app/BilibiliTvClientSecurityTest.kt` | FIXED_PENDING_ANALYSIS |
| `AZwnn8vhsj5rjNHRFbPn` | HIGH Hotspot（AUTH） | TO_REVIEW | FIX | `US1-T019` | `core_network_component/src/main/java/com/xyoye/common_component/network/config/Api.kt` | `core_network_component/src/test/java/com/xyoye/common_component/network/config/ApiSecurityConfigTest.kt` | FIXED_PENDING_ANALYSIS |

## `/quality/issues/{issueKey}/tasks` 对齐说明

| issueKey | 请求摘要 | 备注 |
|---|---|---|
| `AZwnn9Cfsj5rjNHRFbU8` | `POST /quality/issues/AZwnn9Cfsj5rjNHRFbU8/tasks`，`priority=P1,targetOutcome=FIX` | 对应 NumberPicker DPAD 分支拆分 |
| `AZwnn9Cfsj5rjNHRFbVW` | `POST /quality/issues/AZwnn9Cfsj5rjNHRFbVW/tasks`，`priority=P1,targetOutcome=FIX` | 对应无障碍 performAction 拆分 |
| `AZw4PUuMpBg_nGQ6xCmp` | `POST /quality/issues/AZw4PUuMpBg_nGQ6xCmp/tasks`，`priority=P1,targetOutcome=FIX` | Hotspot 键按流程作为 issueKey 占位映射 |
| `AZwnn8vhsj5rjNHRFbPn` | `POST /quality/issues/AZwnn8vhsj5rjNHRFbPn/tasks`，`priority=P1,targetOutcome=FIX` | Hotspot 键按流程作为 issueKey 占位映射 |

> 说明：本地阶段仅完成代码与测试闭环；最终 `CLOSED/REVIEWED_FIXED` 需以 SonarCloud 新分析线复核为准。
