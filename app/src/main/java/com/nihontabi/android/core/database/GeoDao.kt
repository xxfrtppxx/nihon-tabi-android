package com.nihontabi.android.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GeoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrefectures(prefectures: List<PrefectureEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMunicipalities(municipalities: List<MunicipalityEntity>)

    @Query("SELECT * FROM prefectures ORDER BY id ASC")
    fun observePrefectures(): Flow<List<PrefectureEntity>>

    @Query("SELECT * FROM prefectures ORDER BY id ASC")
    suspend fun getPrefecturesOnce(): List<PrefectureEntity>

    @Query("SELECT * FROM prefectures WHERE id = :id")
    suspend fun getPrefecture(id: Int): PrefectureEntity?

    @Query("SELECT * FROM municipalities WHERE prefectureId = :prefectureId ORDER BY id ASC")
    fun observeMunicipalities(prefectureId: Int): Flow<List<MunicipalityEntity>>

    @Query("SELECT * FROM municipalities WHERE prefectureId = :prefectureId ORDER BY id ASC")
    suspend fun getMunicipalitiesOnce(prefectureId: Int): List<MunicipalityEntity>

    @Query("SELECT * FROM municipalities WHERE id = :id")
    suspend fun getMunicipality(id: Int): MunicipalityEntity?

    @Query("SELECT * FROM municipalities")
    suspend fun getAllMunicipalitiesOnce(): List<MunicipalityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putGeoJson(entity: GeoJsonCacheEntity)

    @Query("SELECT * FROM geo_json_cache WHERE `key` = :key")
    suspend fun getGeoJson(key: String): GeoJsonCacheEntity?
}
