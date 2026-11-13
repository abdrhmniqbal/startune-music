package com.startune.music.presentation.recap

import androidx.compose.runtime.Immutable
import com.startune.music.data.stats.ListeningPersonality
import com.startune.music.data.stats.ListeningRecap
import com.startune.music.data.stats.PlaybackStatsRepository
import com.startune.music.data.stats.PlaybackStatsRepository.SongPlaybackSummary
import com.startune.music.data.stats.StatsTimeRange
import com.startune.music.data.stats.peakIndex
import com.startune.music.utils.GenreParser
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/** How many entries the ranked slides show, the leader included. */
internal const val RECAP_RANKING_SIZE = 5

@Immutable
sealed interface RecapUiState {
    data object Loading : RecapUiState

    /** The period has too little listening (or none) for a story. */
    data class NotEnoughData(
        val range: StatsTimeRange,
        val year: Int,
        val endDate: LocalDate
    ) : RecapUiState

    data class Ready(val model: RecapUiModel) : RecapUiState
}

@Immutable
data class RecapUiModel(
    val range: StatsTimeRange,
    val year: Int,
    /** The date the recap ends: today for a running period, December 31 for a finished year. */
    val endDate: LocalDate,
    val isSoFar: Boolean,
    /** How far the palette list is rotated for this build, so each recap starts fresh. */
    val paletteRotation: Int,
    val slides: ImmutableList<RecapSlide>
)

@Immutable
data class RecapArtistItem(
    val name: String,
    val durationMs: Long,
    val songCount: Int,
    /** The artist's picture, else the cover of their most played track. */
    val imageUri: String?
)

@Immutable
data class RecapAlbumItem(
    val title: String,
    val artUri: String?,
    val durationMs: Long,
    val playCount: Int
)

@Immutable
data class RecapSongItem(
    val songId: String,
    val title: String,
    /** Null when the song has no artist tag. */
    val artist: String?,
    val artUri: String?,
    val durationMs: Long,
    val playCount: Int
)

@Immutable
data class RecapGenreItem(
    val name: String,
    val durationMs: Long,
    /** Share of all listening, from 0 to 1. */
    val share: Float
)

/** One page of the recap story. Each kind appears at most once, so [key] is stable. */
@Immutable
sealed interface RecapSlide {
    val key: String

    data class Intro(
        val range: StatsTimeRange,
        val year: Int,
        val endDate: LocalDate,
        val isSoFar: Boolean
    ) : RecapSlide {
        override val key: String get() = "intro"
    }

    data class Minutes(
        val range: StatsTimeRange,
        val year: Int,
        val endDate: LocalDate,
        val totalMs: Long,
        val activeDays: Int,
        val averageDailyMs: Long
    ) : RecapSlide {
        override val key: String get() = "minutes"
    }

    data class Months(
        /** Twelve entries, January first. */
        val monthlyMs: ImmutableList<Long>,
        val peakMonthIndex: Int
    ) : RecapSlide {
        override val key: String get() = "months"
    }

    data class Clock(
        /** Twenty-four entries, midnight first. */
        val hourlyMs: ImmutableList<Long>,
        val personality: ListeningPersonality,
        val favoriteDay: DayOfWeek?
    ) : RecapSlide {
        override val key: String get() = "clock"
    }

    data class Dedication(
        val range: StatsTimeRange,
        val longestStreakDays: Int,
        val longestSessionMs: Long,
        val biggestDay: LocalDate,
        val biggestDayMs: Long,
        val firstSong: RecapSongItem?
    ) : RecapSlide {
        override val key: String get() = "dedication"
    }

    data class Genres(val genres: ImmutableList<RecapGenreItem>) : RecapSlide {
        override val key: String get() = "genres"
    }

    data class Decade(val decade: Int, val share: Float) : RecapSlide {
        override val key: String get() = "decade"
    }

    data class TopAlbums(
        val albums: ImmutableList<RecapAlbumItem>,
        val uniqueAlbums: Int
    ) : RecapSlide {
        override val key: String get() = "albums"
    }

    data class TopArtists(
        val artists: ImmutableList<RecapArtistItem>,
        val uniqueArtists: Int
    ) : RecapSlide {
        override val key: String get() = "artists"
    }

    data class TopSongs(
        val songs: ImmutableList<RecapSongItem>,
        val uniqueSongs: Int
    ) : RecapSlide {
        override val key: String get() = "songs"
    }

    data class Summary(
        val range: StatsTimeRange,
        val year: Int,
        val endDate: LocalDate,
        val isSoFar: Boolean,
        val totalMs: Long,
        /** The top artists, leader first; at most [RECAP_RANKING_SIZE]. */
        val topArtists: ImmutableList<RecapArtistItem>,
        /** The top songs, leader first; at most [RECAP_RANKING_SIZE]. */
        val topSongs: ImmutableList<RecapSongItem>,
        val topGenre: String?,
        val canPlayTopSongs: Boolean
    ) : RecapSlide {
        override val key: String get() = "summary"
    }
}

/**
 * The story for [recap]: intro, minutes and summary always, every other slide only when it has
 * something to show. The repository's "unknown" buckets are grouping keys, never shown, and
 * neither are the cloud source placeholders ([GenreParser.isSourcePlaceholder]) among genres.
 *
 * @param artistImages picture per artist name, see [RecapArtistItem.imageUri].
 * @param canPlayTopSongs whether any top song is still in the library to play.
 */
