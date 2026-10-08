package com.example.breakout.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val DEG_TO_RAD = (Math.PI / 180.0).toFloat()

fun brickRect(row: Int, col: Int): Rect {
    val brickW = Field.WIDTH / Field.COLS
    return Rect(
        left = col * brickW,
        top = Field.TOP_OFFSET + row * Field.BRICK_HEIGHT,
        right = (col + 1) * brickW,
        bottom = Field.TOP_OFFSET + (row + 1) * Field.BRICK_HEIGHT
    )
}

fun reflect(velocity: Vec2, normal: Vec2): Vec2 {
    val n = normal.normalized()
    val d = velocity.dot(n)
    return velocity - n * (2f * d)
}

fun bounceOffWall(ball: Ball): Ball {
    var pos = ball.position
    var vel = ball.velocity
    if (pos.x - ball.radius < 0f) {
        pos = Vec2(ball.radius, pos.y); vel = Vec2(abs(vel.x), vel.y)
    } else if (pos.x + ball.radius > Field.WIDTH) {
        pos = Vec2(Field.WIDTH - ball.radius, pos.y); vel = Vec2(-abs(vel.x), vel.y)
    }
    if (pos.y - ball.radius < 0f) {
        pos = Vec2(pos.x, ball.radius); vel = Vec2(vel.x, abs(vel.y))
    }
    return Ball(pos, vel, ball.radius)
}

fun reflectOffPaddle(
    velocity: Vec2,
    ballX: Float,
    paddle: Paddle,
    maxBounceDeg: Float = 60f
): Vec2 {
    val speed = velocity.length()
    val hitRatio = ((ballX - paddle.centerX) / paddle.halfWidth).coerceIn(-1f, 1f)
    val angle = hitRatio * maxBounceDeg * DEG_TO_RAD
    val newVx = speed * sin(angle)
    val newVy = -speed * cos(angle)
    val minVertical = speed * 0.25f
    val vy = min(newVy, -minVertical)
    val vxMag = sqrt(max(0f, speed * speed - vy * vy))
    val vx = if (newVx >= 0f) vxMag else -vxMag
    return Vec2(vx, vy)
}

fun circleRectNormal(center: Vec2, radius: Float, rect: Rect): Vec2? {
    val closestX = center.x.coerceIn(rect.left, rect.right)
    val closestY = center.y.coerceIn(rect.top, rect.bottom)
    val dx = center.x - closestX
    val dy = center.y - closestY
    val distSq = dx * dx + dy * dy
    if (distSq > radius * radius) return null
    if (distSq == 0f) {
        val leftPen = center.x - rect.left
        val rightPen = rect.right - center.x
        val topPen = center.y - rect.top
        val bottomPen = rect.bottom - center.y
        val minPen = min(min(leftPen, rightPen), min(topPen, bottomPen))
        return when (minPen) {
            leftPen -> Vec2(-1f, 0f)
            rightPen -> Vec2(1f, 0f)
            topPen -> Vec2(0f, -1f)
            else -> Vec2(0f, 1f)
        }
    }
    val dist = sqrt(distSq)
    return Vec2(dx / dist, dy / dist)
}
