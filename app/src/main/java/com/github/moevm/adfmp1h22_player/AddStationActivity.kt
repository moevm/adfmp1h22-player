package com.github.moevm.adfmp1h22_player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.moevm.adfmp1h22_player.SQLite.SQLiteAllStationsManager
import kotlinx.android.synthetic.main.activity_add_station.*

class AddStationActivity : AppCompatActivity() {

    private var stationCatalogueUpdaterService: StationCatalogueUpdaterService? = null
    private var sBound: Boolean = false

    private val boundServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder : StationCatalogueUpdaterService.Service1Binder = p1 as StationCatalogueUpdaterService.Service1Binder
            if(!sBound) {
                stationCatalogueUpdaterService = binder.getService()
                sBound = true
                if(sBound){
                    val db = stationCatalogueUpdaterService?.db
                    val manager = SQLiteAllStationsManager(db!!)
                    val stationList = manager.getData()
                    val layoutManager = LinearLayoutManager(applicationContext)
                    stationToAddList.layoutManager = layoutManager
                    stationToAddList.adapter = AddStationAdapter(stationList)
                }
                else{
                    Log.d("TAG","NOT BIND")
                }
            }
            Log.d("TAG",sBound.toString())
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            sBound = false
//        val progress = 10
//        for (i in 0..progress) {
//            val station = Station("", "Station ${i}", "", "")
//            stationList.add(i, station)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_station)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        applicationContext.bindService(Intent(this, StationCatalogueUpdaterService::class.java), boundServiceConnection, Context.BIND_AUTO_CREATE)
//        Intent(this, StationCatalogueUpdaterService::class.java).also { intent ->
//            application.bindService(intent, boundServiceConnection, Context.BIND_AUTO_CREATE)
//        }
    }


    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
