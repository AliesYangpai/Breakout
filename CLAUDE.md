# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目简介

一款 Android 平台经典打砖块游戏（Breakout），使用 Kotlin + Jetpack Compose 开发。共 10 个关卡，难度逐级提升。竖屏单屏操作：手指拖动挡板，小球在砖块、挡板、墙壁间反弹。

## 常用命令

所有构建都使用 Gradle wrapper（`./gradlew`）。Android SDK 路径在 `local.properties` 中。

```bash
./gradlew assembleDebug                # 构建 debug APK
./gradlew :app:testDebugUnitTest       # 运行所有 JVM 单元测试（core/ 与 Robolectric）
./gradlew :app:testDebugUnitTest --tests "com.example.breakout.core.GameEngineTest"   # 运行单个测试类
./gradlew :app:testDebugUnitTest --tests "com.example.breakout.core.GameEngineTest.launchBall_sets_running_and_upward_velocity"   # 运行单个测试方法
./gradlew :app:connectedDebugAndroidTest   # 仪器化测试（需连接设备/模拟器）
```

- 单元测试位于 `app/src/test/`，在 JVM 上运行（JUnit4；依赖 Android 的代码如 SharedPreferences 使用 Robolectric）。
- 仪器化 UI 测试位于 `app/src/androidTest/`（AndroidJUnit4 + Compose UI test），需要设备或模拟器。

## 架构

三个层次，按是否依赖 Android 清晰分离：

- **`core/`** — 纯 Kotlin，零 Android 依赖。整个游戏逻辑都建模在这一层，可在 JVM 上直接单测。
  - `Field.kt` — 逻辑坐标空间常量（`WIDTH=360`、`HEIGHT=640`、`COLS=10`）。所有游戏逻辑都在这个逻辑单位中运算，与屏幕像素无关。
  - `Entities.kt` — `Paddle`、`Ball`、`Brick`、`Rect`，以及 `BrickType` 枚举（NORMAL / DURABLE_2 / DURABLE_3 / INDESTRUCTIBLE）。砖块携带 `hp`，`destroyed` 由它派生。
  - `GameEngine.kt` — 状态机（`GameStatus` 枚举：READY/RUNNING/PAUSED/WON/LOST）与每帧 `update(dt)`。持有一个 `GameState`（挡板、球、砖块、生命、状态），通过 `state.copy(...)` 做不可变更新。
  - `Collision.kt` — AABB 圆 vs 矩形的法线检测，以及墙/挡板/砖的反弹。挡板反弹角度由击球点相对挡板中心的偏移决定（`reflectOffPaddle`）。
  - `Levels.kt` — 全部 10 关，纯数据。`rows(...)` 解析字符串（`D`/`2`/`3`/`I`）生成砖块布局。`LevelConfig` 携带 `ballSpeed`、`paddleHalfWidth`、`rows`。
  - `LevelProgress.kt` — 单调递增的解锁计数（`maxUnlockedIndex`）。
- **`data/`** — 持久化。`ProgressStore` 接口及其 `SharedPreferencesProgressStore` 实现（只存一个整数：最大已解锁关卡索引）。
- **`ui/`** — Compose。`BreakoutApp.kt` 是根组件：一个 `sealed interface Screen`（Menu / LevelSelect / Game）驱动导航（配合 `BackHandler`），并持有 `ProgressStore`。`GameScreen.kt` 用 `Canvas` 渲染游戏并驱动游戏循环。

### 游戏循环

`GameScreen` 用 `LaunchedEffect(engine)` 跑一个基于 `withFrameNanos` 的循环，计算 `dt`，仅在 `status == RUNNING` 时调用 `engine.update(dt)`。这是**带钳制的变步长**（`dt` 上限 1/30s，防止低帧率下球穿透），而非固定步长累加器。引擎的 `GameState` 镜像到一个 Compose `mutableStateOf` 供渲染使用。

### 坐标系统（重要）

游戏场地是固定的 360×640 逻辑空间。`Canvas` 计算一个 `scale`/`offset` 将其黑边缩放进视图，触控输入在调用 `engine.movePaddleTo` 前必须先做**像素 → 逻辑坐标**的换算（使用同样的 scale）。参见 `GameScreen.kt` 中的 `toLogicalX` 换算与 `lx`/`ly` 渲染辅助函数。新增的渲染或输入代码必须经过这个变换。

### Compose 状态注意事项（本仓库踩过的真实坑）

- `engine` 和 `gameState` 用 `remember(levelIndex)` 创建，所以切换关卡会重建 engine。任何捕获 engine 的 `LaunchedEffect`/`DisposableEffect` **必须以 `engine` 为 key**——曾有一个以其他东西为 key 的生命周期观察器持有旧闭包，导致切关后自动暂停了错误的关卡（commit 39dcd66）。新增 effect 观察器时，以 engine 实例为 key，而不是关卡索引。

## 设计文档

`docs/superpowers/` 是行为意图的源头：`specs/`（需求、技术栈、交互）和 `test-cases/`（需求 → 测试 ID 的可追溯矩阵）。这些文档描述了仅从代码看不出的意图（难度曲线、状态流转、边界情况）。交互文档定义了拖动与点击的阈值、暂停按钮与发射区域的区分。

## 备注

- 所有面向用户的字符串都是硬编码中文，没有基于资源的本地化。
- `.superpowers/` 是 git-ignored 的草稿目录（不属于产品本身）。
