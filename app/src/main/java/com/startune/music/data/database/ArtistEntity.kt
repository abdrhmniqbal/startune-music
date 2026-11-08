package com.startune.music.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.startune.music.data.model.Artist
import com.startune.music.utils.normalizeMetadataTextOrEmpty

@Entity(
    tableName = "artists",
    indices = [Index(value = ["name"], unique = false)]
)
data class ArtistEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "track_count") val trackCount: Int,
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    @ColumnInfo(name = "custom_image_uri") val customImageUri: String? = null,
    /** Artist bio from Last.fm, cached so repeat views cost no proxy usage. */
    @ColumnInfo(name = "bio") val bio: String? = null,
    @ColumnInfo(name = "bio_updated_at_ms") val bioUpdatedAtMs: Long? = null
)

fun ArtistEntity.toArtist(): Artist {
    return Artist(
        id = this.id,
        name = this.name.normalizeMetadataTextOrEmpty(),
        songCount = this.trackCount,
        imageUrl = this.imageUrl,
        customImageUri = this.customImageUri,
        bio = this.bio,
        bioUpdatedAtMs = this.bioUpdatedAtMs
    )
}

fun List<ArtistEntity>.toArtists(): List<Artist> {
    return this.map { it.toArtist() }
}

fun Artist.toEntity(): ArtistEntity {
    return ArtistEntity(
        id = this.id,
        name = this.name,
        trackCount = this.songCount,
        imageUrl = this.imageUrl,
        customImageUri = this.customImageUri,
        bio = this.bio,
        bioUpdatedAtMs = this.bioUpdatedAtMs
    )
}
