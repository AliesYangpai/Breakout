package com.example.breakout.data

interface ProgressStore {
    fun load(): Int
    fun save(maxUnlockedIndex: Int)
}
