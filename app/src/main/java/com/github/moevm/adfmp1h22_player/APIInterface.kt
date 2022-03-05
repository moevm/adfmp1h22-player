package com.github.moevm.adfmp1h22_player

import retrofit2.Call
import retrofit2.http.GET

interface APIInterface {
    @GET("json/stations/byname/jazz")
    fun AddStationListResources(): Call<AddStationList?>?
}
