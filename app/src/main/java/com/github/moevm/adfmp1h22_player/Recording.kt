package com.github.moevm.adfmp1h22_player

import java.util.UUID
import java.time.Instant

import android.os.Parcelable

import kotlinx.android.parcel.Parcelize

@Parcelize
data class Recording(val uuid: UUID,
                     val metadata: TrackMetaData,
                     val timestamp: Instant,
                     val mime: String,
                     var state: Int,
                     // TODO: duration
                     // TODO: station info
): Parcelable {
    companion object {
        const val STATE_RECORDING = 0
        const val STATE_DONE = 1
        const val STATE_SAVED =2
    }
}
