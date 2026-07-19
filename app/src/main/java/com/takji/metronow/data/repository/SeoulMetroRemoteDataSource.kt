package com.takji.metronow.data.repository

import com.takji.metronow.data.remote.MetroArrivalResponse
import com.takji.metronow.data.remote.SeoulMetroApi

class SeoulMetroRemoteDataSource(
    private val api: SeoulMetroApi,
) : MetroRemoteDataSource {
    override suspend fun arrivals(apiKey: String, stationApiName: String): MetroArrivalResponse =
        api.realtimeStationArrival(apiKey.trim(), stationApiName)
}
