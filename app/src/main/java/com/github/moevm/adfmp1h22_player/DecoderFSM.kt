package com.github.moevm.adfmp1h22_player

import java.nio.ByteBuffer

interface DecoderFSM {
    fun step(c: ByteBuffer)
}
