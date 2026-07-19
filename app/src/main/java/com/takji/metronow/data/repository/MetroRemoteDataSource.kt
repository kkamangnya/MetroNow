package com.takji.metronow.data.repository

import com.takji.metronow.data.remote.MetroArrivalResponse

interface MetroRemoteDataSource {
    suspend fun arrivals(apiKey: String, stationApiName: String): MetroArrivalResponse
}
