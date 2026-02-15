# 日志系统维护指南（本仓库现状）

本文描述 **DDPlayTV（本仓库版本）** 当前的日志输出与抓取方式。

> 说明：本仓库已移除本地 `log.txt / log_old.txt` 的文件落盘能力；日志抓取以 **logcat** 与 **HTTP 日志服务（仅局域网）** 为主。

## 1. 输出通道

- **logcat（默认）**：始终输出，受“日志级别”设置影响
- **HTTP 日志服务（可选，仅局域网调试）**：提供日志页面 + 结构化接口（历史查询 + SSE 实时）

HTTP 日志服务仅用于局域网调试，具备如下安全约束：

- Token 鉴权（Header + URL 参数，Header 优先）
- 强制限制仅局域网来源可访问
- 对外输出默认脱敏（见：`document/architecture/log_redaction_policy.md`）

## 2. App 内开启步骤

1. 打开：`设置` → `开发者设置`
2. 开启：`HTTP 日志服务`
3. 页面会显示：
   - 访问地址（可能包含多网卡/IPv6）
   - Token（可重置；重置后旧 Token 立即失效）
   - 日志保留档位与当前占用（7/14/30 天；1/2/4GB）
   - 操作：重置凭证 / 清空历史日志

## 3. 浏览器访问（日志页面）

推荐在 URL 参数携带 Token：

```text
http://<设备IP>:17010/?token=<token>
```

## 4. 脚本/命令行访问（结构化接口）

### 4.1 获取状态

```bash
curl -H "Authorization: Bearer <token>" "http://<设备IP>:17010/api/v1/status"
```

### 4.2 查询最近日志（默认新到旧）

```bash
curl -H "Authorization: Bearer <token>" \
  "http://<设备IP>:17010/api/v1/logs?limit=200&source=BOTH"
```

### 4.3 实时日志（SSE）

```bash
curl -N -H "Authorization: Bearer <token>" \
  "http://<设备IP>:17010/api/v1/stream?source=BOTH"
```

## 5. ADB logcat 抓取（默认通道）

logcat 会包含系统与其他应用输出，建议按 tag 过滤。

- 本仓库的 logcat tag 形如：`DDLog-<模块>` 或 `DDLog-<模块>-<子 tag>`（Android tag 长度上限 23，超长会被截断）
- 示例（仅查看 DDLog 前缀）：

```bash
adb logcat | rg "DDLog-"
```

只看某个模块（以实际输出为准）：

```bash
adb logcat -s "DDLog-CORE"
```

> 注意：`adb logcat` 可能很吵；尽量使用 `-s` 或管道过滤，不要直接贴全量日志。

## 6. 脱敏与安全提示

- 默认会对 token/cookie/password/URL query 等敏感字段做脱敏处理
- 规则与扩展方式见：`document/architecture/log_redaction_policy.md`

即便已脱敏，仍建议：

- 仅在可信局域网内开启 HTTP 日志服务
- 排查完成后及时关闭 HTTP 服务或重置 Token

## 7. 常见问题

### 7.1 访问 401

Token 不正确或未携带（Header 与 URL 参数同时存在时以 Header 为准），或 Token 已在 TV 端被重置。

### 7.2 访问变慢或出现 429/503

日志服务以“播放体验优先”。当资源紧张或外部请求过于频繁时，会触发限流/降级，请稍后重试或减少客户端数量/请求频率。
