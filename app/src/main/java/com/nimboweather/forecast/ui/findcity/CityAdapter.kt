package com.nimboweather.forecast.ui.findcity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.nimboweather.forecast.R
import com.nimboweather.forecast.data.GeoLocation

class CityAdapter(
    private val onClick: (GeoLocation) -> Unit
) : RecyclerView.Adapter<CityAdapter.VH>() {

    private val items = mutableListOf<GeoLocation>()

    fun submit(list: List<GeoLocation>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_city, parent, false)
        return VH(v, onClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    override fun getItemCount(): Int = items.size

    class VH(v: View, val onClick: (GeoLocation) -> Unit) : RecyclerView.ViewHolder(v) {
        private val name: TextView = v.findViewById(R.id.tvCityName)
        private var current: GeoLocation? = null
        init { v.setOnClickListener { current?.let(onClick) } }
        fun bind(g: GeoLocation) {
            current = g
            name.text = g.display()
        }
    }
}
