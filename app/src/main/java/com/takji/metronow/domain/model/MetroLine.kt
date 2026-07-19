package com.takji.metronow.domain.model

enum class MetroLine(
    val apiId: String,
    val number: String,
    val displayName: String,
    val colorHex: Long,
) {
    LINE_1("1001", "1", "1호선", 0xFF0052A4),
    LINE_2("1002", "2", "2호선", 0xFF00A84D),
    LINE_3("1003", "3", "3호선", 0xFFEF7C1C),
    LINE_4("1004", "4", "4호선", 0xFF00A5DE),
    LINE_5("1005", "5", "5호선", 0xFF996CAC),
    LINE_6("1006", "6", "6호선", 0xFFCD7C2F),
    LINE_7("1007", "7", "7호선", 0xFF747F00),
    LINE_8("1008", "8", "8호선", 0xFFE6186C),
    LINE_9("1009", "9", "9호선", 0xFFBDB092);

    companion object {
        fun fromApiId(apiId: String?): MetroLine? = entries.firstOrNull { it.apiId == apiId }
        fun fromNumber(number: String?): MetroLine? = entries.firstOrNull { it.number == number }
    }
}
