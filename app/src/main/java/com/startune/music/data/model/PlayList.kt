package com.startune.music.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis(),
    val isQueueGenerated: Boolean = false,
    val coverImageUri: String? = null,
    val coverColorArgb: Int? = null,
    val coverIconName: String? = null,
    val coverShapeType: String? = null,
    val coverShapeDetail1: Float? = null,
    val coverShapeDetail2: Float? = null,
    val coverShapeDetail3: Float? = null,
    val coverShapeDetail4: Float? = null,
    val source: String = "LOCAL",
    /** Song count for a mix (a playlist whose source is a mix rule). Null for regular playlists. */
    val mixSongCount: Int? = null,
    /** Picked artist ids for an ARTISTS mix, in pick order. Null for other mixes and playlists. */
    val mixArtistIds: List<Long>? = null,
    /** Picked genres for a GENRES mix, in pick order. Null for other mixes and playlists. */
    val mixGenres: List<String>? = null
)

enum class PlaylistShapeType {
    Circle,
    SmoothRect,
    RotatedPill,
    Star
}
