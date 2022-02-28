package com.github.moevm.adfmp1h22_player

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_save_tracks.*

class SaveTracksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_tracks)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        var tracksList = mutableListOf<Track>()
//
//        val progress = (getIntent().getStringExtra("progress")).toString().toInt();
        val progress = 10
        for(i in 0..progress){
            val track : Track = Track(i,"Item${i}","ItemArtist${i}", false)
            tracksList.add(i, track)
        }

        var SaveTrackAdapter = SaveTracksAdapter(tracksList)

        val layoutManager = LinearLayoutManager(this)
        tracksListRV.layoutManager = layoutManager
        tracksListRV.adapter = SaveTrackAdapter
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
