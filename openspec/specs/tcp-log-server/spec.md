# tcp-log-server Specification

## Purpose
TBD - created by archiving change add-tcp-log-server. Update Purpose after archive.
## Requirements
### Requirement: 开发者设置可开启/关闭 TCP 日志服务

系统 SHALL 在“开发者设置”中提供 TCP 日志服务开关，默认关闭；开启后应用在默认端口 `17010` 监听 TCP 连接，关闭后停止监听并释放端口。

#### Scenario: 默认关闭时不监听端口

- **GIVEN** 用户从未开启过 TCP 日志服务
- **WHEN** 应用启动并进入主界面
- **THEN** 系统不应监听 `17010` 端口

#### Scenario: 开启后开始监听并可连接

- **GIVEN** 用户进入“开发者设置”
- **WHEN** 用户开启 TCP 日志服务开关
- **THEN** 系统开始监听 `17010` 端口
- **AND** 客户端可以通过 TCP 连接到该端口

### Requirement: 日志对所有已连接客户端实时广播

系统 SHALL 将应用日志实时广播给所有连接到 TCP 日志服务的客户端；任一客户端断开或写入失败不得影响其他客户端继续接收。

#### Scenario: 多客户端同时接收同一批日志

- **GIVEN** 两个客户端均已连接到 TCP 日志服务
- **WHEN** 应用产生新的日志事件
- **THEN** 两个客户端都能收到对应日志内容

#### Scenario: 单客户端断开不影响其他客户端

- **GIVEN** 两个客户端均已连接到 TCP 日志服务
- **AND** 其中一个客户端断开连接
- **WHEN** 应用继续产生新的日志事件
- **THEN** 仍连接的客户端持续收到日志内容

### Requirement: TCP 输出格式与 log.txt 保持一致

系统 SHALL 以 UTF-8 文本逐行输出日志，每条日志以 `\n` 结尾；日志内容优先复用 `LogFormatter.format(LogEvent)` 的单行格式，使其与 `log.txt` 输出保持一致，便于脚本解析。

#### Scenario: 客户端收到的日志为单行且包含关键字段

- **GIVEN** 客户端已连接到 TCP 日志服务
- **WHEN** 应用产生一条日志事件
- **THEN** 客户端收到一行以 `\n` 结尾的日志文本
- **AND** 该行包含 `time=`、`level=`、`module=` 等关键字段（与 `log.txt` 一致）

### Requirement: 网络 IO 不得阻塞主线程与日志写入线程

系统 MUST 不在主线程执行 TCP server 的监听/写入等阻塞式 IO；并避免慢客户端导致日志写入线程被阻塞（必要时可丢弃日志或断开慢连接）。

#### Scenario: 开启 TCP 日志服务不影响 UI 响应

- **GIVEN** 用户开启 TCP 日志服务
- **WHEN** 应用持续产生日志且存在网络客户端连接
- **THEN** UI 线程仍保持可交互（返回/播放控制等操作可及时响应）

