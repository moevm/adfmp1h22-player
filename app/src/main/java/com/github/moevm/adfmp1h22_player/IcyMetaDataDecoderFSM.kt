package com.github.moevm.adfmp1h22_player

import android.util.Log
import java.nio.ByteBuffer
import kotlin.math.min


class IcyMetaDataDecoderFSM(
    val metaint: Int,
    val cb: Callback,
) : DecoderFSM {

    interface Callback {
        fun onPayload(c: ByteBuffer)
        fun onMetaData(s: String)
    }

    enum class State {
        PAYLOAD, SIZE, METADATA,
    }

    private var metastt: State = State.PAYLOAD
    private var metactr: Int = 0
    private var metabuf: ByteBuffer? = null

    fun makeMetaData(b: ByteBuffer): String {
        return b.array()
            .takeWhile { it != 0.toByte() }
            .toByteArray()
            .toString(Charsets.UTF_8)
    }

    override fun step(c: ByteBuffer) {
        while (c.hasRemaining()) {
            // Log.d("APPDEBUG", "icy state ${metastt}, rem ${c.remaining()}")
            when (metastt) {
                State.PAYLOAD -> {
                    val n = min(metaint - metactr, c.remaining())
                    metactr += n
                    cb.onPayload(c.slice().limit(n) as ByteBuffer)
                    c.position(c.position() + n)
                    if (metactr == metaint) {
                        metactr = 0
                        metastt = State.SIZE
                    }
                }
                State.SIZE -> {
                    val sz = 16 * c.get()
                    if (sz > 0) {
                        metabuf = ByteBuffer.allocate(sz)
                        metastt = State.METADATA
                    } else {
                        metastt = State.PAYLOAD
                    }
                }
                State.METADATA -> {
                    metabuf!!.let {
                        val n = min(it.remaining(), c.remaining())
                        it.put(c.slice().limit(n) as ByteBuffer)
                        c.position(c.position() + n)
                        if (!it.hasRemaining()) {
                            val s = makeMetaData(it)
                            cb.onMetaData(s)
                            metabuf = null
                            metastt = State.PAYLOAD
                        }
                    }
                }
            }
        }
        // Log.d("APPDEBUG", "icy done")
    }
}
