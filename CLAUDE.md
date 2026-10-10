# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.


# 项目研发工作流总规范
## 整体流程
需求 → 交互（同步输出测试用例） → 开发设计 → 开发编码 → 测试回归 → 版本发布
> 当出现bug、需求变更时，流程回流至【需求阶段】重新评估。

## 通用约束
1. 所有阶段产出文档，统一放在 ./docs/workflow/ 目录。
2. 每个阶段完成后，必须输出【阶段交付物清单】，确认完成，才允许进入下一阶段。
3. 阶段切换指令：`/skill 01-requirement`、`/skill 02-interaction`、`/skill 03-design`、`/skill 04-code`、`/skill 05-test`、`/skill 06-release`。
4. 测试用例必须在交互阶段产出，不能等到编码完成后才写。
5. 发现bug/需求变更，执行回流：触发【01-需求阶段】重新评估范围、优先级。

## 交付物固定要求
- 需求阶段：需求清单、用户故事、验收标准
- 交互阶段：原型说明、交互逻辑、**测试用例文档**
- 开发设计：架构设计、接口文档、数据库设计、风险评估
- 编码开发：代码实现、单元测试、注释
- 测试回归：bug清单、回归测试报告
- 版本发布：版本更新日志、打包清单、发布检查清单

## 工作流文档结构

`docs/workflow/` 是研发流水线各阶段的正式交付物（遵循 `.claude/rules/workflow-doc-rule.md`）。按阶段分六个目录：

| 目录 | 阶段 | 交付物 |
|------|------|--------|
| `1-requirement/` | 需求 | 需求清单、用户故事与验收标准、风险与依赖清单 |
| `2-interaction/` | 交互&测试用例 | 交互逻辑说明、测试用例文档 |
| `3-design/` | 开发设计 | 架构设计、接口文档、数据结构设计、风险清单 |
| `4-code/` | 编码自测 | 开发自测记录 |
| `5-test/` | 测试回归 | Bug 清单、回归测试报告 |
| `6-release/` | 版本发布 | changelog、发布检查清单、版本归档、迭代复盘 |

编写规范（详见 `.claude/rules/workflow-doc-rule.md`）：

- 每份文档开头必写：迭代版本、文档创建人、创建时间、关联需求 ID。
- 需求点、验收标准、测试用例、风险项必须可追溯，关联到上游阶段文档。
- 测试用例固定结构：用例 ID、模块、前置条件、操作步骤、预期结果、优先级、严重等级。
- 风险清单固定结构：风险描述、影响程度、应对方案、负责人。
- 阶段交付物评审基线锁定后，变更不允许直接改原文档，需新增变更记录并回流【需求阶段】评估。
- 文档文件名用英文小写 + 横杠命名，禁止中文空格。

当前迭代版本号 `V1.0.0.20261009`（版本号规则见 `.claude/rules/development-rules.md`）。

