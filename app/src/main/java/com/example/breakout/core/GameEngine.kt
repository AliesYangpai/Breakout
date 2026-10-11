package com.example.breakout.core

import kotlin.math.abs
import kotlin.random.Random

enum class GameStatus { READY, RUNNING, PAUSED, WON, LOST }

data class GameState(
    val levelIndex: Int,
    val lives: Int,
    val status: GameStatus,
    val paddle: Paddle,
    val balls: List<Ball>,
    val bricks: List<Brick>,
    val drops: List<PowerUpDrop> = emptyList(),
    val lasers: List<Laser> = emptyList(),
    val activePowerUps: Set<PowerUpType> = emptySet()
)

fun detectOutcome(bricks: List<Brick>): GameStatus? {
    val hasDestructible = bricks.any { !it.destroyed && !it.type.indestructible }
    return if (hasDestructible) null else GameStatus.WON
}

/** 掉落物与挡板的 AABB 重叠判定。 */
private fun overlaps(a: Rect, b: Rect): Boolean =
    a.right >= b.left && a.left <= b.right && a.bottom >= b.top && a.top <= b.bottom

/**
 * 应用单个道具效果（幂等：已激活则忽略）。纯函数，便于单测。
 * 效果持续到本关结束：不写入跨关可变态，随 GameState 重建自然清除。
 */
internal fun applyPowerUpTo(state: GameState, type: PowerUpType): GameState {
    if (type in state.activePowerUps) return state
    val s = state.copy(activePowerUps = state.activePowerUps + type)
    return when (type) {
        PowerUpType.WIDE_PADDLE ->
            s.copy(paddle = s.paddle.copy(halfWidth = s.paddle.halfWidth * Field.PADDLE_WIDEN_FACTOR))
        PowerUpType.MULTI_BALL -> {
            val src = s.balls.firstOrNull()
            if (src != null && s.balls.size < Field.MAX_BALLS) {
                val speed = src.velocity.length().coerceAtLeast(1f)
                // 新球约 30° 斜向上，水平分量非零，与原球（通常竖直）差异化，避免完全重叠
                val newVel = Vec2(speed * 0.5f, -speed * 0.866f)
                s.copy(balls = s.balls + src.copy(velocity = newVel))
            } else s
        }
        PowerUpType.SLOW_BALL ->
            s.copy(balls = s.balls.map { it.copy(velocity = it.velocity * Field.BALL_SLOW_FACTOR) })
        PowerUpType.LASER -> s
    }
}

/** 逐球移动 + 墙/挡板/砖碰撞，砖被击碎时回调 [drop]（返回 null 表示不掉落）。 */
internal fun stepBalls(state: GameState, dt: Float, drop: (Vec2) -> PowerUpDrop?): GameState {
    var s = state
    val paddleTop = s.paddle.y - s.paddle.height / 2f
    val remainingBalls = mutableListOf<Ball>()
    for (ball in s.balls) {
        var b = ball.copy(position = ball.position + ball.velocity * dt)
        b = bounceOffWall(b)

        if (b.velocity.y > 0f &&
            b.position.y + b.radius >= paddleTop &&
            b.position.y + b.radius <= paddleTop + s.paddle.height + b.radius &&
            abs(b.position.x - s.paddle.centerX) <= s.paddle.halfWidth + b.radius
        ) {
            b = b.copy(
                velocity = reflectOffPaddle(b.velocity, b.position.x, s.paddle),
                position = Vec2(b.position.x, paddleTop - b.radius)
            )
        }

        var velocity = b.velocity
        var position = b.position
        val remaining = mutableListOf<Brick>()
        for (brick in s.bricks) {
            if (brick.destroyed) continue
            val normal = circleRectNormal(position, b.radius, brickRect(brick.row, brick.col))
            if (normal != null) {
                velocity = reflect(velocity, normal)
                position = position + normal * (b.radius + 0.5f)
                val hit = brick.takeHit()
                if (!hit.destroyed) {
                    remaining.add(hit)
                } else {
                    drop(position)?.let { s = s.copy(drops = s.drops + it) }
                }
            } else {
                remaining.add(brick)
            }
        }
        s = s.copy(bricks = remaining)
        b = b.copy(position = position, velocity = velocity)

        if (b.position.y - b.radius <= Field.HEIGHT) {
            remainingBalls.add(b) // 仍在场内；掉落则丢弃
        }
    }
    return s.copy(balls = remainingBalls)
}

