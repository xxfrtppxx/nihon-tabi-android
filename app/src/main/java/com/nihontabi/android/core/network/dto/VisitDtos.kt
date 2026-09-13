package com.nihontabi.android.core.network.dto

import com.nihontabi.android.core.geo.PrefectureDto
import kotlinx.serialization.Serializable

@Serializable
data class VisitPhotoDto(val id: String, val url: String)

@Serializable
data class MunicipalityWithPrefectureDto(
    val id: Int,
    val prefectureId: Int,
    val nameJa: String,
    val nameEn: String,
    val type: String,
    val centroidLat: Double? = null,
    val centroidLng: Double? = null,
    val prefecture: PrefectureDto,
)

/** `status` is always `"visited"` or `"want_to_go"` — see `VisitStatus` in `nihon-tabi-api`'s Prisma schema. */
@Serializable
data class VisitDto(
    val id: String,
    val municipalityId: Int,
    val status: String,
    val visitedOn: String? = null,
    val note: String? = null,
    val rating: Int? = null,
    val photos: List<VisitPhotoDto> = emptyList(),
    val municipality: MunicipalityWithPrefectureDto,
)

@Serializable
data class CreateVisitRequest(
    val municipalityId: Int,
    val status: String,
    val visitedOn: String? = null,
    val note: String? = null,
    val rating: Int? = null,
)

@Serializable
data class UpdateVisitRequest(
    val status: String? = null,
    val visitedOn: String? = null,
    val note: String? = null,
    val rating: Int? = null,
)
