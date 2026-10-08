# 打砖块游戏（Breakout）技术栈选型文档

- 日期：2026-10-08
- 平台：Android
- 依据：`2026-10-08-breakout-game-design.md`、`2026-10-08-breakout-game-interaction.md`

## 1. 选型结论

| 维度 | 选择 |
|------|------|
| 语言 | Kotlin |
| UI 框架 | Jetpack Compose（声明式 UI） |
| 游戏渲染 | Compose `Canvas` + `withFrameNanos` 固定步长循环 |
| 游戏循环 | 手写固定步长（`withFrameNanos` 驱动） |
| 物理/碰撞 | 手写（AABB 碰撞检测） |
| 本地存档 | SharedPreferences（仅存"已解锁关卡数"一个整数） |
| 构建工具 | Gradle Kotlin DSL + 最新稳定版 AGP + Kotlin |

## 2. 方案对比（为何选 A）

| 维度 | 方案 A：Kotlin + Compose + Canvas | 方案 B：Kotlin + SurfaceView | 方案 C：LibGDX |
|------|------|------|------|
| 语言 | Kotlin | Kotlin | Java/Kotlin |
| UI（菜单/关卡选择/弹窗） | Compose 声明式 UI，最快 | 手写 XML 或混用 Compose | Scene2D（较老旧） |
| 游戏渲染 | Compose Canvas + 帧循环 | 独立线程 SurfaceView 自绘 | 引擎自带循环 + 渲染 |
| 游戏循环 | 手写固定步长 | 手写固定步长 | 开箱即用 |
| 物理/碰撞 | 手写（打砖块物理极简） | 手写 | 可选 Box2D |
| 包体/依赖 | 小，无第三方引擎 | 小 | 较大 |
| 适配度 | 高（当前 Android 主流） | 中 | 对 10 关打砖块偏重 |

## 3. 选型理由

1. **贴合需求**：打砖块实体只有挡板、球、砖三类，碰撞是 AABB，物理极简——不需要游戏引擎的物理/粒子系统，手写几十行即可。引入 LibGDX 反而过重。
2. **UI 是重头**：菜单、关卡选择页、暂停/过关/失败弹窗、HUD 占交互文档大部分篇幅。Compose 声明式 UI 做这些最快，`StateFlow`/`MutableState` 天然契合状态机设计。
3. **游戏循环够用**：约 10 行砖 + 1 球 + 1 挡板，`withFrameNanos` 驱动的固定步长循环在 60fps 下完全够。若未来性能吃紧，可无缝换到 SurfaceView（仅游戏区域），无需重写逻辑。
4. **单一技术栈**：一门 Kotlin 从 UI 到逻辑到渲染全包，2026 年 Android 生态最标准的选择，社区资料最全。

## 4. 各组件职责映射

| 组件 | 技术实现 | 对应需求/交互文档章节 |
|------|---------|----------------------|
| 游戏循环 | `withFrameNanos` + 固定时间步长累加器 | 需求 §2 游戏引擎/循环 |
| 实体系统 | Kotlin 数据类 + 纯函数更新 | 需求 §3 实体与碰撞 |
| 关卡数据 | 纯 Kotlin 数据结构（网格 + 砖类型） | 需求 §4 难度曲线 |
| 状态机 | 枚举 + `sealed class` 状态 | 需求 §5 状态流转 |
| UI 页面 | Compose `@Composable` 屏幕 | 交互 §3 各页面 |
| HUD/弹窗 | Compose 组件 | 交互 §3.3/3.4/3.5/3.6 |
| 触控输入 | Compose `pointerInput`（`detectDragGestures`/`detectTapGestures`） | 交互 §4.3 |
| 存档 | SharedPreferences | 需求 §5 关卡解锁逻辑 |

## 5. 版本与环境（实现阶段锁定具体版本号）

- Kotlin：最新稳定版（实现时锁定）。
- AGP（Android Gradle Plugin）：最新稳定版。
- Compose BOM：最新稳定版（用于统一 Compose 依赖版本）。
- `minSdk` / `targetSdk`：实现阶段按目标机型确定，建议 `minSdk 24+` 覆盖绝大多数设备。
- 目标语言：Kotlin 单一语言，不引入 Java 源码。

## 6. 明确排除

- 不做跨平台（Flutter/React Native）——明确指定 Android 平台。
- 不引入游戏引擎（LibGDX/Godot/Unity）。
- 不引入第三方物理引擎（Box2D 等）。
- 不使用 SQLite/Room——存档只有一个整数，SharedPreferences 足够。
