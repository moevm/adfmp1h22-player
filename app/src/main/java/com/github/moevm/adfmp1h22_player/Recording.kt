package com.github.moevm.adfmp1h22_player

import java.util.UUID

import android.os.Parcelable

import kotlinx.android.parcel.Parcelize

data class Recording(val uuid: UUID,
                     val metadata: TrackMetaData,
                     // TODO: timestamp
                     // TODO: duration
)
