package com.takji.metronow.domain.model

import kotlin.math.max

enum class ArrivalPosition(val progress: Float) {
    TWO_OR_MORE_STATIONS_AWAY(0.12f),
    PREVIOUS_STATION_APPROACHING(0.26f),
    PREVIOUS_STATION_ARRIVED(0.32f),
    PREVIOUS_STATION_DEPARTED(0.39f),
    APPROACHING(0.47f),
    ARRIVED(0.50f),
    DEPARTED(0.68f),
    RUNNING(0.18f),
    UNKNOWN(0.50f),
}

data class MetroArrival(
    val line: MetroLine,
    val stationName: String,
    val direction: Direction,
    val trainNumber: String,
    val destination: String,
    val trainType: String,
    val statusText: String,
    val detailText: String,
    val arrivalCode: String,
    val secondsUntilArrival: Int?,
    val position: ArrivalPosition,
    val receivedAtMillis: Long,
) {
    fun serviceLabel(): String? = trainType.trim().takeIf { it.isNotEmpty() && it != "일반" }

    fun statusWithService(): String = serviceLabel()?.let { "$it · $statusText" } ?: statusText

    fun etaText(nowMillis: Long = System.currentTimeMillis()): String {
        val original = secondsUntilArrival ?: return statusText.ifBlank { "운행 중" }
        val elapsed = max(0L, (nowMillis - receivedAtMillis) / 1_000L).toInt()
        val seconds = max(0, original - elapsed)
        return when {
            seconds <= 20 -> "곧 도착"
            seconds < 60 -> "${seconds}초"
            else -> "${(seconds + 59) / 60}분"
        }
    }
}

data class ArrivalSnapshot(
    val presetId: String,
    val fetchedAtMillis: Long = 0L,
    val primary: List<MetroArrival> = emptyList(),
    val opposite: List<MetroArrival> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val apiKeyMissing: Boolean = false,
) {
    fun ageText(nowMillis: Long = System.currentTimeMillis()): String {
        if (fetchedAtMillis <= 0L) return "업데이트 전"
        val seconds = max(0L, (nowMillis - fetchedAtMillis) / 1_000L)
        return when {
            seconds < 10 -> "방금"
            seconds < 60 -> "${seconds}초 전"
            seconds < 3_600 -> "${seconds / 60}분 전"
            else -> "오래된 정보"
        }
    }

    fun isStale(nowMillis: Long = System.currentTimeMillis()): Boolean =
        fetchedAtMillis > 0L && nowMillis - fetchedAtMillis > 3 * 60_000L
}