/** 掉落物下落、挡板拾取（触发道具效果）、落出屏幕消失。 */
internal fun stepDrops(state: GameState, dt: Float): GameState {
    var s = state
    val paddleRect = Rect(
        left = s.paddle.centerX - s.paddle.halfWidth,
        top = s.paddle.y - s.paddle.height / 2f,
        right = s.paddle.centerX + s.paddle.halfWidth,
        bottom = s.paddle.y + s.paddle.height / 2f
    )
    val remaining = mutableListOf<PowerUpDrop>()
    for (drop in s.drops) {
        val d = drop.copy(position = drop.position + Vec2(0f, Field.DROP_SPEED * dt))
        val dropRect = Rect(
            left = d.position.x - Field.DROP_RADIUS,
            top = d.position.y - Field.DROP_RADIUS,
            right = d.position.x + Field.DROP_RADIUS,
            bottom = d.position.y + Field.DROP_RADIUS
        )
        when {
            overlaps(dropRect, paddleRect) -> s = applyPowerUpTo(s, drop.type)
            d.position.y - Field.DROP_RADIUS > Field.HEIGHT -> Unit // 落出屏幕，消失
            else -> remaining.add(d)
        }
    }
    return s.copy(drops = remaining)
}

/** 激光向上飞行、命中砖块造成 1 点伤害（硬砖无伤），命中后激光消失。 */
internal fun stepLasers(state: GameState, dt: Float, drop: (Vec2) -> PowerUpDrop?): GameState {
    var s = state
    val remaining = mutableListOf<Laser>()
    for (laser in s.lasers) {
        val l = laser.copy(y = laser.y - Field.LASER_SPEED * dt)
        var brickIndex = -1
        for ((i, brick) in s.bricks.withIndex()) {
            if (brick.destroyed) continue
            if (circleRectNormal(Vec2(l.x, l.y), Field.LASER_RADIUS, brickRect(brick.row, brick.col)) != null) {
                brickIndex = i
                break
            }
        }
        if (brickIndex >= 0) {
            val brick = s.bricks[brickIndex]
            val after = brick.takeHit()
            s = if (after.destroyed) {
                var next = s.copy(bricks = s.bricks.filterIndexed { idx, _ -> idx != brickIndex })
                drop(Vec2(l.x, l.y))?.let { next = next.copy(drops = next.drops + it) }
                next
            } else {
                s.copy(bricks = s.bricks.mapIndexed { idx, b -> if (idx == brickIndex) after else b })
            }
        } else if (l.y > 0f) {
            remaining.add(l) // 继续飞行；飞出顶部（y<=0）则移除
        }
    }
    return s.copy(lasers = remaining)
}

/** 掉命与过关判定：仅当所有球均掉落才扣命（REQ-17）。 */
internal fun resolveLifeAndOutcome(state: GameState): GameState {
    var s = state
    if (s.balls.isEmpty()) {
        val newLives = s.lives - 1
        s = if (newLives <= 0) {
            s.copy(lives = 0, status = GameStatus.LOST)
        } else {
            s.copy(
                lives = newLives,
                status = GameStatus.READY,
                balls = listOf(Ball(
                    position = Vec2(s.paddle.centerX, Field.PADDLE_Y - Field.PADDLE_HEIGHT / 2f - Field.BALL_RADIUS - 1f),
                    velocity = Vec2(0f, 0f),
                    radius = Field.BALL_RADIUS
                ))
            )
        }
    } else {
        s = s.copy(status = detectOutcome(s.bricks) ?: GameStatus.RUNNING)
    }
    return s
}

