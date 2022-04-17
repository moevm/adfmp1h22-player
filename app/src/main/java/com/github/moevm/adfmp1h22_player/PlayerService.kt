package com.github.moevm.adfmp1h22_player

import java.nio.channels.AsynchronousFileChannel
import java.nio.file.StandardOpenOption

import android.content.ServiceConnection

import java.net.URISyntaxException
import java.util.concurrent.TimeoutException
import java.net.UnknownHostException
import java.net.ConnectException

import java.net.URI

import java.util.LinkedList

import android.content.Context

import android.app.*
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Icon
import android.media.*
import android.os.*
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.core.view.drawToBitmap
import androidx.lifecycle.MutableLiveData
import androidx.media.app.NotificationCompat.MediaStyle
import com.bumptech.glide.Glide
import com.squareup.picasso.Picasso
import okhttp3.internal.notify
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.api.Request
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue


fun parseTrackTitle(s: String): TrackMetaData {
    val spl = s.split(" - ", limit = 2)
    return if (spl.size == 2) {
        TrackMetaData(
            s,
            title = spl[1],
            artist = spl[0],
        )
    } else {
        TrackMetaData(s, s, null)
    }
}


class PlayerService : Service() {

    companion object {
        val CMD_START_PLAYING_STATION = 0
        val CMD_STOP_PLAYBACK = 1
        val CMD_PAUSE_PLAYBACK = 2
        val CMD_RESUME_PLAYBACK = 3
        val CMD_DEBUG_INFO = 10

        val TAG = "PlayerService"
        val NOTIF_CHANNEL_ID = "main"
        val NOTIF_ID = 1

        val MP3_SAMPLES_PER_FRAME = 1152

        // Size of cache until we start dropping frames
        val MAX_CACHE_SECONDS = 5

        // Normal size of cache. When dropping, keep it this full
        val MAX_CACHE_SECONDS_SOFT = 3

        val IDLE_TIMEOUT = 10000.toLong() // ms
    }

    class TerminationMarker : Throwable()

    class UnsupportedCodecException(
        public val codecName: String,
    ) : Exception()

    class UnsupportedTransportException(
        public val transportName: String,
    ) : Exception()

    class ConnectionClosedException : Exception()

    lateinit var mMainHandler: Handler

    var mAudioSid: Int = -1
    lateinit var mThread: PlayerThread
    lateinit var mHandler: Handler
    val mMetaData = MutableLiveData<TrackMetaData?>()
    val mPlaybackState = MutableLiveData<PlaybackState>(PlaybackState.STOPPED)
    val mStation = MutableLiveData<Station>()
    val mRecMgrConn = RecMgrSvrConnection()
    var mediaStyle: MediaStyle = MediaStyle()

