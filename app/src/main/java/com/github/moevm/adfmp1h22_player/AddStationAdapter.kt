package com.github.moevm.adfmp1h22_player

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.moevm.adfmp1h22_player.SQLite.SQLHelper
import com.github.moevm.adfmp1h22_player.SQLite.SQLiteAddedStationsManager
import com.github.moevm.adfmp1h22_player.SQLite.SQLiteAllStationsManager
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*

class AddStationAdapter(private val stations: List<Station>, private val selectedStations: List<Station>, private val manager: SQLiteAddedStationsManager) :
    RecyclerView.Adapter<AddStationAdapter.Holder>(),
    View.OnClickListener
{
    override fun getItemCount(): Int = stations.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val v : View = LayoutInflater.from(parent.context).inflate(R.layout.item_add_station, parent, false)
        v.setOnClickListener(this)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        var station = stations[position]
        holder.trName.text = station.name
        holder.itemView.tag = station
//        Log.d("TAG", selectedStations.indexOf(station).toString())
//        Log.d("TAG", selectedStations.toString())
//        Log.d("TAG", station.toString())
        if(station.codec != "MP3" || station.streamUrl.split(":")[0] == "https"){
            holder.itemView.isEnabled = false
            holder.button.setClickable(false)
            holder.button.setEnabled(false)
            holder.button.visibility = android.view.View.INVISIBLE
            holder.trName.setTextColor(Color.RED)
        }
        if(selectedStations.indexOf(station) != -1){
            holder.itemView.isEnabled = false
            holder.button.setClickable(false)
            holder.button.setEnabled(false)
            holder.trName.setTextColor(Color.LTGRAY)
            holder.button.setColorFilter(Color.LTGRAY)
        }
        val imageURL = station.faviconUrl
        if(imageURL != ""){
            Picasso.get()
                .load(imageURL)
                .error(R.drawable.ic_station_placeholder_54)
                .into(holder.favicon)
        }
        holder.button.setOnClickListener {
            Log.d("TAG", "click on +")
            holder.itemView.isEnabled = false
            holder.trName.setTextColor(Color.LTGRAY)
            holder.button.setColorFilter(Color.LTGRAY)
            holder.button.setClickable(false)
            holder.button.setEnabled(false)
            val t : Thread = object  : Thread(){
                override fun run(){
                    manager.insertRow(station)
                }
            }
            t.start()
        }
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val trName : TextView = view.findViewById(R.id.station_name)
        val favicon : ImageView = view.findViewById(R.id.station_favicon)
        val button : ImageButton = view.findViewById(R.id.imageAddButton)
    }

    override fun onClick(p0: View?) {
        val station = (p0?.tag as Station)
        Log.d("TAG", "$station")
        ///
        // Запускать проигрывание.
        ///
    }

}