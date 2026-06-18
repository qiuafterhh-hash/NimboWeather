package com.nimboweather.forecast.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/** Daily high/low dual-line trend (high = orange, low = cyan) with day labels. */
class DailyTrendView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class P(val day: String, val min: Int, val max: Int)

    var points: List<P> = emptyList()
        set(value) { field = value; invalidate() }

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d

    private val hi = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(2.5f); color = Color.parseColor("#FF9A4D")
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val lo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(2.5f); color = Color.parseColor("#7FE0FF")
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val hiDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF9A4D") }
    private val loDot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#7FE0FF") }
    private val valPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = dp(13f); isFakeBoldText = true
    }
    private val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B3FFFFFF"); textAlign = Paint.Align.CENTER; textSize = dp(12f)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), dp(150f).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pts = points
        if (pts.size < 2) return

        val padX = dp(28f)
        val top = dp(26f)
        val bottom = height - dp(22f)
        val h = bottom - top

        val allMin = pts.minOf { it.min }
        val allMax = pts.maxOf { it.max }
        val range = (allMax - allMin).coerceAtLeast(1)
        val n = pts.size
        val step = (width - 2 * padX) / (n - 1)
        fun x(i: Int) = padX + i * step
        fun y(t: Int) = top + (1f - (t - allMin).toFloat() / range) * h

        val hiPath = Path(); val loPath = Path()
        pts.forEachIndexed { i, p ->
            if (i == 0) { hiPath.moveTo(x(i), y(p.max)); loPath.moveTo(x(i), y(p.min)) }
            else { hiPath.lineTo(x(i), y(p.max)); loPath.lineTo(x(i), y(p.min)) }
        }
        canvas.drawPath(hiPath, hi)
        canvas.drawPath(loPath, lo)

        pts.forEachIndexed { i, p ->
            val px = x(i)
            canvas.drawCircle(px, y(p.max), dp(3f), hiDot)
            canvas.drawCircle(px, y(p.min), dp(3f), loDot)
            canvas.drawText("${p.max}°", px, y(p.max) - dp(8f), valPaint)
            canvas.drawText("${p.min}°", px, y(p.min) + dp(16f), valPaint)
            canvas.drawText(p.day, px, bottom + dp(16f), dayPaint)
        }
    }
}
