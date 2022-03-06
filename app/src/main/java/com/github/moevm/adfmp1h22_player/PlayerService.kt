package com.github.moevm.adfmp1h22_player

import android.util.Log

import android.os.Build
import android.app.NotificationManager
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat
import android.app.Notification

import java.lang.Thread
import android.os.Handler
import android.os.Looper
import android.os.Message

import android.os.Binder
import android.os.IBinder
import android.content.Intent
import android.app.Service

// TODO
// - HTTP
//   - Make requests
//   - ContentType
// - Buffers
//   - Skip and save metadata
//   - Parse MP3 frames
// - Audio
//   - MediaCodec
//   - AudioTrack (basically copy the docs)

// NOTE
// HTTP: Use Jetty.
// https://mvnrepository.com/artifact/org.eclipse.jetty/jetty-client
// probably the gradle dependency string should be
// “org.eclipse.jetty:jetty-client:11.0.8”

private const val CMD_START_PLAYING_STATION = 0
private const val CMD_STOP_PLAYBACK = 1
// val CMD_PAUSE_PLAYBACK = 2
// val CMD_RESUME_PLAYBACK = 3

class PlayerService : Service() {

    val NOTIF_CHANNEL_ID = "main"
    val NOTIFY_ID = 1

    var mThread: PlayerThread? = null

    class PlayerThread : Thread() {
        lateinit var mHandler: Handler
        lateinit var mLooper: Looper

        // TODO #1
        // 1. Set up Jetty
        // 2. Set up a connection
        // 3. Log bitrate, other headers

        // TODO #2
        // 1. Receive segments
        // 2. Parse metadata
        // 3. Log metadata

        // TODO #3
        // 1. Parse MP3 headers
        // 2. Skip MP3 data
        // 3. Log header info

        // TODO #4
        // 1. Set up a MediaCodec
        // 2. Set up an AudioTrack
        // 3. Feed frames through the thing

        override fun run() {
            Looper.prepare()

            mLooper = Looper.myLooper()!!
            mHandler = Handler(mLooper) { msg: Message ->
                when (msg.what) {
                    CMD_START_PLAYING_STATION -> {
                        Log.d("APPDEBUG", "start ${msg.obj as String}")
                        true
                    }
                    CMD_STOP_PLAYBACK -> {
                        Log.d("APPDEBUG", "stop")
                        true
                    }
                    else -> false
                }
            }

            Looper.loop()
        }
    }

    fun startPlayingStation(s: Station) {
        mThread?.let {
            it.mHandler.obtainMessage(
                CMD_START_PLAYING_STATION,
                s.streamUrl,
            ).sendToTarget()
        }
    }

    fun stopPlayback() {
        mThread?.let {
            it.mHandler.obtainMessage(CMD_STOP_PLAYBACK)
                .sendToTarget()
        }
        stopSelf()
    }

    inner class PlayerServiceBinder : Binder() {
        val service: PlayerService
            get () = this@PlayerService
    }

    override fun onStartCommand(intent: Intent, flags: Int, id: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = PlayerServiceBinder()

    private fun makeNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Default",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(chan)
            NotificationCompat.Builder(this, chan.id)
        } else {
            NotificationCompat.Builder(this)
        }

        val notif = builder
            .setSmallIcon(R.drawable.ic_note)
            .setContentTitle("Radio Player")
            .setContentText("Service is running")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        return notif
    }

    override fun onCreate() {
        mThread = PlayerThread().also {
            it.start()
        }

        val notif = makeNotification()
        startForeground(NOTIFY_ID, notif)
    }

    override fun onDestroy() {
        mThread?.let {
            it.mLooper.quitSafely()
            it.join()
            mThread = null
        }
    }
}
