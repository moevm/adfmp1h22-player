package com.github.moevm.adfmp1h22_player

import android.util.Log

import org.eclipse.jetty.client.HttpClient
import java.lang.Error

import android.os.Build
import android.app.NotificationManager
import android.app.NotificationChannel
import androidx.core.app.NotificationCompat
import android.app.Notification

import android.os.HandlerThread
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
    var mHandler: Handler? = null

    class PlayerThread : HandlerThread("PlayerThread") {

        // TODO #3
        // 1. Parse MP3 headers
        // 2. Skip MP3 data
        // 3. Log header info

        // TODO #4
        // 1. Set up a MediaCodec
        // 2. Set up an AudioTrack
        // 3. Feed frames through the thing

        lateinit var hc: HttpClient

        var metaint: Int? = null
        var decoder: DecoderFSM? = null

        fun reset() {
            metaint = null
            decoder = null
        }

        fun handleMessage(msg: Message): Boolean {
            return when (msg.what) {
                CMD_START_PLAYING_STATION -> {
                    reset()
                    val url = msg.obj as String
                    Log.d("APPDEBUG", "start $url")
                    hc.newRequest(url)
                        .header("icy-metadata", "1")
                        .onResponseHeader { _, f ->
                            val v = f.getValue()
                            when (f.getLowerCaseName()) {
                                "icy-metaint" ->
                                    metaint = v.toIntOrNull()
                                "content-type" ->
                                    Log.d("APPDEBUG", "content-type $v")
                                "icy-br" ->
                                    Log.d("APPDEBUG", "bitrate $v")
                                "icy-name" ->
                                    Log.d("APPDEBUG", "station name $v")
                                "server" ->
                                    Log.d("APPDEBUG", "station srv $v")
                            }
                            true
                        }
                        .onResponseHeaders { r ->
                            val mi = metaint
                            if (mi == null) {
                                r.abort(Error("Missing icy-metaint header"))
                                return@onResponseHeaders
                            }
                            decoder = IcyMetaDataDecoderFSM(
                                mi, object : IcyMetaDataDecoderFSM.Callback {
                                    override fun onMetaData(s: String) {
                                        Log.d("APPDEBUG", "metadata: $s")
                                    }
                                }
                            )
                        }
                        .onResponseContent { _, c ->
                            decoder?.step(c)
                        }
                        .send {
                            Log.d("APPDEBUG", "done")
                        }
                    true
                }
                CMD_STOP_PLAYBACK -> {
                    Log.d("APPDEBUG", "stop")
                    true
                }
                else -> false
            }
        }

        override fun run() {
            Log.d("APPDEBUG", "thread starting")
            hc = HttpClient()
            hc.start()
            Log.d("APPDEBUG", "looper starting")
            super.run()
            Log.d("APPDEBUG", "looper exited")
            hc.stop()
            Log.d("APPDEBUG", "thread stopping")
        }
    }

    fun startPlayingStation(s: Station) {
        mHandler?.let {
            it.obtainMessage(
                CMD_START_PLAYING_STATION,
                s.streamUrl,
            ).sendToTarget()
        }
    }

    fun stopPlayback() {
        mHandler?.let {
            it.obtainMessage(CMD_STOP_PLAYBACK)
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
            mHandler = Handler(it.looper, it::handleMessage)
        }

        val notif = makeNotification()
        startForeground(NOTIFY_ID, notif)
    }

    override fun onDestroy() {
        mThread?.let {
            it.looper.quitSafely()
            it.join()
            mThread = null
        }
    }
}
