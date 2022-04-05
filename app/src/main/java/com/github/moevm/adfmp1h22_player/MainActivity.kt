package com.github.moevm.adfmp1h22_player

import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private var current_station: Station? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startService(Intent(applicationContext, StationCatalogueUpdaterService::class.java))

        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2

            override fun createFragment(pos: Int): Fragment = when (pos) {
                0 -> PlayerFragment().also {
                    it.onStopListener = {
                        setPlayingStation(null)
                    }
                    it.queryState = {
                        current_station != null
                    }
                }
                1 -> StationListFragment().also {
                    it.onSetStation = { s: Station ->
                        setPlayingStation(s)
                    }
                }
                else -> throw IllegalArgumentException("Invalid fragment index")
            }
        }

        pager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(pos: Int) {
                    if (pos == 0) {
                        if (current_station?.let {
                                setTitle(it.name)
                                false
                            } ?: true) {
                            setTitle(R.string.app_name)
                        }
                    } else {
                        setTitle(R.string.app_name)
                    }
                }
            }
        )

        Log.d("LIFECYCLE", "creating main activity with $savedInstanceState")
        pager.setCurrentItem(savedInstanceState?.getInt("page") ?: 1, false)


    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
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
                startActivity(Intent(this, HistoryActivity::class.java))
                true
            }
            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun setPlayingStation(s: Station?) {
        current_station = s
        if (s != null) {
            pager.setCurrentItem(0)
        } else {
            pager.setCurrentItem(1)
        }
    }
}
