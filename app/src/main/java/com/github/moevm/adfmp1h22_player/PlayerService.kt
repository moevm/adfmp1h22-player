package com.github.moevm.adfmp1h22_player

import android.util.Log
import java.lang.Thread

import java.util.concurrent.ConcurrentLinkedQueue
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.AudioTrack
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager

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


class PlayerService : Service() {

    companion object {
        val CMD_START_PLAYING_STATION = 0
        val CMD_STOP_PLAYBACK = 1
        // val CMD_PAUSE_PLAYBACK = 2
        // val CMD_RESUME_PLAYBACK = 3
        val CMD_DEBUG_INFO = 10

        val TAG = "PlayerService"
        val NOTIF_CHANNEL_ID = "main"
        val NOTIF_ID = 1

        val MP3_SAMPLES_PER_FRAME = 1152
    }

    var mThread: PlayerThread? = null
    var mHandler: Handler? = null

    class PlayerThread(
        private val userAgent: String
    ) : HandlerThread("PlayerThread") {

        lateinit var hc: HttpClient

        var handler: Handler? = null
        var sid: Int? = null

        var metaint: Int? = null
        var content_type: String? = null
        var decoder: DecoderFSM? = null

        val bqueue = ConcurrentLinkedQueue<ByteBuffer>()
        val freelist = ConcurrentLinkedQueue<ByteBuffer>()
        var current_buffer: ByteBuffer? = null

        var decoder_codec: MediaCodec? = null
        var sample_rate: Int = -1
        var timestamp: Long = 0.toLong()
        var player: AudioTrack? = null

        fun reset() {
            metaint = null
            content_type = null
            decoder = null
        }

        private fun setupPlayer(fmt: MediaFormat) {
            val chcfg = when (fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)) {
                1 -> AudioFormat.CHANNEL_OUT_MONO
                2 -> AudioFormat.CHANNEL_OUT_STEREO
                else -> AudioFormat.CHANNEL_OUT_STEREO // idk
            }
            val enc = fmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
            val at = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sample_rate)
                        .setEncoding(enc)
                        .setChannelMask(chcfg)
                        .build()
                )
                .setBufferSizeInBytes(
                    AudioTrack.getMinBufferSize(sample_rate, chcfg, enc)
                )
                .setSessionId(sid!!)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            val state = at.getState()
            when (state) {
                AudioTrack.STATE_INITIALIZED -> {
                    Log.i(TAG, "player init ok")
                }
                else -> {
                    Log.w(TAG, "player init NOT OK but $state")
                }
            }

            player?.release()
            player = at
        }

        private fun setupDecoderCodec(
            ctype: String,
            freq: Int,
            channels: Int,
        ): Boolean {
            return when (ctype) {
                "audio/mpeg" -> {
                    val mc = MediaCodec.createDecoderByType("audio/mpeg")
                    val fmt = MediaFormat.createAudioFormat(
                        "audio/mpeg",
                        freq, channels,
                    )
                    mc.setCallback(
                        object : MediaCodec.Callback() {
                            override fun onError(
                                mc: MediaCodec,
                                e: MediaCodec.CodecException,
                            ) {
                                Log.e(TAG, "MediaCodec: onError ${e.toString()}", e)
                            }

                            override fun onInputBufferAvailable(
                                mc: MediaCodec,
                                index: Int,
                            ) {

                                val buf = mc.getInputBuffer(index)
                                if (buf == null) {
                                    Log.w(TAG, "onIBA: no promised input buffer $index")
                                    return
                                }

                                buf.clear()

                                val inb = bqueue.poll()
                                if (inb != null) {
                                    if (inb.remaining() <= buf.remaining()) {
                                        buf.put(inb)
                                        inb.clear()
                                        freelist.add(inb)
                                    } else {
                                        Log.w(TAG, "onIBA: MediaCodec buffer too small")
                                    }
                                }

                                val n = buf.position()
                                buf.rewind()

                                mc.queueInputBuffer(index, 0, n, timestamp, 0)

                                timestamp += MP3_SAMPLES_PER_FRAME * 1000000 / sample_rate

                            }

                            override fun onOutputBufferAvailable(
                                mc: MediaCodec,
                                index: Int,
                                info: MediaCodec.BufferInfo,
                            ) {

                                val buf = mc.getOutputBuffer(index)
                                if (buf == null) {
                                    Log.w(TAG, "onOBA: no promised output buffer $index")
                                    return
                                }

                                player?.let { at ->
                                    if (at.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                                        Log.i(TAG, "starting playback")
                                        at.play()
                                    }
                                    at.write(buf, buf.remaining(), AudioTrack.WRITE_BLOCKING)
                                }

                                mc.releaseOutputBuffer(index, false)
                            }

                            override fun onOutputFormatChanged(
                                mc: MediaCodec,
                                fmt: MediaFormat,
                            ) {
                                Log.i(TAG, "onOFC: $fmt")
                                setupPlayer(fmt)
                            }
                        },
                        handler!!
                    )
                    mc.configure(fmt, null, null, 0)
                    mc.start()
                    decoder_codec = mc
                    true
                }
                else -> false
            }
        }

        private fun setupRequest(url: String) {
            reset()
            hc.newRequest(url)
                .header("icy-metadata", "1")
                .agent(userAgent)
                .onResponseHeader { _, f ->
                    val v = f.getValue()
                    when (f.getLowerCaseName()) {
                        "icy-metaint" ->
                            metaint = v.toIntOrNull()
                        "content-type" -> {
                            content_type = v
                            Log.d(TAG, "content-type $v")
                        }
                        "icy-br" ->
                            Log.d(TAG, "bitrate $v")
                        "icy-name" ->
                            Log.d(TAG, "station name $v")
                        "server" ->
                            Log.d(TAG, "station srv $v")
                    }
                    true
                }
                .onResponseHeaders { r ->
                    var fsm: DecoderFSM = when (content_type) {
                        "audio/mpeg" -> Mp3HeaderDecoderFSM(
                            object : Mp3HeaderDecoderFSM.Callback {
                                override fun onFormat(
                                    frame_len: Int, freq_hz: Int,
                                    mode: Mp3HeaderDecoderFSM.Mode,
                                ) {
                                    if (decoder_codec == null) {
                                        Log.i(TAG, "setup decoder codec")
                                        setupDecoderCodec(
                                            content_type!!,
                                            freq_hz,
                                            mode.channelsCount(),
                                        )
                                    }
                                    sample_rate = freq_hz

                                    var buf: ByteBuffer? = null
                                    while (true) {
                                        val b = freelist.poll()
                                        if (b == null || b.capacity() >= frame_len) {
                                            buf = b
                                            break;
                                        }
                                    }
                                    if (buf == null) {
                                        buf = ByteBuffer.allocate(frame_len * 3 / 2)
                                    }

                                    if (buf == null) {
                                        Log.w(TAG, "no buffer allocated")
                                    }
                                    if (buf?.position() != 0) {
                                        Log.w(TAG, "dirty buffer")
                                    }

                                    current_buffer = buf
                                }

                                override fun onPayload(c: ByteBuffer) {
                                    current_buffer?.let {
                                        if (c.remaining() < it.remaining()) {
                                            it.put(c)
                                        }
                                    }
                                }

                                override fun onFrameDone() {
                                    current_buffer?.let {
                                        val n = it.position()
                                        if (n > 0) {

                                            it.limit(it.position())
                                            it.rewind()

                                            bqueue.add(it)
                                            current_buffer = null
                                        } else {
                                            freelist.add(it)
                                        }
                                    }
                                }
                            }
                        )
                        else -> {
                            Log.e(TAG, "aborting, content-type: $content_type")

                            // TODO: I don’t think the abort will work

                            r.abort(Exception("Unsupported format $content_type"))
                            return@onResponseHeaders
                        }
                    }

                    metaint?.let {
                        val fsm1 = fsm
                        fsm = IcyMetaDataDecoderFSM(
                            it, object : IcyMetaDataDecoderFSM.Callback {
                                override fun onPayload(c: ByteBuffer) {
                                    fsm1.step(c)
                                }

                                override fun onMetaData(s: String) {
                                }
                            }
                        )
                    }

                    decoder = fsm
                }
                .onResponseContent { _, c ->
                    decoder!!.step(c)
                }
                .send { r ->
                    handler!!.post {
                        player?.release()
                        decoder_codec?.release()
                        if (r.isFailed()) {
                            try {
                                Log.w(TAG, "http failed")
                                r.getRequestFailure()?.let {
                                    Log.d(TAG, "req  fail: ${it.toString()}", it)
                                }
                                r.getResponseFailure()?.let {
                                    Log.d(TAG, "resp fail: ${it.toString()}", it)
                                }
                            } catch (e: Exception) {
                                Log.d(TAG, "exception while handling request failure", e)
                            }
                        }
                    }
                }
        }

        fun handleMessage(msg: Message): Boolean {
            return when (msg.what) {
                CMD_START_PLAYING_STATION -> {
                    val url = msg.obj as String
                    setupRequest(url)
                    true
                }
                CMD_STOP_PLAYBACK -> {

                    // TODO: stop event loop from within itself. Try to
                    // join thread in onDestroy and force quit on
                    // failure.

                    // TODO: try to gracefully shut down the request.
                    // This should be possible if we create the
                    // connection ourselves.

                    true
                }
                CMD_DEBUG_INFO -> {
                    Log.d(TAG, "sizes: bqueue:${bqueue.size} freelist:${freelist.size}")
                    true
                }
                else -> false
            }
        }

        override fun run() {
            hc = HttpClient()
            hc.start()
            // TODO: handle exceptions
            super.run()
            hc.stop()
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

    fun logDebugInfo() {
        mHandler?.let {
            it.obtainMessage(CMD_DEBUG_INFO)
                .sendToTarget()
        }
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
        val am = getSystemService(AudioManager::class.java)
        val sid = am.generateAudioSessionId()

        mThread = PlayerThread(resources.getString(R.string.user_agent))
            .also { thread ->
                 thread.start()
                 val h = Handler(thread.looper, thread::handleMessage)
                 mHandler = h
                 thread.handler = h // NOTE: there’s probably a race here
                 thread.sid = sid
            }

        val notif = makeNotification()
        startForeground(NOTIF_ID, notif)
    }

    override fun onDestroy() {
        mThread?.let {
            it.looper.quitSafely()
            it.join()
            mThread = null
        }
    }
}
