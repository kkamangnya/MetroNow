package com.takji.metronow.domain.model

data class Station(
    val id: String,
    val lineNumber: String,
    val displayName: String,
    val apiName: String,
    val order: Int,
) {
    val line: MetroLine
        get() = MetroLine.fromNumber(lineNumber) ?: MetroLine.LINE_2
}

data class StationNeighbors(
    val previous: Station,
    val current: Station,
    val next: Station,
)

data class DirectionDescription(
    val direction: Direction,
    val title: String,
    val subtitle: String,
)
