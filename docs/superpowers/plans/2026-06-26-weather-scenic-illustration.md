# 底部天气剖影插图 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在首页底部加一条随天气变化的纯代码矢量景观剖影(层叠山丘地平线),位于天空+粒子之上、卡片之下,提升整体样式且包体零增长。

**Architecture:** 抽出纯函数 `ScenicPalettes.from(icon) -> ScenicPalette`(8 场景调色板,可单测);新增 `WeatherScenicView`(Canvas 绘制分层山丘 + 场景切换淡化),作为 `pageRoot` 的子视图插在 `WeatherFxView` 之前(底部锚定);`WeatherPageFragment` 在已有 `icon` 处调 `setScene(icon)` 同步驱动。

**Tech Stack:** Kotlin · Android View `onDraw` + `Canvas`/`Path`/`Paint` · JUnit 4(纯函数单测)· 无新增依赖、无资源、包体不变。

设计依据:`docs/superpowers/specs/2026-06-26-weather-scenic-illustration-design.md`

---

## 文件结构

- **新建** `app/src/main/java/com/nimboweather/forecast/ui/home/ScenicPalette.kt` — `data class ScenicPalette` + `object ScenicPalettes`(纯逻辑,无 Android 依赖)。
- **新建** `app/src/test/java/com/nimboweather/forecast/ui/home/ScenicPaletteTest.kt` — 调色板映射单测。
- **新建** `app/src/main/java/com/nimboweather/forecast/ui/home/WeatherScenicView.kt` — Canvas 绘制底部山丘剖影 + 切换淡化。
- **修改** `app/src/main/res/layout/fragment_weather_page.xml` — 在 `WeatherFxView` 前插入 `WeatherScenicView`(`@id/weatherScenic`)。
- **修改** `app/src/main/java/com/nimboweather/forecast/ui/home/WeatherPageFragment.kt` — 调 `setScene(icon)`。
- **不动**:`SkyGradient`、`WeatherFxView`、`FxMapper`、其余模块;包体不变。

---

## Task 1: 纯逻辑 `ScenicPalette` + `ScenicPalettes`(TDD)

**Files:**
- Create: `app/src/main/java/com/nimboweather/forecast/ui/home/ScenicPalette.kt`
- Test: `app/src/test/java/com/nimboweather/forecast/ui/home/ScenicPaletteTest.kt`

- [ ] **Step 1: 写失败测试**

写入 `app/src/test/java/com/nimboweather/forecast/ui/home/ScenicPaletteTest.kt`:

```kotlin
package com.nimboweather.forecast.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenicPaletteTest {

    @Test fun snow_has_snow_caps() {
        val p = ScenicPalettes.from("13d")
        assertTrue(p.snowCaps)
        assertFalse(p.fogVeil)
    }

    @Test fun fog_has_veil() {
        val p = ScenicPalettes.from("50n")
        assertTrue(p.fogVeil)
        assertFalse(p.snowCaps)
    }

    @Test fun clear_day_has_no_caps_or_veil() {
        val p = ScenicPalettes.from("01d")
        assertFalse(p.snowCaps)
        assertFalse(p.fogVeil)
    }

    @Test fun clear_day_and_night_differ() {
        assertTrue(ScenicPalettes.from("01d") != ScenicPalettes.from("01n"))
    }

    @Test fun clouds_day_and_night_differ() {
        // 02/03/04 with day vs night must produce different palettes
        assertTrue(ScenicPalettes.from("04d") != ScenicPalettes.from("04n"))
    }

    @Test fun fewclouds_night_is_clouds_night_not_clear() {
        // SkyGradient treats 02/03/04 the same; scenic follows that (NOT FxScene's 02n=clear)
        assertEquals(ScenicPalettes.from("03n"), ScenicPalettes.from("04n"))
        assertTrue(ScenicPalettes.from("02n") != ScenicPalettes.from("01n"))
    }

    @Test fun rain_and_storm_differ() {
        assertTrue(ScenicPalettes.from("10d") != ScenicPalettes.from("11d"))
    }

    @Test fun null_icon_falls_back_to_clear_day() {
        assertEquals(ScenicPalettes.from("01d"), ScenicPalettes.from(null))
    }
}
```

- [ ] **Step 2: 运行确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests "com.nimboweather.forecast.ui.home.ScenicPaletteTest"`
Expected: 编译失败 / `Unresolved reference: ScenicPalettes`。

- [ ] **Step 3: 写最小实现**

写入 `app/src/main/java/com/nimboweather/forecast/ui/home/ScenicPalette.kt`:

```kotlin
package com.nimboweather.forecast.ui.home

