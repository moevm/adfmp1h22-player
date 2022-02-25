package com.github.moevm.adfmp1h22_player

import android.util.Log
import android.widget.Toast
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import kotlinx.android.synthetic.main.activity_main.*

import com.github.moevm.adfmp1h22_player.PlayerFragment
import com.github.moevm.adfmp1h22_player.StationListFragment


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2

            override fun createFragment(pos: Int): Fragment = when (pos) {
                0 -> PlayerFragment().also {
                    it.onStopListener = {
                        pager.currentItem = 1
                    }
                }
                1 -> StationListFragment()
                else -> throw IllegalArgumentException("Invalid fragment index")
            }
        }
        Log.d("LIFECYCLE", "creating main activity with $savedInstanceState")
        pager.setCurrentItem(savedInstanceState?.getInt("page") ?: 1, false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d("LIFECYCLE", "saving main activity")
        outState.putInt("page", pager.currentItem)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.playback_history -> {
                Toast.makeText(this, "No Playback history activity yet",
                               Toast.LENGTH_LONG)
                    .show()
                true
            }
            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
