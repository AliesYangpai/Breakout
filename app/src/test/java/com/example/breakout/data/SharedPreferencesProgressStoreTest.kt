package com.example.breakout.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SharedPreferencesProgressStoreTest {

    private fun context(): Context =
        ApplicationProvider.getApplicationContext()

    @Test
    fun load_returns_zero_when_no_saved_value() {
        val store = SharedPreferencesProgressStore(context())
        assertEquals(0, store.load())
    }

    @Test
    fun save_then_load_round_trips_value() {
        val store = SharedPreferencesProgressStore(context())
        store.save(5)
        assertEquals(5, store.load())
    }

    @Test
    fun save_overwrites_previous_value() {
        val store = SharedPreferencesProgressStore(context())
        store.save(3)
        store.save(8)
        assertEquals(8, store.load())
    }

    @Test
    fun separate_stores_share_same_prefs_file() {
        // 两个 store 实例应指向同一份 SharedPreferences（同名 prefs 文件）
        val storeA = SharedPreferencesProgressStore(context())
        val storeB = SharedPreferencesProgressStore(context())
        storeA.save(4)
        assertEquals(4, storeB.load())
    }
}