internal fun buildRecapSlides(
    recap: ListeningRecap,
    isSoFar: Boolean,
    artistImages: Map<String, String>,
    canPlayTopSongs: Boolean
): List<RecapSlide> {
    val summary = recap.summary
    val totalMs = summary.totalDurationMs

    val artists = summary.topArtists
        .filter { it.artist != PlaybackStatsRepository.UNKNOWN_ARTIST && it.totalDurationMs > 0L }
        .take(RECAP_RANKING_SIZE)
        .map {
            RecapArtistItem(
                name = it.artist,
                durationMs = it.totalDurationMs,
                songCount = it.uniqueSongs,
                imageUri = artistImages[it.artist]
            )
        }
    val albums = summary.topAlbums
        .filter { it.album != PlaybackStatsRepository.UNKNOWN_ALBUM && it.totalDurationMs > 0L }
        .take(RECAP_RANKING_SIZE)
        .map {
            RecapAlbumItem(
                title = it.album,
                artUri = it.albumArtUri,
                durationMs = it.totalDurationMs,
                playCount = it.playCount
            )
        }
    // Jellyfin and Navidrome songs without a genre are stored under the source's name.
    val genres = summary.topGenres
        .filter {
            it.genre != PlaybackStatsRepository.UNKNOWN_GENRE &&
                !GenreParser.isSourcePlaceholder(it.genre) &&
                it.totalDurationMs > 0L
        }
        .take(RECAP_RANKING_SIZE)
        .map {
            RecapGenreItem(
                name = it.genre,
                durationMs = it.totalDurationMs,
                share = if (totalMs > 0L) (it.totalDurationMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f
            )
        }
    val songs = summary.topSongs
        .filter { it.totalDurationMs > 0L }
        .take(RECAP_RANKING_SIZE)
        .map { it.toRecapItem() }

    return buildList {
        add(
            RecapSlide.Intro(
                range = recap.range,
                year = recap.year,
                endDate = recap.endDate,
                isSoFar = isSoFar
            )
        )
        add(
            RecapSlide.Minutes(
                range = recap.range,
                year = recap.year,
                endDate = recap.endDate,
                totalMs = totalMs,
                activeDays = summary.activeDays,
                // The Stats view's own per-day figure for this range, so the two screens agree.
                averageDailyMs = summary.averageDailyDurationMs
            )
        )
        // Only a year has twelve month buckets; the other ranges show their own timeline in Stats.
        if (recap.range == StatsTimeRange.YEAR) {
            peakIndex(recap.monthlyDurationsMs)?.let { peak ->
                add(RecapSlide.Months(monthlyMs = recap.monthlyDurationsMs.toImmutableList(), peakMonthIndex = peak))
            }
        }
        if (recap.hourlyDurationsMs.any { it > 0L }) {
            add(
                RecapSlide.Clock(
                    hourlyMs = recap.hourlyDurationsMs.toImmutableList(),
                    personality = recap.personality,
                    favoriteDay = recap.favoriteDayOfWeek
                )
            )
        }
        // A single day has no streak and its biggest day is today, so nothing to dedicate.
        if (recap.range != StatsTimeRange.DAY) {
            recap.biggestDay?.let { biggestDay ->
                add(
                    RecapSlide.Dedication(
                        range = recap.range,
                        longestStreakDays = summary.longestStreakDays,
                        longestSessionMs = summary.longestSessionDurationMs,
                        biggestDay = biggestDay,
                        biggestDayMs = recap.biggestDayDurationMs,
                        firstSong = recap.firstSong?.let { first ->
                            RecapSongItem(
                                songId = first.songId,
                                title = first.title,
                                artist = first.artist.displayableArtist(),
                                artUri = first.albumArtUri,
                                durationMs = 0L,
                                playCount = 0
                            )
                        }
                    )
                )
            }
        }
        if (genres.isNotEmpty()) add(RecapSlide.Genres(genres.toImmutableList()))
        recap.topDecade?.let { decade -> add(RecapSlide.Decade(decade = decade, share = recap.topDecadeShare)) }
        if (albums.isNotEmpty()) {
            add(RecapSlide.TopAlbums(albums = albums.toImmutableList(), uniqueAlbums = recap.uniqueAlbums))
        }
        if (artists.isNotEmpty()) {
            add(RecapSlide.TopArtists(artists = artists.toImmutableList(), uniqueArtists = recap.uniqueArtists))
        }
        if (songs.isNotEmpty()) {
            add(RecapSlide.TopSongs(songs = songs.toImmutableList(), uniqueSongs = summary.uniqueSongs))
        }
        add(
            RecapSlide.Summary(
                range = recap.range,
                year = recap.year,
                endDate = recap.endDate,
                isSoFar = isSoFar,
                totalMs = totalMs,
                topArtists = artists.toImmutableList(),
                topSongs = songs.toImmutableList(),
                topGenre = genres.firstOrNull()?.name,
                canPlayTopSongs = canPlayTopSongs
            )
        )
    }
}

private fun SongPlaybackSummary.toRecapItem() = RecapSongItem(
    songId = songId,
    title = title,
    artist = artist.displayableArtist(),
    artUri = albumArtUri,
    durationMs = totalDurationMs,
    playCount = playCount
)

/** Null for a blank tag or the repository's own fallback name. */
private fun String.displayableArtist(): String? =
    takeIf { it.isNotBlank() && it != PlaybackStatsRepository.UNKNOWN_ARTIST }
