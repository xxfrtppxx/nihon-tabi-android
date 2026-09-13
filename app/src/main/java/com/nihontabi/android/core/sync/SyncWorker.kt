package com.nihontabi.android.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nihontabi.android.core.database.VisitDao
import com.nihontabi.android.core.database.VisitEntity
import com.nihontabi.android.core.network.VisitsApi
import com.nihontabi.android.core.network.dto.CreateVisitRequest
import com.nihontabi.android.core.network.dto.UpdateVisitRequest
import com.nihontabi.android.core.network.dto.VisitDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Pushes locally pending visit creates/updates/deletes to the API, then
 * pulls the fresh `/visits` list to reconcile — the background half of the
 * "write local first, sync when online" flow (`VisitsRepository` is the
 * foreground half). Enqueued by [SyncScheduler], constrained to a connected
 * network.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val visitDao: VisitDao,
    private val visitsApi: VisitsApi,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            pushPending()
            pullLatest()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private suspend fun pushPending() {
        for (visit in visitDao.getPendingOnce()) {
            when {
                visit.pendingDelete -> {
                    if (visit.remoteId != null) visitsApi.remove(visit.remoteId)
                    visitDao.deleteByLocalId(visit.localId)
                }
                visit.remoteId == null -> {
                    val created = visitsApi.create(
                        CreateVisitRequest(
                            municipalityId = visit.municipalityId,
                            status = visit.status,
                            visitedOn = visit.visitedOn,
                            note = visit.note,
                            rating = visit.rating,
                        ),
                    )
                    visitDao.upsert(visit.copy(remoteId = created.id, pendingSync = false))
                }
                else -> {
                    visitsApi.update(
                        visit.remoteId,
                        UpdateVisitRequest(
                            status = visit.status,
                            visitedOn = visit.visitedOn,
                            note = visit.note,
                            rating = visit.rating,
                        ),
                    )
                    visitDao.upsert(visit.copy(pendingSync = false))
                }
            }
        }
    }

    private suspend fun pullLatest() {
        val remote = visitsApi.list()
        visitDao.clearSynced()
        visitDao.upsertAll(remote.map { it.toEntity() })
    }

    private fun VisitDto.toEntity() = VisitEntity(
        localId = id,
        remoteId = id,
        municipalityId = municipalityId,
        prefectureId = municipality.prefectureId,
        municipalityNameEn = municipality.nameEn,
        municipalityNameJa = municipality.nameJa,
        prefectureNameEn = municipality.prefecture.nameEn,
        prefectureNameJa = municipality.prefecture.nameJa,
        region = municipality.prefecture.region,
        status = status,
        visitedOn = visitedOn,
        note = note,
        rating = rating,
        updatedAtMillis = System.currentTimeMillis(),
        pendingSync = false,
        pendingDelete = false,
    )

    companion object {
        private const val MAX_ATTEMPTS = 5
    }
}
