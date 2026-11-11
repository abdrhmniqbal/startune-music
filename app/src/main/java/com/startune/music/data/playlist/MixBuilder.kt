package com.startune.music.data.playlist

import com.startune.music.data.DailyMixManager
import com.startune.music.data.model.MixRule
import com.startune.music.data.model.Song
import com.startune.music.data.playlist.nlp.GenreTaxonomy
import com.startune.music.data.playlist.nlp.NlpText
import com.startune.music.utils.GenreParser
import java.util.concurrent.TimeUnit

/**
 * Pure, side-effect-free selection logic for rule-based ("smart") playlists.
 *
 * Extracted from `PlaylistViewModel` so the boundary behaviour (the 30-day
 * "forgotten" threshold, the `playCount <= 2` "new gem" cutoff, the tie-break
 * ordering) can be unit-tested without a ViewModel, repository, or Android
 * runtime. The ViewModel is responsible only for fetching the inputs.
 */
object MixBuilder {

    /** Days of inactivity after which a favorite is considered "forgotten". */
    const val FORGOTTEN_FAVORITE_DAYS = 30L

    /** Maximum play count for a song to still qualify as a "new gem". */
    const val NEW_GEM_MAX_PLAY_COUNT = 2

    /**
     * Resolves the ordered list of song IDs for [rule] given the library and engagement data.
     *
     * @param now current epoch millis (injected so tests are deterministic).
     * @param limit requested size; clamped to `1..allSongs.size`.
     * @param artistIds picked artists for [MixRule.ARTISTS], in pick order;
     *   ignored by every other rule.
     * @param genreNames picked genres for [MixRule.GENRES], in pick order
     *   (canonical library names, or the typed token when the NLP taxonomy
     *   knows it); ignored by every other rule.
     */
    fun buildSongIds(
        rule: MixRule,
        allSongs: List<Song>,
        engagements: Map<String, DailyMixManager.SongEngagementStats>,
        favoriteIds: Set<String>,
        now: Long,
        limit: Int,
        artistIds: List<Long> = emptyList(),
        genreNames: List<String> = emptyList(),
    ): List<String> {
        if (allSongs.isEmpty()) return emptyList()

        val songById = allSongs.associateBy { it.id }
        val safeLimit = limit.coerceAtLeast(1).coerceAtMost(allSongs.size)

        val pickedSongs = when (rule) {
            MixRule.TOP_PLAYED -> {
                engagements.entries
                    .filter { it.value.playCount > 0 || it.value.totalPlayDurationMs > 0L }
                    .sortedWith(
                        compareByDescending<Map.Entry<String, DailyMixManager.SongEngagementStats>> { it.value.playCount }
                            .thenByDescending { it.value.totalPlayDurationMs }
                            .thenByDescending { it.value.lastPlayedTimestamp }
                    )
                    .mapNotNull { (songId, _) -> songById[songId] }
                    .take(safeLimit)
            }

            MixRule.RECENTLY_PLAYED -> {
                engagements.entries
                    .filter { it.value.lastPlayedTimestamp > 0L }
                    .sortedByDescending { it.value.lastPlayedTimestamp }
                    .mapNotNull { (songId, _) -> songById[songId] }
                    .take(safeLimit)
            }

            MixRule.FORGOTTEN_FAVORITES -> {
                val staleThreshold = now - TimeUnit.DAYS.toMillis(FORGOTTEN_FAVORITE_DAYS)
                allSongs
                    .asSequence()
                    .filter { favoriteIds.contains(it.id) }
                    .sortedWith(
                        compareBy<Song> { engagements[it.id]?.lastPlayedTimestamp ?: 0L }
                            .thenBy { it.title.lowercase() }
                    )
                    .filter { song ->
                        (engagements[song.id]?.lastPlayedTimestamp ?: 0L) < staleThreshold
                    }
                    .take(safeLimit)
                    .toList()
            }

            MixRule.NEW_GEMS -> {
                allSongs
                    .asSequence()
                    .sortedWith(
                        compareByDescending<Song> { it.dateAdded }
                            .thenBy { engagements[it.id]?.playCount ?: 0 }
                    )
                    .filter { song -> (engagements[song.id]?.playCount ?: 0) <= NEW_GEM_MAX_PLAY_COUNT }
                    .take(safeLimit)
                    .toList()
            }

            MixRule.ARTISTS -> {
                val pool = allSongs.filter { song -> song.artists.any { it.id in artistIds } }
                if (pool.isEmpty() || artistIds.isEmpty()) return emptyList()
                val selected = pickFromPool(pool, engagements, now, limit)
                // Round-robin across the picked artists, in the order they
                // were picked, so the mix sounds like a mixture instead of
                // one artist then the next.
                val byArtist = selected.groupBy { song ->
                    song.artists.firstOrNull { it.id in artistIds }?.id ?: artistIds.first()
                }
                val artistOrder = artistIds.filter { it in byArtist }
                val ordered = mutableListOf<Song>()
                var index = 0
                while (ordered.size < selected.size) {
                    var progressed = false
                    for (artistId in artistOrder) {
                        val group = byArtist[artistId] ?: continue
                        if (index < group.size) {
                            ordered.add(group[index])
                            progressed = true
                        }
                    }
                    if (!progressed) break
                    index++
                }
                ordered
            }

            MixRule.GENRES -> {
                val pool = allSongs.filter { song -> matchesAnyGenre(song.genre, genreNames) }
                if (pool.isEmpty() || genreNames.isEmpty()) return emptyList()
                val selected = pickFromPool(pool, engagements, now, limit)
                // Round-robin across the picked genres, in the order they
                // were picked, so the mix sounds like a mixture instead of
                // one genre then the next.
                val byGenre = selected.groupBy { song ->
                    genreNames.firstOrNull { genre -> matchesGenre(song.genre, genre) } ?: genreNames.first()
                }
                val genreOrder = genreNames.filter { it in byGenre }
                val ordered = mutableListOf<Song>()
                var index = 0
                while (ordered.size < selected.size) {
                    var progressed = false
                    for (genre in genreOrder) {
                        val group = byGenre[genre] ?: continue
                        if (index < group.size) {
                            ordered.add(group[index])
                            progressed = true
                        }
                    }
                    if (!progressed) break
                    index++
                }
                ordered
            }
        }

        return pickedSongs.map { it.id }.distinct()
    }

