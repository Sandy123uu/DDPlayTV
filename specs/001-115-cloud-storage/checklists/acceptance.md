# 115 Cloud 存储源验收记录（P1）

> 注意：请勿在本文档/截图/日志中粘贴完整 Cookie（UID/CID/SEID/KID）。如需留存日志，务必先做脱敏（仅保留长度/前后若干位或使用 `<redacted>`）。

## 基本信息

- 日期：2026-02-01
- 分支：`001-115-cloud-storage`
- 提交：未提交（当前 HEAD：`90094a31a`）
- 设备/系统：Windows 11 + WSL2（仅构建门禁）
- 构建类型：Debug / Release

## 构建门禁（已执行）

- `./gradlew verifyModuleDependencies`：`BUILD SUCCESSFUL`
- `./gradlew lint`：`BUILD SUCCESSFUL`
- `./gradlew assembleDebug`：`BUILD SUCCESSFUL`

## 手动验收用例（待执行）

### P1：挂载 115 Cloud 并播放视频

- 记录：未验收（待补充）
- 建议留存：
  - 新增存储源 → 展示二维码截图
  - 状态轮询（等待/已扫码/已确认）截图
  - 根目录列表截图（含目录/文件区分）
  - 进入子目录与返回截图
  - 同一视频文件在不同播放内核（Media3/mpv/VLC）下的播放成功截图或关键日志（可选）
  - 刷新/排序/搜索（含清空搜索恢复上下文）截图（可选）
  - 授权失效时提示与重新授权入口截图（可选）
