package com.startune.music.data.model

/** Entity kinds a playback queue can come from and be jumped back into. */
enum class PlayedEntityType {
    ALBUM,
    ARTIST,
    PLAYLIST,
    MIX
}

/**
 * The structured source of a playback queue: what the user played FROM.
 *
 * Today the queue only carries a display string for its source name,
 * so Jump back in and the player's "From" line have nothing to work with. This carries
 * the type, the entity id, and the display name together, from the screen that started
 * playback to the single place that builds the queue.
 *
 * [historyType], [historyId] and [historyName] are the play-history row for the source.
 * They are null for [Search]: a search is not an entity that can be jumped back into,
 * so search plays are carried as the current source only and never written to history.
 */
sealed interface PlaybackSource {
    val historyType: PlayedEntityType?
    val historyId: String?
    val historyName: String?

    data class Album(val albumId: Long, val name: String) : PlaybackSource {
        override val historyType = PlayedEntityType.ALBUM
        override val historyId = albumId.toString()
        override val historyName = name
    }

    data class Artist(val artistId: Long, val name: String) : PlaybackSource {
        override val historyType = PlayedEntityType.ARTIST
        override val historyId = artistId.toString()
        override val historyName = name
    }

    data class Playlist(val playlistId: String, val name: String) : PlaybackSource {
        override val historyType = PlayedEntityType.PLAYLIST
        override val historyId = playlistId
        override val historyName = name
    }

    data class Mix(val mixId: String, val name: String) : PlaybackSource {
        override val historyType = PlayedEntityType.MIX
        override val historyId = mixId
        override val historyName = name
    }

    data class Search(val query: String) : PlaybackSource {
        override val historyType = null
        override val historyId = null
        override val historyName = null
    }
}
