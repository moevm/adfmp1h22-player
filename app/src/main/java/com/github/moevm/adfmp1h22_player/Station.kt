package com.github.moevm.adfmp1h22_player

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Station(
    val changeuuid: String,
    val name: String,
    val faviconUrl: String
    ): Parcelable
