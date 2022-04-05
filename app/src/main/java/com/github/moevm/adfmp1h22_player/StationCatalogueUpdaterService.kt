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


    fun updateAddedList(manager: SQLiteAllStationsManager){

        Log.d("TAG", "updateAddedList")
        val apiInterface = APIClient().getClient()?.create(APIInterface::class.java)


        val stL = ArrayList<Station>()
        GlobalScope.launch{
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
                    var progress = resource.size - 1
                    for (i in 0..progress) {
                        if(progress % 10 == 0) {
                            val station = Station(
                                resource[i].changeuuid.toString(),
                                resource[i].name.toString(),
                                resource[i].favicon.toString()
                            )
                            stL.add(station)
                        }
                    }
                    manager.insertRows(stL)
                    manager.deleteTable("AllStations")
                    manager.replace("AllStations_new", "AllStations")
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        db = SQLHelper(applicationContext)

        val manager = SQLiteAllStationsManager(db!!)

        val createTable_new = "CREATE TABLE IF NOT EXISTS ${SQLiteContract.AllStationsTable.TABLE_NAME_NEW} (" +
                SQLiteContract.AllStationsTable.COLUMN_ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT," +
                SQLiteContract.AllStationsTable.COLUMN_CHANGEUUID + " TEXT," +
                SQLiteContract.AllStationsTable.COLUMN_NAME + " TEXT," +
                SQLiteContract.AllStationsTable.COLUMN_FAVICON + " TEXT," +
                SQLiteContract.AllStationsTable.COLUMN_FAVICON_DATE +
                " INTEGER NOT NULL)"
        manager.createTable(createTable_new)

        updateAddedList(manager)
        Log.d("TAG", "StationCatalogueUpdaterService started")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TAG", "StationCatalogueUpdaterService destroyed")
    }
}