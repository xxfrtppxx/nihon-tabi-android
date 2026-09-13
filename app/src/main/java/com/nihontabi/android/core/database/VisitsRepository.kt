package com.nihontabi.android.core.database

import com.nihontabi.android.core.sync.SyncScheduler
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for visits: the UI reads/writes Room only, writes
 * are marked pending immediately and returned without waiting on the
 * network, and [SyncScheduler] is nudged to reconcile with the API in the
 * background — the "write local first, sync when online" flow described in
 * SYSTEM_DESIGN.md's offline strategy.
 */
@Singleton
class VisitsRepository @Inject constructor(
    private val visitDao: VisitDao,
    private val syncScheduler: SyncScheduler,
) {
    fun observeAll(): Flow<List<VisitEntity>> = visitDao.observeAll()

    fun observeForMunicipality(municipalityId: Int): Flow<List<VisitEntity>> =
        visitDao.observeForMunicipality(municipalityId)

    suspend fun save(
        localId: String?,
        municipality: MunicipalityEntity,
        prefecture: PrefectureEntity,
        status: String,
        visitedOn: String?,
        note: String?,
        rating: Int?,
    ) {
        val existing = localId?.let { visitDao.getByLocalId(it) }
        val entity = VisitEntity(
            localId = localId ?: UUID.randomUUID().toString(),
            remoteId = existing?.remoteId,
            municipalityId = municipality.id,
            prefectureId = prefecture.id,
            municipalityNameEn = municipality.nameEn,
            municipalityNameJa = municipality.nameJa,
            prefectureNameEn = prefecture.nameEn,
            prefectureNameJa = prefecture.nameJa,
            region = prefecture.region,
            status = status,
            visitedOn = visitedOn,
            note = note,
            rating = rating,
            updatedAtMillis = System.currentTimeMillis(),
            pendingSync = true,
            pendingDelete = false,
        )
        visitDao.upsert(entity)
        syncScheduler.requestSync()
    }

    suspend fun delete(localId: String) {
        val existing = visitDao.getByLocalId(localId) ?: return
        if (existing.remoteId == null) {
            // Never made it to the server — nothing to push, just drop it.
            visitDao.deleteByLocalId(localId)
        } else {
            visitDao.upsert(existing.copy(pendingDelete = true, pendingSync = false))
            syncScheduler.requestSync()
        }
    }
}
