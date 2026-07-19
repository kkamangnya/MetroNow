package com.takji.metronow.domain.model

enum class Direction {
    UP,
    DOWN,
    INNER,
    OUTER;

    fun label(line: MetroLine): String = when {
        line == MetroLine.LINE_2 && this == INNER -> "내선순환"
        line == MetroLine.LINE_2 && this == OUTER -> "외선순환"
        this == UP -> "상행"
        this == DOWN -> "하행"
        else -> name
    }

    fun matches(apiDirection: String?): Boolean = when (this) {
        UP -> apiDirection?.contains("상행") == true
        DOWN -> apiDirection?.contains("하행") == true
        INNER -> apiDirection?.contains("내선") == true
        OUTER -> apiDirection?.contains("외선") == true
    }

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        INNER -> OUTER
        OUTER -> INNER
    }

    companion object {
        fun optionsFor(line: MetroLine): List<Direction> =
            if (line == MetroLine.LINE_2) listOf(INNER, OUTER) else listOf(UP, DOWN)
    }
}
