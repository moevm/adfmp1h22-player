package com.github.moevm.adfmp1h22_player

import android.util.Log

import android.os.Handler

import java.nio.ByteBuffer
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler

import java.util.LinkedList

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaCodec
import android.media.MediaFormat

fun writeADTSHeader(b: ByteBuffer, framesz: Int) {
    b.put(0xFF.toByte())
    b.put(0xF1.toByte())
    b.put(0x50.toByte())
    b.put((0x80 or ((framesz shr 11) and 3)).toByte())
    b.put(((framesz shr 3) and 255).toByte())
    b.put((0x1F or ((framesz and 7) shl 5)).toByte())
    b.put(0xFC.toByte())
}

class StreamRecorder(
    private val handler: Handler,
    private val cb: Callback,
) {

    companion object {
        val TAG = "StreamRecorder"

        val FILE_IO_BUFFER_SIZE = 65536 // 64KiB
    }

    interface Callback {
        fun onOpenChannel(r: Recording): AsynchronousFileChannel
        fun onTrackDone(r: Recording, chan: AsynchronousFileChannel)
        fun onStop()
    }

    private class Frame(
        public var endflag: Boolean,
        public var timestamp: Long,
        public val buf: ByteBuffer,
    )

    private class TrackInfo(
        public val r: Recording,
        public val chan: AsynchronousFileChannel,
        public var pos: Long,
    )

    private var enc: MediaCodec? = null
    private val encqueue = LinkedList<Frame>()
    private val encfreelist = LinkedList<Frame>()

    private var track: TrackInfo? = null
    private val trackqueue = LinkedList<Recording?>()

    private var chancurbuf: ByteBuffer? = null
    private val chanqueue = LinkedList<ByteBuffer?>()
    private val chanfreelist = LinkedList<ByteBuffer>()
    private var chanbusyslot: ByteBuffer? = null

    private fun startNextTrack() {
        val t = track
        if (t != null) {
            cb.onTrackDone(t.r, t.chan)
        }

        if (trackqueue.isEmpty()) {
            Log.d(TAG, "no next track")
            track = null
        } else {
            val r = trackqueue.remove()
            if (r == null) {
                doStop()
            } else {
                Log.d(TAG, "start new track $r")
                setupRecording(r)
            }
        }
    }

    private fun onWriteDone(n: Int) {
        handler.post {
            val t = track
            if (t == null) {
                Log.w(TAG, "onWriteDone: track is null")
                return@post
            }
            t.pos += n.toLong()

            val b = chanbusyslot
            if (b == null) {
                Log.w(TAG, "onWriteDone: busy slot empty")
                return@post
            }

            b.clear()
            chanfreelist.add(b)

            chanbusyslot = null
            maybeDoOutput()
        }
    }

    private fun onWriteError(exc: Throwable) {
        Log.e(TAG, "write error: $exc", exc)
    }

    private class ComplHandler : CompletionHandler<Int, StreamRecorder> {
        override fun completed(
            result: Int,
            attachment: StreamRecorder,
        ) {
            attachment.onWriteDone(result)
        }

        override fun failed(
            exc: Throwable,
            attachment: StreamRecorder,
        ) {
            attachment.onWriteError(exc)
        }
    }

    private fun maybeDoOutput() {
        if (chanbusyslot != null || chanqueue.isEmpty()) {
            return
        }

        val t = track
        if (t == null) {
            Log.w(TAG, "track is null in maybeDoOutput")
            return
        }

        // NOTE: not poll() because null is an end flag
        val b = chanqueue.remove()

        if (b == null) {
            Log.d(TAG, "end flag")

            // end flag
            startNextTrack()
        } else {
            chanbusyslot = b
            t.chan.write(b, t.pos, this, ComplHandler())
        }
    }

    private fun getChanBuffer(n: Int): ByteBuffer {
        var b = chancurbuf

        if (b != null && b.remaining() < n) {
            b.flip()
            chanqueue.add(b)
            b = null
            maybeDoOutput()
        }

        // Either first time or not enough space in current buffer
        if (b == null) {
            b = chanfreelist.poll()

            if (b == null) {
                b = ByteBuffer.allocate(FILE_IO_BUFFER_SIZE)
            }
        }

        chancurbuf = b
        return b!!
    }

    private fun mediaCodecCallback() = object : MediaCodec.Callback() {
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

            val ib = encqueue.poll()
            if (ib != null) {

                if (ib.endflag) {
                    Log.i(TAG, "onIBA: seeing end flag")
                    mc.queueInputBuffer(index, 0, 0, 0,
                                         MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    encfreelist.add(ib)
                } else {
                    buf.put(ib.buf)
                    ib.buf.clear()
                    encfreelist.add(ib)

                    val n = buf.position()
                    buf.flip()

                    mc.queueInputBuffer(index, 0, n, ib.timestamp, 0)
                }

            } else {

                // WARN: drained
                mc.queueInputBuffer(index, 0, 0, 0, 0)

            }
        }

        override fun onOutputBufferAvailable(
            mc: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo,
        ) {
            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {

                Log.i(TAG, "onOBA: enqueue end flag")
                mc.releaseOutputBuffer(index, false)

                // flush file io
                chancurbuf?.let {
                    it.flip()
                    chanqueue.add(it)
                }
                chancurbuf = null
                chanqueue.add(null)

                maybeDoOutput()
                return
            }

            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0
                || info.size == 0) {
                mc.releaseOutputBuffer(index, false)
                return
            }

            val buf = mc.getOutputBuffer(index)
            if (buf == null) {
                Log.w(TAG, "onOBA: no promised output buffer $index")
                return
            }

            val b = getChanBuffer(buf.remaining() + 7)
            writeADTSHeader(b, buf.remaining() + 7)
            b.put(buf)

            mc.releaseOutputBuffer(index, false)
        }

        override fun onOutputFormatChanged(
            mc: MediaCodec,
            fmt: MediaFormat,
        ) {
            Log.i(TAG, "onOFC: $fmt")
        }
    }

    private fun setupRecording(r: Recording) {

        val chan = cb.onOpenChannel(r)
        track = TrackInfo(r, chan, 0)

        val fmt = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            44100, 2,
        )
        fmt.setInteger(MediaFormat.KEY_BIT_RATE, 128*1000)

        // TODO: don’t start recorder until we know the PCM format, i.e.
        // until the decoder is initialized.
        fmt.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4608)
        fmt.setInteger(MediaFormat.KEY_AAC_PROFILE,
                       MediaCodecInfo.CodecProfileLevel.AACObjectLC)

        var e = enc
        if (e == null) {
            val mcl = MediaCodecList(MediaCodecList.ALL_CODECS)
            val mcn = mcl.findEncoderForFormat(fmt)
            Log.i(TAG, "creating codec name $mcn")
            e = MediaCodec.createByCodecName(mcn)
            e.setCallback(mediaCodecCallback(), handler)
            enc = e
        } else {
            e.stop()
        }
        e.configure(fmt, null, null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE)
        Log.i(TAG, "enc input fmt: ${e.inputFormat}")
        e.start()

    }

    private fun getFrame(n: Int): Frame {
        var f: Frame? = null
        while (true) {
            val x = encfreelist.poll()
            if (x == null) {
                break
            }
            if (x.buf.capacity() >= n) {
                f = x
                break
            } else {
                // TODO: stats
                Log.i(TAG, "PCM buffer from freelist too small for $n")
            }
        }
        if (f == null) {
            // TODO: stats
            f = Frame(false, -1, ByteBuffer.allocate(n))
        }
        return f
    }

    private fun postEndFlag() {
        val f = getFrame(0)
        f.endflag = true
        f.timestamp = -1
        f.buf.clear()
        encqueue.add(f)
    }

    private fun doStop() {
        Log.i(TAG, "doStop")
        cb.onStop()
        enc?.release()
    }

    // NOTE: user interface below

    fun onNewTrack(r: Recording) {
        if (track == null) {
            setupRecording(r)
        } else {
            postEndFlag()
            trackqueue.add(r)
        }
    }

    fun onPCMBuffer(timestamp: Long, b: ByteBuffer) {
        val f = getFrame(b.remaining())
        f.endflag = false
        f.timestamp = timestamp
        f.buf.clear()
        f.buf.put(b)
        f.buf.flip()
        encqueue.add(f)
    }

    fun onStop() {
        if (track != null) {
            postEndFlag()
            trackqueue.add(null)
        } else {
            doStop()
        }
    }

}
