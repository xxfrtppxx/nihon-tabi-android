package com.nihontabi.android.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PrefectureEntity::class,
        MunicipalityEntity::class,
        GeoJsonCacheEntity::class,
        VisitEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class NihonTabiDatabase : RoomDatabase() {
    abstract fun geoDao(): GeoDao
    abstract fun visitDao(): VisitDao
}
