# 模块排查报告：:repository:danmaku

- 模块：:repository:danmaku
- 负责人：AI（Codex）
- 日期：2026-02-04
- 范围：repository/danmaku/

> ID 规则：使用 `document/code_quality_audit/config/module_id_prefixes.yaml` 中的 PREFIX  
> - Finding：`REPO_DANMAKU-F###`  
> - Task：`REPO_DANMAKU-T###`

## 1) 背景与职责

- 模块职责（做什么）
  - 以 Gradle module 形式封装本地预编译 AAR：`repository/danmaku/DanmakuFlameMaster.aar`，供业务模块以 `project(":repository:danmaku")` 依赖方式接入。
  - 提供第三方弹幕渲染能力（`master.flame.danmaku.*`），由 `:player_component` 在播放页渲染链路中使用。
- 模块职责（不做什么）
  - 不承载任何业务代码；除 `build.gradle.kts` 与 AAR 产物外不应新增 Kotlin/Java 实现。
  - 不应被除 `:player_component` 之外的业务模块直接依赖（避免将第三方库类型扩散到更多模块）。
- 关键入口/关键路径（示例）
  - AAR 封装：`repository/danmaku/build.gradle.kts` + `artifacts.add("default", file("DanmakuFlameMaster.aar"))`
  - 二进制产物：`repository/danmaku/DanmakuFlameMaster.aar`
  - 依赖声明（消费方）：`player_component/build.gradle.kts` + `dependencies { implementation(project(":repository:danmaku")) }`
  - 典型使用点：`player_component/src/main/java/com/xyoye/player/controller/danmu/DanmuView.kt` + `DanmuView`（依赖 `DanmakuContext.create()`；渲染主入口）
  - 典型解析点：`player_component/src/main/java/com/xyoye/danmaku/BiliDanmakuParser.java` + `BiliDanmakuParser`（`public class ... extends BaseDanmakuParser`）
- 依赖边界
  - 对外（被依赖）：仅 `:player_component`（见 `player_component/build.gradle.kts`）。
  - 对内（依赖）：无（本模块仅提供 AAR 工件，不应再依赖其它工程模块）。
  - 边界疑点：AAR 作为二进制“黑盒”，若缺少版本/来源信息，会放大升级与安全审计成本（见 Findings）。

## 2) 排查维度与方法

- 维度：重复实现 / 冗余 / 复用机会 / 架构一致性风险 / 安全与隐私风险（明显高风险项）
- 方法：`rg` 先行 + 必要时 ast-grep 确证；结合门禁与依赖治理文档复核
  - `rg`：定位 `master.flame.danmaku` 的引用分布，确认依赖扩散范围  
    - `rg "master\\.flame\\.danmaku" -n player_component/src/main/java`
  - ast-grep：确证关键语法形态（避免纯文本误判）  
    - Java：`public class $NAME extends BaseDanmakuParser`（定位 `BiliDanmakuParser`）  
    - Kotlin：`DanmakuContext.create()`（定位 `DanmuView` 的核心入口）

## 3) Findings（发现列表）

> 证据最低要求：文件路径 + 关键符号名（类/方法/函数）  
> 多实现必须填写：有意/无意 + 保留/统一/废弃结论与理由

| ID | 类别 | 标题 | 证据（至少路径+符号） | 多实现（有意/无意） | 结论（保留/统一/废弃） | 落点建议 | Impact | Effort | P | 风险/依赖 |
|---|---|---|---|---|---|---|---|---|---|---|
| REPO_DANMAKU-F001 | ArchitectureRisk | 预编译 AAR 缺少来源/版本/License 等元信息，升级与合规审计不可追溯 | `repository/danmaku/DanmakuFlameMaster.aar`（二进制）；`repository/danmaku/build.gradle.kts` + `artifacts.add("default", file("DanmakuFlameMaster.aar"))` | N/A | Unify | `repository/danmaku/`（补齐元信息）+ 全仓统一规范（建议由 `buildSrc` 或文档约束） | Medium | Small | P1 | 需要确认 AAR 上游来源、版本号、授权协议；否则难以在安全事件/升级时快速定位影响 |
| REPO_DANMAKU-F002 | Redundancy | 多个 wrapper 模块重复使用“手写 default artifact”封装方式，构建脚本口径易漂移 | `repository/danmaku/build.gradle.kts` + `configurations.maybeCreate("default")`；对比：`repository/video_cache/build.gradle.kts` + 同类写法 | Intentional | Unify | `buildSrc` 提供统一 prebuilt-aar 约定插件/脚本，减少每个 wrapper 重复配置 | Medium | Medium | P2 | 需要验证对现有依赖解析无破坏；注意 Gradle/AGP 升级兼容性与配置缓存影响 |

## 4) Refactor Tasks（治理任务）

| ID | 关联 Finding | 目标 | 范围 | 验收标准 | Impact | Effort | P | 负责人 | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| REPO_DANMAKU-T001 | REPO_DANMAKU-F001 | 为 AAR 增加可追溯元信息（来源/版本/License/校验和/更新流程） | 新增 `repository/danmaku/README.md`（中文）；可选新增 `repository/danmaku/LICENSE` 或在 README 中明确 License 与引用位置 | 1) README 明确：上游项目/下载地址、版本号、License、AAR SHA256、更新步骤；2) 任意人可按文档复现升级；3) 不影响现有依赖解析 | Medium | Small | P1 | AI（Codex） | Done |
| REPO_DANMAKU-T002 | REPO_DANMAKU-F002 | 统一 prebuilt AAR wrapper 的 Gradle 封装方式，减少脚本重复与漂移 | 在 `buildSrc` 提供约定插件（`setup.prebuilt-aar`），并迁移 `repository/*` wrapper 的 `build.gradle.kts` 使用统一写法（覆盖 6 个 AAR wrapper） | 1) wrapper 模块仍可被正常依赖（`assembleDebug` 可通过）；2) wrapper 脚本结构一致、可读；3) 不引入额外 module 依赖；4) `./gradlew verifyModuleDependencies` 通过 | Medium | Medium | P2 | AI（Codex） | Done |

## 5) 风险与回归关注点

- 行为回退风险：升级/替换 AAR 可能影响弹幕绘制（字体/描边/透明度/性能）、解析兼容性（B 站特殊弹幕）、以及 TV 焦点/弹幕开关交互。
- 回归成本：需要至少覆盖 1) 本地视频 + 本地弹幕；2) B 站弹幕下载 + 渲染；3) TV/移动端渲染性能与同步准确性（seek/暂停/倍速）。

## 6) 备注（历史背景/待确认点）

- 当前未在仓库内看到该 AAR 的上游版本与变更记录，建议优先补齐可追溯元信息后再做升级评估。
