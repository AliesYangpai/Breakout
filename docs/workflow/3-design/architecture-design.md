# 打砖块游戏（Breakout）技术架构设计文档

- 迭代版本：V1.1.0.20261011
- 文档创建人：Alie
- 创建时间：2026-10-09
- 关联需求：REQ-01 ~ REQ-18（见 `../1-requirement/requirement-list.md`）
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
| `Field.kt` | 逻辑坐标空间常量 + 道具/激光参数常量（掉落概率、倍率、速度、上限等） |
| `Vec2.kt` | 二维向量及加/减/标量乘/长度/归一化/点积 |
| `Entities.kt` | `Rect`、`Paddle`、`Ball`、`Brick`、`BrickType`、`PowerUpType`、`PowerUpDrop`、`Laser` |
| `Collision.kt` | AABB 圆 vs 矩形法线检测、墙/挡板/砖反弹 |
| `GameEngine.kt` | `GameStatus` 状态机 + `GameState` + 每帧 `update(dt)`（含掉落物生成/拾取、激光生成/命中、多球、道具效果） |
| `Levels.kt` | 全部 10 关纯数据（`LevelConfig` + 字符串网格解析） |
| `LevelProgress.kt` | 单调递增的解锁计数 |

> 道具/激光/掉落物全部建模为 `core` 包内纯 Kotlin 数据类与函数，不引入任何 Android 依赖，保证 JVM 单测可行（NFR-06）。

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

## 6. 道具 / 激光 / 多球 / 输入设计（新增）

### 6.1 道具掉落（REQ-13）
- 击碎可破坏砖时，以配置概率 `DROP_CHANCE` 判定是否掉落；硬砖永不触发。
- 掉落物 `PowerUpDrop` 自被击碎砖中心生成，垂直向下匀速（`DROP_SPEED`）。
- 随机源抽象为可注入函数（`Random` 或 `(Float) -> Boolean` 判定器），便于单测注入固定种子/确定性判定（应对 RISK-12/15）。

### 6.2 四种道具效果（REQ-14/REQ-15）
- 效果用 `PowerUpType` 枚举表示，`GameState` 持有 `activePowerUps: Set<PowerUpType>`（不同道具可叠加，同种幂等——用 Set 天然去重）。
- 加长挡板：拾取时 `paddle.halfWidth` 乘以 `PADDLE_WIDEN_FACTOR`（一次生效，Set 去重避免重复乘）。
- 多球：拾取时新增一个球（复制当前球并调整角度差异化），受 `MAX_BALLS` 上限约束。
- 慢速球：拾取时所有球速度乘以 `BALL_SLOW_FACTOR`。
- 激光：拾取时 `activePowerUps` 加入 LASER，运行中点击触发发射。
- 效果持续到本关结束：`reset()`/切关重建 `GameState` 即自然清除，无跨关共享可变态。

### 6.3 激光（REQ-16）
- `Laser` 实体：`x`（发射列位置）、`y`、向上速度 `LASER_SPEED`。
- 每次点击从挡板左右两端（`paddle.centerX ± paddle.halfWidth` 内侧）各生成一束，共两束。
- 每帧激光向上位移，用 `circleRectNormal`/`brickRect` 检测命中的砖；命中后该砖 `takeHit()`（1 点伤害，硬砖无伤），激光消失。
- 激光飞出屏幕顶部则消失。

### 6.4 多球与掉命（REQ-17）
- `GameState.ball` 由单个 `Ball` 改为 `balls: List<Ball>`（`Ball` 结构不变，见数据结构设计）。
- 每帧对所有球做墙/挡板/砖碰撞；掉出屏幕底部的球从列表移除。
- 仅当 `balls` 全部移除（空）才 `lives - 1` 并回 READY；仍有一球则继续 RUNNING。
- READY 状态仅一球吸附挡板；发射仅作用于该吸附球。

### 6.5 底部空白区移动（REQ-18）与单击分流
- 输入层在抬手无拖动时按 y 分流：`y > 挡板下沿` → `movePaddleTo(x)`（移动）；否则按状态分流发射球/激光。
- `GameEngine` 新增 `tap(x, y)` 语义化入口（内部完成分流与状态守卫），UI 层仅负责坐标换算后调用，避免分流逻辑散落 UI（应对 RISK-13）。

## 7. 关键实现要点

- **挡板反弹**：`reflectOffPaddle` 按击球点相对挡板中心偏移计算反弹角（`maxBounceDeg=60°`），并强制最小垂直分量（`speed * 0.25f`）防卡死。
- **砖碰撞**：`circleRectNormal` 求法线，命中后 `reflect` 反弹并加 `radius + 0.5f` 位置偏移推出重叠区。
- **耐久砖颜色**：`brickColor` 按剩余 `hp` 比例调整 alpha，耐久越高颜色越实。
- **切后台自动暂停**：`DisposableEffect(engine, lifecycleOwner)` 观察 `ON_STOP`，key 用 `engine` 避免切关后闭包持有旧 engine。
- **过关回调仅一次**：`LaunchedEffect(gameState.status)` 在 `WON` 时触发 `onCompleted`。
- **多球渲染/碰撞**：UI 渲染改为遍历 `balls`；碰撞逻辑内联到 `update` 循环对每个球执行。
- **掉落物/激光渲染**：新增 `PowerUpDrop`/`Laser` 绘制分支，掉落物用道具颜色圆/方块，激光用细长竖条。

## 8. 单元测试范围

| 模块 | 测试点 |
|------|--------|
| `GameEngineTest` 新增 | 掉落物生成/拾取/落出消失、四种道具效果、多球掉命（全掉光才扣）、激光双束/伤害/硬砖无伤、底部空白区移动、`tap` 分流 |
| `EntitiesTest` 新增 | `PowerUpType` 枚举、`PowerUpDrop`/`Laser` 结构、`takeHit` 对硬砖/耐久砖 |
| `CollisionTest` | 激光命中判定复用现有 `circleRectNormal`，无需新增独立测试 |

## 9. 修订记录

| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|---------|--------|
| V1.0.0.20261010 | 2026-10-09 | 依据新工作流规范初始化技术架构设计 | Alie |
| V1.1.0.20261011 | 2026-10-11 | 新增 §6 道具/激光/多球/输入设计、§8 单测范围；扩展 core 包实体清单与状态机说明（球改为列表） | Alie |
