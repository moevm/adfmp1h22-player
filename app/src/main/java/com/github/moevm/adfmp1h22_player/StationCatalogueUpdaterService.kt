package com.github.moevm.adfmp1h22_player

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.github.moevm.adfmp1h22_player.SQLite.SQLHelper
import com.github.moevm.adfmp1h22_player.SQLite.SQLiteAllStationsManager
import com.github.moevm.adfmp1h22_player.SQLite.SQLiteContract
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StationCatalogueUpdaterService : Service() {

    var db : SQLHelper? = null

    private val binder by lazy { Service1Binder() }
    inner class Service1Binder: Binder(){
        fun getService(): StationCatalogueUpdaterService{
            Log.d("TAG", "getService")
            return this@StationCatalogueUpdaterService
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    private fun fillingAddedList(manager: SQLiteAllStationsManager) {

        Log.d("TAG", "fillingAddedList")
        val apiInterface = APIClient().getClient()?.create(APIInterface::class.java)

        val stL = ArrayList<Station>()
        if(manager.emptyTable(SQLiteContract.AllStationsTable.TABLE_NAME)){
            Log.d("TAG", "in EMPTY TABLE")
            val call: Call<AddStationList?>? = apiInterface!!.AddStationListResources()
            Log.d("TAG", call?.request()?.headers.toString())
            call?.enqueue(object  : Callback<AddStationList?> {
                override fun onResponse(
                    call: Call<AddStationList?>,
                    response: Response<AddStationList?>
                ) {
                    Log.d("TAG", response.code().toString())
                    val resource: AddStationList? = response.body()
                    if(resource != null){
                        stL.clear()
                        var progress = resource.size - 1
                        for (i in 0..progress) {
                            val station = Station(
                                resource[i].changeuuid.toString(),
                                resource[i].stationuuid.toString(),
                                resource[i].name.toString(),
                                resource[i].url.toString(),
                                resource[i].codec.toString(),
                                resource[i].favicon.toString(),
                            )
                            stL.add(station)
                        }
                        val t : Thread = object  : Thread(){
                            override fun run(){
                                manager.insertRows(stL)
//                                manager.deleteTable("AllStations")
//                                manager.replace("AllStations_new", "AllStations")
                            }
                        }
                        t.start()
                    }
                    else{
                        Log.d("TAG", "Error in StationListFragmrnt.kt")
                    }
                }
                override fun onFailure(call: Call<AddStationList?>, t: Throwable) {
                    call.cancel()
                }
            })
        }
    }

    private fun updateAddedList(manager: SQLiteAllStationsManager){
        Log.d("TAG", "updateAddedList")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        db = SQLHelper(applicationContext)

        val manager = SQLiteAllStationsManager(db!!)
        val t : Thread = object  : Thread() {
            override fun run() {
                if(manager.emptyTable(SQLiteContract.AllStationsTable.TABLE_NAME)){
                    fillingAddedList(manager)
                }
                else{
                    updateAddedList(manager)
                }
            }
        }
        t.start()
        Log.d("TAG", "StationCatalogueUpdaterService started")
        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d("TAG", "StationCatalogueUpdaterService destroyed")
    }
}
