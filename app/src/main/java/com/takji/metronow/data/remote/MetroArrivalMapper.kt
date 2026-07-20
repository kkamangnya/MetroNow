package com.takji.metronow.data.remote

import com.takji.metronow.domain.model.ArrivalPosition
import com.takji.metronow.domain.model.Direction
import com.takji.metronow.domain.model.MetroArrival
import com.takji.metronow.domain.model.MetroBranch
import com.takji.metronow.domain.model.MetroLine
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MetroArrivalMapper {
    private val receivedAtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val seoulZone = ZoneId.of("Asia/Seoul")

    fun map(
        dto: MetroArrivalDto,
        expectedLine: MetroLine,
        nowMillis: Long,
        branch: MetroBranch = MetroBranch.MAIN,
    ): MetroArrival? {
        if (dto.subwayId != expectedLine.apiId) return null
        val direction = direction(dto.updnLine, expectedLine, branch) ?: return null
        val receivedAt = parseReceivedAt(dto.recptnDt) ?: nowMillis
        val status = dto.arvlMsg2.orEmpty().ifBlank { dto.arvlMsg3.orEmpty() }.ifBlank { "운행 중" }
        return MetroArrival(
            line = expectedLine,
            stationName = dto.statnNm.orEmpty(),
            direction = direction,
            trainNumber = dto.btrainNo.orEmpty(),
            destination = dto.trainLineNm?.substringBefore(" -")?.ifBlank { null }
                ?: dto.bstatnNm?.let { "${it}행" }.orEmpty().ifBlank { "행선지 미상" },
            trainType = dto.btrainSttus.orEmpty().ifBlank { "일반" },
            statusText = status,
            detailText = dto.arvlMsg3.orEmpty(),
            arrivalCode = dto.arvlCd.orEmpty(),
            secondsUntilArrival = dto.barvlDt?.toIntOrNull()?.takeIf { it >= 0 },
            position = position(dto.arvlCd, status),
            receivedAtMillis = receivedAt,
        )
    }

    private fun direction(raw: String?, line: MetroLine, branch: MetroBranch): Direction? {
        val value = raw.orEmpty()
        return when {
            line == MetroLine.LINE_2 && branch != MetroBranch.MAIN &&
                (value.contains("상행") || value.contains("내선")) -> Direction.UP
            line == MetroLine.LINE_2 && branch != MetroBranch.MAIN &&
                (value.contains("하행") || value.contains("외선")) -> Direction.DOWN
            line == MetroLine.LINE_2 && value.contains("내선") -> Direction.INNER
            line == MetroLine.LINE_2 && value.contains("외선") -> Direction.OUTER
            value.contains("상행") -> Direction.UP
            value.contains("하행") -> Direction.DOWN
            else -> null
        }
    }

    private fun position(code: String?, message: String): ArrivalPosition = when (code) {
        "0" -> ArrivalPosition.APPROACHING
        "1" -> ArrivalPosition.ARRIVED
        "2" -> ArrivalPosition.DEPARTED
        "3" -> ArrivalPosition.PREVIOUS_STATION_DEPARTED
        "4" -> ArrivalPosition.PREVIOUS_STATION_APPROACHING
        "5" -> ArrivalPosition.PREVIOUS_STATION_ARRIVED
        "99" -> when {
            Regex("[2-9]전역").containsMatchIn(message) || Regex("\\d{2,}전역").containsMatchIn(message) ->
                ArrivalPosition.TWO_OR_MORE_STATIONS_AWAY
            message.contains("전역") -> ArrivalPosition.PREVIOUS_STATION_APPROACHING
            else -> ArrivalPosition.RUNNING
        }
        else -> ArrivalPosition.UNKNOWN
    }

    private fun parseReceivedAt(raw: String?): Long? = runCatching {
        LocalDateTime.parse(raw, receivedAtFormatter).atZone(seoulZone).toInstant().toEpochMilli()
    }.getOrNull()
}
