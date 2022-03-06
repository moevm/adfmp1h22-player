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
import androidx.viewpager2.widget.ViewPager2
import kotlinx.android.synthetic.main.activity_main.*

import android.content.ServiceConnection
import android.content.ComponentName
import android.os.IBinder


class MainActivity : AppCompatActivity() {

    private var current_station: Station? = null

    // TODO: fun setPlaybackState (pause/resume, stop)
    // TODO: fun startPlayingStation (Station)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2

            override fun createFragment(pos: Int): Fragment = when (pos) {
                0 -> PlayerFragment().also {
                    it.onStopListener = {
                        setPlayingStation(null)
                    }
                    it.queryState = {
                        if (current_station != null) {
                            PlaybackState.PLAYING
                        } else {
                            PlaybackState.STOPPED
                        }
                    }
                    // TODO: setState
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

    override fun onDestroy() {
        super.onDestroy()
        doUnbind()
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

    private var mServiceConnection: PlayerServiceConnection? = null
    private var mServiceBinder: PlayerService.PlayerServiceBinder? = null

    inner class PlayerServiceConnection(
        private val action: (PlayerService) -> Unit
    ) : ServiceConnection {
        override fun onServiceConnected(n: ComponentName, s: IBinder) {
            mServiceBinder = s as PlayerService.PlayerServiceBinder
            action(s.service)
        }

        override fun onServiceDisconnected(n: ComponentName) {
            mServiceBinder = null;
        }
    }

    private fun withPlayerService(action: (PlayerService) -> Unit) {
        val b = mServiceBinder
        if (b == null) {
            val i = Intent(this, PlayerService::class.java)
            startForegroundService(i)

            val c = PlayerServiceConnection(action)
            mServiceConnection = c
            bindService(i, c, 0)
        } else {
            action(b.service)
        }
    }

    private fun doUnbind() {
        mServiceConnection?.let {
            unbindService(it)
            mServiceConnection = null
            mServiceBinder = null
        }
    }

    fun setPlayingStation(s: Station?) {
        if (s == current_station) {
            return
        }

        current_station = s
        if (s != null) {

            withPlayerService {
                it.startPlayingStation(s)
            }

            pager.setCurrentItem(0)

        } else {

            withPlayerService {
                it.stopPlayback()
            }

            pager.setCurrentItem(1)
        }
    }
}
