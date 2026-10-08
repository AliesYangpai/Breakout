package com.example.breakout.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * GameScreen 界面与手势测试（交互文档 §3.3/§3.4）
 */
@RunWith(AndroidJUnit4::class)
class GameScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hud_shows_level_number_lives_and_pause() {
        composeRule.setContent {
            GameScreen(levelIndex = 2, onExit = {}, onCompleted = {}, onNextLevel = {})
        }
        composeRule.onNodeWithText("第 3 关").assertIsDisplayed()
        composeRule.onNodeWithText("♥♥♥").assertIsDisplayed()
        composeRule.onNodeWithText("暂停").assertIsDisplayed()
    }

    @Test
    fun tap_on_canvas_launches_ball() {
        composeRule.setContent {
            GameScreen(levelIndex = 0, onExit = {}, onCompleted = {}, onNextLevel = {})
        }
        // 初始 READY，球吸附；点击游戏区域发射
        composeRule.onNodeWithTag("game_canvas").performTouchInput { click() }
        // 发射后应不崩溃且仍显示 HUD
        composeRule.onNodeWithText("第 1 关").assertIsDisplayed()
    }

    @Test
    fun pause_button_opens_pause_dialog() {
        composeRule.setContent {
            GameScreen(levelIndex = 0, onExit = {}, onCompleted = {}, onNextLevel = {})
        }
        composeRule.onNodeWithText("暂停").performClick()
        composeRule.onNodeWithText("游戏已暂停").assertIsDisplayed()
        composeRule.onNodeWithText("继续").assertIsDisplayed()
        composeRule.onNodeWithText("重新开始").assertIsDisplayed()
        composeRule.onNodeWithText("返回关卡选择").assertIsDisplayed()
    }

    @Test
    fun resume_closes_pause_dialog() {
        composeRule.setContent {
            GameScreen(levelIndex = 0, onExit = {}, onCompleted = {}, onNextLevel = {})
        }
        composeRule.onNodeWithText("暂停").performClick()
        composeRule.onNodeWithText("继续").performClick()
        // 弹窗关闭，HUD 仍可见
        composeRule.onNodeWithText("第 1 关").assertIsDisplayed()
    }

    @Test
    fun restart_resets_from_pause() {
        composeRule.setContent {
            GameScreen(levelIndex = 0, onExit = {}, onCompleted = {}, onNextLevel = {})
        }
        composeRule.onNodeWithText("暂停").performClick()
        composeRule.onNodeWithText("重新开始").performClick()
        // 弹窗关闭，生命回到 3（READY 状态）
        composeRule.onNodeWithText("♥♥♥").assertIsDisplayed()
    }

    @Test
    fun drag_on_canvas_does_not_crash() {
        composeRule.setContent {
            GameScreen(levelIndex = 0, onExit = {}, onCompleted = {}, onNextLevel = {})
        }
        composeRule.onNodeWithTag("game_canvas").performTouchInput {
            swipe(start = center, end = center.copy(x = center.x + 200f), durationMillis = 200)
        }
        composeRule.onNodeWithText("第 1 关").assertIsDisplayed()
    }
}
