# 打砖块游戏（Breakout）实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 Kotlin + Jetpack Compose 实现一款 10 关、难度逐级提升的经典打砖块游戏。

**Architecture:** 纯 Kotlin 核心逻辑（实体、碰撞、关卡数据、游戏引擎、进度）与 Compose UI 层分离。核心包零 Android 依赖，可在 JVM 上用 JUnit 直接单测；UI 层用 Compose `Canvas` + `withFrameNanos` 驱动固定步长游戏循环。关卡数据用字符串网格描述（`D`=普通砖、`2`/`3`=耐久砖、`I`=硬砖、空格=空），改关卡无需改逻辑。

**Tech Stack:** Kotlin 2.0.21、AGP 8.7.2、Jetpack Compose（BOM 2024.10.01）、Material3、SharedPreferences、JUnit 4。

## Global Constraints

- `minSdk = 24`，`targetSdk = 35`，`compileSdk = 35`，JVM target `17`。
- 包名/namespace/applicationId：`com.example.breakout`。
- 纯逻辑代码放在 `core` 包内，**禁止 import 任何 `android.*` 类**（保证 JVM 可测）。
- 固定 3 条命/关（`Field.MAX_LIVES = 3`）。
- 过关规则：击碎全部**可破坏**砖即过关，硬砖不计算在内。
- 难度递增：球速非递减、挡板宽度非递增、硬砖仅第 7 关起出现。
- 语言为 Kotlin 单一语言，不引入 Java 源码、不引入游戏/物理引擎、不引入 Navigation 库（用状态驱动导航）。

---

### Task 1: 项目脚手架

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/example/breakout/MainActivity.kt`
- Create: `app/src/main/java/com/example/breakout/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/example/breakout/ui/BreakoutApp.kt`

**Interfaces:**
- Produces: 可构建、可运行的 Android 工程骨架（后续 Task 在此之上添加 `core` 逻辑与 UI 屏幕）。
- 无 Consumes。

- [ ] **Step 1: 初始化 git 仓库**

```bash
cd /Users/wentianyu/Comprehensive/Develop/AiWork/Breakout
git init
```

- [ ] **Step 2: 创建 `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Breakout"
include(":app")
```

- [ ] **Step 3: 创建根 `build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
}
```

- [ ] **Step 4: 创建 `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 5: 创建 `app/build.gradle.kts`**

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.breakout"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.breakout"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    testImplementation("junit:junit:4.13.2")
}
```

- [ ] **Step 6: 创建 `app/src/main/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:label="打砖块"
        android:allowBackup="true"
        android:supportsRtl="true">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:configChanges="orientation|screenSize|keyboardHidden"
            android:theme="@android:style/Theme.Material.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 7: 创建 `MainActivity.kt`**

```kotlin
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
```

- [ ] **Step 8: 创建 `ui/theme/Theme.kt`**

```kotlin
package com.example.breakout.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun BreakoutTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(), content = content)
}
```

- [ ] **Step 9: 创建临时占位 `ui/BreakoutApp.kt`（后续 Task 8 会替换为完整导航）**

```kotlin
package com.example.breakout.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun BreakoutApp() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("打砖块")
    }
}
```

- [ ] **Step 10: 生成 Gradle wrapper 并构建验证**

> 前置条件：本机需安装 JDK 17 与 Android SDK（或 Android Studio）。若无 `gradle` 命令，可直接用 Android Studio 打开本目录自动同步。

