package com.nimboweather.forecast.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.nimboweather.forecast.R
import com.nimboweather.forecast.data.HourlyForecast
import com.nimboweather.forecast.data.IconUrls

class HourlyAdapter : RecyclerView.Adapter<HourlyAdapter.VH>() {

    private val items = mutableListOf<HourlyForecast>()

    fun submit(list: List<HourlyForecast>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_hourly, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val time: TextView = v.findViewById(R.id.tvTime)
        private val icon: ImageView = v.findViewById(R.id.ivHourIcon)
        private val temp: TextView = v.findViewById(R.id.tvHourTemp)
        fun bind(h: HourlyForecast) {
            time.text = h.timeLabel
            temp.text = "${h.temp}°"
            icon.load(IconUrls.owm(h.icon))
        }
    }
}
