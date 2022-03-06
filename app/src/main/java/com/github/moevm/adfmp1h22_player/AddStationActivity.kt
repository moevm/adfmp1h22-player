package com.github.moevm.adfmp1h22_player

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_add_station.*

class AddStationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_station)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val stationList = intent?.getParcelableArrayListExtra<Station>("stationList")
        val layoutManager = LinearLayoutManager(this)
        stationToAddList.layoutManager = layoutManager
        if(stationList != null){
            stationToAddList.adapter = AddStationAdapter(stationList)
        }
        else{
            Log.d("TAG", "Error in AddStationActivity")
        }
    }



    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}