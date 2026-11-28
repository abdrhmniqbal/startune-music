package com.startune.music.data.repository

import com.startune.music.data.database.AlbumEntity
import com.startune.music.data.database.ArtistEntity
import com.startune.music.data.database.LocalPlaylistDao
import com.startune.music.data.database.MusicDao
import com.startune.music.data.database.PlayedEntityDao
import com.startune.music.data.database.PlayedEntityEntity
import com.startune.music.data.database.PlaylistEntity
import com.startune.music.data.database.PlaylistSongEntity
import com.startune.music.data.database.SongEntity
import com.startune.music.data.model.PlaybackSource
import com.startune.music.data.model.PlayedEntityType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlayedEntityRepositoryTest {

    private fun repository(
        playedEntityDao: PlayedEntityDao = mockk(relaxed = true),
        musicDao: MusicDao = mockk(relaxed = true),
        localPlaylistDao: LocalPlaylistDao = mockk(relaxed = true)
    ) = PlayedEntityRepository(playedEntityDao, musicDao, localPlaylistDao)

    @Test
    fun `recordPlayed writes one row per entity with the play time`() = runTest {
        val dao = mockk<PlayedEntityDao>(relaxed = true)
        val row = slot<PlayedEntityEntity>()

        repository(playedEntityDao = dao).recordPlayed(PlaybackSource.Album(7L, "Dreams"))

        coVerify(exactly = 1) { dao.record(capture(row)) }
        assertEquals("ALBUM", row.captured.entityType)
        assertEquals("7", row.captured.entityId)
        assertEquals("Dreams", row.captured.entityName)
        assertTrue(row.captured.playedAt > 0L)
    }

    @Test
    fun `recordPlayed ignores search because a search is not an entity`() = runTest {
        val dao = mockk<PlayedEntityDao>(relaxed = true)

        repository(playedEntityDao = dao).recordPlayed(PlaybackSource.Search("aespa"))

        coVerify(exactly = 0) { dao.record(any()) }
    }

    @Test
    fun `observeJumpBackIn resolves display data from the library and keeps the recorded name for a mix with no playlist row`() =
        runTest {
            val dao = mockk<PlayedEntityDao>()
            val musicDao = mockk<MusicDao>()
            val localPlaylistDao = mockk<LocalPlaylistDao>()
            every { dao.observeRecentPlayed(4) } returns flowOf(
                listOf(
                    PlayedEntityEntity("MIX", "for-you", "For You Mix", 400L),
                    PlayedEntityEntity("PLAYLIST", "p1", "Old Playlist Name", 300L),
                    PlayedEntityEntity("ARTIST", "3", "Old Artist Name", 200L),
                    PlayedEntityEntity("ALBUM", "7", "Old Album Name", 100L)
                )
            )
            every { musicDao.getAlbumById(7L) } returns flowOf(
                AlbumEntity(
                    id = 7L,
                    title = "Dreams",
                    artistName = "Fleetwood Mac",
                    artistId = 3L,
                    albumArtUriString = "content://album/7",
                    songCount = 11,
                    dateAdded = 0L,
                    year = 1977
                )
            )
            every { musicDao.getArtistById(3L) } returns flowOf(
                ArtistEntity(
                    id = 3L,
                    name = "Fleetwood Mac",
                    trackCount = 42,
                    imageUrl = "https://example.test/artist.jpg"
                )
            )
            coEvery { localPlaylistDao.getPlaylistById("p1") } returns PlaylistEntity(
                id = "p1",
                name = "Road Trip",
                coverImageUri = "content://cover/p1"
            )
            coEvery { localPlaylistDao.getPlaylistById("for-you") } returns null

            val items = repository(dao, musicDao, localPlaylistDao)
                .observeJumpBackIn(4).first()

            assertEquals(4, items.size)
            assertEquals(PlayedEntityType.MIX, items[0].type)
            assertEquals("for-you", items[0].id)
            assertEquals("For You Mix", items[0].title)
            assertNull(items[0].artworkUri)
            assertEquals(PlayedEntityType.PLAYLIST, items[1].type)
            assertEquals("Road Trip", items[1].title)
            assertEquals("content://cover/p1", items[1].artworkUri)
            assertEquals(PlayedEntityType.ARTIST, items[2].type)
            assertEquals("Fleetwood Mac", items[2].title)
            assertEquals("https://example.test/artist.jpg", items[2].artworkUri)
            assertEquals(PlayedEntityType.ALBUM, items[3].type)
            assertEquals("Dreams", items[3].title)
            assertEquals("Fleetwood Mac", items[3].subtitle)
            assertEquals("content://album/7", items[3].artworkUri)
        }

    @Test
    fun `observeJumpBackIn drops entities that no longer exist and rows it cannot parse`() = runTest {
        val dao = mockk<PlayedEntityDao>()
        val musicDao = mockk<MusicDao>()
        val localPlaylistDao = mockk<LocalPlaylistDao>()
        every { dao.observeRecentPlayed(4) } returns flowOf(
            listOf(
                PlayedEntityEntity("ALBUM", "gone", "Gone", 400L),
                PlayedEntityEntity("ALBUM", "9", "Deleted Album", 300L),
                PlayedEntityEntity("NOPE", "1", "Unknown Type", 200L),
                PlayedEntityEntity("ARTIST", "5", "Kept", 100L)
            )
        )
        every { musicDao.getAlbumById(9L) } returns flowOf(null)
        every { musicDao.getArtistById(5L) } returns flowOf(
            ArtistEntity(id = 5L, name = "Kept", trackCount = 1)
        )
        coEvery { localPlaylistDao.getPlaylistById(any()) } returns null

        val items = repository(dao, musicDao, localPlaylistDao)
            .observeJumpBackIn(4).first()

        assertEquals(1, items.size)
        assertEquals(PlayedEntityType.ARTIST, items[0].type)
        assertEquals("5", items[0].id)
    }

    @Test
    fun `observeJumpBackIn resolves a collage for a playlist with no custom cover`() = runTest {
        val dao = mockk<PlayedEntityDao>()
        val musicDao = mockk<MusicDao>()
        val localPlaylistDao = mockk<LocalPlaylistDao>()
        every { dao.observeRecentPlayed(4) } returns flowOf(
            listOf(PlayedEntityEntity("PLAYLIST", "p2", "No Cover", 100L))
        )
        coEvery { localPlaylistDao.getPlaylistById("p2") } returns PlaylistEntity(
            id = "p2",
            name = "No Cover"
        )
        every { localPlaylistDao.observePlaylistSongs("p2") } returns flowOf(
            listOf(
                PlaylistSongEntity("p2", "10", 0),
                PlaylistSongEntity("p2", "11", 1),
                PlaylistSongEntity("p2", "12", 2),
                PlaylistSongEntity("p2", "13", 3)
            )
        )
        coEvery { musicDao.getSongsByIdsListSimple(listOf(10L, 11L, 12L, 13L)) } returns listOf(
            songEntity(10L, albumId = 1L, art = "content://art/1"),
            songEntity(11L, albumId = 1L, art = "content://art/1-other"),
            songEntity(12L, albumId = 2L, art = "content://art/2"),
            songEntity(13L, albumId = 3L, art = null)
        )

        val items = repository(dao, musicDao, localPlaylistDao)
            .observeJumpBackIn(4).first()

        assertEquals(1, items.size)
        // Same album twice collapses to one cell, a song without art is skipped.
        assertEquals(listOf("content://art/1", "content://art/2"), items[0].collageArtworkUris)
    }

    private fun songEntity(id: Long, albumId: Long, art: String?) = SongEntity(
        id = id,
        title = "Song $id",
        artistName = "Artist",
        artistId = 1L,
        albumName = "Album $albumId",
        albumId = albumId,
        contentUriString = "content://song/$id",
        albumArtUriString = art,
        duration = 1000L,
        genre = null,
        filePath = "/song/$id.mp3",
        parentDirectoryPath = "/",
        mimeType = null,
        bitrate = null,
        sampleRate = null,
        artistsJson = null
    )
}
