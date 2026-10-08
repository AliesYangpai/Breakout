package com.example.breakout.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionTest {

    private val paddle = Paddle(centerX = 180f, halfWidth = 70f, y = Field.PADDLE_Y, height = Field.PADDLE_HEIGHT)

    @Test
    fun reflect_flips_vertical_wall() {
        val v = reflect(Vec2(100f, -50f), Vec2(1f, 0f))
        assertEquals(-100f, v.x, 0.0001f)
        assertEquals(-50f, v.y, 0.0001f)
    }

    @Test
    fun bounceOffWall_left_wall() {
        val b = Ball(Vec2(2f, 100f), Vec2(-30f, 20f), 7f)
        val out = bounceOffWall(b)
        assertEquals(7f, out.position.x, 0.0001f)
        assertTrue(out.velocity.x > 0f)
    }

    @Test
    fun bounceOffWall_top_wall() {
        val b = Ball(Vec2(100f, 2f), Vec2(30f, -40f), 7f)
        val out = bounceOffWall(b)
        assertEquals(7f, out.position.y, 0.0001f)
        assertTrue(out.velocity.y > 0f)
    }

    @Test
    fun paddle_center_hits_straight_up() {
        val v = reflectOffPaddle(Vec2(0f, -400f), 180f, paddle)
        assertEquals(0f, v.x, 0.01f)
        assertTrue(v.y < 0f)
        assertEquals(400f, v.length(), 0.5f)
    }

    @Test
    fun paddle_edge_hits_sideways() {
        val v = reflectOffPaddle(Vec2(0f, -400f), 250f, paddle)
        assertTrue(v.x > 0f)
        assertTrue(v.y < 0f)
        assertEquals(400f, v.length(), 0.5f)
    }

    @Test
    fun circleRectNormal_outside_is_null() {
        assertNull(circleRectNormal(Vec2(180f, 30f), 7f, brickRect(0, 0)))
    }

    @Test
    fun circleRectNormal_hit_from_below_points_away_from_brick() {
        // Normal convention: points from the rect surface toward the ball (outward).
        // The engine uses `position + normal * (radius + eps)` to separate the ball,
        // so a ball below the brick (y-down coords) yields an outward normal with n.y > 0.
        val rect = brickRect(0, 0)
        val center = Vec2(rect.centerX(), rect.bottom + 3f)
        val n = circleRectNormal(center, 7f, rect)
        assertTrue(n != null && n.y > 0f)
    }

    @Test
    fun brickRect_col_width() {
        val r0 = brickRect(0, 0)
        val r1 = brickRect(0, 1)
        assertEquals(0f, r0.left, 0.0001f)
        assertEquals(r0.right, r1.left, 0.0001f)
        assertEquals(Field.WIDTH / Field.COLS, r0.width(), 0.0001f)
    }
}
