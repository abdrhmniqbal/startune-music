package com.startune.music.presentation.viewmodel

import com.startune.music.data.DailyMixManager
import com.startune.music.data.model.Song
import com.startune.music.data.preferences.UserPreferencesRepository
import com.startune.music.data.repository.MusicRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mix ids used by the generic mix detail page. "daily" is the Daily Mix,
 * "for-you" is the For You Mix (the Home personalized mix).
 */
object MixIds {
    const val DAILY = "daily"
    const val FOR_YOU = "for-you"
}

/**
 * Manages Daily Mix and Your Mix state.
 * Extracted from PlayerViewModel to improve modularity.
 *
 * Responsibilities:
 * - Generate and update daily/your mixes
 * - Persist and restore mix state
 * - Check if mix needs updating based on day change
 */
@Singleton
class DailyMixStateHolder @Inject constructor(
    private val dailyMixManager: DailyMixManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val musicRepository: MusicRepository
) {
    private var scope: CoroutineScope? = null
    private var updateJob: Job? = null

    private val _dailyMixSongs = MutableStateFlow<ImmutableList<Song>>(persistentListOf())
    val dailyMixSongs: StateFlow<ImmutableList<Song>> = _dailyMixSongs.asStateFlow()

    private val _yourMixSongs = MutableStateFlow<ImmutableList<Song>>(persistentListOf())
    val yourMixSongs: StateFlow<ImmutableList<Song>> = _yourMixSongs.asStateFlow()

    /**
     * Initialize with coroutine scope from ViewModel.
     */
    fun initialize(coroutineScope: CoroutineScope) {
        scope = coroutineScope
    }

    /**
     * Remove a song from the daily mix.
     */
    fun removeFromDailyMix(songId: String) {
        _dailyMixSongs.update { currentList ->
            currentList.filterNot { it.id == songId }.toImmutableList()
        }
    }

    /**
     * Remove a song from the For You Mix and backfill it with another song so
     * the mix keeps its size. The backfill uses the same ranking as the mix
     * generation. The new list is persisted, so the replacement survives a
     * restart. When the library has no song left to backfill with, the mix
     * just shrinks by one.
     */
    fun removeFromYourMix(
        songId: String,
        favoriteSongIdsFlow: kotlinx.coroutines.flow.Flow<Set<String>>
    ) {
        scope?.launch(Dispatchers.IO) {
            val current = _yourMixSongs.value
            if (current.none { it.id == songId }) return@launch
            val remaining = current.filterNot { it.id == songId }
            val allSongs = musicRepository.getAllSongsOnce()
            val favoriteIds = favoriteSongIdsFlow.first()
            val excludeIds = remaining.map { it.id }.toSet() + songId
            val replacement = dailyMixManager.pickBackfillSong(allSongs, favoriteIds, excludeIds)
            val updated = (remaining + listOfNotNull(replacement)).toImmutableList()
            _yourMixSongs.value = updated
            userPreferencesRepository.saveYourMixSongIds(updated.map { it.id })
        }
    }

    /**
     * Regenerates only the For You Mix, with the given song count.
     */
    fun updateYourMix(
        favoriteSongIdsFlow: kotlinx.coroutines.flow.Flow<Set<String>>,
        limit: Int
    ) {
        updateJob?.cancel()
        updateJob = scope?.launch(Dispatchers.IO) {
            val allSongs = musicRepository.getAllSongsOnce()
            if (allSongs.isNotEmpty()) {
                val favoriteIds = favoriteSongIdsFlow.first()
                val yourMix = dailyMixManager.generateYourMix(allSongs, favoriteIds, limit)
                _yourMixSongs.value = yourMix.toImmutableList()
                userPreferencesRepository.saveYourMixSongIds(yourMix.map { it.id })
            }
        }
    }

    /**
     * Update the daily mix with new songs.
     * Uses getAllSongsOnce() to load songs on-demand instead of keeping a permanent subscription.
     */
    fun updateDailyMix(favoriteSongIdsFlow: kotlinx.coroutines.flow.Flow<Set<String>>) {
        updateJob?.cancel()
        updateJob = scope?.launch(Dispatchers.IO) {
            val allSongs = musicRepository.getAllSongsOnce()
            if (allSongs.isNotEmpty()) {
                val favoriteIds = favoriteSongIdsFlow.first()

                val mix = dailyMixManager.generateDailyMix(allSongs, favoriteIds)
                _dailyMixSongs.value = mix.toImmutableList()
                userPreferencesRepository.saveDailyMixSongIds(mix.map { it.id })

                val yourMixLimit = userPreferencesRepository.forYouMixSongCountFlow.first()
                val yourMix = dailyMixManager.generateYourMix(allSongs, favoriteIds, yourMixLimit)
                _yourMixSongs.value = yourMix.toImmutableList()
                userPreferencesRepository.saveYourMixSongIds(yourMix.map { it.id })
            } else {
                _yourMixSongs.value = persistentListOf()
            }
        }
    }

    /**
     * Load persisted daily mix from storage using direct DB queries by IDs
     * instead of combining with the full allSongs flow.
     */
    fun loadPersistedDailyMix() {
        scope?.launch {
            val dailyMixIds = userPreferencesRepository.dailyMixSongIdsFlow.first()
            if (dailyMixIds.isNotEmpty() && _dailyMixSongs.value.isEmpty()) {
                val songs = withContext(Dispatchers.IO) {
                    musicRepository.getSongsByIds(dailyMixIds).first()
                }
                if (songs.isNotEmpty()) {
                    val songMap = songs.associateBy { it.id }
                    val orderedSongs = dailyMixIds.mapNotNull { songMap[it] }
                    _dailyMixSongs.value = orderedSongs.toImmutableList()
                }
            }
        }

        scope?.launch {
            val yourMixIds = userPreferencesRepository.yourMixSongIdsFlow.first()
            if (yourMixIds.isNotEmpty() && _yourMixSongs.value.isEmpty()) {
                val songs = withContext(Dispatchers.IO) {
                    musicRepository.getSongsByIds(yourMixIds).first()
                }
                if (songs.isNotEmpty()) {
                    val songMap = songs.associateBy { it.id }
                    val orderedSongs = yourMixIds.mapNotNull { songMap[it] }
                    _yourMixSongs.value = orderedSongs.toImmutableList()
                }
            }
        }
    }

    /**
     * Force update the daily mix regardless of day.
     */
    fun forceUpdate(favoriteSongIdsFlow: kotlinx.coroutines.flow.Flow<Set<String>>) {
        scope?.launch {
            updateDailyMix(favoriteSongIdsFlow)
            userPreferencesRepository.saveLastDailyMixUpdateTimestamp(System.currentTimeMillis())
        }
    }

    /**
     * Check if daily mix needs updating (new day) and update if so.
     */
    fun checkAndUpdateIfNeeded(favoriteSongIdsFlow: kotlinx.coroutines.flow.Flow<Set<String>>) {
        scope?.launch {
            val lastUpdate = userPreferencesRepository.lastDailyMixUpdateFlow.first()
            val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val lastUpdateDay = Calendar.getInstance().apply {
                timeInMillis = lastUpdate
            }.get(Calendar.DAY_OF_YEAR)

            if (today != lastUpdateDay) {
                updateDailyMix(favoriteSongIdsFlow)
                userPreferencesRepository.saveLastDailyMixUpdateTimestamp(System.currentTimeMillis())
            }
        }
    }

    fun onCleared() {
        updateJob?.cancel()
        scope = null
    }
}
