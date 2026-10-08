package com.example.breakout.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MenuScreen(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("打砖块", style = MaterialTheme.typography.displayMedium)
        Text("共 10 关 · 难度逐级提升", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
        Button(onClick = onStart, modifier = Modifier.padding(top = 32.dp)) {
            Text("开始游戏")
        }
    }
}
