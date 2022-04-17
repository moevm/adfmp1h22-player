package com.github.moevm.adfmp1h22_player

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HistoryAdapter(private val tracks: List<TrackMetaData>) :
    RecyclerView.Adapter<HistoryAdapter.Holder>()
{

    override fun getItemCount(): Int = tracks.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v : View = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        var track = tracks[position]
        holder.itemView.tag = track
        holder.artist.text = track.artist
        holder.trName.text = track.title
    }


    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val trName : TextView = view.findViewById(R.id.trackName)
        val artist: TextView = view.findViewById(R.id.artist)
        val img : ImageView = view.findViewById(R.id.imageStationAvatar)
    }

}