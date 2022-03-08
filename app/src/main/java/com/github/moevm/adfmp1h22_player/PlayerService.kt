package com.github.moevm.adfmp1h22_player

import android.util.Log

import java.util.LinkedList
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

    class PlayerThread(
        private val userAgent: String
    ) : HandlerThread("PlayerThread") {

        // TODO #4
        // 1. Set up a MediaCodec
        // 2. Set up an AudioTrack
        // 3. Feed frames through the thing

        // Seeing awful performance. My guess is that we’re using the bqueue too much

        lateinit var hc: HttpClient

        var handler: Handler? = null
        var sid: Int? = null

        var metaint: Int? = null
        var content_type: String? = null
        var decoder: DecoderFSM? = null

        val bqueue = LinkedList<ByteBuffer>()

        var decoder_codec: MediaCodec? = null
        var codec_bufidx = LinkedList<Int>()
        var codec_buf: ByteBuffer? = null
        var sample_rate: Int = -1
        var timestamp: Long = 0.toLong()

        var n_decoding: Int = 0

        var player: AudioTrack? = null

        fun reset() {
            metaint = null
            content_type = null
            decoder = null
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
                                //Log.d("APPDEBUG", "MediaCodec: onError ${e.toString()}")
                            }

                            override fun onInputBufferAvailable(
                                mc: MediaCodec,
                                index: Int,
                            ) {
                                codec_bufidx.addLast(index)
                                Log.d("APPDEBUG", "input buffer arrived       <>${codec_bufidx.size}/$n_decoding")

                                if (bqueue.size > 0) {
                                    val b = mc.getInputBuffer(index)
                                    codec_buf = b
                                    if (b != null) {
                                        while (bqueue.size > 0) {
                                            b.put(bqueue.removeFirst())
                                        }
                                    } else {
                                        //Log.d("APPDEBUG", "no promised buffer")
                                    }
                                }
                            }

                            override fun onOutputBufferAvailable(
                                mc: MediaCodec,
                                index: Int,
                                info: MediaCodec.BufferInfo,
                            ) {
                                n_decoding--
                                Log.d("APPDEBUG", "decoded buffer available   <>${codec_bufidx.size}/$n_decoding")
                                player?.let { at ->
                                    val buf = mc.getOutputBuffer(index)
                                    if (buf == null) {
                                        return@let
                                    }
                                    if (at.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                                        at.play()
                                    }
                                    //Log.d("APPDEBUG", "playing frame ${buf.remaining()}")
                                    at.write(buf, buf.remaining(), AudioTrack.WRITE_BLOCKING)
                                }
                                mc.releaseOutputBuffer(index, false)
                            }

                            override fun onOutputFormatChanged(
                                mc: MediaCodec,
                                fmt: MediaFormat,
                            ) {
                                //Log.d("APPDEBUG", "MediaCodec: onOutputFormatChanged ${fmt.toString()}")
                                val chcfg = when (fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)) {
                                    1 -> AudioFormat.CHANNEL_OUT_MONO
                                    2 -> AudioFormat.CHANNEL_OUT_STEREO
                                    else -> AudioFormat.CHANNEL_OUT_STEREO // idk
                                }
                                val enc = fmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
                                val fmt2 = AudioFormat.Builder()
                                    .setSampleRate(sample_rate)
                                    .setEncoding(enc)
                                    .setChannelMask(chcfg)
                                    .build()
                                val at = AudioTrack.Builder()
                                    .setAudioAttributes(
                                        AudioAttributes.Builder()
                                            .setUsage(AudioAttributes.USAGE_MEDIA)
                                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                            .build()
                                    )
                                    .setAudioFormat(fmt2)
                                    .setBufferSizeInBytes(
                                        AudioTrack.getMinBufferSize(sample_rate, chcfg, enc)
                                    )
                                    .setSessionId(sid!!)
                                    .setTransferMode(AudioTrack.MODE_STREAM)
                                    .build()

                                val state = at.getState()
                                when (state) {
                                    AudioTrack.STATE_INITIALIZED -> {
                                        //Log.d("APPDEBUG", "player init ok")
                                    }
                                    else -> {
                                        //Log.d("APPDEBUG", "player init NOT OK but $state")
                                    }
                                }

                                //Log.d("APPDEBUG", "player stream type ${at.getStreamType()}")
                                //Log.d("APPDEBUG", "player play state ${at.getPlayState()}")
                                //Log.d("APPDEBUG", "player routed to ${at.getRoutedDevice()}")

                                player = at
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
                                    if (decoder_codec == null) {
                                        //Log.d("APPDEBUG", "setup decoder codec")
                                        setupDecoderCodec(
                                            content_type!!,
                                            freq_hz,
                                            mode.channelsCount(),
                                        )
                                    }
                                    sample_rate = freq_hz
                                    //Log.d("APPDEBUG", "mp3 fmt $br_kbps $freq_hz $mode")
                                }

                                override fun onPayload(c: ByteBuffer) {
                                    // val n = c.remaining()
                                    //Log.d("APPDEBUG", "mp3 payload $n")
                                    var buf = codec_buf
                                    if (buf == null && codec_bufidx.size > 0) {
                                        buf = decoder_codec?.getInputBuffer(
                                            codec_bufidx.getFirst())
                                        codec_buf = buf
                                    }
                                    if (buf != null) {
                                        //Log.d("APPDEBUG", "buf put ${c.remaining()}")
                                        buf.put(c)
                                    } else {
                                        // TODO: request buffer manuall instead
                                        Log.d("APPDEBUG", "bqueueing")
                                        val bc = ByteBuffer.allocate(
                                            c.remaining())
                                        bc.put(c)
                                        bc.rewind()
                                        bqueue.addLast(bc)
                                        while (bqueue.size > 16) {
                                            bqueue.removeFirst()
                                        }
                                    }
                                }

                                override fun onFrameDone() {
                                    //Log.d("APPDEBUG", "mp3 frame done")
                                    bqueue.clear()
                                    val mc = decoder_codec
                                    val cb = codec_buf
                                    if (mc == null || codec_bufidx.size == 0 || cb == null) {
                                        //Log.d("APPDEBUG", "no frame to finish $mc ${codec_bufidx.size} $cb")
                                        return
                                    }
                                    mc.queueInputBuffer(codec_bufidx.removeFirst(),
                                                        0, cb.position(),
                                                        timestamp, 0)
                                    ++n_decoding
                                    Log.d("APPDEBUG", "queued input buffer        <>${codec_bufidx.size}/$n_decoding")
                                    codec_buf = null
                                    timestamp += 1152000000 / sample_rate / 10 //TMP / 10
                                }
                            }
                        )
                        else -> {
                            //Log.d("APPDEBUG", "aborting")
                            r.abort(Exception("Unsupported format $content_type"))
                            return@onResponseHeaders
                        }
                    }

                    metaint?.let {
                        val fsm1 = fsm
                        fsm = IcyMetaDataDecoderFSM(
                            it, object : IcyMetaDataDecoderFSM.Callback {
                                override fun onPayload(c: ByteBuffer) {
                                    //Log.d("APPDEBUG", "icy -> mp3")
                                    fsm1.step(c)
                                }

                                override fun onMetaData(s: String) {
                                    //Log.d("APPDEBUG", "metadata: $s")
                                }
                            }
                        )
                    }

                    decoder = fsm
                }
                .onResponseContent { _, c ->
                    // TODO: try event loop mode?

                    // val bc = ByteBuffer.allocate(
                    //     c.remaining())
                    // bc.put(c)
                    // bc.rewind()
                    // handler!!.post {
                        //Log.d("APPDEBUG", "posted stuff runs")
                        // decoder?.step(bc)
                    // }
                    decoder?.step(c)
                }
                .send { r ->
                    //Log.d("APPDEBUG", "done")
                    if (r.isFailed()) {
                        try {
                            Log.d("APPDEBUG", "failed")
                            r.getRequestFailure()?.let {
                                Log.d("APPDEBUG", "req  fail: ${it.toString()}", it)
                            }
                            r.getResponseFailure()?.let {
                                Log.d("APPDEBUG", "resp fail: ${it.toString()}", it)
                            }
                        } catch (e: Exception) {
                            Log.d("APPDEBUG", "exception in result")
                        }
                    }
                }
        }

        fun handleMessage(msg: Message): Boolean {
            return when (msg.what) {
                CMD_START_PLAYING_STATION -> {
                    val url = msg.obj as String
                    //Log.d("APPDEBUG", "start $url")
                    setupRequest(url)
                    true
                }
                CMD_STOP_PLAYBACK -> {
                    //Log.d("APPDEBUG", "stop")
                    true
                }
                else -> false
            }
        }

        override fun run() {
            //Log.d("APPDEBUG", "thread starting")
            hc = HttpClient()
            hc.start()
            //Log.d("APPDEBUG", "looper starting")
            super.run()
            //Log.d("APPDEBUG", "looper exited")
            hc.stop()
            //Log.d("APPDEBUG", "thread stopping")
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
