package com.github.moevm.adfmp1h22_player

import java.io.IOException
import java.io.Reader
import java.io.InputStreamReader

import android.util.Log
import android.util.JsonReader

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.MotionEvent
import android.widget.Toast
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.StorageStrategy
import androidx.viewpager2.widget.ViewPager2
import com.github.moevm.adfmp1h22_player.SQLite.SQLHelper
import com.github.moevm.adfmp1h22_player.SQLite.SQLiteAddedStationsManager
import com.github.moevm.adfmp1h22_player.SQLite.SQLiteAllStationsManager
import com.github.moevm.adfmp1h22_player.SQLite.SQLiteContract

import com.google.android.material.snackbar.Snackbar

import kotlinx.android.synthetic.main.fragment_station_list.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Throws IOException
//fun readStationList(r: JsonReader): List<Station> {
//    r.beginArray()
//    val l = mutableListOf<Station>()
//    while (r.hasNext()) {
//        r.beginObject()
//        var changeUuid: String? = null
//        var stationuuid : String? = null
//        var name: String? = null
//        var streamUrl: String? = null
//        var faviconUrl: String? = null
//        while (r.hasNext()) {
//            when (r.nextName()) {
//                "changeuuid" -> changeUuid = r.nextString()
//                "stationuuid" -> stationuuid = r.nextString()
//                "name" -> name = r.nextString()
//                "url" -> streamUrl = r.nextString()
//                "favicon" -> faviconUrl = r.nextString()
//                else -> r.skipValue()
//            }
//        }
//        r.endObject()
//        if (changeUuid != null
//            && stationuuid != null
//            && name != null
//            && streamUrl != null
//            && faviconUrl != null) {
//            l.add(Station(
//                      changeUuid,
//                      stationuuid,
//                      name,
//                      streamUrl,
//                      faviconUrl,
//            ))
//        }
//    }
//    r.endArray()
//    return l
//}

class StationListFragment : Fragment(R.layout.fragment_station_list) {

    var onSetStation: ((Station) -> Unit)? = null
    var action_mode: ActionMode? = null
    lateinit var tracker: SelectionTracker<String>
    val a = StationListAdapter { s: Station ->
        onSetStation?.let { it(s) }
    }
    var db : SQLHelper? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        Log.d("TAG", "In onViewCreated !!!!!!!!!!!!")

        add_fab.setOnClickListener {
            val intent = Intent(context, AddStationActivity::class.java)
            startActivity(intent)
        }

        station_list.adapter = a
        tracker = SelectionTracker.Builder<String>(
            "station-list-selection",
            station_list,
            object : ItemKeyProvider<String>(ItemKeyProvider.SCOPE_CACHED) {
                override fun getKey(pos: Int): String = a.currentList.get(pos).stationuuid
                override fun getPosition(k: String): Int {
                    for (i in 0 until station_list.childCount) {
                        val v = station_list.getChildAt(i)
                        (station_list.getChildViewHolder(v) as? StationListAdapter.StationViewHolder)?.let {
                            if (it.stationUuid == k) {
                                return i
                            }
                        }
                    }
                    return -1
                }
            },
            object : ItemDetailsLookup<String>() {
                override fun getItemDetails(e: MotionEvent): ItemDetails<String>? {
                    val v = station_list.findChildViewUnder(e.x, e.y)
                    if (v != null) {
                        val h = station_list.getChildViewHolder(v) as? StationListAdapter.StationViewHolder
                        if (h != null) {
                            return object : ItemDetails<String>() {
                                override fun getPosition(): Int
                                    = h.absoluteAdapterPosition
                                override fun getSelectionKey(): String
                                    = h.stationUuid
                            }
                        } else {
                            return null
                        }
                    } else {
                        return null
                    }
                }
            },
            StorageStrategy.createStringStorage()
        ).build()
        tracker.addObserver(object : SelectionTracker.SelectionObserver<String>() {
            override fun onSelectionChanged() {
                val sel = tracker.selection

                activity?.let{
                    val pager = it.findViewById(R.id.pager) as ViewPager2
                    pager.setUserInputEnabled(sel.isEmpty())
                }

                when {
                    sel.isEmpty() && action_mode != null -> {
                        action_mode?.finish()
                        action_mode = null
                    }
                    !sel.isEmpty() && action_mode == null -> {
                        activity?.let {
                            action_mode = (it as AppCompatActivity)
                                .startSupportActionMode(actionModeCallback())
                        }
                    }
                }

                action_mode?.let {
                    val title = resources.getQuantityString(
                        R.plurals.station_list_item_selection,
                        sel.size(),
                        sel.size()
                    )
                    it.setTitle(title)
                    val ma_info = it.getMenu()
                        .findItem(R.id.station_list_item_info)
                    ma_info.setVisible(sel.size() == 1)
                }
            }
        })
        a.tracker = tracker

        db = context?.let { SQLHelper(it) }
        val manager = SQLiteAddedStationsManager(db!!)
        val l = manager.getData();
        a.submitList(l)
    }

    override fun onResume() {
        super.onResume()
        db = context?.let { SQLHelper(it) }
        val manager = SQLiteAddedStationsManager(db!!)
        val l = manager.getData();
        Log.d("TAG", l.size.toString())
        a.submitList(l)
    }

    fun actionModeCallback(): ActionMode.Callback
        = object : ActionMode.Callback {
            override fun onActionItemClicked(
                am: ActionMode, mi: MenuItem
            ): Boolean {
                return when (mi.itemId) {
                    R.id.station_list_item_delete -> {
                        val n = tracker.getSelection().size()
                        Toast.makeText(context, "Would delete $n station(s)",
                                       Toast.LENGTH_SHORT)
                            .show()
                        db = context?.let { SQLHelper(it) }
                        val manager = SQLiteAddedStationsManager(db!!)
                        val l = manager.getData();
                        tracker.selection.forEach{
                            for(i in l){
                                if(i.stationuuid == it.toString()){
                                    Log.d("TAG", i.toString())
                                    l.remove(i)
                                    a.submitList(l)
                                    manager.deleteRow(i)
                                    break
                                }
                            }
                        }
                        tracker.clearSelection()
                        true
                    }
                    R.id.station_list_item_info -> {
                        // TODO: actually pass selection
                        db = context?.let { SQLHelper(it) }
                        val manager = SQLiteAddedStationsManager(db!!)
                        val l = manager.getData();
                        var info :String = ""
                        tracker.selection.forEach{
                            for(i in l){
                                if(i.stationuuid == it.toString()){
                                    Log.d("TAG", i.toString())
                                    info += i.name
                                    info += "$"
                                    info += i.streamUrl
                                    info += "$"
                                    info += i.codec
                                    info += "$"
                                    info += i.homepage
                                    info += "$"
                                    info += i.country
                                }
                            }
                        }
                        tracker.clearSelection()
                        val intent = Intent(context, StationInfoActivity::class.java)
                        intent.putExtra("info", info)
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }
            override fun onCreateActionMode(am: ActionMode, m: Menu): Boolean {
                am.menuInflater.inflate(R.menu.station_list_menu, m)
                return true
            }
            override fun onDestroyActionMode(am: ActionMode) {
                tracker.clearSelection()
            }
            override fun onPrepareActionMode(am: ActionMode, m: Menu): Boolean {
                return false
            }
        }
}

