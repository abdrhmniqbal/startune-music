package com.startune.music.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A scrobble awaiting submission to Last.fm. The table is the pending queue: rows are deleted
 * on successful submission or permanent rejection, so no status column exists.
 *
 * Metadata is snapshotted at enqueue time — the song may be edited or deleted before the queue
 * flushes, and Last.fm should receive what was actually played.
 */
@Entity(tableName = "lastfm_pending_scrobbles")
data class LastFmPendingScrobbleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    /** Epoch millis of when the listen started (Last.fm `timestamp` semantics, seconds). */
    @ColumnInfo(name = "listened_at_ms") val listenedAtMs: Long,
    @ColumnInfo(name = "track_name") val trackName: String,
    @ColumnInfo(name = "artist_name") val artistName: String,
    @ColumnInfo(name = "release_name") val releaseName: String? = null,
    @ColumnInfo(name = "duration_ms") val durationMs: Long? = null,
    @ColumnInfo(name = "attempts", defaultValue = "0") val attempts: Int = 0,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long
)
