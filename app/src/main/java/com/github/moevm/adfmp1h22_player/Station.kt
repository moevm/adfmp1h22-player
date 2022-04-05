package com.github.moevm.adfmp1h22_player

import android.os.Parcelable
//<<<<<<< HEAD
//import com.google.gson.annotations.SerializedName
//import kotlinx.android.parcel.Parcelize
//import java.io.Serializable
//
//@Parcelize
//data class Station(
//    val changeuuid: String,
//    val name: String,
//    val faviconUrl: String
//    ): Parcelable
//=======

import kotlinx.android.parcel.Parcelize

@Parcelize
data class Station(val changeuuid: String,
                   val name: String,
                   val streamUrl: String,
                   val faviconUrl: String) : Parcelable
//>>>>>>> main
