package com.startune.music.data.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * One card in the Home "Jump back in" section: the most recently played album,
 * artist, playlist, or mix.
 *
 * [subtitle] is the second line when the entity has one (the artist name for an
 * album); otherwise the card shows the type label. [artworkUri] is null when the
 * entity has no artwork; the card renders a fallback tile then. Playlist and mix
 * items also carry their cover color and icon so the fallback tile paints like
 * the playlist's own cover. [collageArtworkUris] holds up to 4 distinct album
 * artworks from the playlist's or mix's songs, for the 2x2 grid shown when the
 * playlist has no custom cover image or color.
 */
data class JumpBackInItem(
    val type: PlayedEntityType,
    /** Album/artist id as text, or the playlist/mix id as-is. */
    val id: String,
    val title: String,
    val subtitle: String?,
    val artworkUri: String?,
    val coverColorArgb: Int? = null,
    val coverIconName: String? = null,
    val collageArtworkUris: ImmutableList<String> = persistentListOf()
)
