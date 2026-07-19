package com.takji.metronow.data.local

import android.content.Context
import com.google.gson.Gson
import com.takji.metronow.domain.model.Direction
import com.takji.metronow.domain.model.DirectionDescription
import com.takji.metronow.domain.model.MetroLine
import com.takji.metronow.domain.model.Station
import com.takji.metronow.domain.model.StationNeighbors

class StationCatalog(context: Context, gson: Gson = Gson()) {
    private val stations: List<Station>

    init {
        val json = context.assets.open("seoul_metro_stations.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val asset = gson.fromJson(json, StationAsset::class.java)
        stations = asset.lines.flatMap { line ->
            line.stations.mapIndexed { index, item ->
                Station(
                    id = "${line.number}-${item.name}",
                    lineNumber = line.number,
                    displayName = if (item.name.endsWith("역")) item.name else "${item.name}역",
                    apiName = item.apiName ?: item.name.removeSuffix("역"),
                    order = index,
                )
            }
        }
    }

    fun stationsFor(line: MetroLine, query: String = ""): List<Station> {
        val normalized = query.trim().removeSuffix("역")
        return stations.filter { station ->
            station.line == line && (
                normalized.isBlank() ||
                    station.displayName.contains(normalized, ignoreCase = true) ||
                    station.apiName.contains(normalized, ignoreCase = true)
                )
        }
    }

    fun station(id: String): Station? = stations.firstOrNull { it.id == id }

    fun station(line: MetroLine, apiName: String): Station? =
        stations.firstOrNull { it.line == line && it.apiName == apiName }

    fun neighbors(stationId: String, direction: Direction): StationNeighbors? {
        val current = station(stationId) ?: return null
        val list = stationsFor(current.line)
        val index = list.indexOfFirst { it.id == current.id }.takeIf { it >= 0 } ?: return null
        val loops = current.line == MetroLine.LINE_2
        fun at(raw: Int): Station = when {
            loops -> list[(raw + list.size) % list.size]
            else -> list[raw.coerceIn(0, list.lastIndex)]
        }
        val forward = direction == Direction.DOWN || direction == Direction.INNER
        val previous = if (forward) at(index - 1) else at(index + 1)
        val next = if (forward) at(index + 1) else at(index - 1)
        return StationNeighbors(previous, current, next)
    }

    fun directionDescriptions(stationId: String): List<DirectionDescription> {
        val current = station(stationId) ?: return emptyList()
        return Direction.optionsFor(current.line).map { direction ->
            DirectionDescription(
                direction = direction,
                title = direction.label(current.line),
                subtitle = "→ ${directionHint(stationId, direction)}",
            )
        }
    }

    fun directionHint(stationId: String, direction: Direction): String {
        val current = station(stationId) ?: return "방면 정보 없음"
        if (current.line == MetroLine.LINE_2) {
            val hubs = setOf("잠실", "성수", "왕십리", "시청", "홍대입구", "신도림", "사당", "교대", "강남")
            val found = mutableListOf<String>()
            var cursor = current
            repeat(stationsFor(current.line).size - 1) {
                cursor = neighbors(cursor.id, direction)?.next ?: cursor
                val name = cursor.displayName.removeSuffix("역")
                if (name in hubs && name !in found) found += name
            }
            if (found.isNotEmpty()) return "${found.take(2).joinToString(" · ")} 방면"
        }
        val adjacent = neighbors(stationId, direction) ?: return "방면 정보 없음"
        return "${adjacent.next.displayName.removeSuffix("역")} · ${step(adjacent.next, direction, 2).displayName.removeSuffix("역")} 방면"
    }

    private fun step(station: Station, direction: Direction, count: Int): Station {
        var result = station
        repeat(count - 1) {
            result = neighbors(result.id, direction)?.next ?: result
        }
        return result
    }

    private data class StationAsset(val lines: List<LineAsset>)
    private data class LineAsset(val number: String, val stations: List<StationAssetItem>)
    private data class StationAssetItem(val name: String, val apiName: String? = null)
}
