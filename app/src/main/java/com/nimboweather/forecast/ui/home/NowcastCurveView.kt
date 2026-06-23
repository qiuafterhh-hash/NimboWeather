package com.nimboweather.forecast.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * A 15-minute precipitation bar curve for the next ~2 hours. Pure Canvas, no map tiles —
 * deliberately lightweight for low-end / data-sensitive devices. Bars scale against a fixed
 * reference (moderate rain) so light drizzle reads as light, not a downpour. Sub-threshold
 * steps are drawn faint. See `docs/precip-nowcast-spec.md`.
 */
class NowcastCurveView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    /** Precipitation in mm, one value per 15-minute step, index 0 = now. */
    var series: List<Double> = emptyList()
        set(value) { field = value; invalidate() }

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d

    private val barWet = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#7FE0FF") }
    private val barDry = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#33FFFFFF") }
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B3FFFFFF"); textAlign = Paint.Align.CENTER; textSize = dp(11f)
    }

    private val rect = RectF()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, dp(96f).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = series
        if (s.isEmpty()) return

        val padX = dp(8f)
        val bottomPad = dp(18f)         // room for minute labels
        val topPad = dp(6f)
        val chartBottom = height - bottomPad
        val chartTop = topPad
        val chartH = chartBottom - chartTop

        // Fixed reference so bar height is honest: 2 mm/15-min ≈ moderate rain fills the chart.
        val refMm = 2.0
        val threshold = 0.1

        val n = s.size
        val slot = (width - 2 * padX) / n
        val barW = slot * 0.55f

        s.forEachIndexed { i, mm ->
            val cx = padX + slot * (i + 0.5f)
            val frac = (mm / refMm).coerceIn(0.0, 1.0).toFloat()
            val barH = (chartH * frac).coerceAtLeast(dp(2f)) // always a visible stub
            rect.set(cx - barW / 2, chartBottom - barH, cx + barW / 2, chartBottom)
            canvas.drawRoundRect(rect, dp(3f), dp(3f), if (mm >= threshold) barWet else barDry)

            // Label every other step in minutes-from-now (15-min grain).
            if (i % 2 == 0) {
                val mins = i * 15
                val label = if (mins == 0) "now" else "+${mins}m"
                canvas.drawText(label, cx, height - dp(4f), timePaint)
            }
        }
    }
}