```bash
gradle wrapper --gradle-version 8.9
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`，生成 `app/build/outputs/apk/debug/app-debug.apk`。

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "chore: scaffold Android + Compose project"
```

---

### Task 2: 核心数据模型（Vec2 / 实体 / 砖）

**Files:**
- Create: `app/src/main/java/com/example/breakout/core/Vec2.kt`
- Create: `app/src/main/java/com/example/breakout/core/Entities.kt`
- Test: `app/src/test/java/com/example/breakout/core/EntitiesTest.kt`

**Interfaces:**
- Produces:
  - `data class Vec2(x: Float, y: Float)` — 运算符 `+`、`-`、`*`(scalar)、`unaryMinus`、`length()`、`normalized()`、`dot(other)`。
  - `data class Rect(left, top, right, bottom)` — `centerX()`、`centerY()`、`width()`、`height()`。
  - `data class Paddle(centerX: Float, halfWidth: Float, y: Float, height: Float)`
  - `data class Ball(position: Vec2, velocity: Vec2, radius: Float)`
  - `enum class BrickType(val hits: Int, val indestructible: Boolean = false)`：`NORMAL(1)`、`DURABLE_2(2)`、`DURABLE_3(3)`、`INDESTRUCTIBLE(Int.MAX_VALUE, true)`。
  - `data class Brick(row, col, type, hp = type.hits)` — `val destroyed: Boolean`、`fun takeHit(): Brick`。
- Consumes: 无。

- [ ] **Step 1: 写失败测试 `EntitiesTest.kt`**

```kotlin
package com.example.breakout.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class EntitiesTest {

    @Test
    fun vec2_arithmetic() {
        val a = Vec2(3f, 4f)
        val b = Vec2(1f, 1f)
        assertEquals(Vec2(4f, 5f), a + b)
        assertEquals(Vec2(2f, 3f), a - b)
        assertEquals(Vec2(6f, 8f), a * 2f)
        assertEquals(Vec2(-3f, -4f), -a)
    }

    @Test
    fun vec2_length_and_normalized() {
        assertEquals(5f, Vec2(3f, 4f).length(), 0.0001f)
        val n = Vec2(3f, 4f).normalized()
        assertEquals(1f, n.length(), 0.0001f)
    }

    @Test
    fun vec2_zero_normalized_is_safe() {
        val n = Vec2(0f, 0f).normalized()
        assertEquals(1f, n.length(), 0.0001f)
    }

    @Test
    fun brick_takeHit_reduces_hp() {
        val brick = Brick(0, 0, BrickType.DURABLE_2)
        assertFalse(brick.destroyed)
        val after1 = brick.takeHit()
        assertEquals(1, after1.hp)
        assertFalse(after1.destroyed)
        val after2 = after1.takeHit()
        assertTrue(after2.destroyed)
    }

    @Test
    fun brick_indestructible_never_destroyed() {
        val brick = Brick(0, 0, BrickType.INDESTRUCTIBLE)
        assertFalse(brick.destroyed)
        val after = brick.takeHit()
        assertEquals(brick.hp, after.hp)
        assertFalse(after.destroyed)
    }

    @Test
    fun rect_geometry() {
        val r = Rect(10f, 20f, 30f, 50f)
        assertEquals(20f, r.centerX(), 0.0001f)
        assertEquals(35f, r.centerY(), 0.0001f)
        assertEquals(20f, r.width(), 0.0001f)
        assertEquals(30f, r.height(), 0.0001f)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.breakout.core.EntitiesTest"
```

Expected: FAIL，编译错误（`Vec2`、`Rect`、`Brick` 等未定义）。

- [ ] **Step 3: 实现 `Vec2.kt`**

```kotlin
package com.example.breakout.core

import kotlin.math.sqrt

data class Vec2(val x: Float, val y: Float) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
    operator fun minus(other: Vec2) = Vec2(x - other.x, y - other.y)
    operator fun times(scalar: Float) = Vec2(x * scalar, y * scalar)
    operator fun unaryMinus() = Vec2(-x, -y)
    fun length() = sqrt(x * x + y * y)
    fun normalized(): Vec2 {
        val len = length()
        return if (len == 0f) Vec2(0f, -1f) else Vec2(x / len, y / len)
    }
    fun dot(other: Vec2) = x * other.x + y * other.y
}
```

- [ ] **Step 4: 实现 `Entities.kt`**

```kotlin
package com.example.breakout.core

data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun centerX() = (left + right) / 2f
    fun centerY() = (top + bottom) / 2f
    fun width() = right - left
    fun height() = bottom - top
}

data class Paddle(
    val centerX: Float,
    val halfWidth: Float,
    val y: Float,
    val height: Float
)

data class Ball(
    val position: Vec2,
    val velocity: Vec2,
    val radius: Float
)

enum class BrickType(val hits: Int, val indestructible: Boolean = false) {
    NORMAL(1),
    DURABLE_2(2),
    DURABLE_3(3),
    INDESTRUCTIBLE(Int.MAX_VALUE, indestructible = true)
}