    /**
     * The day-seeded tier pick shared by the pool rules (ARTISTS, GENRES):
     * 50% anchor (most played, always there), 45% rotation (played but not
     * top, shuffled by the day), 5% fresh (least played), backfilled from
     * the pool when a tier runs short.
     */
    private fun pickFromPool(
        pool: List<Song>,
        engagements: Map<String, DailyMixManager.SongEngagementStats>,
        now: Long,
        limit: Int
    ): List<Song> {
        val poolLimit = limit.coerceAtLeast(1).coerceAtMost(pool.size)
        val playCountOf = { song: Song -> engagements[song.id]?.playCount ?: 0 }
        val played = pool
            .filter { playCountOf(it) > 0 }
            .sortedWith(
                compareByDescending<Song> { playCountOf(it) }
                    .thenByDescending { engagements[it.id]?.totalPlayDurationMs ?: 0L }
                    .thenByDescending { engagements[it.id]?.lastPlayedTimestamp ?: 0L }
            )
        // Day seed so the rotating and fresh tiers change every day
        // while the anchor (most played) stays put. Offset from the
        // daily mix seed (+17) so the two mixes do not rotate in lockstep.
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }
        val daySeed = calendar.get(java.util.Calendar.YEAR) * 1000 +
            calendar.get(java.util.Calendar.DAY_OF_YEAR) + 41
        val freshCount = (poolLimit * 5 / 100).coerceAtLeast(1)
        val anchorCount = (poolLimit / 2).coerceAtMost(poolLimit - freshCount)
        val rotationCount = poolLimit - anchorCount - freshCount
        val anchor = played.take(anchorCount)
        val rotation = played.drop(anchorCount)
            .shuffled(kotlin.random.Random(daySeed.toLong()))
            .take(rotationCount)
        val unplayed = pool.filter { playCountOf(it) == 0 }
        val leastPlayed = if (unplayed.isNotEmpty()) unplayed
            else played.takeLast(played.size / 4 + 1)
        val fresh = leastPlayed
            .shuffled(kotlin.random.Random((daySeed + 1).toLong()))
            .take(freshCount)
        val picked = (anchor + rotation + fresh).distinct()
        return if (picked.size < poolLimit) {
            picked + pool.shuffled(kotlin.random.Random((daySeed + 2).toLong()))
                .filter { it !in picked }
                .take(poolLimit - picked.size)
        } else {
            picked
        }
    }

    /** Whether any genre in [rawGenre] matches any of [genreNames]. */
    private fun matchesAnyGenre(rawGenre: String?, genreNames: List<String>): Boolean =
        genreNames.any { matchesGenre(rawGenre, it) }

    /**
     * Whether one of the genres in [rawGenre] matches [genre]: same loose
     * key ("K-Pop" matches "kpop"), one containing the other ("Alternative
     * Rock" matches "rock"), or the same NLP taxonomy family (a typed token
     * the taxonomy knows, e.g. "kpop" against a "Korean Pop" tag).
     */
    private fun matchesGenre(rawGenre: String?, genre: String): Boolean {
        val key = GenreParser.looseKey(genre)
        if (key.isEmpty()) return false
        val songKeys = GenreParser.split(rawGenre).map { GenreParser.looseKey(it) }
        if (songKeys.any { songKey ->
                songKey == key ||
                    (minOf(songKey.length, key.length) >= 3 &&
                        (songKey.contains(key) || key.contains(songKey)))
            }
        ) return true
        val pickedFamily = GenreTaxonomy.familyOf(NlpText.normalize(genre)) ?: return false
        return GenreParser.split(rawGenre).any {
            GenreTaxonomy.familyOf(NlpText.normalize(it)) == pickedFamily
        }
    }
}
