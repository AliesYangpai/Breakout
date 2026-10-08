package com.example.breakout.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 导航流转与界面测试（交互文档 §3.1/§3.2）
 */
@RunWith(AndroidJUnit4::class)
class BreakoutAppNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun menuScreen_shows_title_and_start_button() {
        composeRule.setContent { MenuScreen(onStart = {}) }
        composeRule.onNodeWithText("打砖块").assertIsDisplayed()
        composeRule.onNodeWithText("开始游戏").assertIsDisplayed()
    }

    @Test
    fun menu_start_navigates_to_level_select() {
        composeRule.setContent { BreakoutApp() }
        // 首次进入为主菜单
        composeRule.onNodeWithText("开始游戏").assertIsDisplayed()
        composeRule.onNodeWithText("开始游戏").performClick()
        // 进入关卡选择页
        composeRule.onNodeWithText("选择关卡").assertIsDisplayed()
    }

    @Test
    fun levelSelect_shows_locked_and_unlocked_cards() {
        // maxUnlockedIndex = 2 → 第 1/2/3 关解锁，第 4~10 关锁定
        composeRule.setContent {
            LevelSelectScreen(maxUnlockedIndex = 2, onSelect = {}, onBack = {})
        }
        // 已解锁关卡显示数字
        composeRule.onNodeWithText("1").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithText("3").assertIsDisplayed()
        // 未解锁关卡显示锁图标，且不显示数字
        composeRule.onNodeWithText("4").assertDoesNotExist()
        // 至少存在一个锁图标（第 4 关）
        composeRule.onNodeWithText("🔒", useUnmergedTree = true).assertExists()
    }

    @Test
    fun levelSelect_all_locked_after_full_completion_shows_no_latest_highlight() {
        // maxUnlockedIndex = 9（全部解锁），所有卡片显示数字
        composeRule.setContent {
            LevelSelectScreen(maxUnlockedIndex = 9, onSelect = {}, onBack = {})
        }
        composeRule.onNodeWithText("10").assertIsDisplayed()
        composeRule.onNodeWithText("🔒", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun levelSelect_back_returns_to_menu() {
        composeRule.setContent { BreakoutApp() }
        composeRule.onNodeWithText("开始游戏").performClick()
        composeRule.onNodeWithText("选择关卡").assertIsDisplayed()
        composeRule.onNodeWithText("← 返回").performClick()
        composeRule.onNodeWithText("开始游戏").assertIsDisplayed()
    }
}
