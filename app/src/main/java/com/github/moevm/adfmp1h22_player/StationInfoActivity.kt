package com.github.moevm.adfmp1h22_player

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_station_info.*

class StationInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_station_info)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val arguments = intent.extras
        val info = arguments!!["info"].toString().split('$')
        Log.d("TAG", info.toString())
        stationNameView.text = info[0]
        stationUrlViewName.text = info[1]
        stationFormatViewName.text = info[2]
        stationHomePageViewName.text = info[3]
        stationCountryViewName.text = info[4]

    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
