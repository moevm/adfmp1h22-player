package com.github.moevm.adfmp1h22_player

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class AddStationAdapter(private val stations: List<Station>) :
    RecyclerView.Adapter<AddStationAdapter.Holder>()
{

    override fun getItemCount(): Int = stations.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v : View = LayoutInflater.from(parent.context).inflate(R.layout.item_station, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        var station = stations[position]
        holder.trName.text = station.name
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val trName : TextView = view.findViewById(R.id.stationName)
    }

}