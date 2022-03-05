package com.github.moevm.adfmp1h22_player

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_add_station.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class AddStationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_station)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var stationList = mutableListOf<Station>()
        val layoutManager = LinearLayoutManager(this)
        val apiInterface = APIClient().getClient()?.create(APIInterface::class.java)

        GlobalScope.launch {
            val call: Call<AddStationList?>? = apiInterface!!.AddStationListResources()
            call?.enqueue(object  : Callback<AddStationList?>{
                override fun onResponse(
                    call: Call<AddStationList?>,
                    response: Response<AddStationList?>
                ) {
                    Log.d("TAG", response.code().toString())
                    val resource: AddStationList? = response.body()
                    if(resource != null){
                        val progress = resource.size - 1
                        for (i in 0..progress) {
                            val station = Station(resource[i].changeuuid.toString(), resource[i].name.toString(), resource[i].favicon.toString())
                            stationList.add(i, station)
                        }
                        stationToAddList.layoutManager = layoutManager
                        stationToAddList.adapter = AddStationAdapter(stationList)
                    }
                    else{
                        Log.d("TAG", "Error in AddStationActivity.kt")
                    }

                }

                override fun onFailure(call: Call<AddStationList?>, t: Throwable) {
                    call.cancel()
                }

            })
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}