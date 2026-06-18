package com.nimboweather.forecast.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/** Simple page-dots indicator (shows how many city pages + which is current). */
class PageDotsView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val d = resources.displayMetrics.density
    private val active = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val inactive = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#66FFFFFF") }

    private var count = 0
    private var current = 0

    fun set(count: Int, current: Int) {
        this.count = count
        this.current = current
        visibility = if (count > 1) VISIBLE else GONE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (count <= 1) return
        val r = 3.5f * d
        val gap = 9f * d
        val totalW = (count - 1) * gap
        var x = width / 2f - totalW / 2f
        val cy = height / 2f
        for (i in 0 until count) {
            canvas.drawCircle(x, cy, if (i == current) r else r * 0.8f, if (i == current) active else inactive)
            x += gap
        }
    }
}
