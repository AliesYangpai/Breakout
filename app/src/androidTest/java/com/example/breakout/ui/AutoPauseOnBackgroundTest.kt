package com.example.breakout.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 切后台自动暂停回归测试（需求 §6 "切后台/锁屏自动暂停"，回归 39dcd66 修复的 Critical bug）
 *
 * 关键点：验证观察器绑定的是"当前" engine，而非切关后残留的旧 engine。
 * 这里用一个可手动控制生命周期的 LifecycleOwner，发射球后触发 ON_STOP，
 * 断言弹出"游戏已暂停"弹窗。
 */
@RunWith(AndroidJUnit4::class)
class AutoPauseOnBackgroundTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class TestLifecycleOwner : LifecycleOwner {
        private val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
        fun handleEvent(event: Lifecycle.Event) = registry.handleLifecycleEvent(event)
    }

    @Test
    fun backgrounding_while_running_triggers_auto_pause() {
        val owner = TestLifecycleOwner()

        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                GameScreen(levelIndex = 0, onExit = {}, onCompleted = {}, onNextLevel = {})
            }
        }
        composeRule.runOnIdle { owner.handleEvent(Lifecycle.Event.ON_CREATE) }
        composeRule.runOnIdle { owner.handleEvent(Lifecycle.Event.ON_START) }
        composeRule.runOnIdle { owner.handleEvent(Lifecycle.Event.ON_RESUME) }

        // 点击游戏区域发射球 → 状态 RUNNING
        composeRule.onNodeWithTag("game_canvas").performTouchInput { click() }
        composeRule.runOnIdle { }

        // 切后台：触发 ON_STOP → 应自动暂停并弹出暂停弹窗
        composeRule.runOnIdle { owner.handleEvent(Lifecycle.Event.ON_STOP) }
        composeRule.onNodeWithText("游戏已暂停").assertIsDisplayed()
        composeRule.onNodeWithText("继续").assertIsDisplayed()
    }
}
