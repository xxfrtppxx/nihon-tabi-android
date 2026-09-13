package com.nihontabi.android.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Reference data cached from `GET /geo/prefectures` — ~47 rows, refreshed on each successful fetch. */
@Entity(tableName = "prefectures")
data class PrefectureEntity(
    @PrimaryKey val id: Int,
    val nameJa: String,
    val nameEn: String,
    val region: String,
    val centroidLat: Double?,
    val centroidLng: Double?,
    val municipalityCount: Int,
)

/** Reference data cached from `GET /geo/prefectures/:id/municipalities` — fetched lazily per prefecture. */
@Entity(tableName = "municipalities")
data class MunicipalityEntity(
    @PrimaryKey val id: Int,
    val prefectureId: Int,
    val nameJa: String,
    val nameEn: String,
    val type: String,
    val centroidLat: Double?,
    val centroidLng: Double?,
)

/**
 * Raw GeoJSON boundary files, cached permanently once fetched (they almost
 * never change) — `key` is `"prefectures"` for the country file or
 * `"municipalities:<prefectureId>"` for a per-prefecture file.
 */
@Entity(tableName = "geo_json_cache")
data class GeoJsonCacheEntity(
    @PrimaryKey val key: String,
    val json: String,
)
