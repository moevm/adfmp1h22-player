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

    private var service1: Service1? = null
    private var sBound: Boolean = false

    private val boundServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder : Service1.Service1Binder = p1 as Service1.Service1Binder
            if(!sBound) {
                service1 = binder.getService()
                sBound = true
                if(sBound){
                    val db = service1?.db
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
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_station)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        applicationContext.bindService(Intent(this, Service1::class.java), boundServiceConnection, Context.BIND_AUTO_CREATE)
//        Intent(this, Service1::class.java).also { intent ->
//            application.bindService(intent, boundServiceConnection, Context.BIND_AUTO_CREATE)
//        }
    }


    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}