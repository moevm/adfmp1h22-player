package com.github.moevm.adfmp1h22_player

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_sava_tracks.*

class SaveTracksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sava_tracks)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var tracksList = mutableListOf<Track>()
//
//        val progress = (getIntent().getStringExtra("progress")).toString().toInt();
        val progress = 10
        for(i in 0..progress){
            val track : Track = Track(i,"Item${i}","ItemArtist${i}", false)
            tracksList.add(i, track)
        }

        var SaveTrackAdapter = SavaTracksAdapter(tracksList)

        val layoutManager = LinearLayoutManager(this)
        tracksListRV.layoutManager = layoutManager
        tracksListRV.adapter = SaveTrackAdapter
    }
}