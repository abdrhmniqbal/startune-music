package com.startune.music.data.repository

import com.startune.music.data.database.LocalPlaylistDao
import com.startune.music.data.database.MusicDao
import com.startune.music.data.database.PlayedEntityDao
import com.startune.music.data.database.PlayedEntityEntity
import com.startune.music.data.model.JumpBackInItem
import com.startune.music.data.model.PlaybackSource
import com.startune.music.data.model.PlayedEntityType
import com.startune.music.data.database.PlaylistEntity
import com.startune.music.data.database.toSong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest

/**
 * Records what the user played FROM and resolves the Home "Jump back in" cards.
 *
 * Recording happens at the single place that builds a playback queue, so no play
 * path is missed; a failed write never breaks playback (the caller records
 * fire-and-forget). The table holds at most one row per entity, so ordering by
 * play time is the Jump back in order and replaying an entity moves it to the
 * top instead of duplicating it.
 */
@Singleton
class PlayedEntityRepository @Inject constructor(
    private val playedEntityDao: PlayedEntityDao,
    private val musicDao: MusicDao,
    private val localPlaylistDao: LocalPlaylistDao
) {
    /**
     * Records that the user played FROM [source]. Search is not an entity, so it
     * is carried as the current source only and never written here.
     */
    suspend fun recordPlayed(source: PlaybackSource) {
        val type = source.historyType ?: return
        val id = source.historyId ?: return
        val name = source.historyName ?: return
        playedEntityDao.record(
            PlayedEntityEntity(
                entityType = type.name,
                entityId = id,
                entityName = name,
                playedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * The most recently played entities, latest first, with display data resolved
     * from the library. Entities that no longer exist are dropped, except mix rows
     * with no playlist behind them: those are the built-in For You and Daily mixes
     * (and, until newer plays push them out, a deleted user mix), which keep their
     * recorded name and no artwork.
     */
    fun observeJumpBackIn(limit: Int): Flow<List<JumpBackInItem>> =
        playedEntityDao.observeRecentPlayed(limit)
            .mapLatest { rows -> resolveRows(rows) }
            .flowOn(Dispatchers.IO)

    private suspend fun resolveRows(rows: List<PlayedEntityEntity>): List<JumpBackInItem> {
        return rows.mapNotNull { row ->
            val type = runCatching { PlayedEntityType.valueOf(row.entityType) }.getOrNull()
                ?: return@mapNotNull null
            when (type) {
                PlayedEntityType.ALBUM -> {
                    val albumId = row.entityId.toLongOrNull() ?: return@mapNotNull null
                    val album = musicDao.getAlbumById(albumId).first() ?: return@mapNotNull null
                    JumpBackInItem(
                        type = type,
                        id = album.id.toString(),
                        title = album.title,
                        subtitle = album.artistName.takeIf { it.isNotBlank() },
                        artworkUri = album.albumArtUriString?.takeIf { it.isNotBlank() }
                    )
                }
                PlayedEntityType.ARTIST -> {
                    val artistId = row.entityId.toLongOrNull() ?: return@mapNotNull null
                    val artist = musicDao.getArtistById(artistId).first() ?: return@mapNotNull null
                    JumpBackInItem(
                        type = type,
                        id = artist.id.toString(),
                        title = artist.name,
                        subtitle = null,
                        artworkUri = artist.customImageUri?.takeIf { it.isNotBlank() }
                            ?: artist.imageUrl?.takeIf { it.isNotBlank() }
                    )
                }
                PlayedEntityType.PLAYLIST -> {
                    val playlist = localPlaylistDao.getPlaylistById(row.entityId)
                        ?: return@mapNotNull null
                    JumpBackInItem(
                        type = type,
                        id = playlist.id,
                        title = playlist.name,
                        subtitle = null,
                        artworkUri = playlist.coverImageUri?.takeIf { it.isNotBlank() },
                        coverColorArgb = playlist.coverColorArgb,
                        coverIconName = playlist.coverIconName?.takeIf { it.isNotBlank() },
                        collageArtworkUris = playlistCollageUris(playlist)
                    )
                }
                PlayedEntityType.MIX -> {
                    val playlist = localPlaylistDao.getPlaylistById(row.entityId)
                    if (playlist == null) {
                        JumpBackInItem(
                            type = type,
                            id = row.entityId,
                            title = row.entityName,
                            subtitle = null,
                            artworkUri = null
                        )
                    } else {
                        JumpBackInItem(
                            type = type,
                            id = playlist.id,
                            title = playlist.name,
                            subtitle = null,
                            artworkUri = playlist.coverImageUri?.takeIf { it.isNotBlank() },
                            coverColorArgb = playlist.coverColorArgb,
                            coverIconName = playlist.coverIconName?.takeIf { it.isNotBlank() },
                            collageArtworkUris = playlistCollageUris(playlist)
                        )
                    }
                }
            }
        }
    }

    /**
     * Up to 4 distinct album artworks from a playlist's songs, in playlist
     * order, for the 2x2 grid. Only resolved when the playlist has no custom
     * cover image or color, because those paint the card instead. Mirrors the
     * distinct-album rule of the playlist collage: one album never fills
     * several cells, and two albums sharing one cover collapse to one cell.
     */
    private suspend fun playlistCollageUris(playlist: PlaylistEntity): ImmutableList<String> {
        if (!playlist.coverImageUri.isNullOrBlank() || playlist.coverColorArgb != null) {
            return persistentListOf()
        }
        val order = localPlaylistDao.observePlaylistSongs(playlist.id).first()
            .mapIndexed { index, row -> row.songId.toLongOrNull() to index }
            .filter { it.first != null }
            .associate { it.first!! to it.second }
        if (order.isEmpty()) return persistentListOf()
        val entities = musicDao.getSongsByIdsListSimple(order.keys.toList())
            .sortedBy { order[it.id] ?: Int.MAX_VALUE }
        val seenAlbumIds = HashSet<Long>()
        val seenUris = HashSet<String>()
        val out = ArrayList<String>()
        for (entity in entities) {
            val uri = entity.toSong().albumArtUriString?.takeIf { it.isNotBlank() } ?: continue
            if (entity.albumId > 0 && entity.albumId in seenAlbumIds) continue
            if (uri in seenUris) continue
            if (entity.albumId > 0) seenAlbumIds.add(entity.albumId)
            seenUris.add(uri)
            out.add(uri)
            if (out.size >= COLLAGE_MAX_COVERS) break
        }
        return out.toImmutableList()
    }

    companion object {
        const val COLLAGE_MAX_COVERS = 4
    }
}
