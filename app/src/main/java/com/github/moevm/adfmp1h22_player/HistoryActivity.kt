package com.github.moevm.adfmp1h22_player

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.activity_save_tracks.*

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var historyList = mutableListOf<Track>()

        val progress = 10
        for(i in 0..progress){
            val track : Track = Track(i,"Item${i}","ItemArtist${i}", false)
            historyList.add(i, track)
        }

        var HistoryAdapter = HistoryAdapter(historyList)

        val layoutManager = LinearLayoutManager(this)
        historyListRV.layoutManager = layoutManager
        historyListRV.adapter = HistoryAdapter

    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}