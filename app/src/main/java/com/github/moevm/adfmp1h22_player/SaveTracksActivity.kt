package com.github.moevm.adfmp1h22_player

import android.widget.Toast

import android.content.SharedPreferences
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree

import android.util.Log

import java.util.LinkedList

import android.content.ServiceConnection
import android.content.ComponentName
import android.os.Binder
import android.os.IBinder

import android.content.Context
import android.net.Uri
import android.content.Intent

import android.view.Menu
import android.view.MenuItem

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_save_tracks.*

class SaveTracksActivity : AppCompatActivity() {

    companion object {
        const val TAG = "SaveTracksActivity"
    }

    private lateinit var mPref: SharedPreferences
    private val mSaveQueue = LinkedList<(Uri) -> Unit>()

    private val mGetSaveDirectory =
        registerForActivityResult(OpenDocumentTree()) { uri ->

            if (uri != null) {
                Log.d(TAG, "set save folder: $uri")
                mPref.edit().putString("folder", uri.toString()).apply()

                val cr = getContentResolver()
                cr.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                while (!mSaveQueue.isEmpty()) {
                    val cb = mSaveQueue.remove()
                    cb(uri)
                }
            }
        }

    private fun requestSaveDirectory(cb: (Uri) -> Unit) {
        val saveurl = mPref.getString("folder", null)
        val u = if (saveurl != null) {
            Uri.parse(saveurl)
        } else {
            null
        }

        Log.d(TAG, "rsd u = $u")

        if (u == null) {
            val do_launch = mSaveQueue.isEmpty()
            mSaveQueue.add(cb)
            if (do_launch) {
                mGetSaveDirectory.launch(null)
            }
        } else {
            cb(u)
        }
    }

    val saveTrackAdapter: SaveTracksAdapter = SaveTracksAdapter {
        requestSaveDirectory { uri ->
            val rec = it
            Log.d(TAG, "save of $rec")
            withRecordingsService {
                it.saveRecording(uri, rec) { ok ->
                    val text = if (ok) {
                        "Track saved"
                    } else {
                        "Error saving track"
                    }
                    Toast.makeText(this, text, Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_tracks)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mPref = getSharedPreferences("Table", Context.MODE_PRIVATE)!!

        val i = Intent(this, RecordingManagerService::class.java)
        startService(i)
        bindService(i, mServiceConnection, 0)

        tracksListRV.adapter = saveTrackAdapter
        val layoutManager = LinearLayoutManager(this)
        tracksListRV.layoutManager = layoutManager
    }

    private val mServiceConnection = RecordingsServiceConnection()

    inner class RecordingsServiceConnection : ServiceConnection {
        private var mServiceBinder: RecordingManagerService.ServiceBinder? = null
        private val mCallbackQueue =
            LinkedList<(RecordingManagerService) -> Unit>()

        override fun onServiceConnected(n: ComponentName, sb: IBinder) {
            mServiceBinder = sb as RecordingManagerService.ServiceBinder
            sb.service.mRecordingsList.observe(this@SaveTracksActivity) { rl ->
                Log.d(TAG, "received recordings list of size ${rl.size}")

                saveTrackAdapter.setRecordings(rl)
            }
            while (!mCallbackQueue.isEmpty()) {
                val cb = mCallbackQueue.remove()
                cb(sb.service)
            }
        }

        override fun onServiceDisconnected(n: ComponentName) {
            mServiceBinder = null;
        }

        fun doAction(cb: (RecordingManagerService) -> Unit) {
            val b = mServiceBinder
            if (b != null) {
                cb(b.service)
            } else {
                mCallbackQueue.add(cb)
            }
        }
    }

    private fun withRecordingsService(action: (RecordingManagerService) -> Unit) {
        Log.d(TAG, "with rec svc")
        mServiceConnection.doAction(action)
    }

    private fun doUnbind() {
        unbindService(mServiceConnection)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.save_tracks_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return true
        return when (item.itemId) {
            R.id.save_tracks_tmp_save_last -> {
                Log.d(TAG, "start save")

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
