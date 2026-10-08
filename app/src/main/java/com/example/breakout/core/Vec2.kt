package com.example.breakout.core

import kotlin.math.sqrt

data class Vec2(val x: Float, val y: Float) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)
    operator fun unaryMinus() = Vec2(-x, -y)
    fun length() = sqrt(x * x + y * y)
    fun normalized(): Vec2 {
        val len = length()
        return if (len == 0f) Vec2(0f, -1f) else Vec2(x / len, y / len)
    }
    fun dot(other: Vec2) = x * other.x + y * other.y
}
