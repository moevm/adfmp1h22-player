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

class StreamRecorder(
    private val handler: Handler,
    private val cb: Callback,
) {

    companion object {
        val TAG = "StreamRecorder"
    }

    // TODO
    //
    // Figure out what exactly we’re getting:
    // - How AAC frames look like
    // - How containers look like
    // - Are we getting AAC in a container
    // - What players expect, how playable files look like

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

    private val chanqueue = LinkedList<ByteBuffer?>()
    private val chanfreelist = LinkedList<ByteBuffer>()
    private var chanbusyslot: ByteBuffer? = null

    private fun startNextTrack() {
        val t = track
        if (t != null) {
            cb.onTrackDone(t.r, t.chan)
        }

        if (trackqueue.isEmpty()) {
            track = null
        } else {
            val r = trackqueue.remove()
            if (r == null) {
                doStop()
            } else {
                setupRecording(r)
            }
        }
    }

    private fun onWriteDone(n: Int) {
        Log.d(TAG, "onWriteDone")

        val t = track
        if (t == null) {
            Log.w(TAG, "onWriteDone: track is null")
            return
        }
        t.pos += n.toLong()

        val b = chanbusyslot
        if (b == null) {
            Log.w(TAG, "onWriteDone: busy slot empty")
            return
        }

        b.clear()
        chanfreelist.add(b)

        chanbusyslot = null
        maybeDoOutput()
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
            // end flag
            startNextTrack()
        } else {
            chanbusyslot = b

            val bs = ByteArray(if (b.remaining() > 4) 4
                               else b.remaining())
            b.mark()
            b.get(bs)
            b.reset()
            Log.d(TAG, "t.chan.write (${b.remaining()} at ${t.pos}): ${bs.toList()}}")

            t.chan.write(b, t.pos, this, ComplHandler())
        }
    }

    private fun getChanBuffer(n: Int): ByteBuffer {
        var b: ByteBuffer? = null
        while (true) {
            val x = chanfreelist.poll()
            if (x == null) {
                break
            }
            if (x.capacity() >= n) {
                b = x
                break
            } else {
                // TODO: stats
                Log.i(TAG, "File channel buffer from freelist too small for $n")
            }
        }
        if (b == null) {
            // TODO: stats
            b = ByteBuffer.allocate(n)
            if (b == null) {
                Log.e(TAG, "Failed to allocate ByteBuffer of size $n")
            }
        }
        b!!.clear()
        return b
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
                    mc.queueInputBuffer(index, 0, 0, 0,
                                         MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                    encfreelist.add(ib)
                } else {
                    Log.d(TAG, "bufs: ${buf.remaining()}, want ${ib.buf.remaining()}")
                    buf.put(ib.buf)
                    ib.buf.clear()
                    encfreelist.add(ib)

                    val n = buf.position()
                    buf.rewind()

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
                mc.releaseOutputBuffer(index, false)
                chanqueue.add(null)
                maybeDoOutput()
                return
            }

            // TODO: maybe check BUFFER_FLAG_CODEC_CONFIG

            if (info.size == 0) {
                return
            }

            val buf = mc.getOutputBuffer(index)
            if (buf == null) {
                Log.w(TAG, "onOBA: no promised output buffer $index")
                return
            }

            val b = getChanBuffer(buf.remaining())
            b.put(buf)

            mc.releaseOutputBuffer(index, false)

            b.flip()
            chanqueue.add(b)
            maybeDoOutput()
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
        fmt.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4608)
        fmt.setInteger(MediaFormat.KEY_AAC_PROFILE,
                       MediaCodecInfo.CodecProfileLevel.AACObjectLC)

        var e = enc
        if (e == null) {
            val mcl = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (c in mcl.getCodecInfos().asSequence().filter {
                     x -> x.isEncoder()
            }) {
                Log.d(TAG, "encoder ${c.name} cn ${c.canonicalName}isVendor=${c.isVendor()}")
                for (t in c.supportedTypes) {
                    Log.d(TAG, "  type $t")
                }
            }
            val mcn = mcl.findEncoderForFormat(fmt)
            Log.i(TAG, "creating codec name $mcn")
            e = MediaCodec.createByCodecName(mcn)
            e.setCallback(mediaCodecCallback())
            enc = e
        } else {
            e.stop()
        }
        e.configure(fmt, null, null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE)
        Log.d(TAG, "enc input fmt: ${e.inputFormat}")
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
