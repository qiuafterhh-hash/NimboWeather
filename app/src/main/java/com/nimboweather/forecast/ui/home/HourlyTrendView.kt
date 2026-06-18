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

/** Hourly temperature trend: a smooth line with points, temp labels above and time
 *  labels below, and a soft gradient fill — the AccuWeather-style mini chart. */
class HourlyTrendView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class P(val label: String, val temp: Int)

    var points: List<P> = emptyList()
        set(value) { field = value; requestLayout(); invalidate() }

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d

    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(2.5f); color = Color.parseColor("#7FE0FF")
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = dp(15f); isFakeBoldText = true
    }
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#B3FFFFFF"); textAlign = Paint.Align.CENTER; textSize = dp(12f)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, dp(140f).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pts = points
        if (pts.size < 2) return

        val padX = dp(24f)
        val topPad = dp(26f)    // room for temp label
        val bottomPad = dp(22f) // room for time label
        val chartTop = topPad
        val chartBottom = height - bottomPad
        val chartH = chartBottom - chartTop

        val temps = pts.map { it.temp }
        val minT = temps.min()
        val maxT = temps.max()
        val range = (maxT - minT).coerceAtLeast(1)

        val n = pts.size
        val step = (width - 2 * padX) / (n - 1)
        fun x(i: Int) = padX + i * step
        fun y(t: Int) = chartTop + (1f - (t - minT).toFloat() / range) * chartH

        // line path
        val linePath = Path()
        pts.forEachIndexed { i, p ->
            val px = x(i); val py = y(p.temp)
            if (i == 0) linePath.moveTo(px, py) else linePath.lineTo(px, py)
        }
        // gradient fill under the line
        val fillPath = Path(linePath)
        fillPath.lineTo(x(n - 1), chartBottom)
        fillPath.lineTo(x(0), chartBottom)
        fillPath.close()
        fill.shader = LinearGradient(
            0f, chartTop, 0f, chartBottom,
            Color.parseColor("#4D7FE0FF"), Color.parseColor("#0A7FE0FF"), Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, fill)
        canvas.drawPath(linePath, line)

        pts.forEachIndexed { i, p ->
            val px = x(i); val py = y(p.temp)
            canvas.drawCircle(px, py, dp(3f), dot)
            canvas.drawText("${p.temp}°", px, py - dp(10f), tempPaint)
            canvas.drawText(p.label, px, chartBottom + dp(16f), timePaint)
        }
    }
}
