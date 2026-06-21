package com.nimboweather.forecast.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Radial compass weather dial (the home hero). A live magnetometer compass: the
 * tick ring, the N/E/S/W labels and a prominent red **North** marker rotate with the
 * device heading (rotation-vector sensor), while the temperature-centred weather
 * content stays fixed and upright in the middle. A small fixed marker at the top
 * indicates the direction the device is facing. Drawn over the dynamic sky.
 *
 * If no rotation-vector sensor is present (e.g. some emulators) the heading stays 0
 * and the dial simply shows North up.
 */
class CompassDialView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs), SensorEventListener {

    data class DialData(
        val temp: String,
        val symbol: String,
        val feels: String,
        val condition: String,
        val max: String,
        val min: String,
        val rain: String,
        val pressure: String,
        val windText: String,
        val windDeg: Int? = null
    )

    var data: DialData? = null
        set(value) { field = value; invalidate() }

    // --- heading (device azimuth, degrees, 0 = N, clockwise) ---
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val rotMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var heading = 0f

    private val d = resources.displayMetrics.density
    private fun dp(v: Float) = v * d
    private val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private val bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(1.4f); color = Color.argb(61, 255, 255, 255)
    }
    private val ringInner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(1f); color = Color.argb(16, 255, 255, 255)
    }
    private val tickMinor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(1.2f); color = Color.argb(97, 255, 255, 255); strokeCap = Paint.Cap.ROUND
    }
    private val tickMajor = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = dp(2f); color = Color.argb(178, 255, 255, 255); strokeCap = Paint.Cap.ROUND
    }
    private val northMarker = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF5A4D") }
    private val topRef = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(210, 255, 255, 255) }

    private val cardinal = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(216, 255, 255, 255); textAlign = Paint.Align.CENTER; textSize = dp(13f); typeface = medium
    }
    private val cardinalN = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF7A6E"); textAlign = Paint.Align.CENTER; textSize = dp(13f); typeface = bold
    }
    private val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = dp(46f); typeface = bold
    }
    private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(217, 255, 255, 255); textAlign = Paint.Align.CENTER; textSize = dp(13f)
    }
    private val cond = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = dp(15f); typeface = bold
    }
    private val windPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(235, 199, 230, 255); textAlign = Paint.Align.CENTER; textSize = dp(13f)
    }
    private val sideLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 255, 255, 255); textAlign = Paint.Align.CENTER; textSize = dp(12f)
    }
    private val sideValue = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(235, 255, 255, 255); textAlign = Paint.Align.CENTER; textSize = dp(13f); typeface = medium
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        rotationSensor?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        sensorManager?.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
        SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
        SensorManager.getOrientation(rotMatrix, orientationAngles)
        val az = ((Math.toDegrees(orientationAngles[0].toDouble()).toFloat()) + 360f) % 360f
        var diff = az - heading
        while (diff < -180f) diff += 360f
        while (diff > 180f) diff -= 360f
        if (abs(diff) < 0.2f) return            // ignore micro-jitter (and stop redrawing when still)
        heading = (heading + diff * 0.18f + 360f) % 360f   // low-pass smoothing
        if (isShown) invalidate()
    }

    private fun polar(cx: Float, cy: Float, angDeg: Float, radius: Float): Pair<Float, Float> {
        val a = Math.toRadians(angDeg.toDouble())
        return (cx + (sin(a) * radius).toFloat()) to (cy - (cos(a) * radius).toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - dp(6f)

        canvas.drawCircle(cx, cy, r, ring)
        canvas.drawCircle(cx, cy, r - dp(30f), ringInner)

        // rotating tick ring (offset by the heading), bolder every 45°
        var ang = 0
        while (ang < 360) {
            val major = ang % 45 == 0
            val outer = r - dp(3f)
            val inner = outer - if (major) dp(13f) else dp(7f)
            val (x1, y1) = polar(cx, cy, ang - heading, outer)
            val (x2, y2) = polar(cx, cy, ang - heading, inner)
            canvas.drawLine(x1, y1, x2, y2, if (major) tickMajor else tickMinor)
            ang += 5
        }

        // prominent red North marker — rotates to wherever North actually is
        val nAng = 0f - heading
        run {
            val (tx, ty) = polar(cx, cy, nAng, r - dp(22f))
            val (x1, y1) = polar(cx, cy, nAng - 3.4f, r - dp(2f))
            val (x2, y2) = polar(cx, cy, nAng + 3.4f, r - dp(2f))
            canvas.drawPath(Path().apply { moveTo(tx, ty); lineTo(x1, y1); lineTo(x2, y2); close() }, northMarker)
        }

        // fixed marker at the top = the direction the device is facing
        run {
            val (tx, ty) = polar(cx, cy, 0f, r - dp(2f))
            canvas.drawPath(Path().apply { moveTo(tx, ty + dp(14f)); lineTo(tx - dp(6f), ty); lineTo(tx + dp(6f), ty); close() }, topRef)
        }

        val data = data ?: return

        // cardinal letters (upright) at their rotated positions
        drawCardinal(canvas, "N", cx, cy, r, 0f - heading, cardinalN)
        drawCardinal(canvas, "E", cx, cy, r, 90f - heading, cardinal)
        drawCardinal(canvas, "S", cx, cy, r, 180f - heading, cardinal)
        drawCardinal(canvas, "W", cx, cy, r, 270f - heading, cardinal)

        // centre content — fixed & upright, temperature is the focus
        canvas.drawText(data.temp + data.symbol, cx, cy - r * 0.20f + dp(16f), tempPaint)
        canvas.drawText("Feels like ${data.feels}", cx, cy + r * 0.04f, sub)

        val sx = r * 0.53f
        canvas.drawText("Max ${data.max}", cx - sx, cy - r * 0.03f, sideValue)
        canvas.drawText("Min ${data.min}", cx + sx, cy - r * 0.03f, sideValue)
        canvas.drawText("Rain ${data.rain}", cx - sx, cy + r * 0.28f, sideLabel)
        canvas.drawText(data.pressure, cx + sx, cy + r * 0.28f, sideLabel)
        canvas.drawText(data.condition, cx, cy + r * 0.48f, cond)
        canvas.drawText(data.windText, cx, cy + r * 0.61f, windPaint)
    }

    private fun drawCardinal(canvas: Canvas, text: String, cx: Float, cy: Float, r: Float, ang: Float, paint: Paint) {
        val (px, py) = polar(cx, cy, ang, r - dp(30f))
        canvas.drawText(text, px, py + dp(5f), paint)
    }
}
