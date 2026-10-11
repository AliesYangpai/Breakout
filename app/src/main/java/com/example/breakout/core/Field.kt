package com.example.breakout.core

object Field {
    const val WIDTH = 360f
    const val HEIGHT = 640f
    const val COLS = 10
    const val BRICK_HEIGHT = 24f
    const val TOP_OFFSET = 80f
    const val PADDLE_Y = HEIGHT - 48f
    const val PADDLE_HEIGHT = 12f
    const val BALL_RADIUS = 7f
    const val MAX_LIVES = 3

    // 道具掉落与效果（V1.1 新增）
    const val DROP_CHANCE = 0.2f
    const val DROP_SPEED = 120f
    const val DROP_RADIUS = 8f
    const val PADDLE_WIDEN_FACTOR = 1.5f
    const val MAX_BALLS = 4
    const val BALL_SLOW_FACTOR = 0.6f
    const val LASER_SPEED = 600f
    const val LASER_RADIUS = 3f
}
