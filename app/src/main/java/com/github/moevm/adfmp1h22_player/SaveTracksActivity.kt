package com.github.moevm.adfmp1h22_player

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_sava_tracks.*

class SaveTracksActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sava_tracks)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        imageButton.setOnClickListener{
            imageButton.visibility = android.view.View.GONE
            imageButton2.visibility = android.view.View.VISIBLE

        }

    }
}