package com.nimboweather.forecast.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.nimboweather.forecast.R
import com.nimboweather.forecast.data.DailyForecast
import com.nimboweather.forecast.data.IconUrls

class DailyAdapter : RecyclerView.Adapter<DailyAdapter.VH>() {

    private val items = mutableListOf<DailyForecast>()

    fun submit(list: List<DailyForecast>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_daily, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val day: TextView = v.findViewById(R.id.tvDay)
        private val icon: ImageView = v.findViewById(R.id.ivDayIcon)
        private val desc: TextView = v.findViewById(R.id.tvDayDesc)
        private val range: TextView = v.findViewById(R.id.tvRange)
        fun bind(d: DailyForecast) {
            day.text = d.dayLabel
            desc.text = d.desc ?: ""
            range.text = "${d.min}° / ${d.max}°"
            icon.load(IconUrls.owm(d.icon))
        }
    }
}
