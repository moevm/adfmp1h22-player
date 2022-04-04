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

        val FILE_IO_BUFFER_SIZE = 65536 // 64KiB
    }

    interface Callback {
        fun onOpenChannel(r: Recording): AsynchronousFileChannel
        fun onTrackDone(r: Recording, chan: AsynchronousFileChannel)
        fun onStop()
    }

    private class TrackInfo(
        public val r: Recording,
        public val chan: AsynchronousFileChannel,
        public var pos: Long,
    )

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

    private fun flushChan() {
        chancurbuf?.let {
            it.flip()
            chanqueue.add(it)
        }
        chancurbuf = null
        chanqueue.add(null)
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

    private fun setupRecording(r: Recording) {
        val chan = cb.onOpenChannel(r)
        track = TrackInfo(r, chan, 0)
    }

    private fun postEndFlag() {
        flushChan()
        maybeDoOutput()
    }

    private fun doStop() {
        Log.i(TAG, "doStop")
        cb.onStop()
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

    fun onFrame(buf: ByteBuffer) {
        val b = getChanBuffer(buf.remaining())
        b.put(buf)
    }

    fun onStop() {
        if (track != null) {
            postEndFlag()
            trackqueue.add(null)
        } else {
            doStop()
        }
    }

    fun debugInfo() {
        Log.d(TAG, "chan: queue:${chanqueue.size} freelist:${chanfreelist.size}")

        val cb = chancurbuf
        val cur = if (cb != null) {
            "${cb.position()}/${cb.limit()}"
        } else {
            "(none)"
        }
        Log.d(TAG, "current: ${cur}")
    }

}
