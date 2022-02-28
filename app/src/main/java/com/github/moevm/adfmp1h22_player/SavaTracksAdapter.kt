package com.github.moevm.adfmp1h22_player

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class SavaTracksAdapter(private val tracks: List<Track>) :
    RecyclerView.Adapter<SavaTracksAdapter.Holder>(),
    View.OnClickListener
{

    override fun getItemCount(): Int = tracks.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v : View = LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false)
        v.setOnClickListener(this)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        var track = tracks[position]
        holder.itemView.tag = track
        holder.artist.text = track.artist
        holder.trName.text = track.name
        if(track.status){
            holder.done.visibility = android.view.View.VISIBLE
            holder.download.visibility = android.view.View.GONE
            holder.itemView.isEnabled = false
            holder.trName.setTextColor(Color.LTGRAY)
            holder.artist.setTextColor(Color.LTGRAY)
            holder.done.setColorFilter(Color.LTGRAY)

        }
    }

    override fun onClick(p0: View?) {
        (p0?.tag as Track).status = true
        onBindViewHolder(Holder(p0), tracks.indexOf((p0?.tag as Track)))
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val trName : TextView = view.findViewById(R.id.trackName)
        val artist: TextView = view.findViewById(R.id.artist)
        val img : ImageView = view.findViewById(R.id.imageStationAvatar)
        val download : ImageView = view.findViewById(R.id.imageDownload)
        val done : ImageView = view.findViewById(R.id.imageDone)
    }

}