package com.github.moevm.adfmp1h22_player

import android.util.Log

import android.os.Bundle

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

import java.util.UUID
import java.time.Instant

import androidx.lifecycle.MutableLiveData

import android.os.HandlerThread
import android.os.Handler
import android.os.Looper
import android.os.Message

import android.os.Binder
import android.os.IBinder
import android.content.Intent
import android.app.Service

import android.content.ContentValues
import android.database.Cursor
import com.github.moevm.adfmp1h22_player.SQLite.SQLHelper
import com.github.moevm.adfmp1h22_player.SQLite.SQLiteContract

class RecordingManagerService : Service() {

    companion object {
        val CMD_CLEAN_UP = 0
        val CMD_REQUEST_NEW_RECORDING = 1
        val CMD_FINISH_RECORDING = 2
        val CMD_SAVE_RECORDING = 3 // move or copy into user’s shared storage
        val CMD_FETCH_RECORDINGS = 4 // internal

        // NOTE: listing is always available in mRecordingsList, but it
        // will take some time to fill after service is started

        val TAG = "RecordingManagerService"

        val KEY_METADATA = "metadata"

        val PATH_PREFIX = "rec/"
    }

    lateinit var mThread: ManagerThread
    lateinit var mHandler: Handler

    lateinit var mStorageDir: String
    val mRecordingsList = MutableLiveData<MutableList<Recording>>(ArrayList<Recording>())

    class ManagerThread(
        private val mDbHelper: SQLHelper,
        private val mStorageDir: String,
        private val mCb: Callback,
    ) : HandlerThread("RecordingManagerService.ManagerThread") {

        interface Callback {
            fun onRecordings(rs: MutableList<Recording>)
            fun onNewRecording(r: Recording)
            fun onRecordingFinished(r: Recording)
        }

        lateinit var mHandler: Handler

        private fun decodeRecording(cur: Cursor): Recording {
            return Recording(
                uuid = UUID.fromString(cur.getString(0)),
                metadata = TrackMetaData(
                    original = cur.getString(1),
                    title = cur.getString(3),
                    artist = (if (cur.getType(2) == Cursor.FIELD_TYPE_NULL) null
                              else cur.getString(2)),
                ),
                timestamp = Instant.ofEpochMilli(cur.getLong(4)),
                state = cur.getInt(5),
            )
        }

        private fun encodeRecording(r: Recording): ContentValues {
            SQLiteContract.RecordingsTable.run {
                val cv = ContentValues()
                cv.put(COLUMN_UUID, r.uuid.toString())
                cv.put(COLUMN_TRACK_ORIGTITLE, r.metadata.original)
                cv.put(COLUMN_TRACK_ARTIST, r.metadata.artist)
                cv.put(COLUMN_TRACK_TITLE, r.metadata.title)
                cv.put(COLUMN_TIMESTAMP, r.timestamp.toEpochMilli())
                cv.put(COLUMN_STATE, r.state)
                return cv
            }
        }

        fun handleMessage(msg: Message): Boolean {
            return when (msg.what) {
                CMD_FETCH_RECORDINGS -> {
                    Log.i(TAG, "CMD_FETCH_RECORDINGS")

                    val l = ArrayList<Recording>()

                    mDbHelper.readableDatabase.let { db ->
                        SQLiteContract.RecordingsTable.run {
                            val cur = db.query(
                                TABLE_NAME,
                                arrayOf(
                                    COLUMN_UUID,
                                    COLUMN_TRACK_ORIGTITLE,
                                    COLUMN_TRACK_ARTIST,
                                    COLUMN_TRACK_TITLE,
                                    COLUMN_TIMESTAMP,
                                    COLUMN_STATE,
                                ),
                                "$COLUMN_STATE != 0",
                                arrayOf<String>(),
                                null,
                                null,
                                "$COLUMN_TIMESTAMP DESC"
                            )
                            cur.moveToFirst()
                            while (! cur.isAfterLast()) {
                                val r = decodeRecording(cur)
                                Log.d(TAG, "FETCH: $r")
                                l.add(r)
                                cur.moveToNext()
                            }
                        }
                    }

                    mCb.onRecordings(l)
                    true
                }
                CMD_CLEAN_UP -> {
                    Log.w(TAG, "CMD_CLEAN_UP not implemented")
                    true
                }
                CMD_REQUEST_NEW_RECORDING -> {
                    Log.i(TAG, "CMD_REQUEST_NEW_RECORDING")

                    val cb = msg.obj as (Recording) -> Unit
                    val md = msg.getData()
                        .getParcelable<TrackMetaData>(KEY_METADATA)!!

                    val r = Recording(UUID.randomUUID(), md, Instant.now(),
                                      Recording.STATE_RECORDING)
                    mDbHelper.writableDatabase.let { db ->
                        db.insert(SQLiteContract.RecordingsTable.TABLE_NAME,
                                  null, encodeRecording(r))
                    }

                    Files.createDirectories(Paths.get(mStorageDir, PATH_PREFIX))
                    mHandler.postDelayed({
                        cb(r)
                        mCb.onNewRecording(r)
                    }, 10)
                    true
                }
                CMD_FINISH_RECORDING -> {
                    Log.i(TAG, "CMD_FINISH_RECORDING")
                    val r = msg.obj as Recording

                    mDbHelper.writableDatabase.let { db ->
                        SQLiteContract.RecordingsTable.run {
                            val cv = ContentValues()
                            cv.put(COLUMN_STATE, Recording.STATE_DONE)
                            db.update(TABLE_NAME, cv,
                                      "$COLUMN_UUID = ?",
                                      arrayOf(r.uuid.toString()))
                        }
                    }

                    mCb.onRecordingFinished(r)
                    true
                }
                CMD_SAVE_RECORDING -> {
                    // NOTE: should accept callback
                    Log.w(TAG, "CMD_SAVE_RECORDING not implemented")
                    true
                }
                else -> false
            }
        }

        override protected fun onLooperPrepared() {
            mHandler = Handler(looper)
        }

    }

