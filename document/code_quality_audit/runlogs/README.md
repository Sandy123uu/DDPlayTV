# 运行记录（Runlogs）

本目录用于保存“可复现的执行记录”，包括：

- 初始化脚本输出（例如模块骨架/状态表生成）
- Gradle 门禁执行记录（必须确认输出末尾 `BUILD SUCCESSFUL` 或 `BUILD FAILED`）
- **过滤后的**日志片段（如 `adb logcat`），用于支撑 Finding 的证据

## 命名建议

- `init_audit_docs.md`：初始化脚本输出
- `gates_YYYYMMDD.md`：门禁执行记录（或使用更具体的名称，例如 `foundation_gates.md` / `final_gates.md`）
- `quickstart_smoke.md`：按 quickstart 走通的冒烟记录
- `validate_audit_docs.md`：文档一致性校验输出（覆盖率/必填字段/ID 基础校验）

## adb logcat 过滤规范（强制）

`adb logcat` 内容可能包含系统与其他应用输出，**禁止全文倾倒**。必须提供明确过滤条件（tag / pid / 关键字）。

推荐方式：

- 按 pid（优先，最干净）：
  - `adb logcat --pid $(adb shell pidof -s com.okamihoro.ddplaytv)`
- 按 tag（按项目实际 tag 调整）：
  - `adb logcat -s DDPlayTV Player Subtitle`
- 二次过滤（只截取必要片段）：
  - `adb logcat --pid <pid> | rg "关键字"`

记录要求：

- 只保留与证据直接相关的 20~200 行片段（按实际需要）
- 在 runlog 中标注过滤条件与触发步骤，避免后续无法复现
