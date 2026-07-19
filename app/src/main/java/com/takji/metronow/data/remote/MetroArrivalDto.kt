package com.takji.metronow.data.remote

import com.google.gson.annotations.SerializedName

data class MetroArrivalResponse(
    @SerializedName("errorMessage") val errorMessage: SeoulApiMessageDto? = null,
    @SerializedName("RESULT") val result: SeoulApiResultDto? = null,
    @SerializedName("realtimeArrivalList") val arrivals: List<MetroArrivalDto>? = null,
)

data class SeoulApiMessageDto(
    val status: Int? = null,
    val code: String? = null,
    val message: String? = null,
    val total: Int? = null,
)

data class SeoulApiResultDto(
    @SerializedName("CODE") val code: String? = null,
    @SerializedName("MESSAGE") val message: String? = null,
)

data class MetroArrivalDto(
    val subwayId: String? = null,
    val updnLine: String? = null,
    val trainLineNm: String? = null,
    val statnFid: String? = null,
    val statnTid: String? = null,
    val statnId: String? = null,
    val statnNm: String? = null,
    val ordkey: String? = null,
    val btrainSttus: String? = null,
    val barvlDt: String? = null,
    val btrainNo: String? = null,
    val bstatnId: String? = null,
    val bstatnNm: String? = null,
    val recptnDt: String? = null,
    val arvlMsg2: String? = null,
    val arvlMsg3: String? = null,
    val arvlCd: String? = null,
    val lstcarAt: String? = null,
)
