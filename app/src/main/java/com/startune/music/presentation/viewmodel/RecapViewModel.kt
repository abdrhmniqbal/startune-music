package com.startune.music.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startune.music.data.model.Song
import com.startune.music.data.preferences.UserPreferencesRepository
import com.startune.music.data.repository.ArtistImageRepository
import com.startune.music.data.repository.MusicRepository
import com.startune.music.data.stats.ListeningRecap
import com.startune.music.data.stats.PlaybackStatsRepository
import com.startune.music.data.stats.StatsTimeRange
import com.startune.music.di.DispatcherProvider
import com.startune.music.presentation.recap.RecapSchedule
import com.startune.music.presentation.recap.RecapSlide
import com.startune.music.presentation.recap.RecapUiModel
import com.startune.music.presentation.recap.RecapUiState
import com.startune.music.presentation.recap.buildRecapSlides
import com.startune.music.presentation.stats.resolveStatsArtistImages
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Builds the recap story for the Stats tab's range in the `range` navigation argument. YEAR
 * also takes the `year` argument, so a previous year can be opened; the other ranges always
 * cover the running period up to now. The story is a snapshot: plays recorded while it is
 * open do not reshuffle the slides under the reader.
 */
@HiltViewModel
class RecapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playbackStatsRepository: PlaybackStatsRepository,
    private val musicRepository: MusicRepository,
    private val artistImageRepository: ArtistImageRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    val range: StatsTimeRange = savedStateHandle.get<String>(RANGE_ARG)
        ?.let { runCatching { StatsTimeRange.valueOf(it) }.getOrNull() }
        ?: StatsTimeRange.YEAR
    val year: Int = savedStateHandle.get<Int>(YEAR_ARG) ?: LocalDate.now().year

    private val _uiState = MutableStateFlow<RecapUiState>(RecapUiState.Loading)
    val uiState: StateFlow<RecapUiState> = _uiState.asStateFlow()

    /** The year's most played songs still in the library, most played first. */
    @Volatile
    var topSongsQueue: List<Song> = emptyList()
        private set

    init {
        viewModelScope.launch {
            try {
                load()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Exception) {
                Timber.e(throwable, "Failed to build the %s recap", range)
                _uiState.value = RecapUiState.NotEnoughData(range, year, LocalDate.now())
            }
        }
    }

    /**
     * Shows the story as soon as the listening data is in, with each artist's most played cover
     * standing in for their picture, then swaps the pictures in: those can wait on the network.
     */
    private suspend fun load() {
        val songs = withContext(dispatchers.io) { musicRepository.getAllSongsOnce() }
        val recap = playbackStatsRepository.loadRecap(range, year, songs)
        if (recap == null) {
            _uiState.value = RecapUiState.NotEnoughData(range, year, LocalDate.now())
            return
        }
        if (recap.summary.totalDurationMs < RecapSchedule.minListeningMs(range)) {
            _uiState.value = RecapUiState.NotEnoughData(range, year, recap.endDate)
            return
        }

        val songsById = songs.associateBy { it.id }
        topSongsQueue = recap.summary.songs.mapNotNull { songsById[it.songId] }

        // The recap ends at its period's end (today while the period is still running).
        val isSoFar = RecapSchedule.isSoFar(range, recap.endDate, LocalDate.now())
        // Each build starts on the next palette, so the recap feels new; the rotation is
        // advanced once per build, not per picture swap, so reopening it does not reshuffle.
        val paletteRotation = userPreferencesRepository.nextRecapPaletteRotation()
        val covers = recap.summary.topArtists
            .mapNotNull { artist -> recap.mostPlayedCoverOf(artist.artist, songsById)?.let { artist.artist to it } }
            .toMap()
        val model = buildModel(recap, isSoFar, covers, paletteRotation)
        _uiState.value = RecapUiState.Ready(model)

        val shownArtists = model.slides
            .filterIsInstance<RecapSlide.TopArtists>()
            .singleOrNull()
            ?.artists
            ?.map { it.name }
            .orEmpty()
        if (shownArtists.isEmpty()) return
        val pictures = try {
            withContext(dispatchers.io) {
                resolveStatsArtistImages(shownArtists, musicRepository, artistImageRepository)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Exception) {
            // The story is already up with cover stand-ins; keep it rather than failing it.
            Timber.w(throwable, "Failed to load artist pictures for the %s recap", range)
            return
        }
        if (pictures.isNotEmpty()) {
            _uiState.value = RecapUiState.Ready(buildModel(recap, isSoFar, covers + pictures, paletteRotation))
        }
    }

    private suspend fun buildModel(
        recap: ListeningRecap,
        isSoFar: Boolean,
        artistImages: Map<String, String>,
        paletteRotation: Int
    ): RecapUiModel {
        val slides = withContext(dispatchers.default) {
            buildRecapSlides(
                recap = recap,
                isSoFar = isSoFar,
                artistImages = artistImages,
                canPlayTopSongs = topSongsQueue.isNotEmpty()
            )
        }
        return RecapUiModel(
            range = recap.range,
            year = recap.year,
            endDate = recap.endDate,
            isSoFar = isSoFar,
            paletteRotation = paletteRotation,
            slides = slides.toImmutableList()
        )
    }

    companion object {
        /** Navigation argument holding the recap's Stats tab range (a StatsTimeRange name). */
        const val RANGE_ARG = "range"

        /** Navigation argument holding the recap's calendar year, for a YEAR recap. */
        const val YEAR_ARG = "year"
    }
}

/**
 * Cover of the most played track by [artistName], for artists without a picture of their own.
 * Matches the way stats split multi-artist songs into their separate artists.
 */
private fun ListeningRecap.mostPlayedCoverOf(artistName: String, songsById: Map<String, Song>): String? {
    val key = artistName.trim().lowercase(Locale.ROOT)
    return summary.songs.firstNotNullOfOrNull { played ->
        val cover = played.albumArtUri?.takeIf { it.isNotBlank() } ?: return@firstNotNullOfOrNull null
        val song = songsById[played.songId] ?: return@firstNotNullOfOrNull null
        val songArtists = song.artists.map { it.name }.ifEmpty { listOf(song.displayArtist) }
        cover.takeIf { songArtists.any { it.trim().lowercase(Locale.ROOT) == key } }
    }
}
