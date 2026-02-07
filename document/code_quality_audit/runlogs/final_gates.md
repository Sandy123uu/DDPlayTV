# 质量与架构门禁执行记录（Phase 6 / Final Gates）

- 日期：2026-02-04
- 环境：Windows 11 + WSL2
- 目标：执行推荐的“质量 + 架构”门禁集合，并明确 `BUILD SUCCESSFUL / BUILD FAILED`

## 1) verifyArchitectureGovernance

命令：

```bash
./gradlew verifyArchitectureGovernance --console=plain
```

结果：`BUILD SUCCESSFUL`

关键输出（尾部）：

```text
> Task :app:lintDebug
> Task :app:lint
> Task :verifyArchitectureGovernance

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.9/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 27s
920 actionable tasks: 95 executed, 825 up-to-date
```

