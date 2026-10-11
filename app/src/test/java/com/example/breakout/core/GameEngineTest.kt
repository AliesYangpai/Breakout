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
        assertTrue(e.state.balls.first().velocity.y < 0f)
        assertEquals(Levels.all[0].ballSpeed, e.state.balls.first().velocity.length(), 0.5f)
    }

    @Test
    fun launchBall_noop_when_not_ready() {
        val e = engine()
        e.launchBall()
        val vel = e.state.balls.first().velocity
        e.launchBall() // already RUNNING, should not change
        assertEquals(vel, e.state.balls.first().velocity)
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
        assertEquals(e.state.paddle.centerX, e.state.balls.first().position.x, 0.0001f)
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
        val frozen = e.state.balls.first().position
        e.update(1f / 60f)
        assertEquals(frozen, e.state.balls.first().position)
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

    // ===== REQ-18 底部空白区移动 =====

    @Test
    fun tap_below_paddle_moves_paddle() {
        val e = engine()
        e.launchBall() // RUNNING
        val targetX = 100f
        e.tap(targetX, Field.PADDLE_Y + 20f) // 挡板下方空白区
        assertEquals(targetX, e.state.paddle.centerX, 0.0001f)
        assertEquals(1, e.state.balls.size) // 不发射、不新增球
    }

    @Test
    fun tap_below_paddle_clamps_to_field() {
        val e = engine()
        e.launchBall()
        e.tap(9999f, Field.PADDLE_Y + 20f)
        assertEquals(Field.WIDTH - e.state.paddle.halfWidth, e.state.paddle.centerX, 0.0001f)
    }

    // ===== REQ-16 激光 =====

    @Test
    fun tap_running_with_laser_fires_two_lasers() {
        val e = engine()
        e.launchBall()
        e.activatePowerUp(PowerUpType.LASER)
        e.tap(180f, 100f) // 游戏区（非空白区）
        assertEquals(2, e.state.lasers.size)
        val p = e.state.paddle
        val xs = e.state.lasers.map { it.x }.sorted()
        assertEquals(p.centerX - p.halfWidth, xs[0], 0.0001f)
        assertEquals(p.centerX + p.halfWidth, xs[1], 0.0001f)
    }

    @Test
    fun tap_running_without_laser_does_nothing() {
        val e = engine()
        e.launchBall()
        e.tap(180f, 100f)
        assertEquals(0, e.state.lasers.size)
        assertEquals(GameStatus.RUNNING, e.state.status)
        assertEquals(1, e.state.balls.size)
    }

    @Test
    fun tap_ready_launches_ball_not_laser() {
        val e = engine()
        e.activatePowerUp(PowerUpType.LASER)
        e.tap(180f, 100f) // READY → 发射球
        assertEquals(GameStatus.RUNNING, e.state.status)
        assertEquals(0, e.state.lasers.size)
        assertTrue(e.state.balls.first().velocity.y < 0f)
    }

    @Test
    fun laser_hits_brick_for_one_damage() {
        val e = GameEngine(Levels.all[0], 0)
        // 第 0 关第 0 行第 5 列砖 rect(180,80,216,104)；激光置于其正下方，下一帧上移命中
        val stateWithLaser = e.state.copy(lasers = listOf(Laser(198f, 110f)), status = GameStatus.RUNNING)
        val after = stepLasers(stateWithLaser, 1f / 60f) { null }
        // NORMAL 砖 1 点伤害即摧毁，应被移除
        assertTrue(after.bricks.none { it.row == 0 && it.col == 5 })
    }

    @Test
    fun laser_hits_indestructible_no_damage() {
        val e = GameEngine(Levels.all[6], 6) // 第 7 关含硬砖
        // 第 7 关 row1 col1 是硬砖 rect(36,104,72,128)；激光置于其正下方，下一帧上移命中
        val stateWithLaser = e.state.copy(lasers = listOf(Laser(54f, 135f)), status = GameStatus.RUNNING)
        val after = stepLasers(stateWithLaser, 1f / 60f) { null }
        val brick = after.bricks.first { it.row == 1 && it.col == 1 }
        assertTrue(brick.type.indestructible)
        assertEquals(BrickType.INDESTRUCTIBLE.hits, brick.hp) // 无伤
    }

    // ===== REQ-14/REQ-15 道具效果与叠加 =====

    @Test
    fun wide_paddle_increases_half_width() {
        val e = engine()
        val before = e.state.paddle.halfWidth
        e.activatePowerUp(PowerUpType.WIDE_PADDLE)
        assertEquals(before * Field.PADDLE_WIDEN_FACTOR, e.state.paddle.halfWidth, 0.0001f)
    }

    @Test
    fun multi_ball_adds_second_ball() {
        val e = engine()
        e.launchBall()
        e.activatePowerUp(PowerUpType.MULTI_BALL)
        assertEquals(2, e.state.balls.size)
    }

    @Test
    fun slow_ball_reduces_speed() {
        val e = engine()
        e.launchBall()
        val before = e.state.balls.first().velocity.length()
        e.activatePowerUp(PowerUpType.SLOW_BALL)
        val after = e.state.balls.first().velocity.length()
        assertEquals(before * Field.BALL_SLOW_FACTOR, after, 0.01f)
    }

    @Test
    fun same_powerup_is_idempotent() {
        val e = engine()
        e.activatePowerUp(PowerUpType.WIDE_PADDLE)
        val once = e.state.paddle.halfWidth
        e.activatePowerUp(PowerUpType.WIDE_PADDLE)
        assertEquals(once, e.state.paddle.halfWidth, 0.0001f)
        assertEquals(setOf(PowerUpType.WIDE_PADDLE), e.state.activePowerUps)
    }

    @Test
    fun different_powerups_stack() {
        val e = engine()
        e.activatePowerUp(PowerUpType.WIDE_PADDLE)
        e.activatePowerUp(PowerUpType.LASER)
        assertTrue(PowerUpType.WIDE_PADDLE in e.state.activePowerUps)
        assertTrue(PowerUpType.LASER in e.state.activePowerUps)
    }

    // ===== REQ-17 多球掉命 =====

    @Test
    fun multi_ball_all_dropped_costs_one_life() {
        val e = engine()
        e.launchBall()
        e.activatePowerUp(PowerUpType.MULTI_BALL)
        assertEquals(2, e.state.balls.size)
        e.movePaddleTo(0f) // 移开挡板，让球全部掉落
        var guard = 0
        while (e.state.status == GameStatus.RUNNING && guard < 20000) {
            e.update(1f / 60f)
            guard++
        }
        assertEquals(GameStatus.READY, e.state.status)
        assertEquals(2, e.state.lives) // 全掉光才扣 1 命
    }

    @Test
    fun reset_clears_powerups() {
        val e = engine()
        e.activatePowerUp(PowerUpType.WIDE_PADDLE)
        e.activatePowerUp(PowerUpType.LASER)
        e.reset()
        assertTrue(e.state.activePowerUps.isEmpty())
        assertEquals(Levels.all[0].paddleHalfWidth, e.state.paddle.halfWidth, 0.0001f)
        assertEquals(1, e.state.balls.size)
    }
}
