package com.github.moevm.adfmp1h22_player

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Station(val changeuuid: String,
                   val stationuuid: String,
                   val name: String,
                   val streamUrl: String,
                   val codec: String,
                   val homepage: String,
                   val country: String,
                   val faviconUrl: String) : Parcelable
