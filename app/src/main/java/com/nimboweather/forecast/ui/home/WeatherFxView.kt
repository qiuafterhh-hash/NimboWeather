package com.nimboweather.forecast.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

/**
 * Ambient animated weather background: drifting clouds, falling rain/snow, and
 * lightning flashes — driven by the current OWM condition. Sits behind the
 * (translucent) cards so the effect shows through.
 */
class WeatherFxView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class Fx { NONE, RAIN, SNOW, CLOUDS, STORM }

    private val d = resources.displayMetrics.density
    private var fx = Fx.NONE
    private var running = false
    private var lastNs = 0L
    private var flashAlpha = 0f
    private var nextFlashMs = 2000f

    private class Drop(var x: Float, var y: Float, val len: Float, val speed: Float)
    private class Flake(var x: Float, var y: Float, val r: Float, val vx: Float, val vy: Float)
    private class Cloud(var x: Float, var y: Float, val w: Float, val h: Float, val speed: Float, val alpha: Int)

    private val drops = ArrayList<Drop>()
    private val flakes = ArrayList<Flake>()
    private val clouds = ArrayList<Cloud>()

    private val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#80CFE6FF"); strokeWidth = 2f * d; strokeCap = Paint.Cap.ROUND
    }
    private val snowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#CCFFFFFF") }
    private val cloudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val flashPaint = Paint()

    fun setCondition(icon: String?) {
        fx = when (icon?.take(2)) {
            "11" -> Fx.STORM
            "09", "10" -> Fx.RAIN
            "13" -> Fx.SNOW
            "02", "03", "04", "50" -> Fx.CLOUDS
            else -> Fx.NONE
        }
        seed()
        startLoop()
        invalidate()
    }

    private fun seed() {
        if (width == 0 || height == 0) return
        drops.clear(); flakes.clear(); clouds.clear()
        when (fx) {
            Fx.RAIN, Fx.STORM -> repeat(90) {
                drops.add(Drop(Random.nextFloat() * width, Random.nextFloat() * height, dp(10f) + Random.nextFloat() * dp(14f), dp(420f) + Random.nextFloat() * dp(260f)))
            }
            Fx.SNOW -> repeat(70) {
                flakes.add(Flake(Random.nextFloat() * width, Random.nextFloat() * height, dp(2f) + Random.nextFloat() * dp(2.5f), (Random.nextFloat() - 0.5f) * dp(20f), dp(40f) + Random.nextFloat() * dp(40f)))
            }
            Fx.CLOUDS -> repeat(4) {
                clouds.add(Cloud(Random.nextFloat() * width, dp(40f) + Random.nextFloat() * height * 0.5f, dp(140f) + Random.nextFloat() * dp(120f), dp(48f) + Random.nextFloat() * dp(24f), dp(10f) + Random.nextFloat() * dp(14f), 24 + Random.nextInt(24)))
            }
            Fx.NONE -> {}
        }
    }

    private fun dp(v: Float) = v * d

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) { seed() }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); startLoop() }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); running = false }

    private fun startLoop() {
        if (fx == Fx.NONE) { running = false; return }
        if (!running) { running = true; lastNs = System.nanoTime(); postInvalidateOnAnimation() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (fx == Fx.NONE) return
        val now = System.nanoTime()
        val dt = ((now - lastNs) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastNs = now

        when (fx) {
            Fx.RAIN, Fx.STORM -> {
                for (p in drops) {
                    p.y += p.speed * dt
                    if (p.y > height) { p.y = -p.len; p.x = Random.nextFloat() * width }
                    canvas.drawLine(p.x, p.y, p.x + dp(2f), p.y + p.len, rainPaint)
                }
                if (fx == Fx.STORM) {
                    nextFlashMs -= dt * 1000f
                    if (nextFlashMs <= 0f) { flashAlpha = 0.55f; nextFlashMs = 2500f + Random.nextFloat() * 5000f }
                    if (flashAlpha > 0f) {
                        flashPaint.color = Color.argb((flashAlpha * 255).toInt(), 255, 255, 255)
                        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), flashPaint)
                        flashAlpha -= dt * 1.6f
                    }
                }
            }
            Fx.SNOW -> for (p in flakes) {
                p.y += p.vy * dt; p.x += p.vx * dt
                if (p.y > height) { p.y = -p.r; p.x = Random.nextFloat() * width }
                canvas.drawCircle(p.x, p.y, p.r, snowPaint)
            }
            Fx.CLOUDS -> for (c in clouds) {
                c.x += c.speed * dt
                if (c.x - c.w > width) c.x = -c.w
                cloudPaint.alpha = c.alpha
                canvas.drawOval(c.x, c.y, c.x + c.w, c.y + c.h, cloudPaint)
            }
            Fx.NONE -> {}
        }
        if (running && isShown) postInvalidateOnAnimation()
    }
}
