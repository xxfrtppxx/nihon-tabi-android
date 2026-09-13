package com.nihontabi.android.core.geo

import retrofit2.http.GET
import retrofit2.http.Path

interface GeoApi {
    @GET("geo/prefectures")
    suspend fun prefectures(): List<PrefectureDto>

    @GET("geo/prefectures/{id}/municipalities")
    suspend fun municipalities(@Path("id") prefectureId: Int): List<MunicipalityDto>

    @GET("geo/files/prefectures.geojson")
    suspend fun prefecturesGeoJson(): GeoFeatureCollection

    @GET("geo/files/municipalities/{prefectureId}.geojson")
    suspend fun municipalitiesGeoJson(@Path("prefectureId") prefectureId: Int): GeoFeatureCollection
}
