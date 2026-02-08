# US1 Hotspot 评审与处置日志

- 记录时间：2026-02-08
- 契约端点：`POST /quality/hotspots/{hotspotKey}/review`
- 当前阶段：本地修复 + 单测验证，待 SonarCloud 分析后执行线上评审提交

| hotspotKey | 风险级别 | 基线状态 | 处置动作 | 评审结论（预期） | 代码证据 | 测试证据 |
|---|---|---|---|---|---|---|
| `AZw4PUuMpBg_nGQ6xCmp` | HIGH | TO_REVIEW | FIX | REVIEWED_FIXED | `bilibili_component/src/main/java/com/xyoye/common_component/bilibili/app/BilibiliTvClient.kt`（移除硬编码凭据语义，改为运行时凭据提供） | `bilibili_component/src/test/java/com/xyoye/common_component/bilibili/app/BilibiliTvClientSecurityTest.kt` |
| `AZwnn8vhsj5rjNHRFbPn` | HIGH | TO_REVIEW | FIX | REVIEWED_FIXED | `core_network_component/src/main/java/com/xyoye/common_component/network/config/Api.kt`（敏感端点统一别名与 HTTPS 校验） | `core_network_component/src/test/java/com/xyoye/common_component/network/config/ApiSecurityConfigTest.kt` |

## 评审请求体样例（与契约对齐）

### `AZw4PUuMpBg_nGQ6xCmp`

```json
{
  "issueKey": "AZw4PUuMpBg_nGQ6xCmp",
  "riskLevel": "HIGH",
  "disposition": "FIX",
  "rationale": "凭据仅从运行时存储获取，缺失时显式失败；不再暴露硬编码密钥语义。",
  "reviewer": "quality-owner"
}
```

### `AZwnn8vhsj5rjNHRFbPn`

```json
{
  "issueKey": "AZwnn8vhsj5rjNHRFbPn",
  "riskLevel": "HIGH",
  "disposition": "FIX",
  "rationale": "将敏感端点命名去敏并强制 HTTPS 校验，避免 AUTH 误判与配置漂移。",
  "reviewer": "quality-owner"
}
```

> 注意：按数据模型规则，高风险热点仅允许 `disposition=FIX`，不得接受风险。
