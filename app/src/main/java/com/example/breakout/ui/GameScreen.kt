package com.example.breakout.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.breakout.core.Brick
import com.example.breakout.core.BrickType
import com.example.breakout.core.Field
import com.example.breakout.core.GameEngine
import com.example.breakout.core.GameStatus
import com.example.breakout.core.Levels
import com.example.breakout.core.PowerUpType
import com.example.breakout.core.brickRect
import kotlin.math.abs
import kotlin.math.min

private val PADDLE_COLOR = Color(0xFF4FC3F7)
private val BALL_COLOR = Color.White
private val NORMAL_BRICK_COLOR = Color(0xFFEF5350)
private val DURABLE_BRICK_COLOR = Color(0xFFFFB74D)
private val INDESTRUCTIBLE_COLOR = Color(0xFF9E9E9E)
private val LASER_COLOR = Color(0xFFE040FB)

@Composable
fun GameScreen(
    levelIndex: Int,
    onExit: () -> Unit,
    onCompleted: (Int) -> Unit,
    onNextLevel: () -> Unit
) {
    val engine = remember(levelIndex) { GameEngine(Levels.all[levelIndex], levelIndex) }
    var gameState by remember(levelIndex) { mutableStateOf(engine.state) }
    var showPause by remember { mutableStateOf(false) }

    // 游戏循环
    LaunchedEffect(engine) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last == 0L) last = now
                val dt = ((now - last) / 1_000_000_000f).coerceAtMost(1f / 30f)
                last = now
                if (engine.state.status == GameStatus.RUNNING) engine.update(dt)
                gameState = engine.state
            }
        }
    }

    // 过关回调（仅一次）
    LaunchedEffect(gameState.status) {
        if (gameState.status == GameStatus.WON) onCompleted(levelIndex)
    }

    // 切后台自动暂停
    // key 用 engine：切关后 engine 重建，观察器必须重新绑定到新 engine，否则闭包持有旧 engine
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(engine, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && engine.state.status == GameStatus.RUNNING) {
                engine.pause()
                gameState = engine.state
                showPause = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        Column(Modifier.fillMaxSize()) {
            Hud(
                level = levelIndex + 1,
                lives = gameState.lives,
                onPause = {
                    engine.pause()
                    gameState = engine.state
                    showPause = true
                }
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("game_canvas")
                    .pointerInput(engine) {
                        awaitEachGesture {
                            // 触摸坐标是像素，需换算回逻辑坐标(0..Field.WIDTH/HEIGHT)再传给引擎
                            val fieldScale = min(size.width / Field.WIDTH, size.height / Field.HEIGHT)
                            val fieldOffsetX = (size.width - Field.WIDTH * fieldScale) / 2f
                            val fieldOffsetY = (size.height - Field.HEIGHT * fieldScale) / 2f
                            fun toLogicalX(px: Float) = (px - fieldOffsetX) / fieldScale
                            fun toLogicalY(py: Float) = (py - fieldOffsetY) / fieldScale

                            val down = awaitFirstDown(requireUnconsumed = false)
                            val startX = down.position.x
                            var isDrag = false
                            down.consume()
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.positionChanged()) {
                                    val dx = change.position.x - startX
                                    if (abs(dx) > 8f) isDrag = true
                                    if (isDrag) engine.movePaddleTo(toLogicalX(change.position.x))
                                    change.consume()
                                }
                                if (!change.pressed) {
                                    if (!isDrag) {
                                        // 单击分流交给引擎：空白区移动 / READY 发球 / 持激光发射激光
                                        engine.tap(toLogicalX(change.position.x), toLogicalY(change.position.y))
                                    }
                                    gameState = engine.state
                                    break
                                }
                            }
                        }
                    }
            ) {
                val scale = min(size.width / Field.WIDTH, size.height / Field.HEIGHT)
                val offsetX = (size.width - Field.WIDTH * scale) / 2f
                val offsetY = (size.height - Field.HEIGHT * scale) / 2f
                fun lx(x: Float) = offsetX + x * scale
                fun ly(y: Float) = offsetY + y * scale

                // 砖块
                gameState.bricks.forEach { brick ->
                    val rect = brickRect(brick.row, brick.col)
                    val color = brickColor(brick)
                    drawRect(
                        color = color,
                        topLeft = Offset(lx(rect.left), ly(rect.top)),
                        size = Size((rect.right - rect.left) * scale, (rect.bottom - rect.top) * scale)
                    )
                }

                // 挡板
                val paddle = gameState.paddle
                drawRect(
                    color = PADDLE_COLOR,
                    topLeft = Offset(lx(paddle.centerX - paddle.halfWidth), ly(paddle.y - paddle.height / 2f)),
                    size = Size(paddle.halfWidth * 2f * scale, paddle.height * scale)
                )
                if (PowerUpType.LASER in gameState.activePowerUps) {
                    drawRect(
                        color = LASER_COLOR,
                        topLeft = Offset(lx(paddle.centerX - paddle.halfWidth), ly(paddle.y - paddle.height / 2f)),
                        size = Size(paddle.halfWidth * 2f * scale, paddle.height * scale),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // 球（多球）
                gameState.balls.forEach { ball ->
                    drawCircle(
                        color = BALL_COLOR,
                        radius = ball.radius * scale,
                        center = Offset(lx(ball.position.x), ly(ball.position.y))
                    )
                }

                // 掉落物
                gameState.drops.forEach { drop ->
                    val color = powerUpColor(drop.type)
                    drawCircle(
                        color = color,
                        radius = Field.DROP_RADIUS * scale,
                        center = Offset(lx(drop.position.x), ly(drop.position.y))
                    )
                }

                // 激光
                gameState.lasers.forEach { laser ->
                    drawRect(
                        color = LASER_COLOR,
                        topLeft = Offset(lx(laser.x - Field.LASER_RADIUS), ly(laser.y - 12f)),
                        size = Size(Field.LASER_RADIUS * 2f * scale, 24f * scale)
                    )
                }
            }
        }

        if (showPause) {
            PauseDialog(
                onResume = { showPause = false; engine.resume() },
                onRestart = { showPause = false; engine.reset(); gameState = engine.state },
                onExit = { showPause = false; onExit() }
            )
        }

        if (gameState.status == GameStatus.WON) {
            WonDialog(
                isLastLevel = levelIndex == Levels.all.size - 1,
                onNext = { if (levelIndex == Levels.all.size - 1) onExit() else onNextLevel() },
                onExit = onExit
            )
        }

        if (gameState.status == GameStatus.LOST) {
            LostDialog(
                onRestart = { engine.reset(); gameState = engine.state },
                onExit = onExit
            )
        }
    }
}

