package com.github.moevm.adfmp1h22_player

import android.util.Log
import java.nio.ByteBuffer
import kotlin.math.min

private val BITRATE_TABLE = arrayOf(
    -1, 32, 40, 48,
    56, 64, 80, 96,
    112, 128, 160, 192,
    224, 256, 320, -1,
)

private val FREQUENCY_TABLE = arrayOf(
    44100,
    48000,
    32000,
    -1,
)

private val MODES_TABLE = arrayOf(
    Mp3HeaderDecoderFSM.Mode.STEREO,
    Mp3HeaderDecoderFSM.Mode.JOINT_STEREO,
    Mp3HeaderDecoderFSM.Mode.DUAL_STEREO,
    Mp3HeaderDecoderFSM.Mode.MONO,
)

class Mp3HeaderDecoderFSM(
    protected val cb: Callback
) : DecoderFSM {

    enum class Mode {
        STEREO, JOINT_STEREO, DUAL_STEREO, MONO,
    }

    interface Callback {
        fun onFormat(br_kbps: Int, freq_hz: Int, mode: Mode)
        fun onPayload(c: ByteBuffer)
        fun onFrameDone()
    }

    enum class State {
        HEADER, PAYLOAD, RESYNC,
    }

    private var stt: State = State.HEADER
    private var rem: Int = 4
    private var br_kbps: Int? = null
    private var freq_hz: Int? = null
    private var padded: Boolean? = null
    private var mode: Mode? = null
    private var hdr = ByteBuffer.allocate(4)

    private fun decodeHeaderByte3(b: Byte): Boolean {
        val bi = b.toInt()

        val br_idx = (bi shr 4) and 0x0F
        val freq_idx = (bi shr 2) and 0x03
        val padded_ = (bi shr 1) and 0x01 == 0x01

        val br_kbps_ = BITRATE_TABLE[br_idx]
        val freq_hz_ = FREQUENCY_TABLE[freq_idx]

        br_kbps = br_kbps_
        freq_hz = freq_hz_
        padded = padded_

        return br_kbps_ > 0 && freq_hz_ > 0
    }

    private fun decodeHeaderByte4(b: Byte): Boolean {
        val mode_idx = (b.toInt() shr 6) and 0x03
        mode = MODES_TABLE[mode_idx]
        return true
    }

    // NOTE: apparently this includes the 4-byte header
    private fun calcFrameLength(): Int {
        val padding = if (padded!!) freq_hz!!-1 else 0
        // val padding = 0         //TEMP
        return (144 * (1000 * br_kbps!!) + padding) / (freq_hz!!)
    }

    override fun step(c: ByteBuffer) {
        while (c.hasRemaining()) {
            Log.d("APPDEBUG", "mp3 state ${stt}, rem ${c.remaining()}")
            when (stt) {
                State.HEADER -> {
                    val b = c.get()
                    val valid = when (rem) {
                        4 -> b == 0xFF.toByte() // Sync
                        3 -> b == 0xFB.toByte() // MPEG-1, Layer 3, No CRC
                        2 -> decodeHeaderByte3(b)
                        1 -> decodeHeaderByte4(b)
                        else -> false // should be unreachable
                    }
                    hdr.put(4 - rem, b)
                    if (!valid) {
                        stt = State.RESYNC
                        rem = 2
                        continue
                    }
                    if (--rem == 0) {
                        cb.onFormat(br_kbps!!, freq_hz!!, mode!!)
                        cb.onPayload(hdr.slice())
                        stt = State.PAYLOAD
                        rem = calcFrameLength() - 4
                    }
                }
                State.PAYLOAD -> {
                    val n = min(c.remaining(), rem)
                    cb.onPayload(c.slice().limit(n) as ByteBuffer)
                    c.position(c.position() + n)
                    rem -= n
                    if (rem == 0) {
                        cb.onFrameDone()
                        stt = State.HEADER
                        rem = 4
                    }
                }
                State.RESYNC -> {
                    when (rem) {
                        2 -> {
                            while (c.hasRemaining()) {
                                if (c.get() == 0xFF.toByte()) {
                                    rem--
                                    break
                                }
                            }
                        }
                        1 -> {
                            if (c.get() == 0xFB.toByte()) {
                                c.position(c.position() - 2)
                                stt = State.HEADER
                                rem = 4
                            } else {
                                c.position(c.position() - 1)
                                rem = 2
                            }
                        }
                    }
                }
            }
        }
        Log.d("APPDEBUG", "mp3 done")
    }

}
