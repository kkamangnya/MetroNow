package com.takji.metronow

import com.takji.metronow.data.remote.MetroArrivalDto
import com.takji.metronow.data.remote.MetroArrivalMapper
import com.takji.metronow.data.remote.MetroArrivalResponse
import com.takji.metronow.data.remote.SeoulApiMessageDto
import com.takji.metronow.data.repository.MetroRemoteDataSource
import com.takji.metronow.data.repository.MetroRepository
import com.takji.metronow.domain.model.Direction
import com.takji.metronow.domain.model.MetroLine
import com.takji.metronow.domain.model.MetroPreset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetroRepositoryTest {
    private val preset = MetroPreset(
        id = "test",
        name = "출근",
        line = MetroLine.LINE_2,
        stationId = "2-강남",
        stationDisplayName = "강남역",
        stationApiName = "강남",
        direction = Direction.OUTER,
    )

    @Test
    fun filtersPrimaryAndOppositeDirections() = runTest {
        val remote = FakeRemote(
            MetroArrivalResponse(
                errorMessage = SeoulApiMessageDto(code = "INFO-000", message = "정상 처리되었습니다."),
                arrivals = listOf(
                    arrival("하행/외선", "60", "2257"),
                    arrival("내선", "180", "2202"),
                    arrival("하행/외선", "300", "2261"),
                    arrival("상행", "10", "other", subwayId = "1007"),
                ),
            ),
        )
        val repository = MetroRepository(remote, MetroArrivalMapper())

        val snapshot = repository.fetchSnapshot(preset, "sample", nowMillis = System.currentTimeMillis())

        assertEquals(listOf("2257", "2261"), snapshot.primary.map { it.trainNumber })
        assertEquals(listOf("2202"), snapshot.opposite.map { it.trainNumber })
        assertEquals("강남", remote.station)
    }

    @Test
    fun returnsKeyMissingWithoutNetworkRequest() = runTest {
        val remote = FakeRemote(MetroArrivalResponse())
        val repository = MetroRepository(remote, MetroArrivalMapper())

        val snapshot = repository.fetchSnapshot(preset, "")

        assertTrue(snapshot.apiKeyMissing)
        assertEquals("앱에서 API 키를 설정하세요", snapshot.errorMessage)
        assertEquals(0, remote.calls)
    }

    @Test
    fun convertsApiErrorToUiState() = runTest {
        val repository = MetroRepository(
            FakeRemote(MetroArrivalResponse(errorMessage = SeoulApiMessageDto(code = "ERROR-301", message = "KEY 오류"))),
            MetroArrivalMapper(),
        )

        val snapshot = repository.fetchSnapshot(preset, "bad-key")

        assertEquals("KEY 오류", snapshot.errorMessage)
        assertTrue(snapshot.primary.isEmpty())
    }

    private fun arrival(direction: String, seconds: String, train: String, subwayId: String = "1002") = MetroArrivalDto(
        subwayId = subwayId,
        updnLine = direction,
        trainLineNm = "성수행 - 잠실방면",
        statnNm = "강남",
        btrainNo = train,
        barvlDt = seconds,
        arvlMsg2 = "운행 중",
        arvlCd = "99",
    )

    private class FakeRemote(private val response: MetroArrivalResponse) : MetroRemoteDataSource {
        var calls: Int = 0
        var station: String? = null
        override suspend fun arrivals(apiKey: String, stationApiName: String): MetroArrivalResponse {
            calls++
            station = stationApiName
            return response
        }
    }
}
