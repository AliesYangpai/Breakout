package com.example.breakout.core

class LevelProgress(initialMaxUnlockedIndex: Int = 0) {
    var maxUnlockedIndex: Int = initialMaxUnlockedIndex
        private set

    fun isUnlocked(index: Int) = index <= maxUnlockedIndex

    fun recordCompletion(index: Int) {
        val next = index + 1
        if (next > maxUnlockedIndex) maxUnlockedIndex = next
    }
}
