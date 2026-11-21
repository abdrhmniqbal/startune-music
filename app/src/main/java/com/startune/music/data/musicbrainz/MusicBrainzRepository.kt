package com.startune.music.data.musicbrainz

import com.startune.music.data.database.MusicDao
import com.startune.music.data.model.Song
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class MusicBrainzRepository @Inject constructor(
    private val api: MusicBrainzApiService,
    private val musicDao: MusicDao
) {
    /**
     * Searches MusicBrainz for real performers with this name, best score
     * first. Only Person and Group are returned: fictional characters and
     * catch-all entries are noise in a picker (the top hit for BIBI is
     * Bibi Blocksberg, a fictitious character). The caller picks; this
     * repository never guesses, because search score alone is wrong here.
     */
    suspend fun searchArtists(name: String): List<MusicBrainzArtist> = withContext(Dispatchers.IO) {
        try {
            api.searchArtists(name)
                .filter { it.type == "Person" || it.type == "Group" }
        } catch (e: Exception) {
            Timber.d(e, "MusicBrainz artist search failed for $name")
            emptyList()
        }
    }

    suspend fun search(song: Song): List<MusicBrainzMatch> = withContext(Dispatchers.IO) {
        api.searchRecording(
            title = song.title,
            artist = song.displayArtist,
            album = song.album,
            durationMs = song.duration.takeIf { it > 0L }
        )
    }

    suspend fun apply(song: Song, match: MusicBrainzMatch) = withContext(Dispatchers.IO) {
        val songId = song.id.toLongOrNull()
            ?: musicDao.getSongIdByContentUri(song.contentUriString)
            ?: error("Song is not present in the unified library")
        musicDao.applyMusicBrainzMatch(
            songId = songId,
            title = match.title,
            artist = match.artist,
            album = match.album,
            year = match.year,
            recordingId = match.recordingId,
            releaseId = match.releaseId,
            artistId = match.artistId
        )
    }
}
