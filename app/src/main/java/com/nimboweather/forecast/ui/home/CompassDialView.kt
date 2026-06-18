package com.nimboweather.forecast.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Radial compass weather dial (the A+C hero). Draws an outer ring + tick marks +
 * N/E/S/W + a red N needle, with the current weather laid out in the center:
 * big temp, feels-like, Max/Min and Rainfall/Pressure on the flanks, condition,
 * and wind at the bottom. The weather icon is an ImageView overlaid by the
 * hosting layout (card_dial.xml).
 */
class CompassDialView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class DialData(
        val temp: String,
        val symbol: String,
        val feels: String,
        val condition: String,
        val max: String,
        val min: String,
        val rain: String,
        val pressure: String,
        val windText: String
    )

    var data: DialData? = null
        set(value) { field = value; invalidate() }

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d

    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(2f); color = Color.parseColor("#4DFFFFFF")
    }
    private val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(1.5f); color = Color.parseColor("#66FFFFFF")
    }
    private val tickMajor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(2f); color = Color.parseColor("#B3FFFFFF")
    }
    private val needle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF5A4D") }
    private val cardinal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCFFFFFF"); textAlign = Paint.Align.CENTER; textSize = dp(13f)
    }
    private val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = dp(54f); isFakeBoldText = true
    }
    private val degPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.LEFT; textSize = dp(18f)
    }
    private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#D9FFFFFF"); textAlign = Paint.Align.CENTER; textSize = dp(13f)
    }
    private val cond = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = dp(15f); isFakeBoldText = true
    }
    private val sideLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99FFFFFF"); textAlign = Paint.Align.CENTER; textSize = dp(11f)
    }
    private val sideValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = dp(14f); isFakeBoldText = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - dp(6f)

        canvas.drawCircle(cx, cy, r, ring)

        // tick marks every 5°, longer at cardinals
        for (i in 0 until 72) {
            val a = Math.toRadians(i * 5.0)
            val sinA = sin(a).toFloat(); val cosA = cos(a).toFloat()
            val outer = r - dp(2f)
            val major = i % 9 == 0
            val len = if (major) dp(12f) else dp(6f)
            val inner = outer - len
            canvas.drawLine(
                cx + sinA * outer, cy - cosA * outer,
                cx + sinA * inner, cy - cosA * inner,
                if (major) tickMajor else tick
            )
        }

        // cardinal labels (just inside ring)
        val cr = r - dp(26f)
        canvas.drawText("N", cx, cy - cr + dp(5f), cardinal)
        canvas.drawText("S", cx, cy + cr + dp(5f), cardinal)
        canvas.drawText("W", cx - cr, cy + dp(5f), cardinal)
        canvas.drawText("E", cx + cr, cy + dp(5f), cardinal)

        // red N needle at top
        val path = Path()
        val ny = cy - r + dp(2f)
        path.moveTo(cx, ny + dp(16f))
        path.lineTo(cx - dp(9f), ny)
        path.lineTo(cx + dp(9f), ny)
        path.close()
        canvas.drawText("N", cx, ny - dp(4f), cardinal)
        canvas.drawColorTriangle(path)

        val data = data ?: return

        // temp (center-top)
        canvas.drawText(data.temp + data.symbol, cx, cy - r * 0.26f, tempPaint)
        canvas.drawText("Feels like ${data.feels}", cx, cy - r * 0.10f, sub)

        // Max (left flank) / Min (right flank)
        val sx = r * 0.46f
        canvas.drawText("Max", cx - sx, cy - dp(6f), sideLabel)
        canvas.drawText(data.max, cx - sx, cy + dp(10f), sideValue)
        canvas.drawText("Min", cx + sx, cy - dp(6f), sideLabel)
        canvas.drawText(data.min, cx + sx, cy + dp(10f), sideValue)

        // Rainfall (lower-left) / Pressure (lower-right)
        canvas.drawText("Rain", cx - sx, cy + r * 0.30f, sideLabel)
        canvas.drawText(data.rain, cx - sx, cy + r * 0.30f + dp(16f), sideValue)
        canvas.drawText("Pressure", cx + sx, cy + r * 0.30f, sideLabel)
        canvas.drawText(data.pressure, cx + sx, cy + r * 0.30f + dp(16f), sideValue)

        // condition + wind (center-bottom; icon overlay sits above this)
        canvas.drawText(data.condition, cx, cy + r * 0.50f, cond)
        canvas.drawText("🫗 ${data.windText}", cx, cy + r * 0.62f, sub)
    }

    private fun Canvas.drawColorTriangle(path: Path) = drawPath(path, needle)
}
