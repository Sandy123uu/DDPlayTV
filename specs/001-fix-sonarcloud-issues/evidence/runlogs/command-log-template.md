# 执行日志模板（Gradle / Sonar / 回归）

- 记录日期：YYYY-MM-DD
- 执行人：
- 分支：`001-fix-sonarcloud-issues`
- 环境：本地 / CI（填写具体）

## 命令记录

| 序号 | 场景 | 命令 | 期望结果 | 实际结果 | 结论 |
|------|------|------|----------|----------|------|
| 1 | 单测 | `./gradlew testDebugUnitTest` | 所有相关单测通过 | 待填写 | PASS/FAIL |
| 2 | 静态检查 | `./gradlew lintDebug` | lint 无阻断错误 | 待填写 | PASS/FAIL |
| 3 | 覆盖率报告 | `./gradlew jacocoTestReport` | 生成 JaCoCo XML | 待填写 | PASS/FAIL |
| 4 | Sonar 分析 | `gh workflow run sonarcloud.yml` 或 CI 自动触发 | 质量门可获取结果 | 待填写 | PASS/FAIL |

## 关键日志尾部（必须粘贴）

### Gradle 日志结尾

```text
<粘贴命令输出最后 30-50 行，并明确包含 BUILD SUCCESSFUL 或 BUILD FAILED>
```

### Sonar 结果摘要

```text
Quality Gate: PASS/FAIL
new_coverage:
new_duplicated_lines_density:
new_security_hotspots_reviewed:
```

## 问题与处理

- 问题描述：
- 影响范围：
- 临时处置：
- 后续动作：

## 复核结论

- 复核人：
- 复核时间：
- 结论：通过 / 驳回（附理由）
