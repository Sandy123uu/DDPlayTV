# Phase 1 Quickstart: SonarCloud 报告问题修复优化

## 1. 前置条件

- 当前分支为 `001-fix-sonarcloud-issues`。
- 已存在基线报告：`.sonarcloud-report/sonarcloud-report.json` 与 `.sonarcloud-report/sonarcloud-report.md`。
- 可本地执行 Gradle（JDK/Android SDK 环境可用）。
- 本轮仅修复一方模块，`repository/*` 仅做豁免说明。

## 2. 建立修复清单（P0 -> P2）

1. 从基线报告提取三类优先对象：
   - 全部 `VULNERABILITY`（必须清零）
   - 全部 `Security Hotspot`（必须 100% 评审）
   - 问题最多前 10 文件（目标降幅 >= 30%）
2. 将问题映射到 `RemediationTaskItem`，记录优先级、目标处置、负责人、复核人。
3. 对 `repository/*` 或外部引入源码问题建立 `ExemptionRecord`，不得直接做深度改造。

## 3. 实施修复与增测

1. 按模块小步提交修复，优先降低高风险与高影响问题。
2. 每个改动点至少补 1 条自动化测试（JVM 单测优先，必要时补 instrumentation）。
3. 触及 TV 交互时，按 DPAD 规范验证焦点可达、可见、可返回。

建议本地最小验证命令：

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
```

如改动涉及播放/焦点等 Android 行为，再追加：

```bash
./gradlew connectedDebugAndroidTest
```

## 4. 质量门验证（分支/PR 分析线）

1. 提交修复后触发 SonarCloud 分析（分支或 PR）。
2. 在 SonarCloud UI 完成所有 hotspot 评审，并处理高风险热点为修复完成。
3. 生成对比快照（基线 vs 当前分析线），确认关键指标：
   - `new_coverage >= 80`
   - `new_duplicated_lines_density <= 3`
   - `new_security_hotspots_reviewed = 100`
   - 漏洞数 = 0

## 5. 验收与归档

1. 输出修复前后对比结论（总问题、高影响、漏洞、热点、前 10 文件变化）。
2. 确认所有遗留项均有处置结论（延期/接受风险/误报）及理由、复核信息。
3. 仅当质量门状态为 `PASS` 且测试全部通过时，进入任务阶段收尾。

