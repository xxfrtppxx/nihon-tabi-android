package com.nihontabi.android.core.geo

import com.nihontabi.android.core.database.GeoDao
import com.nihontabi.android.core.database.GeoJsonCacheEntity
import com.nihontabi.android.core.database.MunicipalityEntity
import com.nihontabi.android.core.database.PrefectureEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reference geo data (prefectures/municipalities rows + their boundary
 * GeoJSON) is fetched once and cached in Room permanently — it almost never
 * changes server-side, matching the "Offline strategy" §SYSTEM_DESIGN.md.
 */
@Singleton
class GeoRepository @Inject constructor(
    private val geoApi: GeoApi,
    private val geoDao: GeoDao,
    private val json: Json,
) {
    fun observePrefectures(): Flow<List<PrefectureEntity>> = geoDao.observePrefectures()

    suspend fun ensurePrefecturesLoaded() {
        if (geoDao.getPrefecturesOnce().isEmpty()) refreshPrefectures()
    }

    suspend fun refreshPrefectures() {
        val remote = geoApi.prefectures()
        geoDao.insertPrefectures(remote.map { it.toEntity() })
    }

    suspend fun getPrefecture(id: Int): PrefectureEntity? = geoDao.getPrefecture(id)

    fun observeMunicipalities(prefectureId: Int): Flow<List<MunicipalityEntity>> =
        geoDao.observeMunicipalities(prefectureId)

    suspend fun ensureMunicipalitiesLoaded(prefectureId: Int) {
        if (geoDao.getMunicipalitiesOnce(prefectureId).isEmpty()) {
            val remote = geoApi.municipalities(prefectureId)
            geoDao.insertMunicipalities(remote.map { it.toEntity() })
        }
    }

    suspend fun getMunicipality(id: Int): MunicipalityEntity? = geoDao.getMunicipality(id)

    suspend fun getPrefecturesGeoJson(): GeoFeatureCollection = getOrFetchGeoJson("prefectures") {
        geoApi.prefecturesGeoJson()
    }

    suspend fun getMunicipalitiesGeoJson(prefectureId: Int): GeoFeatureCollection =
        getOrFetchGeoJson("municipalities:$prefectureId") {
            geoApi.municipalitiesGeoJson(prefectureId)
        }

    private suspend fun getOrFetchGeoJson(
        key: String,
        fetch: suspend () -> GeoFeatureCollection,
    ): GeoFeatureCollection {
        geoDao.getGeoJson(key)?.let { cached ->
            return json.decodeFromString(GeoFeatureCollection.serializer(), cached.json)
        }
        val remote = fetch()
        val text = json.encodeToString(GeoFeatureCollection.serializer(), remote)
        geoDao.putGeoJson(GeoJsonCacheEntity(key, text))
        return remote
    }
}

private fun PrefectureDto.toEntity() = PrefectureEntity(
    id = id,
    nameJa = nameJa,
    nameEn = nameEn,
    region = region,
    centroidLat = centroidLat,
    centroidLng = centroidLng,
    municipalityCount = municipalityCount,
)

private fun MunicipalityDto.toEntity() = MunicipalityEntity(
    id = id,
    prefectureId = prefectureId,
    nameJa = nameJa,
    nameEn = nameEn,
    type = type,
    centroidLat = centroidLat,
    centroidLng = centroidLng,
)
