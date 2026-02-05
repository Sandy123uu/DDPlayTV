# 治理路线图（批次/依赖/风险）

- 日期：2026-02-04
- 输入：`document/code_quality_audit/global/summary.md` 的 `G-F####/G-T####`

> 目标：给出“可落地”的批次建议与依赖顺序，避免在缺少统一口径时贸然做跨模块重构导致回退。

## Batch 1：安全基线（P1 优先，先控风险再扩范围）

- `G-T0024`：统一 TLS 安全默认（移除默认 `$X.hostnameVerifier { _, _ -> true }`）
- `G-T0002`：下线/收敛 `UnsafeOkHttpClient`（release 默认禁用 trust-all）
- `G-T0007`：迁移/隔离 B 站固定凭证（构建期注入/配置隔离）
- `G-T0004`：替换/下线不安全加密实现（固定 IV/默认 key/MD5）
- `G-T0012`（合并项）：统一脱敏工具与默认策略（网络/播放器/缓存/下载）

## Batch 2：可观测性与稳定性收敛（减少策略漂移）

- `G-T0016`：收敛 Bugly 上报门面（`BuglyReporter`/`ErrorReportHelper`）
- `G-T0021`：清理 `printStackTrace()` 并补齐异常上报上下文（player）
- `G-T0061`：移除 `printStackTrace()` 并统一异常处理/脱敏（storage）

## Batch 3：跨模块复用与架构一致性（先抽象再迁移）

- `G-T0046`：抽取统一 OkHttpClientFactory（集中 timeout/拦截器链/安全策略）
- `G-T0062`：统一本地代理能力（`HttpPlayServer`/`VlcProxyServer`）
- `G-T0015`：TV 端 TabLayout+ViewPager2 焦点/按键策略收敛（避免多页漂移）
- `G-T0044`：收敛壳层 Fragment 装载流程（TV/移动端 ARouter show/hide 重复）

## Batch 4：第三方依赖治理（合规/可追溯）

- `G-T0003`（合并项）：为所有 repository wrapper AAR 补齐元信息（README/sha256/License/更新流程）

## 验证建议（ast-grep 快速确证）

- `hostnameVerifier` 默认禁用校验：使用 ast-grep 模式 `$X.hostnameVerifier { _, _ -> true }`（当前全仓 1 处；仅 `OkHttpTlsPolicy.UnsafeTrustAll` 内部使用，需显式 opt-in）。
- `UnsafeOkHttpClient.client` 使用点：模式 `UnsafeOkHttpClient.client`（当前全仓 1 处；已受 `@UnsafeTlsApi` 保护）。
- `printStackTrace()`：模式 `$X.printStackTrace()`（当前全仓约 82 处，建议先在 infra/storage/player 关键链路收敛）。
- Bugly `CrashReport.*` 直接调用：模式 `CrashReport.$METHOD($ARGS)`（当前至少 3 处，建议收敛门面后再扩展扫描）。
