package com.takji.metronow

import com.takji.metronow.data.remote.MetroArrivalDto
import com.takji.metronow.data.remote.MetroArrivalMapper
import com.takji.metronow.domain.model.ArrivalPosition
import com.takji.metronow.domain.model.Direction
import com.takji.metronow.domain.model.MetroLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MetroArrivalMapperTest {
    private val mapper = MetroArrivalMapper()

    @Test
    fun mapsOuterLineArrivalAndPosition() {
        val now = LocalDateTime.of(2026, 7, 20, 8, 0, 30)
            .atZone(ZoneId.of("Asia/Seoul"))
            .toInstant()
            .toEpochMilli()
        val dto = MetroArrivalDto(
            subwayId = "1002",
            updnLine = "하행/외선",
            trainLineNm = "성수행 - 잠실방면",
            statnNm = "강남",
            btrainNo = "2257",
            btrainSttus = "일반",
            barvlDt = "120",
            recptnDt = "2026-07-20 08:00:00",
            arvlMsg2 = "전역 출발",
            arvlMsg3 = "역삼 출발",
            arvlCd = "3",
        )

        val result = requireNotNull(mapper.map(dto, MetroLine.LINE_2, now))

        assertEquals(Direction.OUTER, result.direction)
        assertEquals("성수행", result.destination)
        assertEquals(ArrivalPosition.PREVIOUS_STATION_DEPARTED, result.position)
        assertEquals("2분", result.etaText(now))
    }

    @Test
    fun rejectsOtherLineAndNegativeEta() {
        val otherLine = MetroArrivalDto(subwayId = "1007", updnLine = "상행")
        assertNull(mapper.map(otherLine, MetroLine.LINE_2, System.currentTimeMillis()))

        val negative = MetroArrivalDto(
            subwayId = "1002",
            updnLine = "내선",
            barvlDt = "-30",
            arvlMsg2 = "운행 중",
            recptnDt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
        )
        val mapped = requireNotNull(mapper.map(negative, MetroLine.LINE_2, System.currentTimeMillis()))
        assertNull(mapped.secondsUntilArrival)
        assertEquals("운행 중", mapped.etaText())
    }

    @Test
    fun mapsTwoStationsAwayWithoutInventingCoordinates() {
        val dto = MetroArrivalDto(
            subwayId = "1002",
            updnLine = "내선",
            barvlDt = "360",
            arvlMsg2 = "2전역 전",
            arvlCd = "99",
        )
        val result = requireNotNull(mapper.map(dto, MetroLine.LINE_2, System.currentTimeMillis()))
        assertEquals(ArrivalPosition.TWO_OR_MORE_STATIONS_AWAY, result.position)
        assertEquals("2전역 전", result.statusText)
    }
}
