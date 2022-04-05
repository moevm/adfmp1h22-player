package com.github.moevm.adfmp1h22_player

import java.io.InputStream
import java.nio.ByteBuffer

import org.junit.Test
import org.junit.Assert.*

class Mp3DecoderTest {
    @Test
    fun processSomeBlocks() {
        val BLKSZ = 836
        val NBLK = 10
        val SIZE = BLKSZ*NBLK

        val blk = ByteBuffer.allocate(SIZE)

        val res: InputStream = ClassLoader.getSystemClassLoader()
            .getResourceAsStream("test.mp3")
        while (blk.hasRemaining()) {
            val n = res.read(blk.array())
            blk.position(n + blk.position())
        }
        blk.position(0)

        var x = 0
        var c = 0

        val fsm = Mp3HeaderDecoderFSM(
            object : Mp3HeaderDecoderFSM.Callback {
                override fun onFormat(frame_len: Int, freq_hz: Int,
                                      mode: Mp3HeaderDecoderFSM.Mode) {
                    assertEquals(freq_hz, 44100)
                    assertEquals(mode, Mp3HeaderDecoderFSM.Mode.JOINT_STEREO)
                }

                override fun onPayload(c: ByteBuffer) {
                    x += c.remaining()
                }

                override fun onFrameDone() {
                    c++
                    assertTrue(x >= BLKSZ-1 && x <= BLKSZ)
                    x = 0
                }
            }
        )

        fsm.step(blk.slice())

        assertEquals(c, NBLK)
    }

    @Test
    fun compareContents() {
        val first_bytes = arrayOf(
            0xe8-256,
            0xec-256,
            0xeb-256,
        )
        var checked = false
        var idx = 0

        val blk = ByteBuffer.allocate(1400) // known file size in bytes
        val res: InputStream = ClassLoader.getSystemClassLoader()
            .getResourceAsStream("bad3.mp3")
        while (blk.hasRemaining()) {
            val n = res.read(blk.array())
            blk.position(n + blk.position())
        }
        blk.rewind()

        var idx_in_frame = 0

        val fsm = Mp3HeaderDecoderFSM(
            object : Mp3HeaderDecoderFSM.Callback {
                override fun onFormat(frame_len: Int, freq_hz: Int,
                                      mode: Mp3HeaderDecoderFSM.Mode) {
                    assertEquals(frame_len, 418) // known frame size
                    idx_in_frame = 0
                }

                override fun onPayload(c: ByteBuffer) {
                    while (c.hasRemaining()) {
                        when {
                            idx_in_frame < 4 -> {
                                c.position(c.position() + (4 - idx_in_frame))
                                idx_in_frame = 4
                            }
                            idx_in_frame == 4 -> {
                                idx_in_frame++
                                val b = c.get()
                                assertFalse(checked)
                                assertEquals(b, first_bytes[idx].toByte())
                                checked = true
                            }
                            else -> {
                                idx_in_frame += c.remaining()
                                c.position(c.limit())
                            }
                        }
                    }
                }

                override fun onFrameDone() {
                    assertTrue(checked)
                    idx++
                    checked = false
                }
            }
        )
        val fsm1 = IcyMetaDataDecoderFSM(
            8192, object : IcyMetaDataDecoderFSM.Callback {
                override fun onPayload(c: ByteBuffer) {
                    fsm.step(c)
                }

                override fun onMetaData(s: String) {
                    assertTrue(false)
                }
            }
        )

        fsm1.step(blk.slice())

        assertEquals(idx, 2)
    }

    @Test
    fun checkFrameLengths() {
        val blk = ByteBuffer.allocate(84037) // known file size in bytes
        val res: InputStream = ClassLoader.getSystemClassLoader()
            .getResourceAsStream("yb.bin")
        while (blk.hasRemaining()) {
            val n = res.read(blk.array())
            blk.position(n + blk.position())
        }
        blk.rewind()

        // val SZ = 1400
        val sizes = arrayOf(1400, 1400, 1448, 1448, 1304, 1448, 1448, 1448, 1448, 1448, 1448, 1448, 1448, 1448, 1448, 1448)
        var I = 0

        var len = 0
        var n = 0
        var is_short = false

        val fsm = Mp3HeaderDecoderFSM(
            object : Mp3HeaderDecoderFSM.Callback {
                override fun onFormat(frame_len: Int, freq_hz: Int,
                                      mode: Mp3HeaderDecoderFSM.Mode) {
                    is_short = frame_len != 418
                    len = 0
                }

                override fun onPayload(c: ByteBuffer) {
                    if (len == 0) {
                        val bi = c.get(2).toInt()
                        val padded = (bi shr 1) and 0x01 == 0x01
                        assertEquals(padded, !is_short)
                        // short -> -112, long -> -110
                        // println(c.array().toList())
                    }
                    len += c.remaining()
                }

                override fun onFrameDone() {
                    assertEquals(len, if (is_short) 417 else 418)
                    n++
                }
            }
        )
        val fsm1 = IcyMetaDataDecoderFSM(
            16000, object : IcyMetaDataDecoderFSM.Callback {
                override fun onPayload(c: ByteBuffer) {
                    fsm.step(c)
                }

                override fun onMetaData(s: String) {
                    // ignore
                }
            }
        )

        while (n < 18) {
            val sz = sizes[I]
            I++
            fsm1.step(blk.slice().limit(sz) as ByteBuffer)
            blk.position(blk.position() + sz)
        }

    }
}
