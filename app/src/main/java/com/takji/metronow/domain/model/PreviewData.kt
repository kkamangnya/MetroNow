package com.takji.metronow.domain.model

object PreviewData {
    val preset = MetroPreset(
        id = "preview-gangnam",
        name = "출근길",
        line = MetroLine.LINE_2,
        stationId = "2-gangnam",
        stationDisplayName = "강남역",
        stationApiName = "강남",
        direction = Direction.OUTER,
    )

    val snapshot = ArrivalSnapshot(
        presetId = preset.id,
        fetchedAtMillis = System.currentTimeMillis(),
        primary = listOf(
            MetroArrival(
                line = MetroLine.LINE_2,
                stationName = "강남",
                direction = Direction.OUTER,
                trainNumber = "2257",
                destination = "성수행",
                trainType = "일반",
                statusText = "전역 출발",
                detailText = "역삼 출발",
                arrivalCode = "3",
                secondsUntilArrival = 112,
                position = ArrivalPosition.PREVIOUS_STATION_DEPARTED,
                receivedAtMillis = System.currentTimeMillis(),
            ),
            MetroArrival(
                line = MetroLine.LINE_2,
                stationName = "강남",
                direction = Direction.OUTER,
                trainNumber = "2261",
                destination = "성수행",
                trainType = "일반",
                statusText = "2전역 전",
                detailText = "선릉 도착",
                arrivalCode = "99",
                secondsUntilArrival = 352,
                position = ArrivalPosition.TWO_OR_MORE_STATIONS_AWAY,
                receivedAtMillis = System.currentTimeMillis(),
            ),
        ),
        opposite = listOf(
            MetroArrival(
                line = MetroLine.LINE_2,
                stationName = "강남",
                direction = Direction.INNER,
                trainNumber = "2202",
                destination = "신도림행",
                trainType = "일반",
                statusText = "진입",
                detailText = "교대 출발",
                arrivalCode = "0",
                secondsUntilArrival = 38,
                position = ArrivalPosition.APPROACHING,
                receivedAtMillis = System.currentTimeMillis(),
            ),
            MetroArrival(
                line = MetroLine.LINE_2,
                stationName = "강남",
                direction = Direction.INNER,
                trainNumber = "2206",
                destination = "홍대입구행",
                trainType = "일반",
                statusText = "2전역 전",
                detailText = "서초 도착",
                arrivalCode = "99",
                secondsUntilArrival = 286,
                position = ArrivalPosition.TWO_OR_MORE_STATIONS_AWAY,
                receivedAtMillis = System.currentTimeMillis(),
            ),
        ),
    )
}
