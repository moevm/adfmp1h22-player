package com.github.moevm.adfmp1h22_player

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import kotlinx.android.synthetic.main.fragment_station_list.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StationListFragment : Fragment(R.layout.fragment_station_list) {

    var onSetStation: ((Station) -> Unit)? = null
    var action_mode: ActionMode? = null
    lateinit var tracker: SelectionTracker<String>

//    var stationList = ArrayList<Station>()
//    fun updateAddedList(){
//
//        val apiInterface = APIClient().getClient()?.create(APIInterface::class.java)
//
//        GlobalScope.launch {
//            val call: Call<AddStationList?>? = apiInterface!!.AddStationListResources()
//            Log.d("TAG", call?.request()?.headers.toString())
//            call?.enqueue(object  : Callback<AddStationList?> {
//                override fun onResponse(
//                    call: Call<AddStationList?>,
//                    response: Response<AddStationList?>
//                ) {
//                    Log.d("TAG", response.code().toString())
//                    val resource: AddStationList? = response.body()
//                    if(resource != null){
//                        var progress = resource.size - 1
//                        for (i in 0..progress) {
//                            val station = Station(resource[i].changeuuid.toString(), resource[i].name.toString(), resource[i].favicon.toString())
//                            stationList.add(i, station)
//                        }
//                    }
//                    else{
//                        Log.d("TAG", "Error in StationListFragmrnt.kt")
//                    }
//
//                }
//
//                override fun onFailure(call: Call<AddStationList?>, t: Throwable) {
//                    call.cancel()
//                }
//
//            })
//        }
//    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        updateAddedList()
        add_fab.setOnClickListener {
            val intent = Intent(context, AddStationActivity::class.java)
//            intent.putParcelableArrayListExtra("stationList", stationList)
            startActivity(intent)
        }

        val a = StationListAdapter { s: Station ->
            onSetStation?.let { it(s) }
        }

        station_list.adapter = a

        tracker = SelectionTracker.Builder<String>(
            "station-list-selection",
            station_list,
            object : ItemKeyProvider<String>(ItemKeyProvider.SCOPE_CACHED) {
                override fun getKey(pos: Int): String = a.currentList.get(pos).changeuuid
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

        val l = mutableListOf(
            Station("828a9ec6-9d30-40e9-8c76-fbded56fdc94", "Europa Plus", "http://liveam.tv/img/2494.jpg"),
            Station("2f0c87d1-26e3-4201-a28f-c2e68da578f6", "Вести ФМ (Vesti FM)", ""),
            Station("219fc935-26c5-11e8-91bf-52543be04c81", "Шансон Радио", "https://www.radiobells.com/stations/chanson.jpg"),
            Station("299cfc26-0705-485e-a81a-572e0652d7a5", "ROCK FM", "https://lh3.googleusercontent.com/D3taObR7tfyhwDFY40VS8DIVri7iif5RuzI9C-mXxRwF41vGZ_dO_n6MWM57P-mZczFC=w300"),
            Station("74ba2dcb-c6ad-405e-aa2c-d09226e73920", "Радио «Комсомольская Правда» | КП Россия", "https://www.kp.ru/favicon.ico"),
            Station("352d202f-e753-47fb-aa94-5dd9230fce90", "Ретро FM", "http://retrofm.ru/favicon.ico"),
            Station("c254c600-7e58-443f-9257-e130290e01c7", "Echo Moskva HD", "https://echo.msk.ru/i/icons/apple-icon-120x120.png"),
            Station("fb02bc59-c985-4dce-af18-d3b40fcfe29b", "DFM RUSSIAN DANCE", ""),
            Station("6b54b9a1-2001-11e8-a334-52543be04c81", "дорожное радио (Dorognoe Radio)", "https://dorognoe.ru/thumb/og_image_600x315/sharing_image/2016/ff/91/57cd63d58643c_sharind_dorognoe.jpg"),
            Station("850a3029-7eab-4a61-bfa2-ebe72a3a245d", "Дискотека СССР", "https://cdn2.101.ru/vardata/modules/channel/image/c34932ae363a2b1386c0136d403a2274.png"),
        )
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
                        // TODO: actually delete stuff
                        tracker.clearSelection()
                        true
                    }
                    R.id.station_list_item_info -> {
                        // TODO: actually pass selection
                        tracker.clearSelection()
                        startActivity(Intent(context, StationInfoActivity::class.java))
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
