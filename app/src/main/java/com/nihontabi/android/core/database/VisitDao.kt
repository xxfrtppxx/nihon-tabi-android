package com.nihontabi.android.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {
    @Query("SELECT * FROM visits WHERE pendingDelete = 0 ORDER BY visitedOn DESC")
    fun observeAll(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE municipalityId = :municipalityId AND pendingDelete = 0")
    fun observeForMunicipality(municipalityId: Int): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE pendingSync = 1 OR pendingDelete = 1")
    suspend fun getPendingOnce(): List<VisitEntity>

    @Query("SELECT * FROM visits WHERE localId = :localId")
    suspend fun getByLocalId(localId: String): VisitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(visit: VisitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(visits: List<VisitEntity>)

    @Query("DELETE FROM visits WHERE localId = :localId")
    suspend fun deleteByLocalId(localId: String)

    /** Replaces the whole synced (non-pending) set with what the server returned, keeping any still-pending local rows untouched. */
    @Query("DELETE FROM visits WHERE pendingSync = 0 AND pendingDelete = 0")
    suspend fun clearSynced()
}
