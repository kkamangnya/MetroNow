package com.takji.metronow.data.repository

import com.takji.metronow.data.remote.MetroArrivalMapper
import com.takji.metronow.domain.model.ArrivalSnapshot
import com.takji.metronow.domain.model.MetroPreset
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class MetroRepository(
    private val remote: MetroRemoteDataSource,
    private val mapper: MetroArrivalMapper,
) {
    suspend fun fetchSnapshot(
        preset: MetroPreset,
        apiKey: String,
        nowMillis: Long = System.currentTimeMillis(),
    ): ArrivalSnapshot {
        if (apiKey.isBlank()) {
            return ArrivalSnapshot(
                presetId = preset.id,
                apiKeyMissing = true,
                errorMessage = "앱에서 API 키를 설정하세요",
            )
        }

        return try {
            val response = remote.arrivals(apiKey, preset.stationApiName)
            val resultCode = response.result?.code ?: response.errorMessage?.code
            if (resultCode != null && resultCode != "INFO-000") {
                throw MetroApiException(response.result?.message ?: response.errorMessage?.message ?: "API 요청 실패")
            }
            val mapped = response.arrivals.orEmpty()
                .mapNotNull { mapper.map(it, preset.line, nowMillis) }
                .sortedWith(compareBy(nullsLast()) { it.secondsUntilArrival })

            val primary = mapped.filter { it.direction == preset.direction }.take(3)
            val opposite = mapped.filter { it.direction == preset.direction.opposite() }.take(3)
            ArrivalSnapshot(
                presetId = preset.id,
                fetchedAtMillis = nowMillis,
                primary = primary,
                opposite = opposite,
                errorMessage = if (primary.isEmpty() && opposite.isEmpty()) "양방향 도착정보가 없습니다" else null,
            )
        } catch (error: Throwable) {
            ArrivalSnapshot(
                presetId = preset.id,
                fetchedAtMillis = nowMillis,
                errorMessage = error.userMessage(),
            )
        }
    }

    private fun Throwable.userMessage(): String = when (this) {
        is UnknownHostException -> "네트워크 연결을 확인하세요"
        is SocketTimeoutException -> "응답 시간이 초과되었습니다"
        is MetroApiException -> message ?: "API 요청에 실패했습니다"
        is IOException -> "정보를 불러올 수 없음"
        else -> "정보를 불러올 수 없음"
    }
}

class MetroApiException(message: String) : Exception(message)
