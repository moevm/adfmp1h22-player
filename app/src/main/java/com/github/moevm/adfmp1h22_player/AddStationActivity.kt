package com.github.moevm.adfmp1h22_player

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_add_station.*

class AddStationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_station)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var stationList = mutableListOf<Station>()

        val progress = 10
        for (i in 0..progress) {
            val station = Station("", "Station ${i}", "")
            stationList.add(i, station)
        }

        val layoutManager = LinearLayoutManager(this)
        stationToAddList.layoutManager = layoutManager
        stationToAddList.adapter = AddStationAdapter(stationList)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}