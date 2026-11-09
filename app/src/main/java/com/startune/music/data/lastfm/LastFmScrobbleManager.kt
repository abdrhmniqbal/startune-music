package com.startune.music.data.lastfm

import android.os.SystemClock
import com.startune.music.data.database.LastFmDao
import com.startune.music.data.database.LastFmPendingScrobbleEntity
import com.startune.music.data.database.MusicDao
import com.startune.music.data.database.SongEntity
import com.startune.music.data.preferences.DEFAULT_PLAYED_THRESHOLD_PERCENT
import com.startune.music.data.preferences.UserPreferencesRepository
import com.startune.music.di.AppScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridges finalized listening sessions to the Last.fm submission queue.
 *
 * Fed by [com.startune.music.presentation.viewmodel.ListeningStatsTracker]: the scrobble
 * threshold is evaluated synchronously (cheap math, safe inside the tracker's synchronized
 * finalize), everything else — song resolution, toggle check, queue insert — runs on [scope].
 *
 * The rule mirrors the old Startune scrobbler, except the percentage is not its own
 * setting: a track counts once the listened time reaches the counted-as-played
 * percentage of its duration, capped by [LastFmScrobbleConfig.maxScrobbleDelaySeconds],
 * and tracks shorter than [LastFmScrobbleConfig.minimumTrackDurationSeconds] never count.
 * The percentage and the 5s floor are the same as the played threshold, so a track
 * scrobbles at exactly the moment it counts as played.
 */
@Singleton
class LastFmScrobbleManager @Inject constructor(
    private val musicDao: MusicDao,
    private val lastFmDao: LastFmDao,
    private val lastFmRepository: LastFmRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @AppScope private val scope: CoroutineScope
) {
    private var lastPlayingNowSongId: String? = null
    private var lastPlayingNowRealtimeMs: Long = 0L
    private var playedThresholdPercent: Int = DEFAULT_PLAYED_THRESHOLD_PERCENT

    init {
        scope.launch {
            userPreferencesRepository.playedThresholdPercentFlow.collect { percent ->
                playedThresholdPercent = percent
            }
        }
        // Flush scrobbles left queued by a previous process (e.g. killed while offline).
        scope.launch {
            runCatching {
                if (lastFmRepository.isAuthorized() && lastFmDao.count() > 0) {
                    lastFmRepository.scheduleFlush()
                }
            }.onFailure { Timber.e(it, "Failed to schedule startup Last.fm flush") }
        }
    }

    fun onSessionFinalized(
        songId: String,
        startedAtEpochMs: Long,
        listenedMs: Long,
        trackDurationMs: Long
    ) {
        if (!lastFmRepository.hasSession()) return
        val config = lastFmRepository.accountState.value.config
        if (!config.isEnabled) return
        if (!meetsScrobbleThreshold(listenedMs, trackDurationMs, playedThresholdPercent, config)) return
        scope.launch {
            runCatching { enqueueScrobble(songId, startedAtEpochMs) }
                .onFailure { Timber.e(it, "Failed to enqueue Last.fm scrobble for song=%s", songId) }
        }
    }

    fun onPlayingNow(songId: String) {
        if (!lastFmRepository.isAuthorized()) return
        if (!lastFmRepository.accountState.value.config.nowPlayingEnabled) return
        val now = SystemClock.elapsedRealtime()
        synchronized(this) {
            if (songId == lastPlayingNowSongId && now - lastPlayingNowRealtimeMs < PLAYING_NOW_DEBOUNCE_MS) {
                return
            }
            lastPlayingNowSongId = songId
            lastPlayingNowRealtimeMs = now
        }
        scope.launch {
            runCatching {
                val song = resolveAdmittedSong(songId) ?: return@launch
                lastFmRepository.submitNowPlaying(
                    trackName = song.title,
                    artistName = song.artistName,
                    releaseName = song.albumName.takeIf { it.isNotBlank() },
                    durationMs = song.duration.takeIf { it > 0 }
                )
            }.onFailure { Timber.d(it, "Last.fm now playing failed for song=%s", songId) }
        }
    }

    private suspend fun enqueueScrobble(songId: String, startedAtEpochMs: Long) {
        val song = resolveAdmittedSong(songId) ?: return
        val inserted = lastFmRepository.enqueueScrobble(
            LastFmPendingScrobbleEntity(
                listenedAtMs = startedAtEpochMs,
                trackName = song.title,
                artistName = song.artistName,
                releaseName = song.albumName.takeIf { it.isNotBlank() },
                durationMs = song.duration.takeIf { it > 0 },
                createdAtMs = System.currentTimeMillis()
            )
        )
        // While the session is known-invalid, scrobbles keep queueing but flushing waits for reconnect.
        if (inserted && lastFmRepository.isAuthorized()) {
            lastFmRepository.scheduleFlush()
        }
    }

    /**
     * Resolves the song, or null when it no longer exists or lacks the metadata
     * Last.fm requires.
     */
    private suspend fun resolveAdmittedSong(songId: String): SongEntity? {
        val id = songId.toLongOrNull() ?: return null
        val song = musicDao.getSongByIdOnce(id) ?: return null
        if (song.title.isBlank() || song.artistName.isBlank()) return null
        return song
    }

    companion object {
        private const val PLAYING_NOW_DEBOUNCE_MS = 30_000L
        private const val MIN_SCROBBLE_LISTEN_MS = 5_000L

        fun meetsScrobbleThreshold(
            listenedMs: Long,
            trackDurationMs: Long,
            scrobbleDelayPercent: Int,
            config: LastFmScrobbleConfig
        ): Boolean {
            if (trackDurationMs <= 0L) return false
            val minDurationMs = config.minimumTrackDurationSeconds * 1000L
            if (trackDurationMs < minDurationMs) return false
            val percentThresholdMs = trackDurationMs * scrobbleDelayPercent / 100
            val maxThresholdMs = config.maxScrobbleDelaySeconds * 1000L
            val required = minOf(maxOf(percentThresholdMs, MIN_SCROBBLE_LISTEN_MS), maxThresholdMs)
            return listenedMs >= required
        }
    }
}
