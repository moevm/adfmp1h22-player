package com.github.moevm.adfmp1h22_player

import android.annotation.SuppressLint
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class SaveTracksAdapter(
    private val clickCallback: (Recording) -> Unit,
) : RecyclerView.Adapter<SaveTracksAdapter.Holder>() {

    private var recordings: List<Recording>? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setRecordings(newRecs: List<Recording>) {
        recordings = newRecs
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        Log.d("SaveTracksAdapter", "getItemCount called. Returned ${recordings?.size ?: 0}")
        return recordings?.size ?: 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track, parent, false)
        val h = Holder(v)
        v.setOnClickListener {
            val r = h.rec
            if (r != null) {
                clickCallback(r)
            }
        }
        return h
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        Log.d("SaveTracksAdapter", position.toString() + "   test123")
        val rec = recordings?.get(position)

        holder.rec = rec

        val a = rec?.metadata?.artist
        if (a != null) {
            holder.artist.text = a
            holder.artist.setVisibility(View.VISIBLE)
        } else {
            holder.artist.setVisibility(View.INVISIBLE)
        }

        holder.trName.text = rec?.metadata?.title
        if (rec?.state == Recording.STATE_SAVED) {
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
        val trName: TextView = view.findViewById(R.id.trackName)
        val artist: TextView = view.findViewById(R.id.artist)
        val img: ImageView = view.findViewById(R.id.imageStationAvatar)
        val download: ImageView = view.findViewById(R.id.imageDownload)
        val done: ImageView = view.findViewById(R.id.imageDone)

        var rec: Recording? = null
    }

}