[//]: # (## 项目简介)

[//]: # ()
[//]: # (一款 Android 平台经典打砖块游戏（Breakout），使用 Kotlin + Jetpack Compose 开发。共 10 个关卡，难度逐级提升。竖屏单屏操作：手指拖动挡板，小球在砖块、挡板、墙壁间反弹。)

[//]: # ()
[//]: # (## 开发规范)

[//]: # ()
[//]: # (分支管理、版本号格式（`Va.b.c.d`）、需求/Bug 记录更新等规范见 [`.claude/rules/development-rules.md`]&#40;.claude/rules/development-rules.md&#41;。)

[//]: # ()
[//]: # (## 常用命令)

[//]: # ()
[//]: # (所有构建都使用 Gradle wrapper（`./gradlew`）。Android SDK 路径在 `local.properties` 中。)

[//]: # ()
[//]: # (```bash)

[//]: # (./gradlew assembleDebug                # 构建 debug APK)

[//]: # (./gradlew :app:testDebugUnitTest       # 运行所有 JVM 单元测试（core/ 与 Robolectric）)

[//]: # (./gradlew :app:testDebugUnitTest --tests "com.example.breakout.core.GameEngineTest"   # 运行单个测试类)

[//]: # (./gradlew :app:testDebugUnitTest --tests "com.example.breakout.core.GameEngineTest.launchBall_sets_running_and_upward_velocity"   # 运行单个测试方法)

[//]: # (./gradlew :app:connectedDebugAndroidTest   # 仪器化测试（需连接设备/模拟器）)

[//]: # (```)

[//]: # ()
[//]: # (- 单元测试位于 `app/src/test/`，在 JVM 上运行（JUnit4；依赖 Android 的代码如 SharedPreferences 使用 Robolectric）。)

[//]: # (- 仪器化 UI 测试位于 `app/src/androidTest/`（AndroidJUnit4 + Compose UI test），需要设备或模拟器。)

[//]: # ()
[//]: # (## 架构)

[//]: # ()
[//]: # (三个层次，按是否依赖 Android 清晰分离：)

[//]: # ()
[//]: # (- **`core/`** — 纯 Kotlin，零 Android 依赖。整个游戏逻辑都建模在这一层，可在 JVM 上直接单测。)

[//]: # (  - `Field.kt` — 逻辑坐标空间常量（`WIDTH=360`、`HEIGHT=640`、`COLS=10`）。所有游戏逻辑都在这个逻辑单位中运算，与屏幕像素无关。)

[//]: # (  - `Entities.kt` — `Paddle`、`Ball`、`Brick`、`Rect`，以及 `BrickType` 枚举（NORMAL / DURABLE_2 / DURABLE_3 / INDESTRUCTIBLE）。砖块携带 `hp`，`destroyed` 由它派生。)

[//]: # (  - `GameEngine.kt` — 状态机（`GameStatus` 枚举：READY/RUNNING/PAUSED/WON/LOST）与每帧 `update&#40;dt&#41;`。持有一个 `GameState`（挡板、球、砖块、生命、状态），通过 `state.copy&#40;...&#41;` 做不可变更新。)

[//]: # (  - `Collision.kt` — AABB 圆 vs 矩形的法线检测，以及墙/挡板/砖的反弹。挡板反弹角度由击球点相对挡板中心的偏移决定（`reflectOffPaddle`）。)

[//]: # (  - `Levels.kt` — 全部 10 关，纯数据。`rows&#40;...&#41;` 解析字符串（`D`/`2`/`3`/`I`）生成砖块布局。`LevelConfig` 携带 `ballSpeed`、`paddleHalfWidth`、`rows`。)

[//]: # (  - `LevelProgress.kt` — 单调递增的解锁计数（`maxUnlockedIndex`）。)

[//]: # (- **`data/`** — 持久化。`ProgressStore` 接口及其 `SharedPreferencesProgressStore` 实现（只存一个整数：最大已解锁关卡索引）。)

[//]: # (- **`ui/`** — Compose。`BreakoutApp.kt` 是根组件：一个 `sealed interface Screen`（Menu / LevelSelect / Game）驱动导航（配合 `BackHandler`），并持有 `ProgressStore`。`GameScreen.kt` 用 `Canvas` 渲染游戏并驱动游戏循环。)

[//]: # ()
[//]: # (### 游戏循环)

[//]: # ()
[//]: # (`GameScreen` 用 `LaunchedEffect&#40;engine&#41;` 跑一个基于 `withFrameNanos` 的循环，计算 `dt`，仅在 `status == RUNNING` 时调用 `engine.update&#40;dt&#41;`。这是**带钳制的变步长**（`dt` 上限 1/30s，防止低帧率下球穿透），而非固定步长累加器。引擎的 `GameState` 镜像到一个 Compose `mutableStateOf` 供渲染使用。)

[//]: # ()
[//]: # (### 坐标系统（重要）)

[//]: # ()
[//]: # (游戏场地是固定的 360×640 逻辑空间。`Canvas` 计算一个 `scale`/`offset` 将其黑边缩放进视图，触控输入在调用 `engine.movePaddleTo` 前必须先做**像素 → 逻辑坐标**的换算（使用同样的 scale）。参见 `GameScreen.kt` 中的 `toLogicalX` 换算与 `lx`/`ly` 渲染辅助函数。新增的渲染或输入代码必须经过这个变换。)

[//]: # ()
[//]: # (### Compose 状态注意事项（本仓库踩过的真实坑）)

[//]: # ()
[//]: # (- `engine` 和 `gameState` 用 `remember&#40;levelIndex&#41;` 创建，所以切换关卡会重建 engine。任何捕获 engine 的 `LaunchedEffect`/`DisposableEffect` **必须以 `engine` 为 key**——曾有一个以其他东西为 key 的生命周期观察器持有旧闭包，导致切关后自动暂停了错误的关卡（commit 39dcd66）。新增 effect 观察器时，以 engine 实例为 key，而不是关卡索引。)

[//]: # ()
[//]: # (## 设计文档)

[//]: # ()
[//]: # (`docs/workflow/` 是研发流水线各阶段的正式交付物（`1-requirement/`~`6-release/`），包含需求清单、用户故事与验收标准、交互说明、测试用例、架构设计、接口/数据结构文档、风险清单、开发自测记录、bug 清单、回归报告、发布归档等。文档规范见 `.claude/rules/workflow-doc-rule.md`。)

[//]: # ()
[//]: # (## 备注)

[//]: # ()
[//]: # (- 所有面向用户的字符串都是硬编码中文，没有基于资源的本地化。)

[//]: # (- `.superpowers/` 是 git-ignored 的草稿目录（不属于产品本身）。)

