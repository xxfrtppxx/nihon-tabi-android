package com.nihontabi.android.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A visit/want-to-go record. Written locally first and synced by
 * `SyncWorker` — [localId] is a client-generated UUID used as the stable
 * Room key so the UI can bind to a row before the server has ever seen it;
 * [remoteId] is filled in once `SyncWorker` successfully creates it on the
 * API and is what all subsequent PATCH/DELETE calls address.
 *
 * The municipality/prefecture name fields are denormalized copies (from
 * whichever `VisitDto.municipality`/`.prefecture` produced this row, or from
 * the already-cached geo rows when created locally) rather than a live join
 * — the Timeline and country-map screens need them, and a join against the
 * geo cache tables isn't guaranteed to resolve offline for a municipality
 * whose prefecture the user hasn't drilled into on this device yet (visits
 * sync down from other clients too).
 */
@Entity(tableName = "visits")
data class VisitEntity(
    @PrimaryKey val localId: String,
    val remoteId: String?,
    val municipalityId: Int,
    val prefectureId: Int,
    val municipalityNameEn: String,
    val municipalityNameJa: String,
    val prefectureNameEn: String,
    val prefectureNameJa: String,
    val region: String,
    /** `"visited"` or `"want_to_go"` — see `VisitStatus` in the API's Prisma schema. */
    val status: String,
    val visitedOn: String?,
    val note: String?,
    val rating: Int?,
    val updatedAtMillis: Long,
    val pendingSync: Boolean,
    val pendingDelete: Boolean,
)
