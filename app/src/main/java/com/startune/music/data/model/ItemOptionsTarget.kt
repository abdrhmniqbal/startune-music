package com.startune.music.data.model

/**
 * The item a per-item more menu was opened for. One shared options sheet
 * ([CollectionOptionsBottomSheet]) renders the actions for every target, so
 * rows only need to hand over the item itself instead of wiring a sheet each.
 */
sealed interface ItemOptionsTarget {
    /** Display title shown in the sheet header. */
    val title: String
    /** Queue name used when the target's songs are played. */
    val queueName: String

    // Property types are fully qualified: the nested class names (Album,
    // Artist, Playlist, Genre) would otherwise shadow the model types.
    data class Album(val album: com.startune.music.data.model.Album) : ItemOptionsTarget {
        override val title: String get() = album.title
        override val queueName: String get() = album.title
    }

    data class Artist(val artist: com.startune.music.data.model.Artist) : ItemOptionsTarget {
        override val title: String get() = artist.name
        override val queueName: String get() = artist.name
    }

    data class Playlist(val playlist: com.startune.music.data.model.Playlist) : ItemOptionsTarget {
        override val title: String get() = playlist.name
        override val queueName: String get() = playlist.name
    }

    data class Genre(val genre: com.startune.music.data.model.Genre) : ItemOptionsTarget {
        override val title: String get() = genre.name
        override val queueName: String get() = genre.name
    }

    data class Folder(val folder: MusicFolder) : ItemOptionsTarget {
        override val title: String get() = folder.name
        override val queueName: String get() = folder.name
    }
}
