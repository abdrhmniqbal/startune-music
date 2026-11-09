package com.startune.music.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startune.music.data.model.Artist
import com.startune.music.data.model.Genre
import com.startune.music.data.model.Song
import com.startune.music.data.repository.MusicRepository
import com.startune.music.data.stats.PlaybackStatsRepository
import com.startune.music.data.stats.StatsTimeRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.startune.music.di.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

data class AlbumData(
    val name: String,
    val artUri: String?,
    val songs: List<Song>
)

data class GenreDetailUiState(
    val genre: Genre? = null,
    val songs: List<Song> = emptyList(),
    val rankedSongs: List<Song> = emptyList(),
    val albums: List<AlbumData> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val randomAlbums: List<AlbumData> = emptyList(),
    val randomArtists: List<Artist> = emptyList(),
    val isLoadingGenreName: Boolean = false,
    val isLoadingSongs: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class GenreDetailViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playbackStatsRepository: PlaybackStatsRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(GenreDetailUiState())
    val uiState: StateFlow<GenreDetailUiState> = _uiState.asStateFlow()

    private var artistMap: Map<String, String?> = emptyMap()

    init {
        savedStateHandle.get<String>("genreId")?.let { genreId ->
            val decodedGenreId = java.net.URLDecoder.decode(genreId, "UTF-8")
            loadGenreDetails(decodedGenreId)
        } ?: run {
            _uiState.update { it.copy(error = "Genre ID not found", isLoadingGenreName = false, isLoadingSongs = false) }
        }
    }

    private data class ProcessingResult(
        val genre: Genre,
        val songs: List<Song>,
        val rankedSongs: List<Song>,
        val albums: List<AlbumData>,
        val artists: List<Artist>,
        val randomAlbums: List<AlbumData>,
        val randomArtists: List<Artist>
    )

    private fun loadGenreDetails(genreId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGenreName = true, isLoadingSongs = true, error = null) }

            try {
                val initialGenre = withContext(dispatchers.default) {
                    val genres = musicRepository.getGenres().first()
                    genres.find { it.id.equals(genreId, ignoreCase = true) }
                }

                if (initialGenre != null) {
                    _uiState.update { it.copy(genre = initialGenre, isLoadingGenreName = false) }
                }

                val result = withContext(dispatchers.default) {
                    val genres = musicRepository.getGenres().first()
                    val genre = initialGenre ?: genres.find { it.id.equals(genreId, ignoreCase = true) }
                        ?: Genre(
                            id = genreId,
                            name = genreId.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                            lightColorHex = "#9E9E9E", onLightColorHex = "#000000",
                            darkColorHex = "#616161", onDarkColorHex = "#FFFFFF"
                        )

                    val songs = musicRepository.getMusicByGenre(genre.name).first()
                    val artists = musicRepository.getArtists().first()
                    artistMap = artists.associate { it.name.trim().lowercase() to it.imageUrl }

                    val rankedSongs = rankSongs(songs)
                    val albums = buildAlbums(songs)
                    val genreArtists = buildArtists(songs, artistMap)

                    ProcessingResult(
                        genre,
                        songs,
                        rankedSongs,
                        albums,
                        genreArtists,
                        albums.shuffled().take(GENRE_GRID_PREVIEW),
                        genreArtists.shuffled().take(GENRE_GRID_PREVIEW)
                    )
                }

                _uiState.update {
                    it.copy(
                        genre = result.genre,
                        songs = result.songs,
                        rankedSongs = result.rankedSongs,
                        albums = result.albums,
                        artists = result.artists,
                        randomAlbums = result.randomAlbums,
                        randomArtists = result.randomArtists,
                        isLoadingGenreName = false,
                        isLoadingSongs = false
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        error = "Failed to load genre details: ${e.message}",
                        isLoadingGenreName = false,
                        isLoadingSongs = false
                    )
                }
            }
        }
    }

    private suspend fun rankSongs(songs: List<Song>): List<Song> {
        val playedIds = try {
            playbackStatsRepository
                .loadTopTracks(StatsTimeRange.ALL, songs, songs.size)
                .map { it.songId }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load top songs for genre")
            emptyList()
        }
        val byId = songs.associateBy { it.id }
        val played = playedIds.mapNotNull { byId[it] }
        val playedSet = playedIds.toSet()
        val unplayed = songs.filter { it.id !in playedSet }.sortedBy { it.title.lowercase() }
        return played + unplayed
    }

    private fun buildAlbums(songs: List<Song>): List<AlbumData> {
        return songs.groupBy { it.album }.map { (albumName, albumSongs) ->
            val sortedAlbumSongs = albumSongs.sortedWith(
                compareBy<Song> { it.discNumber ?: 1 }
                    .thenBy { if (it.trackNumber > 0) it.trackNumber else Int.MAX_VALUE }
                    .thenBy { it.title.lowercase() }
            )
            AlbumData(albumName, sortedAlbumSongs.firstOrNull()?.albumArtUriString, sortedAlbumSongs)
        }.sortedWith(
            compareByDescending<AlbumData> { it.songs.maxOfOrNull { song -> song.year } ?: 0 }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun buildArtists(songs: List<Song>, artistMap: Map<String, String?>): List<Artist> {
        return songs.groupBy { it.artist }.map { (artistName, artistSongs) ->
            Artist(
                id = artistSongs.firstOrNull()?.artistId ?: -1L,
                name = artistName,
                songCount = artistSongs.size,
                imageUrl = artistMap[artistName.trim().lowercase()]
            )
        }.sortedBy { it.name.lowercase() }
    }

    companion object {
        const val GENRE_GRID_PREVIEW = 8
    }
}
