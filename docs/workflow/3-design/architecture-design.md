# 打砖块游戏（Breakout）技术架构设计文档

- 迭代版本：V1.0.0.20261010
- 文档创建人：Alie
- 创建时间：2026-10-09
- 关联需求：REQ-01 ~ REQ-12（见 `../1-requirement/requirement-list.md`）
- 上游依据：`../1-requirement/requirement-list.md`、`../2-interaction/interaction-spec.md`

## 1. 技术选型结论

| 维度 | 选择 |
|------|------|
| 语言 | Kotlin（单一语言，无 Java 源码） |
| UI 框架 | Jetpack Compose（声明式 UI） |
| 游戏渲染 | Compose `Canvas` + `withFrameNanos` 帧循环 |
| 游戏循环 | 手写，带钳制的变步长（`dt` 上限 1/30s） |
| 物理/碰撞 | 手写 AABB（圆 vs 矩形法线检测） |
| 本地存档 | SharedPreferences（仅存「最大已解锁关卡索引」一个整数） |
| 构建工具 | Gradle Kotlin DSL + AGP 8.7.2 + Kotlin 2.0.21 |

> 选型结论与理由见本节下表。

## 2. 分层架构

三层结构，按是否依赖 Android 清晰分离：

| 层 | 包 | 依赖 Android | 职责 |
|----|----|------------|------|
| 核心逻辑 | `core` | 否（纯 Kotlin） | 实体、碰撞、关卡数据、游戏引擎、进度逻辑 |
| 持久化 | `data` | 是（Context） | 进度存档接口与 SharedPreferences 实现 |
| UI | `ui` | 是 | Compose 屏幕、导航、渲染、输入、HUD/弹窗 |

依赖方向：`ui → data → core`（单向，`core` 零 Android 依赖，可在 JVM 直接单测）。

### 2.1 core 包（纯 Kotlin）

| 文件 | 职责 |
|------|------|
| `Field.kt` | 逻辑坐标空间常量（`WIDTH=360`、`HEIGHT=640`、`COLS=10`、`MAX_LIVES=3` 等） |
| `Vec2.kt` | 二维向量及加/减/标量乘/长度/归一化/点积 |
| `Entities.kt` | `Rect`、`Paddle`、`Ball`、`Brick`、`BrickType` 枚举 |
| `Collision.kt` | AABB 圆 vs 矩形法线检测、墙/挡板/砖反弹 |
| `GameEngine.kt` | `GameStatus` 状态机 + `GameState` + 每帧 `update(dt)` |
| `Levels.kt` | 全部 10 关纯数据（`LevelConfig` + 字符串网格解析） |
| `LevelProgress.kt` | 单调递增的解锁计数 |

### 2.2 data 包（持久化）

| 文件 | 职责 |
|------|------|
| `ProgressStore.kt` | 接口：`load(): Int` / `save(maxUnlockedIndex: Int)` |
| `SharedPreferencesProgressStore.kt` | SharedPreferences 实现，只存 `max_unlocked_index` |

### 2.3 ui 包（Compose）

| 文件 | 职责 |
|------|------|
| `BreakoutApp.kt` | 根组件，`sealed interface Screen` 驱动导航 + `BackHandler`，持有 `ProgressStore` |
| `MenuScreen.kt` | 主菜单 |
| `LevelSelectScreen.kt` | 关卡选择页（5 列网格） |
| `GameScreen.kt` | `Canvas` 渲染 + 游戏循环 + HUD + 暂停/过关/失败弹窗 |
| `theme/Theme.kt` | Material3 主题 |

## 3. 游戏循环设计

`GameScreen` 用 `LaunchedEffect(engine)` 跑基于 `withFrameNanos` 的循环：

1. 计算 `dt = (now - last) / 1e9`，`dt` 上限钳制为 1/30s。
2. 仅当 `status == RUNNING` 时调用 `engine.update(dt)`。
3. 将 `engine.state` 镜像到 Compose `mutableStateOf` 供渲染。

**变步长 + 钳制**（而非固定步长累加器）：60fps 下等价固定步长；低帧率下通过钳制 `dt` 防止球单步位移过大而穿透砖/挡板。

## 4. 坐标系统（重要）

- 游戏场地固定 360×640 逻辑空间。
- `Canvas` 计算 `scale = min(viewW/360, viewH/640)` 与 `offset`，将黑边缩放进视图。
- **渲染**：`lx(x) = offsetX + x * scale`、`ly(y) = offsetY + y * scale`。
- **输入**：触控像素坐标先用 `toLogicalX(px) = (px - offsetX) / scale` 换算回逻辑坐标，再传给 `engine.movePaddleTo`。
- 新增渲染/输入代码必须经过同一变换。

## 5. 状态机设计

`GameStatus`：`READY / RUNNING / PAUSED / WON / LOST`

| 状态 | 进入条件 | 退出条件 |
|------|---------|---------|
| READY | 初始 / 掉命后 / reset | 点击发射 → RUNNING |
| RUNNING | 发射球 | 暂停 → PAUSED；清空砖 → WON；命尽 → LOST |
| PAUSED | 暂停/切后台 | 继续 → RUNNING；重开 → READY |
| WON | 无可破坏砖 | 下一关 / 返回 |
| LOST | 命尽 | 重开 / 返回 |

状态迁移全部通过 `state.copy(...)` 不可变更新；`launchBall`/`pause`/`resume` 做状态守卫，非法跳转忽略。

## 6. 关键实现要点

- **挡板反弹**：`reflectOffPaddle` 按击球点相对挡板中心偏移计算反弹角（`maxBounceDeg=60°`），并强制最小垂直分量（`speed * 0.25f`）防卡死。
- **砖碰撞**：`circleRectNormal` 求法线，命中后 `reflect` 反弹并加 `radius + 0.5f` 位置偏移推出重叠区。
- **耐久砖颜色**：`brickColor` 按剩余 `hp` 比例调整 alpha，耐久越高颜色越实。
- **切后台自动暂停**：`DisposableEffect(engine, lifecycleOwner)` 观察 `ON_STOP`，key 用 `engine` 避免切关后闭包持有旧 engine。
- **过关回调仅一次**：`LaunchedEffect(gameState.status)` 在 `WON` 时触发 `onCompleted`。

## 7. 修订记录

| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|---------|--------|
| V1.0.0.20261010 | 2026-10-09 | 依据新工作流规范初始化技术架构设计 | Alie |
