package com.example.breakout.core

data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun centerX() = (left + right) / 2f
    fun centerY() = (top + bottom) / 2f
    fun width() = right - left
    fun height() = bottom - top
}

data class Paddle(
    val centerX: Float,
    val halfWidth: Float,
    val y: Float,
    val height: Float
)

data class Ball(
    val position: Vec2,
    val velocity: Vec2,
    val radius: Float
)

enum class BrickType(val hits: Int, val indestructible: Boolean = false) {
    NORMAL(1),
    DURABLE_2(2),
    DURABLE_3(3),
    INDESTRUCTIBLE(Int.MAX_VALUE, indestructible = true)
}

data class Brick(
    val row: Int,
    val col: Int,
    val type: BrickType,
    val hp: Int = type.hits
) {
    val destroyed: Boolean get() = !type.indestructible && hp <= 0
    fun takeHit(): Brick = if (type.indestructible) this else copy(hp = hp - 1)
}