class GameEngine(
    private val config: LevelConfig,
    val levelIndex: Int,
    private val dropRoll: () -> Boolean = { Random.nextFloat() < Field.DROP_CHANCE },
    private val dropPick: () -> PowerUpType = { PowerUpType.entries[Random.nextInt(PowerUpType.entries.size)] }
) {

    var state: GameState = initialState()
        private set

    /** 测试/调试用：直接激活道具效果（不经掉落拾取）。 */
    internal fun activatePowerUp(type: PowerUpType) {
        state = applyPowerUpTo(state, type)
    }

    private fun buildBricks(): List<Brick> {
        val result = mutableListOf<Brick>()
        config.rows.forEachIndexed { row, cells ->
            cells.forEachIndexed { col, type ->
                if (type != null) result.add(Brick(row, col, type))
            }
        }
        return result
    }

    private fun restingBall(centerX: Float): Ball = Ball(
        position = Vec2(centerX, Field.PADDLE_Y - Field.PADDLE_HEIGHT / 2f - Field.BALL_RADIUS - 1f),
        velocity = Vec2(0f, 0f),
        radius = Field.BALL_RADIUS
    )

    fun initialState(): GameState {
        val paddle = Paddle(
            centerX = Field.WIDTH / 2f,
            halfWidth = config.paddleHalfWidth,
            y = Field.PADDLE_Y,
            height = Field.PADDLE_HEIGHT
        )
        return GameState(
            levelIndex = levelIndex,
            lives = Field.MAX_LIVES,
            status = GameStatus.READY,
            paddle = paddle,
            balls = listOf(restingBall(paddle.centerX)),
            bricks = buildBricks()
        )
    }

    fun reset() {
        state = initialState()
    }

    fun movePaddleTo(x: Float) {
        val half = state.paddle.halfWidth
        val clamped = x.coerceIn(half, Field.WIDTH - half)
        state = state.copy(paddle = state.paddle.copy(centerX = clamped))
        if (state.status == GameStatus.READY) {
            state = state.copy(balls = state.balls.map { restingBall(clamped) })
        }
    }

    /** 单击分流：底部空白区移动挡板 / READY 发球 / RUNNING 持激光发射激光（REQ-16/REQ-18）。 */
    fun tap(x: Float, y: Float) {
        val paddleBottom = state.paddle.y + state.paddle.height / 2f
        if (y > paddleBottom) {
            movePaddleTo(x) // 底部空白区：移动挡板，不发射
            return
        }
        when (state.status) {
            GameStatus.READY -> launchBall()
            GameStatus.RUNNING -> if (PowerUpType.LASER in state.activePowerUps) fireLaser()
            else -> Unit // PAUSED/WON/LOST：无操作
        }
    }

    fun launchBall() {
        if (state.status != GameStatus.READY) return
        state = state.copy(
            status = GameStatus.RUNNING,
            balls = state.balls.map { it.copy(velocity = Vec2(0f, -config.ballSpeed)) }
        )
    }

    /** 从挡板左右两端各发射一束激光，向上飞行（REQ-16）。 */
    private fun fireLaser() {
        if (state.status != GameStatus.RUNNING) return
        if (PowerUpType.LASER !in state.activePowerUps) return
        val p = state.paddle
        val top = p.y - p.height / 2f
        state = state.copy(
            lasers = state.lasers + Laser(p.centerX - p.halfWidth, top) + Laser(p.centerX + p.halfWidth, top)
        )
    }

    fun pause() {
        if (state.status == GameStatus.RUNNING) state = state.copy(status = GameStatus.PAUSED)
    }

    fun resume() {
        if (state.status == GameStatus.PAUSED) state = state.copy(status = GameStatus.RUNNING)
    }

    private fun maybeDropAt(position: Vec2): PowerUpDrop? =
        if (dropRoll()) PowerUpDrop(dropPick(), position) else null

    fun update(dt: Float) {
        if (state.status != GameStatus.RUNNING) return
        var s = state
        s = stepBalls(s, dt, ::maybeDropAt)
        s = stepDrops(s, dt)
        s = stepLasers(s, dt, ::maybeDropAt)
        s = resolveLifeAndOutcome(s)
        state = s
    }
}
