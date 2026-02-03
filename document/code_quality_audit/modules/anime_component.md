# 模块排查报告：:anime_component

- 模块：:anime_component
- 负责人：
- 日期：YYYY-MM-DD
- 范围：anime_component/

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`ANIME-F###`  
> - Task：`ANIME-T###`

## 1) 背景与职责

- 模块职责（做什么/不做什么）
- 关键入口/关键路径（可列 3~5 个关键符号）
- 依赖边界（与哪些模块交互，是否存在边界疑点）

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| ANIME-F001 | Duplication | ... | `path/to/File.kt` + `Foo#bar` | Unintentional | Unify | `:core_xxx_component` | High | Small | P1 | ... |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| ANIME-T001 | ANIME-F001 | ... | ... | ... | High | Small | P1 | ... | Draft |

## 5) 风险与回归关注点

- 行为回退风险（哪些场景最敏感）
- 回归成本（需要的账号/媒体文件/设备）

## 6) 备注（历史背景/待确认点）
