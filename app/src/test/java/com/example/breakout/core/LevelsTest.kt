package com.example.breakout.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelsTest {

    @Test
    fun exactly_ten_levels() {
        assertEquals(10, Levels.all.size)
    }

    @Test
    fun every_level_has_at_least_one_destructible_brick() {
        Levels.all.forEachIndexed { i, level ->
            val hasDestructible = level.rows.flatten().any {
                it != null && !it.indestructible
            }
            assertTrue("level ${i + 1} has no destructible brick", hasDestructible)
        }
    }

    @Test
    fun indestructible_only_from_level_7() {
        Levels.all.take(6).forEachIndexed { i, level ->
            val hasIndestructible = level.rows.flatten().any { it?.indestructible == true }
            assertTrue("level ${i + 1} must not have indestructible bricks", !hasIndestructible)
        }
        Levels.all.drop(6).forEachIndexed { i, level ->
            val hasIndestructible = level.rows.flatten().any { it?.indestructible == true }
            assertTrue("level ${i + 7} should have indestructible bricks", hasIndestructible)
        }
    }

    @Test
    fun ball_speed_non_decreasing() {
        Levels.all.zipWithNext { a, b ->
            assertTrue("ball speed must be non-decreasing", b.ballSpeed >= a.ballSpeed)
        }
    }

    @Test
    fun paddle_width_non_increasing() {
        Levels.all.zipWithNext { a, b ->
            assertTrue("paddle width must be non-increasing", b.paddleHalfWidth <= a.paddleHalfWidth)
        }
    }
}
