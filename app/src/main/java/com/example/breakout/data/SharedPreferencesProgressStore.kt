package com.example.breakout.data

import android.content.Context

class SharedPreferencesProgressStore(context: Context) : ProgressStore {

    private val prefs = context.getSharedPreferences("breakout_progress", Context.MODE_PRIVATE)

    override fun load(): Int = prefs.getInt(KEY_MAX_UNLOCKED, 0)

    override fun save(maxUnlockedIndex: Int) {
        prefs.edit().putInt(KEY_MAX_UNLOCKED, maxUnlockedIndex).apply()
    }

    private companion object {
        const val KEY_MAX_UNLOCKED = "max_unlocked_index"
    }
}
