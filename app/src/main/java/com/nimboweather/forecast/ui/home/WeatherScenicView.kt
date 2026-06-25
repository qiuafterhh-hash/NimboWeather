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
