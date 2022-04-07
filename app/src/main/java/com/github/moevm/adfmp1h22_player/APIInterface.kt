package com.github.moevm.adfmp1h22_player

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface APIInterface {
    @GET("json/stations/lastchange")
    fun UpdateAddStationListResources(@Query("offest") offset : Int, @Query("limit") limit : Int): Call<AddStationList?>?

    @GET("json/stations")
    fun AddStationListResources(): Call<AddStationList?>?
}
