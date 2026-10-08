package com.example.breakout.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelProgressTest {

    @Test
    fun initially_only_level_0_unlocked() {
        val p = LevelProgress()
        assertTrue(p.isUnlocked(0))
        assertFalse(p.isUnlocked(1))
        assertFalse(p.isUnlocked(9))
    }

    @Test
    fun completing_a_level_unlocks_the_next() {
        val p = LevelProgress()
        p.recordCompletion(0)
        assertEquals(1, p.maxUnlockedIndex)
        assertTrue(p.isUnlocked(0))
        assertTrue(p.isUnlocked(1))
        assertFalse(p.isUnlocked(2))
    }

    @Test
    fun completion_is_monotonic() {
        val p = LevelProgress()
        p.recordCompletion(3)
        assertEquals(4, p.maxUnlockedIndex)
        p.recordCompletion(1) // completing an earlier level must not reduce progress
        assertEquals(4, p.maxUnlockedIndex)
    }

    @Test
    fun completing_last_level_stays_within_bounds() {
        val p = LevelProgress()
        p.recordCompletion(9)
        assertEquals(10, p.maxUnlockedIndex)
    }
}
