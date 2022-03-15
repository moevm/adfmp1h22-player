package com.github.moevm.adfmp1h22_player

import java.util.LinkedList

import android.content.Context
import android.widget.Toast

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

        val MAX_CACHE_SECONDS = 5
    }

    var mThread: PlayerThread? = null
    var mHandler: Handler? = null

    class PlayerThread(
        private val context: Context,
        private val userAgent: String,
        private val sid: Int,
    ) : HandlerThread("PlayerThread") {

        private class Frame(
            public val buf: ByteBuffer,
            public var meta: String?,
        ) {
            fun clear() {
                buf.clear()
                meta = null
            }
        }

        private class MetaDataRecord(
            public val timestamp: Long,
            public val meta: String,
        )

        private lateinit var hc: HttpClient
        private lateinit var handler: Handler

        private var metaint: Int? = null
        private var content_type: String? = null
        private var decoder: DecoderFSM? = null

        // Used concurrently from HTTP and Handler threads
        private val bqueue = ConcurrentLinkedQueue<Frame>()
        private val freelist = ConcurrentLinkedQueue<Frame>()

        // HTTP threads only
        private var current_frame: Frame? = null
        private var current_meta: String? = null

        private var decoder_codec: MediaCodec? = null
        private var player: AudioTrack? = null

        private var timestamp: Long = 0.toLong()
        private val metaqueue = LinkedList<MetaDataRecord>()
        private var sample_rate: Int = -1
        private var max_frames: Int = 0

        private var stat_allocated_buffers: Int = 0
        private var stat_dropped_buffers: Int = 0

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
                .setSessionId(sid)
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

                                val inf = bqueue.poll()
                                if (inf != null) {
                                    if (inf.buf.remaining() <= buf.remaining()) {
                                        buf.put(inf.buf)
                                        inf.meta?.let {
                                            metaqueue.add(MetaDataRecord(timestamp, it))
                                        }
                                        freelist.add(inf)
                                    } else {
                                        Log.w(TAG, "onIBA: MediaCodec buffer too small")
                                    }
                                } else {
                                    if (!freelist.isEmpty()) {
                                        Log.w(TAG, "drained bqueue")
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

                                if (info.size == 0) {
                                    return
                                }

                                while (!metaqueue.isEmpty()
                                       && info.presentationTimeUs >= metaqueue.get(0).timestamp) {
                                    val m = metaqueue.remove()
                                    Toast.makeText(
                                        context, "RadioPlayer: ${m.meta}",
                                        Toast.LENGTH_LONG,
                                    ).show()
                                }

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
                        handler
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
                                    max_frames =
                                        freq_hz * MAX_CACHE_SECONDS / MP3_SAMPLES_PER_FRAME

                                    var frm: Frame?
                                    while (true) {
                                        val f = freelist.poll()
                                        if (f == null || f.buf.capacity() >= frame_len) {
                                            f?.clear()
                                            frm = f
                                            break;
                                        } else {
                                            Log.w(TAG, "buffer {f.buf.capacity()} too small for ${frame_len}b frame")
                                        }
                                    }
                                    if (frm == null) {
                                        // +1 to be able to reuse buffer
                                        // in case padding=0 here
                                        val buf = ByteBuffer.allocate(frame_len + 1)
                                        frm = Frame(buf, null)
                                        stat_allocated_buffers++
                                    }

                                    current_frame = frm
                                }

                                override fun onPayload(c: ByteBuffer) {
                                    current_frame?.let {
                                        if (c.remaining() <= it.buf.remaining()) {
                                            it.buf.put(c)
                                        } else {
                                            Log.w(TAG, "lost ${c.remaining()}b of payload")
                                        }
                                    }
                                }

                                override fun onFrameDone() {
                                    current_frame?.also { frm ->
                                        val n = frm.buf.position()
                                        if (n > 0) {
                                            current_meta?.let {
                                                frm.meta = it
                                                current_meta = null
                                            }

                                            frm.buf.limit(frm.buf.position())
                                            frm.buf.rewind()

                                            bqueue.add(frm)
                                            current_frame = null

                                            val bqsz = bqueue.size
                                            var ndrop = 0
                                            while (bqsz - ndrop > max_frames) {
                                                ndrop++
                                                bqueue.poll()?.let { f ->
                                                    f.meta?.let {
                                                        current_meta = it
                                                    }
                                                    freelist.add(f)
                                                }
                                                stat_dropped_buffers++
                                            }
                                            if (ndrop > 0) {
                                                Log.w(TAG, "dropped $ndrop frames")
                                            }
                                        } else {
                                            Log.w(TAG, "empty frame")
                                            frm.meta?.let {
                                                current_meta = it
                                            }
                                            freelist.add(frm)
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
                                    // TODO: a better parser?
                                    val s1 = s.removeSurrounding("StreamTitle='", "';")
                                    if (s1.isEmpty() || s1.length != s.length - 15) {
                                        Log.d(TAG, "empty metadata")
                                        return
                                    }
                                    val s2 = s1
                                        .trimStart(Char::isWhitespace)
                                        .trimEnd(Char::isWhitespace)
                                    if (s2.isEmpty()) {
                                        Log.i(TAG, "whitespace-only metadata")
                                        return
                                    }
                                    Log.d(TAG, "new metadata: ${s2}")
                                    current_meta = s2
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
                    handler.post {
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
                    Log.d(TAG, "allocated buffers: ${stat_allocated_buffers}")
                    Log.d(TAG, "dropped buffers  : ${stat_dropped_buffers}")
                    true
                }
                else -> false
            }
        }

        override fun run() {
            hc = HttpClient()
            hc.start()
            try {
                super.run()
            } catch (e: Exception) {
                Log.e(TAG, "Uncaught exception in PlayerThread")
                Toast.makeText(
                    context, "RadioPlayer: Uncaught exception in player service",
                    Toast.LENGTH_LONG,
                ).show()
            }
            hc.stop()
        }

        override protected fun onLooperPrepared() {
            handler = Handler(looper)
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
                NotificationManager.IMPORTANCE_LOW
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

        mThread = PlayerThread(this, resources.getString(R.string.user_agent), sid)
            .also { thread ->
                 thread.start()
                 mHandler = Handler(thread.looper, thread::handleMessage)
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
