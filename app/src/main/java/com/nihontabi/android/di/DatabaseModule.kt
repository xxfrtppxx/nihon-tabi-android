package com.nihontabi.android.di

import android.content.Context
import androidx.room.Room
import com.nihontabi.android.core.database.GeoDao
import com.nihontabi.android.core.database.NihonTabiDatabase
import com.nihontabi.android.core.database.VisitDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NihonTabiDatabase =
        Room.databaseBuilder(context, NihonTabiDatabase::class.java, "nihon-tabi.db").build()

    @Provides
    fun provideGeoDao(db: NihonTabiDatabase): GeoDao = db.geoDao()

    @Provides
    fun provideVisitDao(db: NihonTabiDatabase): VisitDao = db.visitDao()
}
