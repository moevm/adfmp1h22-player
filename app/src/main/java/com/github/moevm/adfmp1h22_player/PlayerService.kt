package com.github.moevm.adfmp1h22_player

import android.util.Log

import org.eclipse.jetty.client.HttpClient
import java.lang.Exception
import java.nio.ByteBuffer

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

        // TODO #4
        // 1. Set up a MediaCodec
        // 2. Set up an AudioTrack
        // 3. Feed frames through the thing

        lateinit var hc: HttpClient

        var metaint: Int? = null
        var content_type: String? = null
        var decoder: DecoderFSM? = null

        fun reset() {
            metaint = null
            content_type = null
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
                                "content-type" -> {
                                    content_type = v
                                    Log.d("APPDEBUG", "content-type $v")
                                }
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
                            var fsm: DecoderFSM = when (content_type) {
                                "audio/mpeg" -> Mp3HeaderDecoderFSM(
                                    object : Mp3HeaderDecoderFSM.Callback {
                                        override fun onFormat(
                                            br_kbps: Int, freq_hz: Int,
                                            mode: Mp3HeaderDecoderFSM.Mode,
                                        ) {
                                            Log.d("APPDEBUG", "mp3 fmt $br_kbps $freq_hz $mode")
                                        }

                                        override fun onPayload(c: ByteBuffer) {
                                            val n = c.remaining()
                                            Log.d("APPDEBUG", "mp3 payload $n")
                                        }

                                        override fun onFrameDone() {
                                            Log.d("APPDEBUG", "mp3 frame done")
                                        }
                                    }
                                )
                                else -> {
                                    Log.d("APPDEBUG", "aborting")
                                    r.abort(Exception("Unsupported format $content_type"))
                                    return@onResponseHeaders
                                }
                            }

                            metaint?.let {
                                val fsm1 = fsm
                                fsm = IcyMetaDataDecoderFSM(
                                    it, object : IcyMetaDataDecoderFSM.Callback {
                                        override fun onPayload(c: ByteBuffer) {
                                            Log.d("APPDEBUG", "icy -> mp3")
                                            fsm1.step(c)
                                        }

                                        override fun onMetaData(s: String) {
                                            Log.d("APPDEBUG", "metadata: $s")
                                        }
                                    }
                                )
                            }

                            decoder = fsm
                        }
                        .onResponseContent { _, c ->
                            decoder?.step(c)
                        }
                        .send { r ->
                            Log.d("APPDEBUG", "done")
                            if (r.isFailed()) {
                                try {
                                Log.d("APPDEBUG", "failed")
                                r.getRequestFailure()?.let {
                                    Log.d("APPDEBUG", "req  fail: ${it.toString()}")
                                }
                                r.getResponseFailure()?.let {
                                    Log.d("APPDEBUG", "resp fail: ${it.toString()}")
                                }
                                } catch (e: Exception) {
                                    Log.d("APPDEBUG", "exception in result")
                                }
                            }
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
