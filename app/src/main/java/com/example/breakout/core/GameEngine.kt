package com.example.breakout.core

import kotlin.math.abs

enum class GameStatus { READY, RUNNING, PAUSED, WON, LOST }

data class GameState(
    val levelIndex: Int,
    val lives: Int,
    val status: GameStatus,
    val paddle: Paddle,
    val ball: Ball,
    val bricks: List<Brick>
)

fun detectOutcome(bricks: List<Brick>): GameStatus? {
    val hasDestructible = bricks.any { !it.destroyed && !it.type.indestructible }
    return if (hasDestructible) null else GameStatus.WON
}

class GameEngine(private val config: LevelConfig, val levelIndex: Int) {

    var state: GameState = initialState()
        private set

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
            ball = restingBall(paddle.centerX),
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
            state = state.copy(ball = restingBall(clamped))
        }
    }

    fun launchBall() {
        if (state.status != GameStatus.READY) return
        state = state.copy(
            status = GameStatus.RUNNING,
            ball = state.ball.copy(velocity = Vec2(0f, -config.ballSpeed))
        )
    }

    fun pause() {
        if (state.status == GameStatus.RUNNING) state = state.copy(status = GameStatus.PAUSED)
    }

    fun resume() {
        if (state.status == GameStatus.PAUSED) state = state.copy(status = GameStatus.RUNNING)
    }

    fun update(dt: Float) {
        if (state.status != GameStatus.RUNNING) return

        var ball = state.ball.copy(position = state.ball.position + state.ball.velocity * dt)
        var bricks = state.bricks

        ball = bounceOffWall(ball)

        val paddle = state.paddle
        val paddleTop = paddle.y - paddle.height / 2f
        if (ball.velocity.y > 0f &&
            ball.position.y + ball.radius >= paddleTop &&
            ball.position.y + ball.radius <= paddleTop + paddle.height + ball.radius &&
            abs(ball.position.x - paddle.centerX) <= paddle.halfWidth + ball.radius
        ) {
            ball = ball.copy(
                velocity = reflectOffPaddle(ball.velocity, ball.position.x, paddle),
                position = Vec2(ball.position.x, paddleTop - ball.radius)
            )
        }

        var velocity = ball.velocity
        var position = ball.position
        val remaining = mutableListOf<Brick>()
        for (brick in bricks) {
            if (brick.destroyed) continue
            val rect = brickRect(brick.row, brick.col)
            val normal = circleRectNormal(position, ball.radius, rect)
            if (normal != null) {
                velocity = reflect(velocity, normal)
                position = position + normal * (ball.radius + 0.5f)
                val hit = brick.takeHit()
                if (!hit.destroyed) remaining.add(hit)
            } else {
                remaining.add(brick)
            }
        }
        bricks = remaining
        ball = ball.copy(position = position, velocity = velocity)

        if (ball.position.y - ball.radius > Field.HEIGHT) {
            val lives = state.lives - 1
            if (lives <= 0) {
                state = state.copy(lives = 0, status = GameStatus.LOST, ball = ball)
            } else {
                state = state.copy(
                    lives = lives,
                    status = GameStatus.READY,
                    ball = restingBall(paddle.centerX),
                    bricks = bricks
                )
            }
            return
        }

        val status = detectOutcome(bricks) ?: GameStatus.RUNNING
        state = state.copy(ball = ball, bricks = bricks, status = status)
    }
}
