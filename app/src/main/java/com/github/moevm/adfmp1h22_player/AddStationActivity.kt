package com.github.moevm.adfmp1h22_player

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.moevm.adfmp1h22_player.SQLite.SQLiteAddedStationsManager
import com.github.moevm.adfmp1h22_player.SQLite.SQLiteAllStationsManager
import kotlinx.android.synthetic.main.activity_add_station.*
import java.util.*


class AddStationActivity : AppCompatActivity() {

    private var stationCatalogueUpdaterService: StationCatalogueUpdaterService? = null
    private var sBound: Boolean = false
    private var binder :StationCatalogueUpdaterService.Service1Binder? = null

    private val boundServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            binder = p1 as StationCatalogueUpdaterService.Service1Binder
            if(!sBound) {
                stationCatalogueUpdaterService = binder!!.getService()
                sBound = true
                if(sBound){
                    val db = stationCatalogueUpdaterService?.db
                    val manager = SQLiteAllStationsManager(db!!)
                    val managerAdd = SQLiteAddedStationsManager(db)
                    val stationList = manager.getData()
                    val selectedStations = managerAdd.getData()
                    Log.d("TAG", selectedStations.toString())
                    val layoutManager = LinearLayoutManager(applicationContext)
                    stationToAddList.layoutManager = layoutManager
                    val adapter = AddStationAdapter(
                        stationList,
                        selectedStations,
                        managerAdd){}
                    stationToAddList.adapter = adapter

                    searchStation.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String?): Boolean {
//                            Log.d("TAG", "in SEARCH")
//                            if(query != null){
//                                Log.d("TAG", "in NOT NULL QUERY")
//                                val newStations :MutableList<Station> = mutableListOf()
//                                for(st in stationList){
//                                    if(st.name.contains(query)){
//                                        newStations.add(st)
//                                    }
//                                }
//                                Log.d("TAG", newStations.toString())
//                                Log.d("TAG", newStations.size.toString())
//                                if (newStations.size > 0) {
//                                    adapter.setStations(newStations)
//                                } else {
//                                    adapter.setStations(newStations)//////??????
//                                }
//                                return false
//                            }else{
//                                return false
//                            }
                            return false
                        }

                        override fun onQueryTextChange(newText: String?): Boolean {
                            ///
                            if(newText != null){
                                val newStations :MutableList<Station> = mutableListOf()
                                for(st in stationList){
                                    if(st.name.contains(newText)){
                                        newStations.add(st)
                                    }
                                }
                                adapter.setStations(newStations)
                                return false
                            }
                            ///
                            return false
                        }
                    })
                }
                else{
                    Log.d("TAG","NOT BIND")
                }
            }
            Log.d("TAG",sBound.toString())
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            sBound = false
            binder = null

        }

    }

//    override fun onResume() {
//        super.onResume()
//        val i = Intent(this, PlayerService::class.java)
//        startService(i)
//        bindService(i, mServiceConnection, 0)
//    }
//
//    override fun onPause() {
//        doUnbind()
//        super.onPause()
//    }
//    private val mServiceConnection = PlayerServiceConnection()
//    inner class PlayerServiceConnection : ServiceConnection {
//        private var mServiceBinder: PlayerService.PlayerServiceBinder? = null
//        private val mCallbackQueue = LinkedList<(PlayerService) -> Unit>()
//
//        override fun onServiceConnected(n: ComponentName, sb: IBinder) {
//            mServiceBinder = sb as PlayerService.PlayerServiceBinder
//            while (!mCallbackQueue.isEmpty()) {
//                val cb = mCallbackQueue.remove()
//                cb(sb.service)
//            }
//        }
//
//        override fun onServiceDisconnected(n: ComponentName) {
//            mServiceBinder = null;
//            Log.e(MainActivity.TAG, "onServiceDisconnected")
//        }
//
//        fun doAction(cb: (PlayerService) -> Unit) {
//            val b = mServiceBinder
//            if (b != null) {
//                cb(b.service)
//            } else {
//                mCallbackQueue.add(cb)
//
//                val i = Intent(this@AddStationActivity, PlayerService::class.java)
//                startService(i)
//                bindService(i, this, 0)
//            }
//        }
//    }
//
//    private fun doUnbind() {
//        unbindService(mServiceConnection)
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_station)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        applicationContext.bindService(Intent(this, StationCatalogueUpdaterService::class.java), boundServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