private fun brickColor(brick: Brick): Color = when {
    brick.type.indestructible -> INDESTRUCTIBLE_COLOR
    brick.type == BrickType.NORMAL -> NORMAL_BRICK_COLOR
    else -> {
        val max = if (brick.type == BrickType.DURABLE_3) 3 else 2
        val ratio = brick.hp.toFloat() / max
        DURABLE_BRICK_COLOR.copy(alpha = 0.4f + 0.6f * ratio)
    }
}

private fun powerUpColor(type: PowerUpType): Color = when (type) {
    PowerUpType.WIDE_PADDLE -> Color(0xFF80D8FF)
    PowerUpType.MULTI_BALL -> Color(0xFFB9F6CA)
    PowerUpType.SLOW_BALL -> Color(0xFFFFF59D)
    PowerUpType.LASER -> LASER_COLOR
}

@Composable
private fun Hud(level: Int, lives: Int, onPause: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("第 $level 关", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Text(
            "♥".repeat(lives) + "♡".repeat(Field.MAX_LIVES - lives),
            color = Color(0xFFFF5252),
            style = MaterialTheme.typography.titleMedium
        )
        TextButton(onClick = onPause) { Text("暂停", color = Color.White) }
    }
}

@Composable
private fun PauseDialog(onResume: () -> Unit, onRestart: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onResume,
        title = { Text("暂停") },
        text = { Text("游戏已暂停") },
        confirmButton = { TextButton(onClick = onResume) { Text("继续") } },
        dismissButton = {
            Column {
                TextButton(onClick = onRestart) { Text("重新开始") }
                TextButton(onClick = onExit) { Text("返回关卡选择") }
            }
        }
    )
}

@Composable
private fun WonDialog(isLastLevel: Boolean, onNext: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onExit,
        title = { Text(if (isLastLevel) "恭喜通关！" else "过关！") },
        text = { Text(if (isLastLevel) "你已通关全部 10 个关卡" else "击碎了全部砖块") },
        confirmButton = { Button(onClick = onNext) { Text(if (isLastLevel) "返回关卡选择" else "下一关") } }
    )
}

@Composable
private fun LostDialog(onRestart: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onExit,
        title = { Text("失败") },
        text = { Text("3 条命已用完") },
        confirmButton = { Button(onClick = onRestart) { Text("重新开始") } },
        dismissButton = { TextButton(onClick = onExit) { Text("返回关卡选择") } }
    )
}
