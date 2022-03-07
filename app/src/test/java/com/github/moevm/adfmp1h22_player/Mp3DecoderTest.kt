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
                override fun onFormat(br_kbps: Int, freq_hz: Int,
                                      mode: Mp3HeaderDecoderFSM.Mode) {
                    assertEquals(br_kbps, 256)
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
}
