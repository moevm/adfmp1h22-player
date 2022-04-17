package com.github.moevm.adfmp1h22_player

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.moevm.adfmp1h22_player.SQLite.SQLHelper
import com.github.moevm.adfmp1h22_player.SQLite.SQLiteAddedStationsManager
import com.github.moevm.adfmp1h22_player.SQLite.SQLiteHistoryManager
import kotlinx.android.synthetic.main.activity_history.*
import kotlinx.android.synthetic.main.activity_save_tracks.*

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val db = SQLHelper(applicationContext)
        val manager = SQLiteHistoryManager(db)
        val historyList = manager.getData();

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