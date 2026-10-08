package com.example.breakout.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class EntitiesTest {

    @Test
    fun vec2_arithmetic() {
        val a = Vec2(3f, 4f)
        val b = Vec2(1f, 1f)
        assertEquals(Vec2(4f, 5f), a + b)
        assertEquals(Vec2(2f, 3f), a - b)
        assertEquals(Vec2(6f, 8f), a * 2f)
        assertEquals(Vec2(-3f, -4f), -a)
    }

    @Test
    fun vec2_length_and_normalized() {
        assertEquals(5f, Vec2(3f, 4f).length(), 0.0001f)
        val n = Vec2(3f, 4f).normalized()
        assertEquals(1f, n.length(), 0.0001f)
    }

    @Test
    fun vec2_zero_normalized_is_safe() {
        val n = Vec2(0f, 0f).normalized()
        assertEquals(1f, n.length(), 0.0001f)
    }

    @Test
    fun brick_takeHit_reduces_hp() {
        val brick = Brick(0, 0, BrickType.DURABLE_2)
        assertFalse(brick.destroyed)
        val after1 = brick.takeHit()
        assertEquals(1, after1.hp)
        assertFalse(after1.destroyed)
        val after2 = after1.takeHit()
        assertTrue(after2.destroyed)
    }

    @Test
    fun brick_indestructible_never_destroyed() {
        val brick = Brick(0, 0, BrickType.INDESTRUCTIBLE)
        assertFalse(brick.destroyed)
        val after = brick.takeHit()
        assertEquals(brick.hp, after.hp)
        assertFalse(after.destroyed)
    }

    @Test
    fun rect_geometry() {
        val r = Rect(10f, 20f, 30f, 50f)
        assertEquals(20f, r.centerX(), 0.0001f)
        assertEquals(35f, r.centerY(), 0.0001f)
        assertEquals(20f, r.width(), 0.0001f)
        assertEquals(30f, r.height(), 0.0001f)
    }
}
