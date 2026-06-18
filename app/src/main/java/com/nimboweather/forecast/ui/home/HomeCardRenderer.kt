package com.nimboweather.forecast.ui.home

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.nimboweather.forecast.R
import com.nimboweather.forecast.data.IconUrls
import com.nimboweather.forecast.ui.adapter.HourlyAdapter
import com.nimboweather.forecast.ui.detail.ForecastDetailActivity

/**
 * Renders an ordered list of [HomeCard]s into a container LinearLayout (the same
 * dynamic-card-list approach Local Weather uses). The CURRENT card is the radial
 * compass dial hero; tapping the dial / hourly / daily cards opens the detail screen.
 */
class HomeCardRenderer(private val context: Context) {

    private val inflater = LayoutInflater.from(context)

    fun render(container: LinearLayout, cards: List<HomeCard>) {
        container.removeAllViews()
        cards.forEach { card ->
            val view = when (card) {
                is HomeCard.Current -> dialCard(container, card)
                is HomeCard.Hourly -> hourlyCard(container, card)
                is HomeCard.Precip -> precipCard(container, card)
                is HomeCard.Details -> detailsCard(container, card)
                is HomeCard.SunriseSunset -> sunsetCard(container, card)
                is HomeCard.Daily -> dailyCard(container, card)
            }
            container.addView(view)
        }
    }

    private fun inflate(layout: Int, parent: ViewGroup): View =
        inflater.inflate(layout, parent, false)

    private fun openDetail() {
        context.startActivity(Intent(context, ForecastDetailActivity::class.java))
    }

    private fun dialCard(parent: ViewGroup, c: HomeCard.Current): View {
        val v = inflate(R.layout.card_dial, parent)
        v.findViewById<CompassDialView>(R.id.dial).data = CompassDialView.DialData(
            temp = c.temp.toString(),
            symbol = c.symbol,
            feels = "${c.feels}${c.symbol}",
            condition = c.desc,
            max = "${c.max}°",
            min = "${c.min}°",
            rain = "${c.rainProb}%",
            pressure = "${c.pressure}hPa",
            windText = c.windText
        )
        v.findViewById<ImageView>(R.id.ivDialIcon).load(IconUrls.owm(c.icon))
        v.setOnClickListener { openDetail() }
        return v
    }

    private fun hourlyCard(parent: ViewGroup, c: HomeCard.Hourly): View {
        val v = inflate(R.layout.card_hourly, parent)
        val rv = v.findViewById<RecyclerView>(R.id.rvHourly)
        rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rv.isNestedScrollingEnabled = false
        rv.adapter = HourlyAdapter().apply { submit(c.items) }
        v.setOnClickListener { openDetail() }
        return v
    }

    private fun dailyCard(parent: ViewGroup, c: HomeCard.Daily): View {
        val v = inflate(R.layout.card_daily, parent)
        val ll = v.findViewById<LinearLayout>(R.id.llDaily)
        c.items.forEach { d ->
            val row = inflate(R.layout.item_daily, ll)
            row.findViewById<TextView>(R.id.tvDay).text = d.dayLabel
            row.findViewById<TextView>(R.id.tvDayDesc).text = d.desc ?: ""
            row.findViewById<TextView>(R.id.tvRange).text = "${d.min}° / ${d.max}°"
            row.findViewById<ImageView>(R.id.ivDayIcon).load(IconUrls.owm(d.icon))
            ll.addView(row)
        }
        v.setOnClickListener { openDetail() }
        return v
    }

    private fun detailsCard(parent: ViewGroup, c: HomeCard.Details): View {
        val v = inflate(R.layout.card_details, parent)
        val grid = v.findViewById<GridLayout>(R.id.glMetrics)
        c.metrics.forEach { m ->
            val cell = inflate(R.layout.item_metric, grid)
            cell.findViewById<TextView>(R.id.tvMetricLabel).text = m.label
            cell.findViewById<TextView>(R.id.tvMetricValue).text = m.value
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            grid.addView(cell, lp)
        }
        return v
    }

    private fun sunsetCard(parent: ViewGroup, c: HomeCard.SunriseSunset): View {
        val v = inflate(R.layout.card_sunset, parent)
        v.findViewById<TextView>(R.id.tvSunrise).text = c.sunrise
        v.findViewById<TextView>(R.id.tvSunset).text = c.sunset
        return v
    }

    private fun precipCard(parent: ViewGroup, c: HomeCard.Precip): View {
        val v = inflate(R.layout.card_precip, parent)
        val ll = v.findViewById<LinearLayout>(R.id.llPrecip)
        c.points.forEach { p ->
            val cell = inflate(R.layout.item_precip, ll)
            cell.findViewById<TextView>(R.id.tvPrecipTime).text = p.time
            cell.findViewById<TextView>(R.id.tvPrecipPop).text = "${p.pop}%"
            ll.addView(cell)
        }
        return v
    }
}
