package com.startune.music.data.database

import com.google.common.truth.Truth.assertThat
import com.startune.music.data.model.ArtistRef
import org.junit.jupiter.api.Test

class SongEntityPrimaryArtistTest {

    private fun entity(artistName: String, artistsJson: String?) = SongEntity(
        id = 1L,
        title = "Track",
        artistName = artistName,
        artistId = 10L,
        albumName = "Album",
        albumId = 20L,
        contentUriString = "content://media/1",
        albumArtUriString = null,
        duration = 180_000L,
        genre = null,
        filePath = "/music/track.mp3",
        parentDirectoryPath = "/music",
        artistsJson = artistsJson
    )

    @Test
    fun `returns primary artist when artistName holds the combined tag`() {
        val json = serializeArtistRefs(
            listOf(
                ArtistRef(id = 10L, name = "FIFTY FIFTY", isPrimary = true),
                ArtistRef(id = 11L, name = "Sabrina Carpenter", isPrimary = false)
            )
        )

        assertThat(entity("FIFTY FIFTY & Sabrina Carpenter", json).primaryArtistName())
            .isEqualTo("FIFTY FIFTY")
    }

    @Test
    fun `returns first artist when no entry is flagged primary`() {
        val json = serializeArtistRefs(
            listOf(
                ArtistRef(id = 10L, name = "FIFTY FIFTY", isPrimary = false),
                ArtistRef(id = 11L, name = "Sabrina Carpenter", isPrimary = false)
            )
        )

        assertThat(entity("FIFTY FIFTY & Sabrina Carpenter", json).primaryArtistName())
            .isEqualTo("FIFTY FIFTY")
    }

    @Test
    fun `falls back to artistName when no artist list was stored`() {
        assertThat(entity("FIFTY FIFTY & Sabrina Carpenter", null).primaryArtistName())
            .isEqualTo("FIFTY FIFTY & Sabrina Carpenter")
    }

    @Test
    fun `falls back to artistName when the stored list is malformed`() {
        assertThat(entity("FIFTY FIFTY & Sabrina Carpenter", "not json").primaryArtistName())
            .isEqualTo("FIFTY FIFTY & Sabrina Carpenter")
    }
}
