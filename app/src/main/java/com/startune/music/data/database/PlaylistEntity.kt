package com.startune.music.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.startune.music.data.model.Playlist
import kotlinx.serialization.json.Json

private val mixListJson = Json { ignoreUnknownKeys = true }

@Entity(
    tableName = "playlists",
    indices = [
        Index(value = ["last_modified"])
    ]
)
data class PlaylistEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_queue_generated")
    val isQueueGenerated: Boolean = false,
    @ColumnInfo(name = "cover_image_uri")
    val coverImageUri: String? = null,
    @ColumnInfo(name = "cover_color_argb")
    val coverColorArgb: Int? = null,
    @ColumnInfo(name = "cover_icon_name")
    val coverIconName: String? = null,
    @ColumnInfo(name = "cover_shape_type")
    val coverShapeType: String? = null,
    @ColumnInfo(name = "cover_shape_detail_1")
    val coverShapeDetail1: Float? = null,
    @ColumnInfo(name = "cover_shape_detail_2")
    val coverShapeDetail2: Float? = null,
    @ColumnInfo(name = "cover_shape_detail_3")
    val coverShapeDetail3: Float? = null,
    @ColumnInfo(name = "cover_shape_detail_4")
    val coverShapeDetail4: Float? = null,
    @ColumnInfo(name = "source")
    val source: String = "LOCAL",
    @ColumnInfo(name = "mix_song_count")
    val mixSongCount: Int? = null,
    @ColumnInfo(name = "mix_artist_ids")
    val mixArtistIds: String? = null,
    @ColumnInfo(name = "mix_genres")
    val mixGenres: String? = null,
)

fun PlaylistEntity.toPlaylist(songIds: List<String>): Playlist {
    return Playlist(
        id = id,
        name = name,
        songIds = songIds,
        createdAt = createdAt,
        lastModified = lastModified,
        isQueueGenerated = isQueueGenerated,
        coverImageUri = coverImageUri,
        coverColorArgb = coverColorArgb,
        coverIconName = coverIconName,
        coverShapeType = coverShapeType,
        coverShapeDetail1 = coverShapeDetail1,
        coverShapeDetail2 = coverShapeDetail2,
        coverShapeDetail3 = coverShapeDetail3,
        coverShapeDetail4 = coverShapeDetail4,
        source = source,
        mixSongCount = mixSongCount,
        mixArtistIds = mixArtistIds?.let {
            runCatching { mixListJson.decodeFromString<List<Long>>(it) }.getOrNull()
        },
        mixGenres = mixGenres?.let {
            runCatching { mixListJson.decodeFromString<List<String>>(it) }.getOrNull()
        },
    )
}

fun Playlist.toEntity(): PlaylistEntity {
    return PlaylistEntity(
        id = id,
        name = name,
        createdAt = createdAt,
        lastModified = lastModified,
        isQueueGenerated = isQueueGenerated,
        coverImageUri = coverImageUri,
        coverColorArgb = coverColorArgb,
        coverIconName = coverIconName,
        coverShapeType = coverShapeType,
        coverShapeDetail1 = coverShapeDetail1,
        coverShapeDetail2 = coverShapeDetail2,
        coverShapeDetail3 = coverShapeDetail3,
        coverShapeDetail4 = coverShapeDetail4,
        source = source,
        mixSongCount = mixSongCount,
        mixArtistIds = mixArtistIds?.let { mixListJson.encodeToString(it) },
        mixGenres = mixGenres?.let { mixListJson.encodeToString(it) },
    )
}
