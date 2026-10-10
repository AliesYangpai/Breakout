# 打砖块游戏（Breakout）接口文档

- 迭代版本：V1.0.0.20261010
- 文档创建人：Alie
- 创建时间：2026-10-09
- 关联需求：REQ-01 ~ REQ-12（见 `../1-requirement/requirement-list.md`）

## 1. 说明

本项目为单机 Android 应用，无网络接口。本文档覆盖模块间公共 API（类/方法签名）。

## 2. core 包公共 API

### 2.1 Field（常量）

```kotlin
object Field {
    const val WIDTH = 360f; const val HEIGHT = 640f
    const val COLS = 10; const val BRICK_HEIGHT = 24f
    const val TOP_OFFSET = 80f; const val PADDLE_Y = 592f
    const val PADDLE_HEIGHT = 12f; const val BALL_RADIUS = 7f
    const val MAX_LIVES = 3
}
```

### 2.2 Vec2

```kotlin
data class Vec2(val x: Float, val y: Float)
    operator fun plus(other: Vec2): Vec2
    operator fun minus(other: Vec2): Vec2
    operator fun times(scalar: Float): Vec2
    operator fun unaryMinus(): Vec2
    fun length(): Float
    fun normalized(): Vec2
    fun dot(other: Vec2): Float
```

### 2.3 实体

```kotlin
data class Rect(left, top, right, bottom: Float)
    fun centerX()/centerY()/width()/height(): Float

data class Paddle(centerX, halfWidth, y, height: Float)
data class Ball(position: Vec2, velocity: Vec2, radius: Float)

enum class BrickType(hits: Int, indestructible: Boolean = false)
    NORMAL(1), DURABLE_2(2), DURABLE_3(3), INDESTRUCTIBLE(Int.MAX_VALUE, true)

data class Brick(row, col: Int, type: BrickType, hp: Int = type.hits)
    val destroyed: Boolean
    fun takeHit(): Brick
```

### 2.4 碰撞函数

```kotlin
fun brickRect(row: Int, col: Int): Rect
fun reflect(velocity: Vec2, normal: Vec2): Vec2
fun bounceOffWall(ball: Ball): Ball
fun reflectOffPaddle(velocity: Vec2, ballX: Float, paddle: Paddle, maxBounceDeg: Float = 60f): Vec2
fun circleRectNormal(center: Vec2, radius: Float, rect: Rect): Vec2?
```

### 2.5 游戏引擎

```kotlin
enum class GameStatus { READY, RUNNING, PAUSED, WON, LOST }

data class GameState(levelIndex, lives: Int, status: GameStatus,
                     paddle: Paddle, ball: Ball, bricks: List<Brick>)

fun detectOutcome(bricks: List<Brick>): GameStatus?

class GameEngine(config: LevelConfig, levelIndex: Int)
    var state: GameState (private set)
    fun initialState(): GameState
    fun reset()
    fun movePaddleTo(x: Float)
    fun launchBall()
    fun pause()
    fun resume()
    fun update(dt: Float)
```

### 2.6 关卡数据与进度

```kotlin
data class LevelConfig(ballSpeed: Float, paddleHalfWidth: Float, rows: List<List<BrickType?>>)

object Levels { val all: List<LevelConfig> }  // 10 关

class LevelProgress(initialMaxUnlockedIndex: Int = 0)
    var maxUnlockedIndex: Int (private set)
    fun isUnlocked(index: Int): Boolean
    fun recordCompletion(index: Int)
```

## 3. data 包公共 API

```kotlin
interface ProgressStore {
    fun load(): Int
    fun save(maxUnlockedIndex: Int)
}

class SharedPreferencesProgressStore(context: Context) : ProgressStore
```

## 4. ui 包公共 API

```kotlin
sealed interface Screen {
    data object Menu; data object LevelSelect; data class Game(val levelIndex: Int)
}

@Composable fun BreakoutApp()
@Composable fun MenuScreen(onStart: () -> Unit)
@Composable fun LevelSelectScreen(maxUnlockedIndex: Int, onSelect: (Int) -> Unit, onBack: () -> Unit)
@Composable fun GameScreen(levelIndex: Int, onExit: () -> Unit,
                           onCompleted: (Int) -> Unit, onNextLevel: () -> Unit)
```

## 5. 修订记录

| 版本 | 日期 | 修改内容 | 修改人 |
|------|------|---------|--------|
| V1.0.0.20261010 | 2026-10-09 | 依据新工作流规范初始化接口文档 | Alie |
