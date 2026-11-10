package com.startune.music.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startune.music.data.model.Song
import com.startune.music.data.repository.MusicRepository
import com.startune.music.data.stats.PlaybackStatsRepository
import com.startune.music.data.stats.PlaybackStatsRepository.PlaybackStatsSummary
import com.startune.music.data.stats.PlaybackStatsRepository.SongPlaybackSummary
import com.startune.music.data.stats.StatsTimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val playbackStatsRepository: PlaybackStatsRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    data class StatsUiState(
        val selectedRange: StatsTimeRange = StatsTimeRange.WEEK,
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val summary: PlaybackStatsSummary? = null,
        val topTracks: ImmutableList<SongPlaybackSummary> = persistentListOf(),
        val topArtistImageUrls: ImmutableMap<String, String> = persistentMapOf(),
        val availableRanges: ImmutableList<StatsTimeRange> = StatsTimeRange.entries.toImmutableList()
    )

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private val _weeklyOverview = MutableStateFlow<PlaybackStatsSummary?>(null)
    val weeklyOverview: StateFlow<PlaybackStatsSummary?> = _weeklyOverview.asStateFlow()

    private val _homeOverviewState =
        MutableStateFlow<HomeStatsOverviewState>(HomeStatsOverviewState.Loading)
    val homeOverviewState: StateFlow<HomeStatsOverviewState> = _homeOverviewState.asStateFlow()

    /** Only the latest home refresh may publish, so an older result never overwrites a newer one. */
    private var homeOverviewJob: Job? = null

    private var refreshJob: Job? = null

    private val _homeTopTracks = MutableStateFlow<ImmutableList<SongPlaybackSummary>>(persistentListOf())
    val homeTopTracks: StateFlow<ImmutableList<SongPlaybackSummary>> = _homeTopTracks.asStateFlow()

    @Volatile
    private var cachedSongs: List<Song>? = null

    init {
        observeStatsRefreshFlow()
        refreshRange(
            range = StatsTimeRange.WEEK,
            showLoading = true,
            updateWeeklyOverview = true
        )
        refreshHomeOverview()
    }

    fun onRangeSelected(range: StatsTimeRange) {
        if (range == _uiState.value.selectedRange && !_uiState.value.isLoading) {
            return
        }
        refreshRange(
            range = range,
            showLoading = true,
            updateWeeklyOverview = range == StatsTimeRange.WEEK
        )
    }

    fun refreshWeeklyOverview() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val songs = loadSongs()
                    playbackStatsRepository.loadSummary(StatsTimeRange.WEEK, songs)
                }
            }.onSuccess { summary ->
                _weeklyOverview.value = summary
            }.onFailure { throwable ->
                Timber.e(throwable, "Failed to load weekly stats overview")
                _weeklyOverview.value = null
            }
        }
    }

    fun refreshHomeOverview() {
        homeOverviewJob?.cancel()
        homeOverviewJob = viewModelScope.launch {
            val (summary, topTracks) = try {
                withContext(Dispatchers.IO) {
                    val songs = loadSongs()
                    val topTracks = playbackStatsRepository.loadTopTracks(
                        StatsTimeRange.DAY,
                        songs,
                        HOME_TOP_TRACKS_LIMIT
                    )
                    val summary = firstSummaryWithActivity(HomeOverviewRanges) { range ->
                        playbackStatsRepository.loadSummary(range, songs)
                    }
                    summary to topTracks
                }
            } catch (cancellation: CancellationException) {
                // A newer refresh replaced this one; it publishes the result instead.
                throw cancellation
            } catch (throwable: Exception) {
                Timber.e(throwable, "Failed to load home stats overview")
                // Keep showing the last good summary rather than flashing the empty state.
                if (_homeOverviewState.value !is HomeStatsOverviewState.Ready) {
                    _homeOverviewState.value = HomeStatsOverviewState.Empty
                }
                return@launch
            }
            _homeOverviewState.value = summary
                ?.let { HomeStatsOverviewState.Ready(it) }
                ?: HomeStatsOverviewState.Empty
            _homeTopTracks.value = topTracks.toImmutableList()
        }
    }

    private fun refreshRange(
        range: StatsTimeRange,
        showLoading: Boolean = true,
        updateWeeklyOverview: Boolean = false
    ) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            if (showLoading) {
                _uiState.update { it.copy(isLoading = true, isRefreshing = false, selectedRange = range) }
            } else {
                _uiState.update { it.copy(isRefreshing = true, selectedRange = range) }
            }
            val loaded = runCatching {
                withContext(Dispatchers.IO) {
                    val songs = loadSongs()
                    val summary = playbackStatsRepository.loadSummary(range, songs)
                    val topTracks = playbackStatsRepository.loadTopTracks(range, songs)
                    val artistImageUrls = summary.topArtists.mapNotNull { artistSummary ->
                        val url = musicRepository.getEffectiveArtistImageUrlByName(artistSummary.artist)
                            ?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                        artistSummary.artist to url
                    }.toMap().toImmutableMap()
                    Triple(summary, topTracks, artistImageUrls)
                }
            }
            loaded.getOrNull()?.let { (summary, _, _) ->
                if (updateWeeklyOverview) {
                    _weeklyOverview.value = summary
                }
            }
            _uiState.update { current ->
                val result = loaded.getOrNull()
                current.copy(
                    isLoading = false,
                    isRefreshing = false,
                    summary = result?.first,
                    topTracks = result?.second?.toImmutableList() ?: persistentListOf(),
                    topArtistImageUrls = result?.third ?: persistentMapOf(),
                    selectedRange = range
                )
            }
            loaded.exceptionOrNull()?.let { Timber.e(it, "Failed to load stats for range %s", range) }
        }
    }

    private fun observeStatsRefreshFlow() {
        viewModelScope.launch {
            playbackStatsRepository.refreshFlow
                .drop(1)
                .collectLatest {
                    val selectedRange = _uiState.value.selectedRange
                    refreshRange(
                        range = selectedRange,
                        showLoading = false,
                        updateWeeklyOverview = selectedRange == StatsTimeRange.WEEK
                    )
                    if (selectedRange != StatsTimeRange.WEEK) {
                        refreshWeeklyOverview()
                    }
                    refreshHomeOverview()
                }
        }
    }

    fun requestStatsRefresh() {
        playbackStatsRepository.requestRefresh()
    }

    fun forceRegenerateStats() {
        cachedSongs = null
        playbackStatsRepository.requestRefresh()
    }

    private suspend fun loadSongs(): List<Song> {
        cachedSongs?.let { existing ->
            if (existing.isNotEmpty()) return existing
        }
        val songs = musicRepository.getAllSongsOnce()
        cachedSongs = songs
        return songs
    }

    private companion object {
        val HomeOverviewRanges = listOf(
            StatsTimeRange.WEEK,
            StatsTimeRange.MONTH,
            StatsTimeRange.YEAR,
            StatsTimeRange.ALL
        )
        const val HOME_TOP_TRACKS_LIMIT = 15
    }
}

/**
 * Loads [ranges] in order and returns the first summary with listening activity, or null when
 * none has any. Ranges after the first match are never loaded.
 */
internal suspend fun firstSummaryWithActivity(
    ranges: List<StatsTimeRange>,
    load: suspend (StatsTimeRange) -> PlaybackStatsSummary
): PlaybackStatsSummary? {
    for (range in ranges) {
        val summary = load(range)
        if (summary.hasListeningActivity()) return summary
    }
    return null
}
