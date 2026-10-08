package com.example.breakout.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private fun engine(): GameEngine = GameEngine(Levels.all[0], 0)

    @Test
    fun initialState_builds_bricks_and_is_ready() {
        val e = engine()
        assertEquals(GameStatus.READY, e.state.status)
        assertEquals(3, e.state.lives)
        assertEquals(30, e.state.bricks.size) // 3 rows × 10 cols
    }

    @Test
    fun launchBall_sets_running_and_upward_velocity() {
        val e = engine()
        e.launchBall()
        assertEquals(GameStatus.RUNNING, e.state.status)
        assertTrue(e.state.ball.velocity.y < 0f)
        assertEquals(Levels.all[0].ballSpeed, e.state.ball.velocity.length(), 0.5f)
    }

    @Test
    fun launchBall_noop_when_not_ready() {
        val e = engine()
        e.launchBall()
        val vel = e.state.ball.velocity
        e.launchBall() // already RUNNING, should not change
        assertEquals(vel, e.state.ball.velocity)
    }

    @Test
    fun movePaddle_clamps_to_field() {
        val e = engine()
        e.movePaddleTo(9999f)
        assertEquals(Field.WIDTH - e.state.paddle.halfWidth, e.state.paddle.centerX, 0.0001f)
        e.movePaddleTo(-9999f)
        assertEquals(e.state.paddle.halfWidth, e.state.paddle.centerX, 0.0001f)
    }

    @Test
    fun movePaddle_in_ready_moves_ball_along() {
        val e = engine()
        e.movePaddleTo(Field.WIDTH / 2f - 40f)
        assertEquals(e.state.paddle.centerX, e.state.ball.position.x, 0.0001f)
    }

    @Test
    fun losing_ball_costs_life_and_resets_to_ready() {
        val e = engine()
        e.launchBall()      // 球从中心 x=180 竖直向上发射
        e.movePaddleTo(0f)  // 发射后把挡板移到最左(RUNNING 状态不动球)，球回落时接不住
        // step until ball falls below bottom (misses paddle)
        var guard = 0
        while (e.state.status == GameStatus.RUNNING && guard < 10000) {
            e.update(1f / 60f)
            guard++
        }
        assertEquals(GameStatus.READY, e.state.status)
        assertEquals(2, e.state.lives)
    }

    @Test
    fun pause_and_resume() {
        val e = engine()
        e.launchBall()
        e.pause()
        assertEquals(GameStatus.PAUSED, e.state.status)
        val frozen = e.state.ball.position
        e.update(1f / 60f)
        assertEquals(frozen, e.state.ball.position)
        e.resume()
        assertEquals(GameStatus.RUNNING, e.state.status)
    }

    @Test
    fun detectOutcome_ignores_indestructible() {
        assertNull(detectOutcome(listOf(Brick(0, 0, BrickType.NORMAL))))
        assertNull(detectOutcome(listOf(Brick(0, 0, BrickType.NORMAL), Brick(0, 1, BrickType.INDESTRUCTIBLE))))
        assertEquals(GameStatus.WON, detectOutcome(emptyList()))
        assertEquals(GameStatus.WON, detectOutcome(listOf(Brick(0, 0, BrickType.INDESTRUCTIBLE))))
        assertEquals(GameStatus.WON, detectOutcome(listOf(Brick(0, 0, BrickType.NORMAL, hp = 0))))
    }
}
