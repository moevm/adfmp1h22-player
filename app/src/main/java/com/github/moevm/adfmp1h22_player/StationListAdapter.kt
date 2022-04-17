package com.github.moevm.adfmp1h22_player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.selection.SelectionTracker
import com.squareup.picasso.Picasso

class StationListAdapter(private val onClick: (Station) -> Unit) :
    ListAdapter<Station, StationListAdapter.StationViewHolder>(DiffStations) {

    class StationViewHolder(
        private val item: View,
        private val onClick: (Station) -> Unit
    ) : RecyclerView.ViewHolder(item) {

        val favicon : ImageView= item.findViewById(R.id.station_favicon)

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

        fun bind(station: Station, selected: Boolean) {
            currentStation = station

            tv_station_name.text = station.name

            item.setActivated(selected)
        }

        val stationUuid: String
            get () = currentStation?.stationuuid ?: ""
    }

    var tracker: SelectionTracker<String>? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        vieTwype: Int
    ): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_added_station, parent, false)
        return StationViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: StationViewHolder, pos: Int) {
        val item = getItem(pos)
        val id = item.stationuuid
        val issel = tracker?.isSelected(id) ?: false
        val imageURL = item.faviconUrl
        if(imageURL != "") {
            Picasso.get()
                .load(imageURL)
                .error(R.drawable.ic_station_placeholder_54)
                .into(holder.favicon)
        }
        holder.bind(item, issel)
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