data class Brick(
    val row: Int,
    val col: Int,
    val type: BrickType,
    val hp: Int = type.hits
) {
    val destroyed: Boolean get() = !type.indestructible && hp <= 0
    fun takeHit(): Brick = if (type.indestructible) this else copy(hp = hp - 1)
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.breakout.core.EntitiesTest"
```

Expected: PASS（全部测试通过）。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/breakout/core app/src/test/java/com/example/breakout/core
git commit -m "feat: add core game entities and vector math"
```

---

### Task 3: 场地常量与碰撞逻辑

**Files:**
- Create: `app/src/main/java/com/example/breakout/core/Field.kt`
- Create: `app/src/main/java/com/example/breakout/core/Collision.kt`
- Test: `app/src/test/java/com/example/breakout/core/CollisionTest.kt`

**Interfaces:**
- Produces:
  - `object Field`：`WIDTH=360f`、`HEIGHT=640f`、`COLS=10`、`BRICK_HEIGHT=24f`、`TOP_OFFSET=80f`、`PADDLE_Y=HEIGHT-48f`、`PADDLE_HEIGHT=12f`、`BALL_RADIUS=7f`、`MAX_LIVES=3`。
  - `fun brickRect(row: Int, col: Int): Rect`
  - `fun reflect(velocity: Vec2, normal: Vec2): Vec2`
  - `fun bounceOffWall(ball: Ball): Ball`
  - `fun reflectOffPaddle(velocity: Vec2, ballX: Float, paddle: Paddle, maxBounceDeg: Float = 60f): Vec2`
  - `fun circleRectNormal(center: Vec2, radius: Float, rect: Rect): Vec2?`
- Consumes: Task 2 的 `Vec2`、`Rect`、`Paddle`、`Ball`。

- [ ] **Step 1: 写失败测试 `CollisionTest.kt`**

```kotlin
package com.example.breakout.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionTest {

    private val paddle = Paddle(centerX = 180f, halfWidth = 70f, y = Field.PADDLE_Y, height = Field.PADDLE_HEIGHT)

    @Test
    fun reflect_flips_vertical_wall() {
        val v = reflect(Vec2(100f, -50f), Vec2(1f, 0f))
        assertEquals(-100f, v.x, 0.0001f)
        assertEquals(-50f, v.y, 0.0001f)
    }

    @Test
    fun bounceOffWall_left_wall() {
        val b = Ball(Vec2(2f, 100f), Vec2(-30f, 20f), 7f)
        val out = bounceOffWall(b)
        assertEquals(7f, out.position.x, 0.0001f)
        assertTrue(out.velocity.x > 0f)
    }

    @Test
    fun bounceOffWall_top_wall() {
        val b = Ball(Vec2(100f, 2f), Vec2(30f, -40f), 7f)
        val out = bounceOffWall(b)
        assertEquals(7f, out.position.y, 0.0001f)
        assertTrue(out.velocity.y > 0f)
    }

    @Test
    fun paddle_center_hits_straight_up() {
        val v = reflectOffPaddle(Vec2(0f, -400f), 180f, paddle)
        assertEquals(0f, v.x, 0.01f)
        assertTrue(v.y < 0f)
        assertEquals(400f, v.length(), 0.5f)
    }

    @Test
    fun paddle_edge_hits_sideways() {
        val v = reflectOffPaddle(Vec2(0f, -400f), 250f, paddle)
        assertTrue(v.x > 0f)
        assertTrue(v.y < 0f)
        assertEquals(400f, v.length(), 0.5f)
    }

    @Test
    fun circleRectNormal_outside_is_null() {
        assertNull(circleRectNormal(Vec2(180f, 30f), 7f, brickRect(0, 0)))
    }

    @Test
    fun circleRectNormal_hit_from_below_points_away_from_brick() {
        // 法线约定：从砖表面指向球（外向），用于把球推出砖。
        val rect = brickRect(0, 0)
        val center = Vec2(rect.centerX(), rect.bottom + 3f)
        val n = circleRectNormal(center, 7f, rect)
        assertTrue(n != null && n.y > 0f)
    }

    @Test
    fun brickRect_col_width() {
        val r0 = brickRect(0, 0)
        val r1 = brickRect(0, 1)
        assertEquals(0f, r0.left, 0.0001f)
        assertEquals(r0.right, r1.left, 0.0001f)
        assertEquals(Field.WIDTH / Field.COLS, r0.width(), 0.0001f)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.breakout.core.CollisionTest"
```

Expected: FAIL（`Field`、`Collision` 等未定义）。

- [ ] **Step 3: 实现 `Field.kt`**

```kotlin
package com.example.breakout.core

object Field {
    const val WIDTH = 360f
    const val HEIGHT = 640f
    const val COLS = 10
    const val BRICK_HEIGHT = 24f
    const val TOP_OFFSET = 80f
    const val PADDLE_Y = HEIGHT - 48f
    const val PADDLE_HEIGHT = 12f
    const val BALL_RADIUS = 7f
    const val MAX_LIVES = 3
}
```

- [ ] **Step 4: 实现 `Collision.kt`**

```kotlin
package com.example.breakout.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val DEG_TO_RAD = (Math.PI / 180.0).toFloat()

fun brickRect(row: Int, col: Int): Rect {
    val brickW = Field.WIDTH / Field.COLS
    return Rect(
        left = col * brickW,
        top = Field.TOP_OFFSET + row * Field.BRICK_HEIGHT,
        right = (col + 1) * brickW,
        bottom = Field.TOP_OFFSET + (row + 1) * Field.BRICK_HEIGHT
    )
}

fun reflect(velocity: Vec2, normal: Vec2): Vec2 {
    val n = normal.normalized()
    val d = velocity.dot(n)
    return velocity - n * (2f * d)
}

fun bounceOffWall(ball: Ball): Ball {
    var pos = ball.position
    var vel = ball.velocity
    if (pos.x - ball.radius < 0f) {
        pos = Vec2(ball.radius, pos.y); vel = Vec2(abs(vel.x), vel.y)
    } else if (pos.x + ball.radius > Field.WIDTH) {
        pos = Vec2(Field.WIDTH - ball.radius, pos.y); vel = Vec2(-abs(vel.x), vel.y)
    }
    if (pos.y - ball.radius < 0f) {
        pos = Vec2(pos.x, ball.radius); vel = Vec2(vel.x, abs(vel.y))
    }
    return Ball(pos, vel, ball.radius)
}

fun reflectOffPaddle(
    velocity: Vec2,
    ballX: Float,
    paddle: Paddle,
    maxBounceDeg: Float = 60f
): Vec2 {
    val speed = velocity.length()
    val hitRatio = ((ballX - paddle.centerX) / paddle.halfWidth).coerceIn(-1f, 1f)
    val angle = hitRatio * maxBounceDeg * DEG_TO_RAD
    val newVx = speed * sin(angle)
    val newVy = -speed * cos(angle)
    val minVertical = speed * 0.25f
    val vy = min(newVy, -minVertical)
    val vxMag = sqrt(max(0f, speed * speed - vy * vy))
    val vx = if (newVx >= 0f) vxMag else -vxMag
    return Vec2(vx, vy)
}

fun circleRectNormal(center: Vec2, radius: Float, rect: Rect): Vec2? {
    val closestX = center.x.coerceIn(rect.left, rect.right)
    val closestY = center.y.coerceIn(rect.top, rect.bottom)
    val dx = center.x - closestX
    val dy = center.y - closestY
    val distSq = dx * dx + dy * dy
    if (distSq > radius * radius) return null
    if (distSq == 0f) {
        val leftPen = center.x - rect.left
        val rightPen = rect.right - center.x
        val topPen = center.y - rect.top
        val bottomPen = rect.bottom - center.y
        val minPen = min(min(leftPen, rightPen), min(topPen, bottomPen))
        return when (minPen) {
            leftPen -> Vec2(-1f, 0f)
            rightPen -> Vec2(1f, 0f)
            topPen -> Vec2(0f, -1f)
            else -> Vec2(0f, 1f)
        }
    }
    val dist = sqrt(distSq)
    return Vec2(dx / dist, dy / dist)
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.breakout.core.CollisionTest"
```

Expected: PASS。

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/breakout/core app/src/test/java/com/example/breakout/core
git commit -m "feat: add field constants and collision logic"
```

---

### Task 4: 关卡数据（10 关定义）

**Files:**
- Create: `app/src/main/java/com/example/breakout/core/Levels.kt`
- Test: `app/src/test/java/com/example/breakout/core/LevelsTest.kt`

**Interfaces:**
- Produces:
  - `data class LevelConfig(val ballSpeed: Float, val paddleHalfWidth: Float, val rows: List<List<BrickType?>>)`。
  - `object Levels { val all: List<LevelConfig> }` — 长度 10，索引 0 对应第 1 关。
- Consumes: Task 2 的 `BrickType`，Task 3 的 `Field`。

- [ ] **Step 1: 写失败测试 `LevelsTest.kt`**

```kotlin
package com.example.breakout.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelsTest {

    @Test
    fun exactly_ten_levels() {
        assertEquals(10, Levels.all.size)
    }

    @Test
    fun every_level_has_at_least_one_destructible_brick() {
        Levels.all.forEachIndexed { i, level ->
            val hasDestructible = level.rows.flatten().any {
                it != null && !it.indestructible
            }
            assertTrue("level ${i + 1} has no destructible brick", hasDestructible)
        }
    }

    @Test
    fun indestructible_only_from_level_7() {
        Levels.all.take(6).forEachIndexed { i, level ->
            val hasIndestructible = level.rows.flatten().any { it?.indestructible == true }
            assertTrue("level ${i + 1} must not have indestructible bricks", !hasIndestructible)
        }
        Levels.all.drop(6).forEachIndexed { i, level ->
            val hasIndestructible = level.rows.flatten().any { it?.indestructible == true }
            assertTrue("level ${i + 7} should have indestructible bricks", hasIndestructible)
        }
    }

    @Test
    fun ball_speed_non_decreasing() {
        Levels.all.zipWithNext { a, b ->
            assertTrue("ball speed must be non-decreasing", b.ballSpeed >= a.ballSpeed)
        }
    }

    @Test
    fun paddle_width_non_increasing() {
        Levels.all.zipWithNext { a, b ->
            assertTrue("paddle width must be non-increasing", b.paddleHalfWidth <= a.paddleHalfWidth)
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.breakout.core.LevelsTest"
```

Expected: FAIL（`Levels` 未定义）。

- [ ] **Step 3: 实现 `Levels.kt`**

```kotlin
package com.example.breakout.core

data class LevelConfig(
    val ballSpeed: Float,
    val paddleHalfWidth: Float,
    val rows: List<List<BrickType?>>
)

object Levels {

    // 行字符串：D=普通砖  2/3=耐久砖  I=硬砖  其他字符=空
    private fun rows(vararg lines: String): List<List<BrickType?>> = lines.map { line ->
        line.take(Field.COLS).map { c ->
            when (c) {
                'D' -> BrickType.NORMAL
                '2' -> BrickType.DURABLE_2
                '3' -> BrickType.DURABLE_3
                'I' -> BrickType.INDESTRUCTIBLE
                else -> null
            }
        }
    }

    val all: List<LevelConfig> = listOf(
        // 第 1 关：入门，3 排普通砖，慢球，宽挡板
        LevelConfig(320f, 70f, rows(
            "DDDDDDDDDD",
            "DDDDDDDDDD",
            "DDDDDDDDDD",
        )),
        // 第 2 关：4 排普通砖
        LevelConfig(340f, 70f, rows(
            "DDDDDDDDDD",
            "DDDDDDDDDD",
            "DDDDDDDDDD",
            "DDDDDDDDDD",
        )),
        // 第 3 关：5 排普通砖，标准挡板
        LevelConfig(380f, 55f, rows(
            "DDDDDDDDDD",
            "DDDDDDDDDD",
            "DDDDDDDDDD",
            "DDDDDDDDDD",
            "DDDDDDDDDD",
        )),
        // 第 4 关：引入耐久砖(2击)
        LevelConfig(400f, 55f, rows(
            "DDDDDDDDDD",
            "2222222222",
            "DDDDDDDDDD",
            "2222222222",
            "DDDDDDDDDD",
        )),
        // 第 5 关：耐久砖比例上升
        LevelConfig(420f, 55f, rows(
            "DDDDDDDDDD",
            "2222222222",
            "2222222222",
            "DDDDDDDDDD",
            "2222222222",
            "DDDDDDDDDD",
        )),
        // 第 6 关：高密度 + 3 击耐久砖
        LevelConfig(460f, 55f, rows(
            "2222222222",
            "DDDDDDDDDD",
            "2222222222",
            "3333333333",
            "DDDDDDDDDD",
            "2222222222",
        )),
        // 第 7 关：引入硬砖（障碍柱），窄挡板
        LevelConfig(460f, 40f, rows(
            "DDDDDDDDDD",
            "DIDDDDDDID",
            "DDDDDDDDDD",
            "DIDDDDDDID",
            "DDDDDDDDDD",
        )),
        // 第 8 关：硬砖 + 耐久砖混合
        LevelConfig(480f, 40f, rows(
            "DDDDDDDDDD",
            "DIDDDDDDID",
            "22DDDDDD22",
            "DIDDDDDDID",
            "DDDDDDDDDD",
            "2222222222",
        )),
        // 第 9 关：硬砖增多，布局更刁钻
        LevelConfig(500f, 40f, rows(
            "DDDDDDDDDD",
            "DIIDDDDIID",
            "DDDDDDDDDD",
            "DIIDDDDIID",
            "DDDDDDDDDD",
            "DIIDDDDIID",
            "DDDDDDDDDD",
        )),
        // 第 10 关：终局，全部元素 + 高密度
        LevelConfig(520f, 40f, rows(
            "DDDDDDDDDD",
            "2222222222",
            "DIIDDDDIID",
            "DDDDDDDDDD",
            "2222222222",
            "DIIDDDDIID",
            "DDDDDDDDDD",
            "3333333333",
        )),
    )
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.breakout.core.LevelsTest"
```

Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/breakout/core app/src/test/java/com/example/breakout/core
git commit -m "feat: define 10 levels with progressive difficulty"
```

---

### Task 5: 游戏引擎（状态机 + 更新逻辑）

**Files:**
- Create: `app/src/main/java/com/example/breakout/core/GameEngine.kt`
- Test: `app/src/test/java/com/example/breakout/core/GameEngineTest.kt`

**Interfaces:**
- Produces:
  - `enum class GameStatus { READY, RUNNING, PAUSED, WON, LOST }`
  - `data class GameState(levelIndex, lives, status, paddle, ball, bricks)`
  - `class GameEngine(config: LevelConfig, levelIndex: Int)`
    - `var state: GameState`（private set）
    - `fun reset()`
    - `fun movePaddleTo(x: Float)`
    - `fun launchBall()`
    - `fun pause()` / `fun resume()`
    - `fun update(dt: Float)`
  - `fun detectOutcome(bricks: List<Brick>): GameStatus?`（纯函数，返回 `WON` 或 `null`）。
- Consumes: Task 2（实体）、Task 3（`Field`、`brickRect`、`bounceOffWall`、`reflectOffPaddle`、`circleRectNormal`、`reflect`）、Task 4（`LevelConfig`）。

- [ ] **Step 1: 写失败测试 `GameEngineTest.kt`**

```kotlin
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
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.breakout.core.GameEngineTest"
```

Expected: FAIL（`GameEngine`、`GameStatus`、`GameState`、`detectOutcome` 未定义）。

- [ ] **Step 3: 实现 `GameEngine.kt`**

```kotlin
package com.example.breakout.core

import kotlin.math.abs

enum class GameStatus { READY, RUNNING, PAUSED, WON, LOST }

data class GameState(
    val levelIndex: Int,
    val lives: Int,
    val status: GameStatus,
    val paddle: Paddle,
    val ball: Ball,
    val bricks: List<Brick>
)

fun detectOutcome(bricks: List<Brick>): GameStatus? {
    val hasDestructible = bricks.any { !it.destroyed && !it.type.indestructible }
    return if (hasDestructible) null else GameStatus.WON
}

class GameEngine(private val config: LevelConfig, val levelIndex: Int) {

    var state: GameState = initialState()
        private set

    private fun buildBricks(): List<Brick> {
        val result = mutableListOf<Brick>()
        config.rows.forEachIndexed { row, cells ->
            cells.forEachIndexed { col, type ->
                if (type != null) result.add(Brick(row, col, type))
            }
        }
        return result
    }

    private fun restingBall(centerX: Float): Ball = Ball(
        position = Vec2(centerX, Field.PADDLE_Y - Field.PADDLE_HEIGHT / 2f - Field.BALL_RADIUS - 1f),
        velocity = Vec2(0f, 0f),
        radius = Field.BALL_RADIUS
    )

    fun initialState(): GameState {
        val paddle = Paddle(
            centerX = Field.WIDTH / 2f,
            halfWidth = config.paddleHalfWidth,
            y = Field.PADDLE_Y,
            height = Field.PADDLE_HEIGHT
        )
        return GameState(
            levelIndex = levelIndex,
            lives = Field.MAX_LIVES,
            status = GameStatus.READY,
            paddle = paddle,
            ball = restingBall(paddle.centerX),
            bricks = buildBricks()
        )
    }

    fun reset() {
        state = initialState()
    }

    fun movePaddleTo(x: Float) {
        val half = state.paddle.halfWidth
        val clamped = x.coerceIn(half, Field.WIDTH - half)
        state = state.copy(paddle = state.paddle.copy(centerX = clamped))
        if (state.status == GameStatus.READY) {
            state = state.copy(ball = restingBall(clamped))
        }
    }

    fun launchBall() {
        if (state.status != GameStatus.READY) return
        state = state.copy(
            status = GameStatus.RUNNING,
            ball = state.ball.copy(velocity = Vec2(0f, -config.ballSpeed))
        )
    }

    fun pause() {
        if (state.status == GameStatus.RUNNING) state = state.copy(status = GameStatus.PAUSED)
    }

    fun resume() {
        if (state.status == GameStatus.PAUSED) state = state.copy(status = GameStatus.RUNNING)
    }

    fun update(dt: Float) {
        if (state.status != GameStatus.RUNNING) return

        var ball = state.ball.copy(position = state.ball.position + state.ball.velocity * dt)
        var bricks = state.bricks

        ball = bounceOffWall(ball)

        val paddle = state.paddle
        val paddleTop = paddle.y - paddle.height / 2f
        if (ball.velocity.y > 0f &&
            ball.position.y + ball.radius >= paddleTop &&
            ball.position.y + ball.radius <= paddleTop + paddle.height + ball.radius &&
            abs(ball.position.x - paddle.centerX) <= paddle.halfWidth + ball.radius
        ) {
            ball = ball.copy(
                velocity = reflectOffPaddle(ball.velocity, ball.position.x, paddle),
                position = Vec2(ball.position.x, paddleTop - ball.radius)
            )
        }

        var velocity = ball.velocity
        var position = ball.position
        val remaining = mutableListOf<Brick>()
        for (brick in bricks) {
            if (brick.destroyed) continue
            val rect = brickRect(brick.row, brick.col)
            val normal = circleRectNormal(position, ball.radius, rect)
            if (normal != null) {
                velocity = reflect(velocity, normal)
                position = position + normal * (ball.radius + 0.5f)
                val hit = brick.takeHit()
                if (!hit.destroyed) remaining.add(hit)
            } else {
                remaining.add(brick)
            }
        }
        bricks = remaining
        ball = ball.copy(position = position, velocity = velocity)

        if (ball.position.y - ball.radius > Field.HEIGHT) {
            val lives = state.lives - 1
            if (lives <= 0) {
                state = state.copy(lives = 0, status = GameStatus.LOST, ball = ball)
            } else {
                state = state.copy(
                    lives = lives,
                    status = GameStatus.READY,
                    ball = restingBall(paddle.centerX),
                    bricks = bricks
                )
            }
            return
        }

        val status = detectOutcome(bricks) ?: GameStatus.RUNNING
        state = state.copy(ball = ball, bricks = bricks, status = status)
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.breakout.core.GameEngineTest"
```

Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/breakout/core app/src/test/java/com/example/breakout/core
git commit -m "feat: add game engine with state machine and collision resolution"
```

---

### Task 6: 进度存档（解锁逻辑 + SharedPreferences）

**Files:**
- Create: `app/src/main/java/com/example/breakout/core/LevelProgress.kt`
- Create: `app/src/main/java/com/example/breakout/data/ProgressStore.kt`
- Create: `app/src/main/java/com/example/breakout/data/SharedPreferencesProgressStore.kt`
- Test: `app/src/test/java/com/example/breakout/core/LevelProgressTest.kt`

**Interfaces:**
- Produces:
  - `class LevelProgress(initialMaxUnlockedIndex: Int = 0)`：`var maxUnlockedIndex`（private set）、`fun isUnlocked(index: Int): Boolean`、`fun recordCompletion(index: Int)`。
  - `interface ProgressStore { fun load(): Int; fun save(maxUnlockedIndex: Int) }`
  - `class SharedPreferencesProgressStore(context: Context) : ProgressStore`
- Consumes: 无（`data` 包依赖 Android `Context`，不参与 JVM 单测）。

- [ ] **Step 1: 写失败测试 `LevelProgressTest.kt`**

```kotlin
package com.example.breakout.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelProgressTest {

    @Test
    fun initially_only_level_0_unlocked() {
        val p = LevelProgress()
        assertTrue(p.isUnlocked(0))
        assertFalse(p.isUnlocked(1))
        assertFalse(p.isUnlocked(9))
    }

    @Test
    fun completing_a_level_unlocks_the_next() {
        val p = LevelProgress()
        p.recordCompletion(0)
        assertEquals(1, p.maxUnlockedIndex)
        assertTrue(p.isUnlocked(0))
        assertTrue(p.isUnlocked(1))
        assertFalse(p.isUnlocked(2))
    }

    @Test
    fun completion_is_monotonic() {
        val p = LevelProgress()
        p.recordCompletion(3)
        assertEquals(4, p.maxUnlockedIndex)
        p.recordCompletion(1) // completing an earlier level must not reduce progress
        assertEquals(4, p.maxUnlockedIndex)
    }

    @Test
    fun completing_last_level_stays_within_bounds() {
        val p = LevelProgress()
        p.recordCompletion(9)
        assertEquals(10, p.maxUnlockedIndex)
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.breakout.core.LevelProgressTest"
```

Expected: FAIL（`LevelProgress` 未定义）。

- [ ] **Step 3: 实现 `LevelProgress.kt`**

```kotlin
package com.example.breakout.core

class LevelProgress(initialMaxUnlockedIndex: Int = 0) {
    var maxUnlockedIndex: Int = initialMaxUnlockedIndex
        private set

    fun isUnlocked(index: Int) = index <= maxUnlockedIndex

    fun recordCompletion(index: Int) {
        val next = index + 1
        if (next > maxUnlockedIndex) maxUnlockedIndex = next
    }
}
```

- [ ] **Step 4: 实现 `data/ProgressStore.kt`**

```kotlin
package com.example.breakout.data

interface ProgressStore {
    fun load(): Int
    fun save(maxUnlockedIndex: Int)
}
```

- [ ] **Step 5: 实现 `data/SharedPreferencesProgressStore.kt`**

```kotlin
package com.example.breakout.data

import android.content.Context

class SharedPreferencesProgressStore(context: Context) : ProgressStore {

    private val prefs = context.getSharedPreferences("breakout_progress", Context.MODE_PRIVATE)

    override fun load(): Int = prefs.getInt(KEY_MAX_UNLOCKED, 0)

    override fun save(maxUnlockedIndex: Int) {
        prefs.edit().putInt(KEY_MAX_UNLOCKED, maxUnlockedIndex).apply()
    }

    private companion object {
        const val KEY_MAX_UNLOCKED = "max_unlocked_index"
    }
}
```

- [ ] **Step 6: 运行测试确认通过**

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.breakout.core.LevelProgressTest"
```

Expected: PASS。

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/breakout/core app/src/main/java/com/example/breakout/data app/src/test/java/com/example/breakout/core
git commit -m "feat: add level progress tracking and persistence"
```

---

### Task 7: 游戏画面（Canvas 渲染 + 游戏循环 + 输入 + HUD/弹窗）

**Files:**
- Create: `app/src/main/java/com/example/breakout/ui/GameScreen.kt`

**Interfaces:**
- Produces: `@Composable fun GameScreen(levelIndex: Int, onExit: () -> Unit, onCompleted: (Int) -> Unit, onNextLevel: () -> Unit)`
  - 游戏循环用 `withFrameNanos` 驱动；Canvas 把逻辑坐标(360×640)等比缩放到实际像素。
  - `onCompleted(levelIndex)` 在状态变为 `WON` 时回调一次。
  - `onNextLevel()` 在非最后一关过关弹窗点"下一关"时回调。
  - `onExit()` 在失败/过关/暂停弹窗点"返回关卡选择"时回调。
- Consumes: Task 5 的 `GameEngine`/`GameStatus`、Task 4 的 `Levels`、Task 3 的 `Field`/`brickRect`、Task 2 的 `Brick`/`BrickType`/`Paddle`/`Ball`。

- [ ] **Step 1: 实现 `GameScreen.kt`（完整代码）**

```kotlin
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
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
import com.example.breakout.core.brickRect
import kotlin.math.abs
import kotlin.math.min

private val PADDLE_COLOR = Color(0xFF4FC3F7)
private val BALL_COLOR = Color.White
private val NORMAL_BRICK_COLOR = Color(0xFFEF5350)
private val DURABLE_BRICK_COLOR = Color(0xFFFFB74D)
private val INDESTRUCTIBLE_COLOR = Color(0xFF9E9E9E)

@Composable
fun GameScreen(
    levelIndex: Int,
    onExit: () -> Unit,
    onCompleted: (Int) -> Unit,
    onNextLevel: () -> Unit
) {
    val engine = remember(levelIndex) { GameEngine(Levels.all[levelIndex], levelIndex) }
    var gameState by remember { mutableStateOf(engine.state) }
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
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
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
                    .pointerInput(engine) {
                        awaitEachGesture {
                            // 触摸坐标是像素，需换算回逻辑坐标(0..Field.WIDTH)再传给引擎
                            val fieldScale = min(size.width / Field.WIDTH, size.height / Field.HEIGHT)
                            val fieldOffsetX = (size.width - Field.WIDTH * fieldScale) / 2f
                            fun toLogicalX(px: Float) = (px - fieldOffsetX) / fieldScale

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
                                    if (!isDrag && engine.state.status == GameStatus.READY) {
                                        engine.launchBall()
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

                // 球
                val ball = gameState.ball
                drawCircle(
                    color = BALL_COLOR,
                    radius = ball.radius * scale,
                    center = Offset(lx(ball.position.x), ly(ball.position.y))
                )
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
```

- [ ] **Step 2: 构建验证**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/breakout/ui/GameScreen.kt
git commit -m "feat: add game screen with canvas rendering and input"
```

---

### Task 8: 菜单、关卡选择页与导航

**Files:**
- Modify: `app/src/main/java/com/example/breakout/ui/BreakoutApp.kt`
- Create: `app/src/main/java/com/example/breakout/ui/MenuScreen.kt`
- Create: `app/src/main/java/com/example/breakout/ui/LevelSelectScreen.kt`

**Interfaces:**
- Produces:
  - `sealed interface Screen { data object Menu; data object LevelSelect; data class Game(levelIndex: Int) }`
  - `@Composable fun MenuScreen(onStart: () -> Unit)`
  - `@Composable fun LevelSelectScreen(maxUnlockedIndex: Int, onSelect: (Int) -> Unit, onBack: () -> Unit)`
  - `@Composable fun BreakoutApp()` — 状态驱动导航，整合进度存档。
- Consumes: Task 7 的 `GameScreen`、Task 6 的 `ProgressStore`/`SharedPreferencesProgressStore`、Task 5 的 `Levels`。

- [ ] **Step 1: 实现 `MenuScreen.kt`**

```kotlin
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
```

- [ ] **Step 2: 实现 `LevelSelectScreen.kt`**

```kotlin
package com.example.breakout.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.breakout.core.Levels

@Composable
fun LevelSelectScreen(
    maxUnlockedIndex: Int,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 返回") }
            Text("选择关卡", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 8.dp))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Levels.all.indices.toList()) { index ->
                LevelCard(
                    number = index + 1,
                    unlocked = index <= maxUnlockedIndex,
                    isLatest = index == maxUnlockedIndex && index < Levels.all.size,
                    onClick = { if (index <= maxUnlockedIndex) onSelect(index) }
                )
            }
        }
    }
}

@Composable
private fun LevelCard(
    number: Int,
    unlocked: Boolean,
    isLatest: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        !unlocked -> MaterialTheme.colorScheme.surfaceVariant
        isLatest -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (!unlocked) Color.Gray else Color.White

    Card(
        modifier = Modifier
            .size(64.dp)
            .clickable(enabled = unlocked, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (unlocked) number.toString() else "🔒",
                color = contentColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}
```

- [ ] **Step 3: 替换 `ui/BreakoutApp.kt`**

```kotlin
package com.example.breakout.ui

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
```

- [ ] **Step 4: 构建验证**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/breakout/ui
git commit -m "feat: add menu, level select, and app navigation"
```

---

### Task 9: 集成收尾与全量验证

**Files:**
- Modify: `app/src/main/java/com/example/breakout/ui/BreakoutApp.kt`（补充返回键处理，见 Step 1）

**Interfaces:**
- Consumes: Task 7/8 全部。
- Produces: 完整可玩版本。

- [ ] **Step 1: 在 `BreakoutApp` 中补充系统返回键处理（退回上一屏）**

在 `BreakoutApp()` 的 `when` 之前插入：

```kotlin
    BackHandler(enabled = screen != Screen.Menu) {
        screen = when (screen) {
            is Screen.Game -> Screen.LevelSelect
            Screen.LevelSelect -> Screen.Menu
            Screen.Menu -> Screen.Menu
        }
    }
```

并在文件顶部 import 增加：

```kotlin
import androidx.activity.compose.BackHandler
```

- [ ] **Step 2: 全量测试**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: 全部测试 PASS（`EntitiesTest`、`CollisionTest`、`LevelsTest`、`GameEngineTest`、`LevelProgressTest`）。

- [ ] **Step 3: 全量构建**

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`。

- [ ] **Step 4: 手动验收（安装到真机/模拟器）**

按以下清单逐项验证：
1. 启动进入主菜单，点击"开始游戏"→ 关卡选择页，仅第 1 关可点。
2. 进入第 1 关，拖动挡板跟随手指；点击发射球；击碎全部砖弹出"过关"。
3. 过关后第 2 关解锁；杀进程重开 app，解锁进度保留。
4. 第 7 关出现灰色硬砖，撞击不消失、球反弹。
5. 球掉落扣一命（♥ 变 ♡），3 命耗尽弹"失败"，可重新开始。
6. 暂停弹窗：继续/重新开始/返回关卡选择均正常。
7. 切后台再回前台，自动进入暂停弹窗。
8. 第 10 关通关显示"恭喜通关"。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/breakout/ui/BreakoutApp.kt
git commit -m "feat: handle system back button and finalize integration"
```

---

## Self-Review 结论

- **Spec 覆盖**：需求文档的 6 大章节均已映射——架构(Task1/5/7)、实体碰撞(Task2/3)、难度曲线(Task4)、状态流转(Task5/7)、错误处理(Task3 的防卡死 + Task5 的钳制/生命重置 + Task7 的切后台自动暂停)。交互文档的页面流转(Task8)、拖动/点击发射(Task7)、碰撞反馈(Task7 的 `brickColor`)。技术选型文档的 Compose+Canvas+SharedPreferences(Task1/6/7)。
- **占位符**：无 TBD/TODO；每个代码步骤均含完整代码。
- **类型一致性**：`LevelConfig(ballSpeed, paddleHalfWidth, rows)`、`GameEngine(config, levelIndex)`、`detectOutcome(bricks)`、`ProgressStore.load()/save()` 在各 Task 间签名一致。
