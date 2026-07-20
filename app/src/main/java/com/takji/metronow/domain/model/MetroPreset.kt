package com.takji.metronow.domain.model

import java.util.UUID

data class MetroPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val line: MetroLine,
    val stationId: String,
    val stationDisplayName: String,
    val stationApiName: String,
    val direction: Direction,
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    val branch: MetroBranch
        get() = MetroBranch.fromStationId(stationId)

    fun directionLabel(value: Direction = direction): String = branch.directionLabel(value, line)

    fun routeDisplayName(): String = branch.displayName?.let { "${line.displayName} $it" } ?: line.displayName
}