/** Color/flag set for one scene's bottom scenic band. Colors are ARGB ints. */
data class ScenicPalette(
    val farHill: Int,
    val nearHill: Int,
    val accent: Int,
    val snowCaps: Boolean,
    val fogVeil: Boolean
)

/**
 * Pure mapping OWM icon code -> scenic palette. Framework-free (no Android View/Color),
 * unit-tested like FxMapper / AirQualityIndex. Follows SkyGradient's 8-scene taxonomy
 * (02/03/04 = clouds day/night), NOT FxScene's CLOUDS/OVERCAST/clear-night split.
 */
object ScenicPalettes {

    private val clearDay = ScenicPalette(
        farHill = 0xFF2E63B4.toInt(), nearHill = 0xFF1E4684.toInt(),
        accent = 0xFF1C5E3A.toInt(), snowCaps = false, fogVeil = false
    )
    private val clearNight = ScenicPalette(
        farHill = 0xFF1A2E50.toInt(), nearHill = 0xFF101F38.toInt(),
        accent = 0xFF14304A.toInt(), snowCaps = false, fogVeil = false
    )
    private val cloudsDay = ScenicPalette(
        farHill = 0xFF45628C.toInt(), nearHill = 0xFF324862.toInt(),
        accent = 0xFF2A4A38.toInt(), snowCaps = false, fogVeil = false
    )
    private val cloudsNight = ScenicPalette(
        farHill = 0xFF263647.toInt(), nearHill = 0xFF18242F.toInt(),
        accent = 0xFF1E3328.toInt(), snowCaps = false, fogVeil = false
    )
    private val rain = ScenicPalette(
        farHill = 0xFF3A4C5C.toInt(), nearHill = 0xFF28363F.toInt(),
        accent = 0xFF24382E.toInt(), snowCaps = false, fogVeil = false
    )
    private val storm = ScenicPalette(
        farHill = 0xFF2A2D38.toInt(), nearHill = 0xFF1B1D24.toInt(),
        accent = 0xFF1A241D.toInt(), snowCaps = false, fogVeil = false
    )
    private val snow = ScenicPalette(
        farHill = 0xFF6F8190.toInt(), nearHill = 0xFF53606B.toInt(),
        accent = 0xFF2C4438.toInt(), snowCaps = true, fogVeil = false
    )
    private val mist = ScenicPalette(
        farHill = 0xFF6B7682.toInt(), nearHill = 0xFF515A63.toInt(),
        accent = 0xFF3A4A40.toInt(), snowCaps = false, fogVeil = true
    )

    fun from(icon: String?): ScenicPalette {
        val night = icon?.endsWith("n") == true
        return when (icon?.take(2)) {
            "01" -> if (night) clearNight else clearDay
            "02", "03", "04" -> if (night) cloudsNight else cloudsDay
            "09", "10" -> rain
            "11" -> storm
            "13" -> snow
            "50" -> mist
            else -> if (night) clearNight else clearDay
        }
    }
}
```

- [ ] **Step 4: 运行确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests "com.nimboweather.forecast.ui.home.ScenicPaletteTest"`
Expected: PASS(全绿)。

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/nimboweather/forecast/ui/home/ScenicPalette.kt \
        app/src/test/java/com/nimboweather/forecast/ui/home/ScenicPaletteTest.kt
git commit -m "feat(home): ScenicPalette + ScenicPalettes pure 8-scene mapping with tests"
```

---

## Task 2: `WeatherScenicView`(Canvas 山丘剖影 + 切换淡化)

**Files:**
- Create: `app/src/main/java/com/nimboweather/forecast/ui/home/WeatherScenicView.kt`

- [ ] **Step 1: 写入完整文件**

写入 `app/src/main/java/com/nimboweather/forecast/ui/home/WeatherScenicView.kt`:

```kotlin
package com.nimboweather.forecast.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import com.nimboweather.forecast.TestEnv

/**
 * Bottom scenic band: layered hill silhouettes + foreground trees, tinted per weather
 * scene ([ScenicPalettes]). Sits behind the cards and in front of the sky gradient, so
 * rain/snow particles ([WeatherFxView]) fall in front of the hills. Pure-code vector —
 * no assets. Static except for a short colour crossfade when the scene changes.
 */
