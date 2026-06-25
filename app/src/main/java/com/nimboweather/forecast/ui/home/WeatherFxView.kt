package com.nimboweather.forecast.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.nimboweather.forecast.TestEnv
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ambient animated weather background driven by [FxSpec] (scene + wind + intensity).
 * A coordinator over independent [FxLayer]s; switching scenes crossfades old→new.
 * Sits behind the translucent cards, so visuals stay restrained for readability.
 */
class WeatherFxView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d

    private var running = false
    private var lastNs = 0L

    private var current: Scene? = null
    private var outgoing: Scene? = null

    // ---- public API ----

    fun setSpec(spec: FxSpec) {
        val cur = current
        if (cur != null && cur.spec.scene == spec.scene) {
            cur.spec = spec // same scene → just update params (intensity/wind)
        } else {
            outgoing = cur?.apply { target = 0f }
            current = if (spec.scene == FxScene.NONE) null
            else Scene(spec).also { if (width > 0) it.resize(width, height) }
        }
        startLoop()
        invalidate()
    }

    /** Back-compat / tests: drive from an OWM icon code with default wind+intensity. */
    fun setCondition(icon: String?) = setSpec(FxSpec(FxMapper.sceneFrom(icon)))

    // ---- lifecycle ----

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        current?.resize(w, h); outgoing?.resize(w, h)
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); startLoop() }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); running = false }

    private fun startLoop() {
        if (current == null && outgoing == null) { running = false; return }
        // Under UI-test automation, render a single static frame instead of looping.
        if (TestEnv.active) { running = false; invalidate(); return }
        if (!running) { running = true; lastNs = System.nanoTime(); postInvalidateOnAnimation() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastNs = now

        outgoing?.let { o ->
            o.update(dt); o.draw(canvas)
            if (o.alpha < 0.02f) outgoing = null
        }
        current?.let { c -> c.update(dt); c.draw(canvas) }

        if (running && isShown && (current != null || outgoing != null)) postInvalidateOnAnimation()
        else running = false
    }

    // ---- scene = a set of layers with a shared crossfade alpha ----

    private inner class Scene(var spec: FxSpec) {
        private val layers: List<FxLayer> = buildLayers(spec.scene)
        var alpha = if (TestEnv.active) 1f else 0f
        var target = 1f

        fun resize(w: Int, h: Int) = layers.forEach { it.resize(w, h) }
        fun update(dt: Float) {
            alpha += (target - alpha) * (dt * FADE_SPEED).coerceIn(0f, 1f)
            layers.forEach { it.update(dt, spec) }
        }
        fun draw(canvas: Canvas) = layers.forEach { it.draw(canvas, alpha) }
    }

    private fun buildLayers(scene: FxScene): List<FxLayer> = when (scene) {
        FxScene.RAIN -> listOf(RainLayer(near = false), RainLayer(near = true))
        FxScene.STORM -> listOf(RainLayer(near = false), RainLayer(near = true), LightningLayer())
        FxScene.SNOW -> listOf(SnowLayer(near = false), SnowLayer(near = true))
        FxScene.CLOUDS -> listOf(CloudLayer(dense = false))
        FxScene.OVERCAST -> listOf(CloudLayer(dense = true))
        FxScene.CLEAR_NIGHT -> listOf(StarLayer())
        FxScene.CLEAR_DAY -> listOf(SunGlowLayer())
        FxScene.FOG -> listOf(FogLayer())
        FxScene.NONE -> emptyList()
    }

    // ---- layer interface + particle holders ----

    private interface FxLayer {
        fun resize(w: Int, h: Int)
        fun update(dt: Float, spec: FxSpec)
        fun draw(canvas: Canvas, alpha: Float)
    }

    private class Drop(var x: Float, var y: Float, val len: Float, val speed: Float)
    private class Flake(var x: Float, var y: Float, val r: Float, val vx: Float, val vy: Float)
    private class Cloud(var x: Float, val y: Float, val w: Float, val h: Float, val speed: Float)
    private class Star(val x: Float, val y: Float, val r: Float, val baseA: Float, val phase: Float, val twinkle: Float)
    private class Haze(var x: Float, val y: Float, val w: Float, val h: Float, val speed: Float, val alpha: Int)

    // ---- rain (near/far parallax, intensity density, wind tilt) ----

    private inner class RainLayer(private val near: Boolean) : FxLayer {
        private val drops = ArrayList<Drop>()
        private var w = 0; private var h = 0
        private var tilt = 0f; private var active = 0
        private val maxDrops = if (near) 70 else 55
        private val baseAlpha = if (near) 0.85f else 0.5f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(if (near) "#D2ECFF" else "#BFE0FF")
            strokeCap = Paint.Cap.ROUND
            strokeWidth = (if (near) 2.3f else 1.4f) * d
        }
        override fun resize(w: Int, h: Int) {
            this.w = w; this.h = h; drops.clear(); repeat(maxDrops) { drops.add(newDrop()) }
        }
        private fun newDrop(): Drop {
            val len = (if (near) dp(15f) else dp(9f)) + Random.nextFloat() * dp(8f)
            val spd = (if (near) dp(900f) else dp(560f)) + Random.nextFloat() * dp(220f)
            return Drop(Random.nextFloat() * w, Random.nextFloat() * h, len, spd)
        }
        override fun update(dt: Float, spec: FxSpec) {
            if (w == 0) return
            tilt = FxMapper.tilt(spec.windDeg, spec.windSpeed)
            active = (maxDrops * spec.intensity).toInt().coerceIn(if (near) 12 else 8, maxDrops)
            for (i in 0 until active) {
                val p = drops[i]
                p.y += p.speed * dt; p.x += p.speed * dt * tilt
                if (p.y > h) { p.y = -p.len; p.x = Random.nextFloat() * w }
            }
        }
        override fun draw(canvas: Canvas, alpha: Float) {
            paint.alpha = (baseAlpha * alpha * 255f).toInt().coerceIn(0, 255)
            for (i in 0 until active) {
                val p = drops[i]
                canvas.drawLine(p.x, p.y, p.x + p.len * tilt, p.y + p.len, paint)
            }
        }
    }

    // ---- snow (parallax, intensity density, wind sway) ----

    private inner class SnowLayer(private val near: Boolean) : FxLayer {
        private val flakes = ArrayList<Flake>()
        private var w = 0; private var h = 0
        private var sway = 0f; private var active = 0
        private val maxFlakes = if (near) 50 else 45
        private val baseAlpha = if (near) 0.9f else 0.55f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        override fun resize(w: Int, h: Int) {
            this.w = w; this.h = h; flakes.clear(); repeat(maxFlakes) { flakes.add(newFlake()) }
        }
        private fun newFlake(): Flake {
            val r = (if (near) dp(2.4f) else dp(1.3f)) + Random.nextFloat() * dp(1.4f)
            val vy = (if (near) dp(70f) else dp(40f)) + Random.nextFloat() * dp(30f)
            return Flake(Random.nextFloat() * w, Random.nextFloat() * h, r, (Random.nextFloat() - 0.5f) * dp(16f), vy)
        }
        override fun update(dt: Float, spec: FxSpec) {
            if (w == 0) return
            sway = FxMapper.tilt(spec.windDeg, spec.windSpeed) * dp(120f)
            active = (maxFlakes * spec.intensity).toInt().coerceIn(if (near) 12 else 8, maxFlakes)
            for (i in 0 until active) {
                val p = flakes[i]
                p.y += p.vy * dt; p.x += (p.vx + sway) * dt
                if (p.y > h) { p.y = -p.r; p.x = Random.nextFloat() * w }
            }
        }
        override fun draw(canvas: Canvas, alpha: Float) {
            paint.alpha = (baseAlpha * alpha * 255f).toInt().coerceIn(0, 255)
            for (i in 0 until active) { val p = flakes[i]; canvas.drawCircle(p.x, p.y, p.r, paint) }
        }
    }

    // ---- clouds (multi-oval fluffy, wind drift; dense = overcast) ----

    private inner class CloudLayer(private val dense: Boolean) : FxLayer {
        private val clouds = ArrayList<Cloud>()
        private var w = 0; private var h = 0; private var dir = 1f
        private val count = if (dense) 6 else 4
        private val baseAlpha = if (dense) 0.5f else 0.32f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (dense) Color.parseColor("#C9CBD2") else Color.WHITE
        }
        override fun resize(w: Int, h: Int) {
            this.w = w; this.h = h; clouds.clear(); repeat(count) { clouds.add(newCloud()) }
        }
        private fun newCloud(): Cloud {
            val cw = dp(150f) + Random.nextFloat() * dp(140f)
            return Cloud(Random.nextFloat() * w, dp(30f) + Random.nextFloat() * h * 0.45f,
                cw, dp(46f) + Random.nextFloat() * dp(26f), dp(8f) + Random.nextFloat() * dp(12f))
        }
        override fun update(dt: Float, spec: FxSpec) {
            if (w == 0) return
            dir = if (FxMapper.tilt(spec.windDeg, spec.windSpeed) >= 0f) 1f else -1f
            val sp = 1f + spec.windSpeed * 0.4f
            for (c in clouds) {
                c.x += c.speed * sp * dir * dt
                if (dir > 0 && c.x - c.w > w) c.x = -c.w
                if (dir < 0 && c.x + c.w < 0) c.x = w + c.w
            }
        }
        override fun draw(canvas: Canvas, alpha: Float) {
            paint.alpha = (baseAlpha * alpha * 255f).toInt().coerceIn(0, 255)
            for (c in clouds) {
                val x = c.x; val y = c.y; val rw = c.w; val rh = c.h
                canvas.drawOval(x, y + rh * 0.25f, x + rw * 0.6f, y + rh, paint)
                canvas.drawOval(x + rw * 0.25f, y, x + rw * 0.85f, y + rh * 0.9f, paint)
                canvas.drawOval(x + rw * 0.45f, y + rh * 0.2f, x + rw, y + rh, paint)
            }
        }
    }

    // ---- clear night: stars + occasional meteor + soft moon glow ----

    private inner class StarLayer : FxLayer {
        private val stars = ArrayList<Star>()
        private var w = 0; private var h = 0; private var t = 0f
        private var meteorOn = false; private var meteorT = 0f; private var nextMeteor = 4f
        private var meteorX = 0f; private var meteorY = 0f
        private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        private val meteorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; strokeCap = Paint.Cap.ROUND; strokeWidth = 2f * d
        }
        override fun resize(w: Int, h: Int) {
            this.w = w; this.h = h; stars.clear()
            repeat(80) {
                stars.add(Star(Random.nextFloat() * w, Random.nextFloat() * h * 0.72f,
                    dp(0.6f) + Random.nextFloat() * dp(1.4f), 0.4f + Random.nextFloat() * 0.6f,
                    Random.nextFloat() * 6.28f, 0.6f + Random.nextFloat() * 1.6f))
            }
        }
        override fun update(dt: Float, spec: FxSpec) {
            t += dt
            if (meteorOn) { meteorT += dt; if (meteorT > 0.9f) meteorOn = false }
            else {
                nextMeteor -= dt
                if (nextMeteor <= 0f && w > 0) {
                    meteorOn = true; meteorT = 0f; nextMeteor = 6f + Random.nextFloat() * 8f
                    meteorX = Random.nextFloat() * w * 0.7f; meteorY = Random.nextFloat() * h * 0.4f
                }
            }
        }
        override fun draw(canvas: Canvas, alpha: Float) {
            glowPaint.alpha = (0.10f * alpha * 255f).toInt().coerceIn(0, 255)
            canvas.drawCircle(w * 0.82f, h * 0.16f, dp(60f), glowPaint)
            for (s in stars) {
                val a = (s.baseA * (0.65f + 0.35f * sin(t * s.twinkle + s.phase))).coerceIn(0f, 1f)
                starPaint.alpha = (a * alpha * 255f).toInt().coerceIn(0, 255)
                canvas.drawCircle(s.x, s.y, s.r, starPaint)
            }
            if (meteorOn) {
                val p = meteorT / 0.9f
                meteorPaint.alpha = ((1f - p) * alpha * 255f).toInt().coerceIn(0, 255)
                val mx = meteorX + p * dp(180f); val my = meteorY + p * dp(120f)
                canvas.drawLine(mx, my, mx - dp(40f), my - dp(26f), meteorPaint)
            }
        }
    }

    // ---- clear day: subtle drifting sun glow ----

    private inner class SunGlowLayer : FxLayer {
        private var w = 0; private var h = 0; private var t = 0f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFF4C8") }
        override fun resize(w: Int, h: Int) { this.w = w; this.h = h }
        override fun update(dt: Float, spec: FxSpec) { t += dt }
        override fun draw(canvas: Canvas, alpha: Float) {
            val pulse = 0.12f + 0.04f * sin(t * 0.5f)
            paint.alpha = (pulse * alpha * 255f).toInt().coerceIn(0, 255)
            val cx = w * 0.78f + sin(t * 0.15f) * dp(20f)
            canvas.drawCircle(cx, h * 0.18f, dp(90f), paint)
        }
    }

    // ---- fog / haze: layered horizontal drift ----

    private inner class FogLayer : FxLayer {
        private val haze = ArrayList<Haze>()
        private var w = 0; private var h = 0
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        override fun resize(w: Int, h: Int) {
            this.w = w; this.h = h; haze.clear()
            repeat(5) { i ->
                haze.add(Haze(Random.nextFloat() * w, h * (0.12f + 0.18f * i),
                    dp(260f) + Random.nextFloat() * dp(160f), dp(70f) + Random.nextFloat() * dp(40f),
                    dp(6f) + Random.nextFloat() * dp(8f), 18 + Random.nextInt(18)))
            }
        }
        override fun update(dt: Float, spec: FxSpec) {
            val sp = 1f + spec.windSpeed * 0.3f
            for (g in haze) { g.x += g.speed * sp * dt; if (g.x - g.w > w) g.x = -g.w }
        }
        override fun draw(canvas: Canvas, alpha: Float) {
            for (g in haze) {
                paint.alpha = (g.alpha / 255f * 0.7f * alpha * 255f).toInt().coerceIn(0, 255)
                canvas.drawOval(g.x, g.y, g.x + g.w, g.y + g.h, paint)
            }
        }
    }

    // ---- lightning: timed soft white flash with decay ----

    private inner class LightningLayer : FxLayer {
        private var w = 0; private var h = 0
        private var flash = 0f; private var next = 2.5f
        private val paint = Paint()
        override fun resize(w: Int, h: Int) { this.w = w; this.h = h }
        override fun update(dt: Float, spec: FxSpec) {
            next -= dt
            if (next <= 0f) { flash = 0.5f; next = 2.5f + Random.nextFloat() * 5f }
            if (flash > 0f) flash = (flash - dt * 1.4f).coerceAtLeast(0f)
        }
        override fun draw(canvas: Canvas, alpha: Float) {
            if (flash <= 0f) return
            paint.color = Color.argb((flash * alpha * 255f).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        }
    }

    private companion object { const val FADE_SPEED = 4f } // ≈ reaches full in ~0.5s
}
