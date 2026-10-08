package com.example.breakout

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.breakout.ui.BreakoutApp
import com.example.breakout.ui.theme.BreakoutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BreakoutTheme {
                BreakoutApp()
            }
        }
    }
}