class WeatherScenicView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d

    // Hill silhouettes are recomputed on size change; the band occupies the bottom
    // BAND_FRACTION of the view height, everything above it is transparent.
    private val farPath = Path()
    private val nearPath = Path()
    private val trees = ArrayList<FloatArray>() // each: [baseX, baseY, halfWidth, height]
    private var bandTop = 0f

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val veilPaint = Paint()

    private var current: ScenicPalette = ScenicPalettes.from(null)
    private var previous: ScenicPalette? = null
    private var fade = 1f // 1 = fully showing `current`; ramps 0->1 on scene change
    private var running = false
    private var lastNs = 0L

    fun setScene(icon: String?) {
        val next = ScenicPalettes.from(icon)
        if (next == current) return
        previous = current
        current = next
        fade = if (TestEnv.active) 1f else 0f
        startFade()
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        bandTop = h * (1f - BAND_FRACTION)
        buildHills(w, h)
    }

    private fun buildHills(w: Int, h: Int) {
        val width = w.toFloat()
        // Far hill: a low, wide rise.
        farPath.reset()
        farPath.moveTo(0f, bandTop + dp(46f))
        farPath.cubicTo(width * 0.30f, bandTop + dp(6f), width * 0.55f, bandTop + dp(54f), width, bandTop + dp(20f))
        farPath.lineTo(width, h.toFloat()); farPath.lineTo(0f, h.toFloat()); farPath.close()
        // Near hill: taller, overlaps the far one.
        nearPath.reset()
        nearPath.moveTo(0f, bandTop + dp(96f))
        nearPath.cubicTo(width * 0.22f, bandTop + dp(58f), width * 0.62f, bandTop + dp(120f), width, bandTop + dp(74f))
        nearPath.lineTo(width, h.toFloat()); nearPath.lineTo(0f, h.toFloat()); nearPath.close()
        // A few foreground pine trees sitting on the near hill ridge.
        trees.clear()
        val ridgeY = bandTop + dp(96f)
        floatArrayOf(0.12f, 0.30f, 0.78f).forEach { fx ->
            trees.add(floatArrayOf(width * fx, ridgeY + dp(6f), dp(11f), dp(34f)))
        }
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); if (fade < 1f) startFade() }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); running = false }

    private fun startFade() {
        if (TestEnv.active) { running = false; invalidate(); return }
        if (!running) { running = true; lastNs = System.nanoTime(); postInvalidateOnAnimation() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        if (fade < 1f) {
            val now = System.nanoTime()
            val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
            lastNs = now
            fade = (fade + dt * FADE_SPEED).coerceAtMost(1f)
        }
        previous?.let { drawScene(canvas, it, 1f) }      // old underneath, fully opaque
        drawScene(canvas, current, if (previous == null) 1f else fade) // new fades in over it

        if (running && fade < 1f && isShown) postInvalidateOnAnimation()
        else { running = false; if (fade >= 1f) previous = null }
    }

    private fun drawScene(canvas: Canvas, p: ScenicPalette, alpha: Float) {
        val a = (alpha * 255f).toInt().coerceIn(0, 255)
        fill.shader = null
        fill.color = withAlpha(p.farHill, a); canvas.drawPath(farPath, fill)
        fill.color = withAlpha(p.nearHill, a); canvas.drawPath(nearPath, fill)
        // trees
        fill.color = withAlpha(p.accent, a)
        for (t in trees) drawPine(canvas, t[0], t[1], t[2], t[3])
        // snow caps: light ridgeline along the near hill top
        if (p.snowCaps) {
            fill.color = withAlpha(0xFFEAF2F8.toInt(), (alpha * 200f).toInt().coerceIn(0, 255))
            canvas.save(); canvas.clipPath(nearPath)
            canvas.drawRect(0f, bandTop + dp(74f), width.toFloat(), bandTop + dp(90f), fill)
            canvas.restore()
        }
        // fog veil: soft white gradient rising from the bottom
        if (p.fogVeil) {
            veilPaint.shader = LinearGradient(
                0f, bandTop + dp(40f), 0f, height.toFloat(),
                Color.argb((alpha * 120f).toInt().coerceIn(0, 255), 255, 255, 255), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, bandTop, width.toFloat(), height.toFloat(), veilPaint)
            veilPaint.shader = null
        }
    }

    private fun drawPine(canvas: Canvas, baseX: Float, baseY: Float, halfW: Float, h: Float) {
        val p = Path()
        p.moveTo(baseX, baseY - h)
        p.lineTo(baseX - halfW, baseY)
        p.lineTo(baseX + halfW, baseY)
        p.close()
        canvas.drawPath(p, fill)
    }

    private fun withAlpha(color: Int, a: Int): Int =
        (color and 0x00FFFFFF) or (a shl 24)

    private companion object {
        const val BAND_FRACTION = 0.30f // bottom 30% of the view
        const val FADE_SPEED = 2.6f     // ≈ reaches full in ~0.4s
    }
}
```

- [ ] **Step 2: 编译确认**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/nimboweather/forecast/ui/home/WeatherScenicView.kt
git commit -m "feat(home): WeatherScenicView layered hill silhouettes with scene crossfade"
```