    fun requestNewRecording(md: TrackMetaData, cb: (Recording) -> Unit) {
        val m = mHandler.obtainMessage(CMD_REQUEST_NEW_RECORDING, cb)
        m.setData(Bundle().also {
                      it.putParcelable(KEY_METADATA, md)
        })
        m.sendToTarget()
    }

    fun notifyRecordingFinished(r: Recording) {
        mHandler.obtainMessage(CMD_FINISH_RECORDING, r)
            .sendToTarget()
    }

    // TODO: saveRecording
    // TODO: cleanUpRecordings

    fun recordingPath(r: Recording): Path {
        return Paths.get(mStorageDir, PATH_PREFIX, r.uuid.toString());
    }

    inner class ServiceBinder : Binder() {
        val service: RecordingManagerService
            get () = this@RecordingManagerService
    }

    override fun onBind(intent: Intent): IBinder? = ServiceBinder()

    override fun onCreate() {
        mStorageDir = getExternalFilesDirs(null)[0].absolutePath

        mThread = ManagerThread(
            SQLHelper(this),
            mStorageDir,
            object : ManagerThread.Callback {
                override fun onRecordings(rs: MutableList<Recording>) {
                    mRecordingsList.postValue(rs)
                }

                override fun onNewRecording(r: Recording) {
                    val l = mRecordingsList.getValue()
                    if (l != null) {
                        l.add(r)
                        mRecordingsList.postValue(l)
                    } else {
                        mRecordingsList.postValue(mutableListOf(r))
                    }
                }

                override fun onRecordingFinished(r: Recording) {
                    mRecordingsList.postValue(mRecordingsList.getValue())
                }
            }
        )
        mThread.start()
        mHandler = Handler(mThread.looper, mThread::handleMessage)

        mHandler.obtainMessage(CMD_FETCH_RECORDINGS)
            .sendToTarget()
    }

    override fun onDestroy() {
        mThread.looper.quitSafely()
        mThread.join(1000)      // wait 1sec
        if (mThread.isAlive()) {
            Log.w(TAG, "failed to join thread")
            mThread.interrupt()
            mThread.join()
        }
    }

}
