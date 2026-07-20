package com.takji.metronow.domain.model

data class Station(
    val id: String,
    val lineNumber: String,
    val displayName: String,
    val apiName: String,
    val order: Int,
    val branch: MetroBranch = MetroBranch.MAIN,
) {
    val line: MetroLine
        get() = MetroLine.fromNumber(lineNumber) ?: MetroLine.LINE_2

    val selectionLabel: String
        get() = branch.displayName?.let { "$displayName · $it" } ?: displayName

    val routeDisplayName: String
        get() = branch.displayName?.let { "${line.displayName} $it" } ?: line.displayName
}

enum class MetroBranch(
    val idToken: String?,
    val displayName: String?,
) {
    MAIN(null, null),
    SEONGSU_BRANCH("seongsu-branch", "성수지선"),
    SINJEONG_BRANCH("sinjeong-branch", "신정지선");

    fun stationId(lineNumber: String, stationName: String): String =
        idToken?.let { "$lineNumber-$it-$stationName" } ?: "$lineNumber-$stationName"

    fun directions(line: MetroLine): List<Direction> = when (this) {
        MAIN -> Direction.optionsFor(line)
        SEONGSU_BRANCH, SINJEONG_BRANCH -> listOf(Direction.UP, Direction.DOWN)
    }

    fun directionLabel(direction: Direction, line: MetroLine): String = when (this) {
        MAIN -> direction.label(line)
        SEONGSU_BRANCH -> when (direction) {
            Direction.UP -> "성수 방면"
            Direction.DOWN -> "신설동 방면"
            else -> direction.label(line)
        }
        SINJEONG_BRANCH -> when (direction) {
            Direction.UP -> "신도림 방면"
            Direction.DOWN -> "까치산 방면"
            else -> direction.label(line)
        }
    }

    fun destination(direction: Direction): String? = when (this) {
        MAIN -> null
        SEONGSU_BRANCH -> if (direction == Direction.UP) "성수" else "신설동"
        SINJEONG_BRANCH -> if (direction == Direction.UP) "신도림" else "까치산"
    }

    companion object {
        fun fromStationId(stationId: String): MetroBranch = entries.firstOrNull { branch ->
            branch.idToken?.let { "-$it-" in stationId } == true
        } ?: MAIN
    }
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
