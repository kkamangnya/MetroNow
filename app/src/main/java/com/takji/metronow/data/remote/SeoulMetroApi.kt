package com.takji.metronow.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface SeoulMetroApi {
    @GET("{apiKey}/json/realtimeStationArrival/0/20/{stationName}")
    suspend fun realtimeStationArrival(
        @Path("apiKey") apiKey: String,
        @Path("stationName") stationName: String,
    ): MetroArrivalResponse
}
