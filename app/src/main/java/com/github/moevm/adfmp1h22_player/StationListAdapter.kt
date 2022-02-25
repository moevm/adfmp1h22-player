package com.github.moevm.adfmp1h22_player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
// import kotlinx.android.synthetic.main.

class StationListAdapter(private val onClick: (Station) -> Unit) :
    ListAdapter<Station, StationListAdapter.StationViewHolder>(DiffStations) {

    class StationViewHolder(
        item: View,
        private val onClick: (Station) -> Unit
    ) : RecyclerView.ViewHolder(item) {

        private val tv_station_name: TextView
            = item.findViewById(R.id.station_name)
        private var currentStation: Station? = null

        init {
            item.setOnClickListener {
                currentStation?.let {
                    onClick(it)
                }
            }
        }

        fun bind(station: Station) {
            currentStation = station

            tv_station_name.text = station.name
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_added_station, parent, false)
        return StationViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: StationViewHolder, pos: Int) {
        holder.bind(getItem(pos))
    }
}

object DiffStations : DiffUtil.ItemCallback<Station>() {
    override fun areItemsTheSame(a: Station, b: Station): Boolean {
        return a == b
    }

    override fun areContentsTheSame(a: Station, b: Station): Boolean {
        return a.name == b.name
    }
}
