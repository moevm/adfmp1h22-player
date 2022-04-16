package com.github.moevm.adfmp1h22_player

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.github.moevm.adfmp1h22_player.SQLite.SQLHelper
import com.github.moevm.adfmp1h22_player.SQLite.SQLiteAllStationsManager
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


class MainActivity : AppCompatActivity() {

    var progressDialog: ProgressDialog? = null

    companion object {
        const val TAG = "MainActivity"
    }

    private var current_station: Station? = null

    private val playbackModel: PlaybackModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startService(Intent(applicationContext, StationCatalogueUpdaterService::class.java))

        var db : SQLHelper? = null
        db = SQLHelper(applicationContext)
        val manager = SQLiteAllStationsManager(db)
        
        if(manager.emptyTable("AllStations")){
            progressDialog = ProgressDialog(this@MainActivity);
            progressDialog!!.setTitle("Getting the catalog");
            progressDialog!!.setMessage("Loading this Content, please wait!");
            progressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            progressDialog!!.show()
            progressDialog!!.setCancelable(false)
            Thread {
                try {
                    while (manager.emptyTable("AllStations")){
                        continue
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                progressDialog!!.dismiss()
            }.start()
        }

        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 2

            override fun createFragment(pos: Int): Fragment = when (pos) {
                0 -> PlayerFragment().also { f ->
                    f.onStopRequested = {
                        withPlayerService { it.stopPlayback() }
                    }
                    f.onPauseRequested = {
                        withPlayerService { it.pausePlayback() }
                    }
                    f.onResumeRequested = {
                        withPlayerService { it.resumePlayback() }
                    }
                }
                1 -> StationListFragment().also { f ->
                    f.onSetStation = { setPlayingStation(it) }
                }
                else -> throw IllegalArgumentException("Invalid fragment index")
            }
        }

        pager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(pos: Int) {
                    if (pos == 0) {
                        if (playbackModel.station.value?.let {
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

        Log.d(TAG, "creating main activity with $savedInstanceState")
        pager.setCurrentItem(savedInstanceState?.getInt("page") ?: 1, false)

        playbackModel.station.observe(this) { s ->
            Log.d(TAG, "main activity station $s")
            if (s == null) {
                pager.setCurrentItem(1)
            } else {
                pager.setCurrentItem(0)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val i = Intent(this, PlayerService::class.java)
        startService(i)
        bindService(i, mServiceConnection, 0)
    }

    override fun onPause() {
        doUnbind()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Log.d(TAG, "saving main activity")
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
            R.id.debug_info -> {
                withPlayerService { it.logDebugInfo() }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private val mServiceConnection = PlayerServiceConnection()

    inner class PlayerServiceConnection : ServiceConnection {
        private var mServiceBinder: PlayerService.PlayerServiceBinder? = null
        private val mCallbackQueue = LinkedList<(PlayerService) -> Unit>()

        override fun onServiceConnected(n: ComponentName, sb: IBinder) {
            mServiceBinder = sb as PlayerService.PlayerServiceBinder
            sb.service.mStation.observe(this@MainActivity) { s ->
                playbackModel.station.setValue(s)
            }
            sb.service.mPlaybackState.observe(this@MainActivity) { stt ->
                playbackModel.state.setValue(stt)
            }
            sb.service.mMetaData.observe(this@MainActivity) { m ->
                playbackModel.metadata.setValue(m)
            }
            while (!mCallbackQueue.isEmpty()) {
                val cb = mCallbackQueue.remove()
                cb(sb.service)
            }
        }

        override fun onServiceDisconnected(n: ComponentName) {
            mServiceBinder = null;
            Log.e(TAG, "onServiceDisconnected")
        }

        fun doAction(cb: (PlayerService) -> Unit) {
            val b = mServiceBinder
            if (b != null) {
                Log.d(TAG, "doAction: immediate")
                cb(b.service)
            } else {
                mCallbackQueue.add(cb)
                Log.d(TAG,
                      "doAction: delaying, queue size ${mCallbackQueue.size}")

                val i = Intent(this@MainActivity, PlayerService::class.java)
                startService(i)
                bindService(i, this, 0)
            }
        }
    }

    private fun withPlayerService(action: (PlayerService) -> Unit) {
        mServiceConnection.doAction(action)
    }

    private fun doUnbind() {
        unbindService(mServiceConnection)
    }

    fun setPlayingStation(s: Station?) {
        if (s == playbackModel.station.value) {
            pager.setCurrentItem(0)
            return
        }

        if (s != null) {
            withPlayerService {
                it.startPlayingStation(s)
            }
        }
    }
}
