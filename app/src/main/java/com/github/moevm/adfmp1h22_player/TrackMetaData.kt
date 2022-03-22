package com.github.moevm.adfmp1h22_player

import android.os.Parcelable

import kotlinx.android.parcel.Parcelize

@Parcelize
data class TrackMetaData(val original: String,
                         val title: String,
                         val artist: String?) : Parcelable
