package com.github.moevm.adfmp1h22_player

import java.util.*
import com.google.gson.annotations.SerializedName

data class AddStationItem (

    @SerializedName("changeuuid")
    val changeuuid: UUID? = null,

    @SerializedName("stationuuid")
    val stationuuid: UUID? = null,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("url")
    val url: String? = null,

    @SerializedName("url_resolved")
    val url_resolved: String? = null,

    @SerializedName("homepage")
    val homepage: String? = null,

    @SerializedName("favicon")
    val favicon: String? = null,

    @SerializedName("tags")
    val tags: String? = null,

    @SerializedName("country")
    val country: String? = null,

    @SerializedName("countrycode")
    val countrycode: String? = null,

    @SerializedName("iso_3166_2")
    val iso_3166_2: String? = null,

    @SerializedName("state")
    val state: String? = null,

    @SerializedName("language")
    val language: String? = null,

    @SerializedName("languagecodes")
    val languagecodes: String? = null,

    @SerializedName("votes")
    val votes: Int? = null,

    @SerializedName("lastchangetime")
    val lastchangetime: String? = null, // YYYY-MM-DD HH:mm:ss

    @SerializedName("lastchangetime_iso8601")
    val lastchangetime_iso8601: String? = null,

    @SerializedName("codec")
    val codec: String? = null,

    @SerializedName("bitrate")
    val bitrate: Int? = null,

    @SerializedName("hls")
    val hls: Int? = null,

    @SerializedName("lastcheckok")
    val lastcheckok: Int? = null,

    @SerializedName("lastchecktime")
    val lastchecktime: String? = null ,// YYYY-MM-DD HH:mm:ss

    @SerializedName("lastchecktime_iso8601")
    val lastchecktime_iso8601: String? = null,

    @SerializedName("lastcheckoktime")
    val lastcheckoktime: String? = null ,// YYYY-MM-DD HH:mm:ss

    @SerializedName("lastcheckoktime_iso8601")
    val lastcheckoktime_iso8601: String? = null,

    @SerializedName("lastlocalchecktime")
    val lastlocalchecktime: String? = null ,// YYYY-MM-DD HH:mm:ss

    @SerializedName("lastlocalchecktime_iso8601")
    val lastlocalchecktime_iso8601: String? = null,

    @SerializedName("clicktimestamp")
    val clicktimestamp: String? = null, // YYYY-MM-DD HH:mm:ss

    @SerializedName("clicktimestamp_iso8601")
    val clicktimestamp_iso8601: String? = null,

    @SerializedName("clickcount")
    val clickcount: Int? = null,

    @SerializedName("clicktrend")
    val clicktrend: Int? = null,

    @SerializedName("ssl_error")
    val ssl_error: Int? = null,

    @SerializedName("geo_lat")
    val geo_lat: Double? = null,

    @SerializedName("geo_long")
    val geo_long: Double? = null,

    @SerializedName("has_extended_info")
    val has_extended_info: Boolean? = null

)