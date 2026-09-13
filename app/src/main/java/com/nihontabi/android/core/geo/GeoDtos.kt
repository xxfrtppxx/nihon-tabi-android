package com.nihontabi.android.core.geo

import kotlinx.serialization.Serializable

@Serializable
data class PrefectureDto(
    val id: Int,
    val nameJa: String,
    val nameEn: String,
    val region: String,
    val centroidLat: Double? = null,
    val centroidLng: Double? = null,
    val municipalityCount: Int = 0,
)

@Serializable
data class MunicipalityDto(
    val id: Int,
    val prefectureId: Int,
    val nameJa: String,
    val nameEn: String,
    val type: String,
    val centroidLat: Double? = null,
    val centroidLng: Double? = null,
)
