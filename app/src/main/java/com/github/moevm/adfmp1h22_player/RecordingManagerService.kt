package com.github.moevm.adfmp1h22_player

import android.provider.DocumentsContract
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import android.os.ParcelFileDescriptor
import java.io.IOException
import java.nio.file.StandardOpenOption
import java.io.FileOutputStream
import android.net.Uri

import android.util.Log

import java.util.HashSet
import android.content.Context
import android.content.SharedPreferences

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
        val KEY_MIME_TYPE = "mime_type"
        val KEY_RECORDING = "recording"
        val KEY_DIRECTORY_URI = "directiry_uri"

        val PATH_PREFIX = "rec/"

        val TRANSFER_BUFFER_SIZE = 1*1024*1024 // 1 MiB
    }

    lateinit var mThread: ManagerThread
    lateinit var mHandler: Handler

    lateinit var mStorageDir: String
    val mRecordingsList =
        MutableLiveData<MutableList<Recording>>(ArrayList<Recording>())

    class ManagerThread(
        private val mDbHelper: SQLHelper,
        private val mStorageDir: String,
        private val mCb: Callback,
    ) : HandlerThread("RecordingManagerService.ManagerThread") {

        interface Callback {
            fun onRecordings(rs: MutableList<Recording>)
            fun onNewRecording(r: Recording)
            fun onRecordingFinished(r: Recording)
            fun getKeepTracksCount(): Int
            fun createFile(
                treeuri: Uri,
                mime: String,
                dispname: String,
            ): ParcelFileDescriptor?
        }

        lateinit var mHandler: Handler

        fun recordingPath(s: String): Path {
            return Paths.get(mStorageDir, PATH_PREFIX, s);
        }

        fun recordingsDir(): Path {
            return Paths.get(mStorageDir, PATH_PREFIX)
        }

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
                mime = cur.getString(5),
                state = cur.getInt(6),
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
                cv.put(COLUMN_MIME_TYPE, r.mime)
                cv.put(COLUMN_STATE, r.state)
                return cv
            }
        }

        private fun handleCmdFetchRecordings() {
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
                            COLUMN_MIME_TYPE,
                            COLUMN_STATE,
                        ),
                        "$COLUMN_STATE != ?",
                        arrayOf<String>(Recording.STATE_RECORDING.toString()),
                        null,
                        null,
                        "$COLUMN_TIMESTAMP DESC"
                    )
                    cur.moveToFirst()
                    while (! cur.isAfterLast()) {
                        val r = decodeRecording(cur)
                        l.add(r)
                        cur.moveToNext()
                    }
                }
            }

            mCb.onRecordings(l)
        }

        private fun handleCmdCleanUp(removeUnknown: Boolean) {
            Log.i(TAG, "CMD_CLEAN_UP removeUnknown=$removeUnknown")

            val maxkeep = mCb.getKeepTracksCount()

            mDbHelper.writableDatabase.let { db ->
                val to_del = HashSet<String>()
                val to_keep = HashSet<String>()

                SQLiteContract.RecordingsTable.run {
                    val cur = db.query(
                        TABLE_NAME,
                        arrayOf(
                            COLUMN_UUID,
                            COLUMN_TIMESTAMP,
                            COLUMN_STATE,
                        ),
                        null,
                        arrayOf<String>(),
                        null,
                        null,
                        "$COLUMN_TIMESTAMP DESC"
                    )
                    cur.moveToFirst()
                    while (! cur.isAfterLast()) {
                        val uuid = cur.getString(0)
                        val state = cur.getInt(2)

                        when {
                            state == Recording.STATE_RECORDING -> {
                                if (removeUnknown) {
                                    to_del.add(uuid)
                                }
                            }
                            !Files.exists(recordingPath(uuid)) -> to_del.add(uuid)
                            to_keep.size < maxkeep -> to_keep.add(uuid)
                            else -> to_del.add(uuid)
                        }

                        cur.moveToNext()
                    }

                    for (uuid in to_del) {
                        Log.d(TAG, "delete row uuid=$uuid")
                        db.delete(TABLE_NAME, "$COLUMN_UUID = ?", arrayOf(uuid))
                    }

                    for (path in Files.list(recordingsDir())) {
                        val basename = path.getFileName().toString()
                        val rem = if (removeUnknown) {
                            !to_keep.contains(basename)
                        } else {
                            to_del.contains(basename)
                        }
                        if (rem) {
                            Log.d(TAG, "delete file $basename")
                            Files.delete(path)
                        }
                    }
                }
            }

            Log.d(TAG, "Clean up done")

            handleCmdFetchRecordings()
        }

        private fun handleCmdRequestNewRecording(cb: (Recording) -> Unit,
                                                 md: TrackMetaData,
                                                 mime: String) {
            Log.i(TAG, "CMD_REQUEST_NEW_RECORDING")

            val r = Recording(UUID.randomUUID(), md, Instant.now(), mime,
                              Recording.STATE_RECORDING)

            mDbHelper.writableDatabase.let { db ->
                db.insert(SQLiteContract.RecordingsTable.TABLE_NAME,
                          null, encodeRecording(r))
            }

            cb(r)
        }

        private fun updateRecordingState(r: Recording, s: Int) {

            r.state = s

            mDbHelper.writableDatabase.let { db ->
                SQLiteContract.RecordingsTable.run {
                    val cv = ContentValues()
                    cv.put(COLUMN_STATE, s)
                    db.update(TABLE_NAME, cv,
                              "$COLUMN_UUID = ?",
                              arrayOf(r.uuid.toString()))
                }
            }
        }

        private fun handleCmdFinishRecording(r: Recording) {
            Log.i(TAG, "CMD_FINISH_RECORDING")

            updateRecordingState(r, Recording.STATE_DONE)

            mCb.onRecordingFinished(r)

            handleCmdCleanUp(false)
        }

        private fun handleCmdSaveRecording(uri: Uri, r: Recording,
                                           cb: (Boolean) -> Unit) {
            Log.i(TAG,
                  "CMD_SAVE_RECORDING to=$uri r=${r.uuid}/${r.metadata.original}")

            val outpfd = mCb.createFile(uri, r.mime, r.metadata.original)
            val outfd = outpfd?.getFileDescriptor()

            if (outfd == null) {
                // TODO: report error
                cb(false)
                return
            }

            val outch = FileOutputStream(outfd).getChannel()

            val uuid = r.uuid.toString()
            val inch = FileChannel.open(recordingPath(uuid),
                                        StandardOpenOption.READ)

            val buf = ByteBuffer.allocate(TRANSFER_BUFFER_SIZE)

            try {
                while (true) {
                    buf.clear()
                    val n = inch.read(buf)
                    Log.d(TAG, "read $n")

                    if (n == -1) {
                        break
                    }
                    buf.flip()

                    while (buf.hasRemaining()) {
                        val m = outch.write(buf)
                        Log.d(TAG, "write $m")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "error while saving file to user dir", e)
                cb(false)
                return
            }

            updateRecordingState(r, Recording.STATE_SAVED)

            handleCmdFetchRecordings()

            cb(true)
        }

        fun handleMessage(msg: Message): Boolean {
            return when (msg.what) {
                CMD_FETCH_RECORDINGS -> {
                    handleCmdFetchRecordings()
                    true
                }
                CMD_CLEAN_UP -> {
                    handleCmdCleanUp(true)
                    true
                }
                CMD_REQUEST_NEW_RECORDING -> {
                    val cb = msg.obj as (Recording) -> Unit
                    val md = msg.getData()
                        .getParcelable<TrackMetaData>(KEY_METADATA)!!
                    val mime = msg.getData()
                        .getCharSequence(KEY_MIME_TYPE).toString()

                    handleCmdRequestNewRecording(cb, md, mime)
                    true
                }
                CMD_FINISH_RECORDING -> {
                    val r = msg.obj as Recording

                    handleCmdFinishRecording(r)
                    true
                }
                CMD_SAVE_RECORDING -> {
                    val cb = msg.obj as (Boolean) -> Unit
                    val r = msg.getData()
                        .getParcelable<Recording>(KEY_RECORDING)!!
                    val saveurl = msg.getData()
                        .getCharSequence(KEY_DIRECTORY_URI)!!.toString()

                    val uri = Uri.parse(saveurl)!!

                    handleCmdSaveRecording(uri, r, cb)
                    true
                }
                else -> false
            }
        }

        override protected fun onLooperPrepared() {
            mHandler = Handler(looper)

            mHandler.post {
                Files.createDirectories(recordingsDir())
                handleCmdFetchRecordings()
            }
        }

    }

    fun requestNewRecording(md: TrackMetaData,
                            mime: String,
                            cb: (Recording) -> Unit) {
        val m = mHandler.obtainMessage(CMD_REQUEST_NEW_RECORDING, cb)
        m.setData(Bundle().also {
                      it.putParcelable(KEY_METADATA, md)
                      it.putCharSequence(KEY_MIME_TYPE, mime)
        })
        m.sendToTarget()
    }

    fun notifyRecordingFinished(r: Recording) {
        mHandler.obtainMessage(CMD_FINISH_RECORDING, r)
            .sendToTarget()
    }

    fun saveRecording(uri: Uri, r: Recording, cb: (Boolean) -> Unit) {
        val m = mHandler.obtainMessage(CMD_SAVE_RECORDING, cb)
        m.setData(Bundle().also {
                      it.putCharSequence(KEY_DIRECTORY_URI, uri.toString())
                      it.putParcelable(KEY_RECORDING, r)
        })
        m.sendToTarget()
    }

    fun cleanUpRecordings() {
        mHandler.obtainMessage(CMD_CLEAN_UP)
            .sendToTarget()
    }

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
                    // TODO: remove this callback?
                }

                override fun onRecordingFinished(r: Recording) {
                    // TODO: remove this callback?
                }

                override fun getKeepTracksCount(): Int {
                    val pref =
                        getSharedPreferences("Table", Context.MODE_PRIVATE)!!
                    return pref.getInt("progress", 10)
                }

                override fun createFile(
                    treeuri: Uri,
                    mime: String,
                    dispname: String,
                ): ParcelFileDescriptor? {

                    Log.d(TAG, "mime:     $mime")
                    Log.d(TAG, "dispname: $dispname")

                    val cr = getContentResolver()

                    val dirdocid = DocumentsContract.getTreeDocumentId(treeuri)
                    val diruri = DocumentsContract
                        .buildDocumentUriUsingTree(treeuri, dirdocid)

                    Log.d(TAG, "dirdocid: $dirdocid")
                    Log.d(TAG, "diruri:   $diruri")

                    val docuri = DocumentsContract
                        .createDocument(cr, diruri, mime, dispname)
                    if (docuri == null) {
                        Log.d(TAG, "createDocument => null")
                        return null
                    }

                    Log.d(TAG, "docuri:   $docuri")

                    return cr.openFileDescriptor(docuri, "w")
                }

            }
        )
        mThread.start()
        mHandler = Handler(mThread.looper, mThread::handleMessage)
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