---

## Task 3: 接入布局 + Fragment 驱动

**Files:**
- Modify: `app/src/main/res/layout/fragment_weather_page.xml`
- Modify: `app/src/main/java/com/nimboweather/forecast/ui/home/WeatherPageFragment.kt`

- [ ] **Step 1: 在布局里插入 ScenicView(WeatherFxView 之前)**

编辑 `app/src/main/res/layout/fragment_weather_page.xml`,在 `<com.nimboweather.forecast.ui.home.WeatherFxView .../>` 之前插入一段。把这块:

```xml
    <com.nimboweather.forecast.ui.home.WeatherFxView
        android:id="@+id/weatherFx"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
```

替换为:

```xml
    <com.nimboweather.forecast.ui.home.WeatherScenicView
        android:id="@+id/weatherScenic"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <com.nimboweather.forecast.ui.home.WeatherFxView
        android:id="@+id/weatherFx"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
```

(scenic 在前 = 画在底层,粒子叠其上;两者都是 match_parent,scenic 内部只画底部 30%。)

- [ ] **Step 2: Fragment 驱动 setScene**

编辑 `app/src/main/java/com/nimboweather/forecast/ui/home/WeatherPageFragment.kt`,在 `UiState.Data` 分支里、`SkyGradient.drawable(icon)` 之后、`setSpec(...)` 一带,加一行 scenic 驱动。把这段:

```kotlin
                        root.background = SkyGradient.drawable(icon)
                        view.findViewById<WeatherFxView>(R.id.weatherFx).setSpec(
```

改为(在 `root.background` 之后插一行):

```kotlin
                        root.background = SkyGradient.drawable(icon)
                        view.findViewById<WeatherScenicView>(R.id.weatherScenic).setScene(icon)
                        view.findViewById<WeatherFxView>(R.id.weatherFx).setSpec(
```

(`WeatherScenicView` 与 `WeatherFxView` 同包 `com.nimboweather.forecast.ui.home`,无需新增 import。)

- [ ] **Step 3: 编译 + 单测确认**

Run: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL(含 `ScenicPaletteTest`,无既有用例回归)。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/res/layout/fragment_weather_page.xml \
        app/src/main/java/com/nimboweather/forecast/ui/home/WeatherPageFragment.kt
git commit -m "feat(home): wire WeatherScenicView into the page driven by current icon"
```

---

## Task 4: 全量验证

**Files:** 无新增改动(仅验证)。

- [ ] **Step 1: 单元测试全绿**

Run: `./gradlew :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL,含 `ScenicPaletteTest` 全绿,既有测试不回归。

- [ ] **Step 2: 整包构建 + 体积**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL → `app/build/outputs/apk/debug/app-debug.apk`;APK 体积与上次相近(纯代码,无新资源)。

- [ ] **Step 3: 真机/模拟器人工核验(对照验收标准)**

`./gradlew installDebug` 后逐项确认:
1. 首页底部出现层叠山丘剖影,位于天空+粒子之上、卡片之下。
2. 切换不同天气城市:剖影颜色随场景变(晴/多云/雨/雷暴/雪/雾各不同);雪有山顶白盖、雾有底部白纱。
3. 切换城市时剖影颜色淡化、无硬跳。
4. 雨/雪粒子落在山丘前方;各场景下卡片文字清晰可读。

> 若颜色/高度需微调,常量集中在 `ScenicPalette.kt`(调色)与 `WeatherScenicView`(`BAND_FRACTION`、山丘 Path、`FADE_SPEED`)。

- [ ] **Step 4: 完成提交(若 Step 3 有微调)**

```bash
git add -A
git commit -m "polish(home): tune scenic palette/heights after device check"
```

---

## 完成标准

- `ScenicPaletteTest` 全绿;`./gradlew :app:testDebugUnitTest` 与 `assembleDebug` 均成功。
- 设计文档 5 条验收标准在真机上满足。
- 包体无新增资源(纯代码矢量)。
- 分支 `feat/weather-scenic-illustration` 上为一串语义化小提交,可走 PR。
```
