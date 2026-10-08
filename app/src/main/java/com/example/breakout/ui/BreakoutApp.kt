package com.example.breakout.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.breakout.core.LevelProgress
import com.example.breakout.data.ProgressStore
import com.example.breakout.data.SharedPreferencesProgressStore

sealed interface Screen {
    data object Menu : Screen
    data object LevelSelect : Screen
    data class Game(val levelIndex: Int) : Screen
}

@Composable
fun rememberProgressStore(): ProgressStore {
    val context = LocalContext.current
    return remember { SharedPreferencesProgressStore(context) }
}

@Composable
fun BreakoutApp() {
    val progressStore = rememberProgressStore()
    var screen by remember { mutableStateOf<Screen>(Screen.Menu) }
    var maxUnlocked by remember { mutableIntStateOf(progressStore.load()) }

    BackHandler(enabled = screen != Screen.Menu) {
        screen = when (screen) {
            is Screen.Game -> Screen.LevelSelect
            Screen.LevelSelect -> Screen.Menu
            Screen.Menu -> Screen.Menu
        }
    }

    when (val s = screen) {
        Screen.Menu -> MenuScreen(onStart = { screen = Screen.LevelSelect })

        Screen.LevelSelect -> LevelSelectScreen(
            maxUnlockedIndex = maxUnlocked,
            onSelect = { screen = Screen.Game(it) },
            onBack = { screen = Screen.Menu }
        )

        is Screen.Game -> GameScreen(
            levelIndex = s.levelIndex,
            onExit = { screen = Screen.LevelSelect },
            onNextLevel = { screen = Screen.Game(s.levelIndex + 1) },
            onCompleted = { index ->
                val progress = LevelProgress(maxUnlocked)
                progress.recordCompletion(index)
                maxUnlocked = progress.maxUnlockedIndex
                progressStore.save(maxUnlocked)
            }
        )
    }
}
