# 模块报告目录

本目录存放“逐模块排查报告”，每个 Gradle module 对应一个文件。

## 文件命名规则（Windows 兼容）

为避免 Windows 路径中 `:` 带来的问题，模块路径转换为文件名：

- 去掉开头 `:`
- 其余 `:` 替换为 `__`

示例：

- `:player_component` → `player_component.md`
- `:repository:danmaku` → `repository__danmaku.md`

## 生成方式

推荐使用初始化脚本生成骨架与状态表：

- `scripts/code_quality_audit/init_audit_docs.py`

脚本会创建/更新：

- `document/code_quality_audit/modules/*.md`（模块报告骨架）
- `document/code_quality_audit/global/module_status.md`（模块覆盖率/状态跟踪表）

