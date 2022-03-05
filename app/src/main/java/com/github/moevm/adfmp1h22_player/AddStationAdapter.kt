package com.github.moevm.adfmp1h22_player

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso


class AddStationAdapter(private val stations: List<Station>) :
    RecyclerView.Adapter<AddStationAdapter.Holder>()
{

    override fun getItemCount(): Int = stations.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v : View = LayoutInflater.from(parent.context).inflate(R.layout.item_added_station, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        var station = stations[position]
        holder.trName.text = station.name

        val imageURL = station.faviconUrl
        if(imageURL != ""){
            Picasso.get().load(imageURL).error(R.drawable.ic_station_placeholder_54).into(holder.favicon)
        }

    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val trName : TextView = view.findViewById(R.id.station_name)
        val favicon : ImageView = view.findViewById(R.id.station_favicon)
    }

}