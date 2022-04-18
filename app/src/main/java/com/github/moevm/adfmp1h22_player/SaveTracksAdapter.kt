package com.github.moevm.adfmp1h22_player

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class SaveTracksAdapter(
    private val clickCallback: (Recording) -> Unit,
) : RecyclerView.Adapter<SaveTracksAdapter.Holder>()
{

    private var recordings: ArrayList<Recording>()

    @SuppressLint("NotifyDataSetChanged")
    fun setStations(newRecs: List<Recording>){
        recordings = newRecs
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = recordings.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v : View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)
        val h = Holder(v)
        v.setOnClickListener {
            val r = h.rec
            if (r != null)
                clickCallback(r)
            }
        }
        return h
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        var rec = recordings[position]

        holder.rec = rec

        val a = rec.metadata.artist
        if (a != null) {
            holder.artist.text = a
            holder.artist.setVisible(true)
        } else {
            holder.artist.setVisible(false)
        }

        holder.trName.text = rec.metadata.title
        if (rec.status == Recording.STATUS_SAVED) {
            holder.done.visibility = android.view.View.VISIBLE
            holder.download.visibility = android.view.View.GONE
            holder.itemView.isEnabled = false
        } else {
            holder.done.visibility = android.view.View.GONE
            holder.download.visibility = android.view.View.VISIBLE
            holder.itemView.isEnabled = true
        }
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val trName : TextView = view.findViewById(R.id.trackName)
        val artist: TextView = view.findViewById(R.id.artist)
        val img : ImageView = view.findViewById(R.id.imageStationAvatar)
        val download : ImageView = view.findViewById(R.id.imageDownload)
        val done : ImageView = view.findViewById(R.id.imageDone)

        var rec: Recording? = null
    }

}