    inner class RecMgrSvrConnection : ServiceConnection {
        private var mServiceBinder: RecordingManagerService.ServiceBinder? = null
        private val mCallbackQueue =
            LinkedList<(RecordingManagerService) -> Unit>()

        override fun onServiceConnected(m: ComponentName, sb: IBinder) {
            Log.i(TAG, "recmgr svc connected: $m")
            mServiceBinder = sb as RecordingManagerService.ServiceBinder

            sb.service.cleanUpRecordings()

            while (!mCallbackQueue.isEmpty()) {
                val cb = mCallbackQueue.remove()
                cb(sb.service)
            }
        }

        override fun onServiceDisconnected(m: ComponentName) {
            mServiceBinder = null
            Log.w(TAG, "recmgr svc disconnected: $m")
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

    class PlayerThread(
        private val userAgent: String,
        private val sid: Int,
        private val cb: Callback,
    ) : HandlerThread("PlayerThread") {

        interface Callback {
            fun onMetaData(m: TrackMetaData)
            fun onPlaybackStateChanged(ps: PlaybackState)
            fun onStationLoading(s: Station?)

            // TODO: some kind of details enum to specify kind of error
            // -- e.g. unknown format, unsupported transport, network
            // unreachable, connection refused, programming error
            fun onError(e: Throwable)

            fun requestRecMgrSvc(cb: (RecordingManagerService) -> Unit)

            fun requestRestart()
        }

        private class Frame(
            public val buf: ByteBuffer,
            public var meta: TrackMetaData?,
        ) {
            fun clear() {
                buf.clear()
                meta = null
            }
        }

        private class MetaDataRecord(
            public val timestamp: Long,
            public val meta: TrackMetaData,
        )

        private lateinit var hc: HttpClient
        private lateinit var handler: Handler

        private var http_ks: (() -> Unit)? = null

        private var metaint: Int? = null
        private var content_type: String? = null
        private var decoder: DecoderFSM? = null

        // Used concurrently from HTTP and Handler threads
        private val bqueue = ConcurrentLinkedQueue<Frame>()
        private val freelist = ConcurrentLinkedQueue<Frame>()

        // HTTP threads only
        private var current_frame: Frame? = null
        private var current_meta: TrackMetaData? = null

        private var decoder_codec: MediaCodec? = null
        private var decoder_ks: (() -> Unit)? = null
        private var player: AudioTrack? = null
        private var paused_bufsize: Int? = null

        private var wantStart: Station? = null
        private var lastStation: Station? = null
        private var recmgr: RecordingManagerService? = null
        private var streamrec: StreamRecorder? = null

        private val metaqueue = LinkedList<MetaDataRecord>()
        private var sample_rate: Int = -1
        private var max_frames: Int = 0
        private var max_frames_soft: Int = 0

        private var stat_allocated_buffers: Int = 0
        private var stat_dropped_buffers: Int = 0
        private var stat_small_buffer: Int = 0
        private var stat_drained: Int = 0

        fun reset() {
            if (lastStation != null) {
                cb.onStationLoading(null)
            }
            lastStation = null

            metaint = null
            content_type = null
            decoder = null

            current_frame?.let {
                it.clear()
                freelist.add(it)
            }
            current_frame = null
            while (true) {
                val x = bqueue.poll()
                if (x == null) {
                    break
                }
                x.clear()
                freelist.add(x)
            }

            paused_bufsize = null

            streamrec?.onStop()
            streamrec = null

            player?.let {
                it.release()
            }
            player = null

            decoder_ks?.let { it() }
            decoder_ks = null
            decoder_codec = null

            http_ks?.let { it() }
            http_ks = null

            metaqueue.clear()
            current_meta = null

            sample_rate = -1
            max_frames = 0
            max_frames_soft = 0

            cb.onPlaybackStateChanged(PlaybackState.STOPPED)
        }

        private fun setupPlayer(fmt: MediaFormat) {
            val chcfg = when (fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)) {
                1 -> AudioFormat.CHANNEL_OUT_MONO
                2 -> AudioFormat.CHANNEL_OUT_STEREO
                else -> AudioFormat.CHANNEL_OUT_STEREO // idk
            }
            for (k in fmt.keys) {
                Log.d(TAG, "fmt key $k")
            }
            // val enc = fmt.getInteger(MediaFormat.KEY_PCM_ENCODING)
            val enc = AudioFormat.ENCODING_PCM_16BIT
            // Log.d(TAG, "pcm enc $enc")
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

            cb.onPlaybackStateChanged(PlaybackState.PLAYING)
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
                    fmt.setInteger(MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                    var endflag = false
                    var timestamp = 0.toLong()
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

                                if (endflag) {
                                    mc.queueInputBuffer(
                                        index, 0, 0, timestamp,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    )
                                    return
                                }

                                // Keep cache volume when paused to
                                // prevent draining on resume and keep
                                // delay from before entering pause
                                val ps = paused_bufsize
                                val can_give = ps == null || bqueue.size > ps
                                val inf = if (can_give) {
                                    bqueue.poll()
                                } else {
                                    null
                                }
                                val t = timestamp
                                if (inf != null) {
                                    if (inf.buf.remaining() <= buf.remaining()) {
                                        buf.put(inf.buf)
                                        inf.meta?.let {
                                            metaqueue.add(MetaDataRecord(timestamp, it))
                                        }
                                        freelist.add(inf)

                                        timestamp += MP3_SAMPLES_PER_FRAME * 1000000 / sample_rate
                                    } else {
                                        Log.w(TAG, "onIBA: MediaCodec buffer too small")
                                    }
                                } else {
                                    if (bqueue.isEmpty() && !freelist.isEmpty()) {
                                        stat_drained++
                                    }
                                }

                                val n = buf.position()
                                buf.rewind()

                                mc.queueInputBuffer(index, 0, n, t, 0)
                            }

                            override fun onOutputBufferAvailable(
                                mc: MediaCodec,
                                index: Int,
                                info: MediaCodec.BufferInfo,
                            ) {

                                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                                    mc.releaseOutputBuffer(index, false)
                                    mc.release()
                                    // TODO: stream recorder onStop
                                    return
                                }

                                if (info.size == 0) {
                                    return
                                }

                                while (!metaqueue.isEmpty()
                                    && info.presentationTimeUs >= metaqueue.get(0).timestamp
                                ) {
                                    cb.onMetaData(metaqueue.remove().meta)
                                }

                                val buf = mc.getOutputBuffer(index)
                                if (buf == null) {
                                    Log.w(TAG, "onOBA: no promised output buffer $index")
                                    return
                                }

                                if (paused_bufsize == null) {
                                    player?.let { at ->
                                        if (at.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
                                            Log.i(TAG, "starting playback")
                                            at.play()
                                        }
                                        at.write(buf, buf.remaining(), AudioTrack.WRITE_BLOCKING)
                                        buf.rewind()
                                    }
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
                    decoder_ks = { endflag = true }
                    true
                }
                else -> false
            }
        }

        private fun setupRequest(req: Request) {
            var tostop = false
            http_ks = { tostop = true }

            req
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
                                    max_frames_soft =
                                        freq_hz * MAX_CACHE_SECONDS_SOFT / MP3_SAMPLES_PER_FRAME

                                    var frm: Frame?
                                    while (true) {
                                        val f = freelist.poll()
                                        if (f == null || f.buf.capacity() >= frame_len) {
                                            f?.clear()
                                            frm = f
                                            break
                                        } else {
                                            stat_small_buffer++
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
                                            current_meta?.let { m ->
                                                frm.meta = m
                                                current_meta = null

                                                // TODO: if first metadata string,
                                                // we may be joining in the middle
                                                // of a song. Shortwave drops the
                                                // first song, should we?

                                                Log.d(TAG, "requesting recording for ${m.original}")
                                                recmgr!!.requestNewRecording(m, "audio/mpeg") { r ->
                                                    handler.post {
                                                        Log.d(TAG, "new track: ${r.uuid}")
                                                        streamrec!!.onNewTrack(r)
                                                    }
                                                }
                                            }

                                            frm.buf.flip()

                                            streamrec!!.onFrame(frm.buf.slice())

                                            bqueue.add(frm)
                                            current_frame = null

                                            val bqsz = bqueue.size
                                            if (bqsz > max_frames) {
                                                var ndrop = 0
                                                while (bqsz - ndrop > max_frames_soft) {
                                                    ndrop++
                                                    bqueue.poll()?.let { f ->
                                                        f.meta?.let {
                                                            current_meta = it
                                                        }
                                                        freelist.add(f)
                                                    }
                                                    stat_dropped_buffers++
                                                }
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
                            r.abort(
                                UnsupportedCodecException(
                                    when (val ct = content_type) {
                                        "audio/aac" -> "AAC"
                                        "audio/aacp" -> "AAC+"
                                        null -> "(no content type)"
                                        else -> ct
                                    }
                                )
                            )
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
                                    val m = parseTrackTitle(s2)
                                    current_meta = m
                                }
                            }
                        )
                    }

                    decoder = fsm
                }
                .onResponseContent { r, buf ->
                    if (tostop) {
                        r.abort(TerminationMarker())
                    } else {
                        decoder?.step(buf)
                    }
                }
        }

        private fun setupStreamRecorder() {
            val srec = StreamRecorder(
                handler,
                object : StreamRecorder.Callback {
                    override fun onOpenChannel(r: Recording): AsynchronousFileChannel {
                        val fn = recmgr!!.recordingPath(r)
                        return AsynchronousFileChannel.open(
                            fn,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.CREATE
                        )
                    }

                    override fun onTrackDone(
                        r: Recording, chan: AsynchronousFileChannel,
                        interrupted: Boolean
                    ) {
                        Log.d(TAG, "track ${r.uuid} done")
                        chan.close()
                        if (!interrupted) {
                            recmgr!!.notifyRecordingFinished(r)
                        }
                    }

                    override fun onStop() {
                        // TODO: not called
                        Log.i(TAG, "StreamRecorder stopped")
                    }
                }
            )
            streamrec?.onStop()
            streamrec = srec
        }

        private fun startPlayingUrl(s: Station) {
            reset()
            cb.onPlaybackStateChanged(PlaybackState.LOADING)
            cb.onStationLoading(s)
            lastStation = s

            val surl = s.streamUrl
            Log.d(TAG, "Playing URL: $surl")

            val uri = URI(surl)
            if (uri.scheme.lowercase() == "https") {
                // instead of failing with NPE
                throw UnsupportedTransportException("HTTPS")
            }

            val req = hc.newRequest(uri)
            setupRequest(req)
            setupStreamRecorder()

            req.send { r ->
                handler.post {
                    if (r.getRequestFailure() == null
                        && r.getResponseFailure() is TerminationMarker
                    ) {
                        Log.i(TAG, "HTTP Response terminated")

                        // No need to call reset() again
                        return@post
                    } else if (r.isFailed()) {
                        try {
                            Log.w(TAG, "http failed")
                            var req_fail: Throwable? = null
                            r.getRequestFailure()?.let {
                                Log.d(TAG, "req  fail $it", it)
                                cb.onError(it)
                                req_fail = it
                            }
                            r.getResponseFailure()?.let {
                                Log.d(TAG, "resp fail $it", it)
                                if (it != req_fail) {
                                    cb.onError(it)
                                }
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "exception while handling request failure", e)
                            cb.onError(e)
                        }
                    } else {
                        // NOTE: probably lost network or station stopped
                        Log.i(TAG, "http stopped")
                        cb.onError(ConnectionClosedException())
                    }

                    reset()
                }
            }
        }

        fun handleMessage(msg: Message): Boolean {
            return when (msg.what) {
                CMD_START_PLAYING_STATION -> {
                    val s = msg.obj as Station
                    if (recmgr == null) {
                        wantStart = s
                    } else {
                        startPlayingUrl(s)
                    }
                    true
                }
                CMD_PAUSE_PLAYBACK -> {
                    val sz = bqueue.size
                    paused_bufsize = if (sz >= max_frames) max_frames - 1 else sz
                    cb.onPlaybackStateChanged(PlaybackState.PAUSED)
                    true
                }
                CMD_RESUME_PLAYBACK -> {
                    paused_bufsize = null
                    cb.onPlaybackStateChanged(PlaybackState.PLAYING)
                    true
                }
                CMD_STOP_PLAYBACK -> {
                    reset()
                    true
                }
                CMD_DEBUG_INFO -> {
                    Log.d(TAG, "sizes: bqueue:${bqueue.size} freelist:${freelist.size}")
                    Log.d(TAG, "allocated buffers: ${stat_allocated_buffers}")
                    Log.d(TAG, "dropped buffers  : ${stat_dropped_buffers}")
                    Log.d(TAG, "drained events   : ${stat_drained}")
                    Log.d(TAG, "queue limits     : ${max_frames_soft} soft / ${max_frames} hard")
                    Log.d(TAG, "buffer too small : ${stat_small_buffer}")
                    streamrec?.debugInfo()
                    true
                }
                else -> false
            }
        }

        override fun run() {
            var err = false

            Log.d(TAG, "starting PlayerThread")

            hc = HttpClient()
            hc.setIdleTimeout(IDLE_TIMEOUT)
            hc.start()
            try {
                super.run()
            } catch (e: Exception) {
                Log.e(TAG, "Uncaught exception in PlayerThread", e)
                cb.onError(e)
                err = true
            }
            Log.d(TAG, "out of looper")

            reset()
            bqueue.clear()
            freelist.clear()

            hc.stop()
            Log.d(TAG, "stopping PlayerThread")

            if (err) {
                cb.requestRestart()
            }
        }

        override protected fun onLooperPrepared() {
            handler = Handler(looper)

            cb.requestRecMgrSvc { svc ->
                handler.post {
                    Log.d(TAG, "received recmgr")
                    recmgr = svc

                    wantStart?.let {
                        startPlayingUrl(it)
                    }
                    wantStart = null
                }
            }
        }
    }

    fun startPlayingStation(s: Station) {
        mHandler.obtainMessage(CMD_START_PLAYING_STATION, s)
            .sendToTarget()
    }

    fun stopPlayback() {
        mHandler.obtainMessage(CMD_STOP_PLAYBACK)
            .sendToTarget()
    }

    fun pausePlayback() {
        mHandler.obtainMessage(CMD_PAUSE_PLAYBACK)
            .sendToTarget()
    }

    fun resumePlayback() {
        mHandler.obtainMessage(CMD_RESUME_PLAYBACK)
            .sendToTarget()
    }

    fun logDebugInfo() {
        mHandler.obtainMessage(CMD_DEBUG_INFO)
            .sendToTarget()
    }

    private fun onMetaData(m: TrackMetaData) {
        mMetaData.postValue(m)
        // TODO: update notification
        this.updateNotification()
        // TODO: update playback history
    }

    private fun onPlaybackStateChanged(ps: PlaybackState) {
        val old = mPlaybackState.getValue()
        if (old == ps) {
            return
        }

        mPlaybackState.postValue(ps)

        Log.d(TAG, "playback state: $old -> $ps")


        this.updateNotification()

        if (old != null) {
            val oldfg = old != PlaybackState.STOPPED
            val newfg = ps != PlaybackState.STOPPED

            when {
                oldfg && !newfg -> {
                    stopForeground(Service.STOP_FOREGROUND_REMOVE)
                }
                !oldfg && newfg -> {
                    // TODO: do this while *requesting* a new playing state?
                    val notif = makeNotification()
                    startForeground(NOTIF_ID, notif)
                }
            }
        }
    }

    private fun onStationLoading(s: Station?) {
        mMetaData.postValue(null)
        mStation.postValue(s)
    }

    inner class PlayerServiceBinder : Binder() {
        val service: PlayerService
            get() = this@PlayerService
    }

    override fun onStartCommand(intent: Intent, flags: Int, id: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = PlayerServiceBinder()

    private fun updateNotification() {
        val nm = getSystemService(NotificationManager::class.java)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Default",
                NotificationManager.IMPORTANCE_HIGH
            )
            chan.enableLights(true)
            chan.lightColor = Color.BLUE
            nm.createNotificationChannel(chan)
            NotificationCompat.Builder(this, chan.id)
        } else {
            NotificationCompat.Builder(this)
        }

        val mySession = MediaSessionCompat(this, TAG)
        val sessionToken = mySession.sessionToken

        val mainPageIntent = Intent(this, MainActivity::class.java)
        val resultPendingIntent = PendingIntent.getActivity(
            this, 0, mainPageIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, PlayerBroadcastReceiver::class.java)
        stopIntent.setAction(PlayerBroadcastReceiver.ACTION_STOP)
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val pauseIntent = Intent(this, PlayerBroadcastReceiver::class.java)
        pauseIntent.setAction(PlayerBroadcastReceiver.ACTION_PAUSE)
        val pausePendingIntent = PendingIntent.getBroadcast(
            this, 0, pauseIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val resumeIntent = Intent(this, PlayerBroadcastReceiver::class.java)
        resumeIntent.setAction(PlayerBroadcastReceiver.ACTION_RESUME)
        val resumePendingIntent = PendingIntent.getBroadcast(
            this, 0, resumeIntent,
            PendingIntent.FLAG_IMMUTABLE
        )


        val stopAction: NotificationCompat.Action = NotificationCompat.Action(
            R.drawable.ic_stop_24, "Stop", stopPendingIntent
        )
        val pauseAction: NotificationCompat.Action = NotificationCompat.Action(
            R.drawable.ic_pause_24, "Pause", pausePendingIntent
        )
        val resumeAction: NotificationCompat.Action = NotificationCompat.Action(
            R.drawable.ic_play_24, "Resume", resumePendingIntent
        )

        if (this.mPlaybackState.value == PlaybackState.PLAYING || this.mPlaybackState.value == PlaybackState.LOADING) {
            builder.addAction(pauseAction)
        } else if (this.mPlaybackState.value == PlaybackState.PAUSED) {
            builder.addAction(resumeAction)
        }

        val notification = builder
            .setSmallIcon(R.drawable.ic_note)
            .setContentTitle(if (mMetaData.value?.title!= null){
                mMetaData.value?.title
            } else{
                mStation.value?.name
            })
            .setContentText(mMetaData.value?.artist)
            .setContentInfo("blablabla")
            .addAction(stopAction)
            .setStyle(
                mediaStyle
                    .setShowActionsInCompactView(0, 1)
                    .setMediaSession(sessionToken)
                    .setCancelButtonIntent(stopPendingIntent)
                    .setShowCancelButton(true)
            )
            .setContentIntent(resultPendingIntent)
            .setColorized(true)
            .setUsesChronometer(true)
            .setColor(Color.MAGENTA)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIF_ID, notification)
    }

    private fun makeNotification(): Notification {
        val nm = getSystemService(NotificationManager::class.java)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "Default",
                NotificationManager.IMPORTANCE_HIGH
            )
            chan.enableLights(true)
            chan.lightColor = Color.BLUE
            nm.createNotificationChannel(chan)
            NotificationCompat.Builder(this, chan.id)
        } else {
            NotificationCompat.Builder(this)
        }

        val mySession = MediaSessionCompat(this, TAG)
        val sessionToken = mySession.sessionToken

        val mainPageIntent = Intent(this, MainActivity::class.java)
        val resultPendingIntent = PendingIntent.getActivity(
            this, 0, mainPageIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, PlayerBroadcastReceiver::class.java)
        stopIntent.setAction(PlayerBroadcastReceiver.ACTION_STOP)
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val pauseIntent = Intent(this, PlayerBroadcastReceiver::class.java)
        pauseIntent.setAction(PlayerBroadcastReceiver.ACTION_PAUSE)
        val pausePendingIntent = PendingIntent.getBroadcast(
            this, 0, pauseIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val resumeIntent = Intent(this, PlayerBroadcastReceiver::class.java)
        resumeIntent.setAction(PlayerBroadcastReceiver.ACTION_RESUME)
        val resumePendingIntent = PendingIntent.getBroadcast(
            this, 0, resumeIntent,
            PendingIntent.FLAG_IMMUTABLE
        )


        val stopAction: NotificationCompat.Action = NotificationCompat.Action(
            R.drawable.ic_stop_24, "Stop", stopPendingIntent
        )
        val pauseAction: NotificationCompat.Action = NotificationCompat.Action(
            R.drawable.ic_pause_24, "Pause", pausePendingIntent
        )
        val resumeAction: NotificationCompat.Action = NotificationCompat.Action(
            R.drawable.ic_play_24, "Resume", resumePendingIntent
        )

        if (this.mPlaybackState.value == PlaybackState.PLAYING || this.mPlaybackState.value == PlaybackState.LOADING) {
            builder.addAction(pauseAction)
        } else if (this.mPlaybackState.value == PlaybackState.PAUSED) {
            builder.addAction(resumeAction)
        }


        val notification = builder
            .setSmallIcon(R.drawable.ic_note)
            .setContentTitle(if (mMetaData.value?.title!= null){
                mMetaData.value?.title
            } else{
                mStation.value?.name
            })
            .setContentText(mMetaData.value?.artist)
            .addAction(stopAction)
            .setStyle(
                mediaStyle
                    .setShowActionsInCompactView(0, 1)
                    .setMediaSession(sessionToken)
                    .setCancelButtonIntent(stopPendingIntent)
                    .setShowCancelButton(true)
            )
            .setContentIntent(resultPendingIntent)
            .setColorized(true)
            .setUsesChronometer(true)
            .setColor(Color.MAGENTA)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()


        return notification
    }

    private fun setupThread() {
        mThread = PlayerThread(
            resources.getString(R.string.user_agent), mAudioSid,
            object : PlayerThread.Callback {
                override fun onMetaData(m: TrackMetaData) {
                    this@PlayerService.onMetaData(m)
                }

                override fun onPlaybackStateChanged(ps: PlaybackState) {
                    this@PlayerService.onPlaybackStateChanged(ps)
                }

                override fun onStationLoading(s: Station?) {
                    this@PlayerService.onStationLoading(s)
                }

                override fun onError(e: Throwable) {
                    val s = when (e) {
                        is URISyntaxException -> "Invalid station URL"
                        is UnsupportedCodecException -> "Station uses unsupported codec ${e.codecName}"
                        is UnsupportedTransportException -> "Station uses unsuppoted transport ${e.transportName}"
                        is ConnectionClosedException -> "Connection closed unexpectedly"
                        is TimeoutException -> "Connection timed out"
                        is UnknownHostException -> "Failed to resolve hostname"
                        is ConnectException -> "Failed to connect"
                        else -> {
                            Log.e(TAG, "Unhandled exception class ${e::class.java.canonicalName}")
                            "Uncaught exception in player service"
                        }
                    }
                    Toast.makeText(
                        this@PlayerService, "RadiuPlayer: $s",
                        Toast.LENGTH_LONG,
                    ).show()
                    // We’ll get an onPlaybackStateChanged as well
                }

                override fun requestRecMgrSvc(
                    cb: (RecordingManagerService) -> Unit,
                ) {
                    mRecMgrConn.doAction(cb)
                }

                override fun requestRestart() {
                    mMainHandler.post {
                        Log.i(TAG, "reinit PlayerThread")
                        setupThread()
                    }
                }
            }
        )
        mThread.start()
        mHandler = Handler(mThread.looper, mThread::handleMessage)
    }

    override fun onCreate() {
        val am = getSystemService(AudioManager::class.java)

        mMainHandler = Handler(getMainLooper())

        mAudioSid = am.generateAudioSessionId()
        setupThread()

        bindService(
            Intent(this, RecordingManagerService::class.java),
            mRecMgrConn,
            Context.BIND_AUTO_CREATE
        )

    }

    override fun onDestroy() {
        mThread.looper.quitSafely()
        mThread.join(1000)       // wait 1sec
        if (mThread.isAlive()) {
            Log.w(TAG, "failed to join thread")
            mThread.interrupt()
            mThread.join()
        }
        unbindService(mRecMgrConn)
    }
}
