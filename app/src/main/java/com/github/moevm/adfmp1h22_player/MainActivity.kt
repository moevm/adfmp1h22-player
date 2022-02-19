package com.github.moevm.adfmp1h22_player

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java )
            startActivity(intent)
        }

        button2.setOnClickListener{
            val intent = Intent(this, SaveTracksActivity::class.java )
            startActivity(intent)
        }
    }
}