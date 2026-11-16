package com.startune.music.presentation.components

import com.startune.music.data.model.Song

/**
 * Distinct album covers for a playlist/mix collage, preferring different
 * albums. A song contributes its cover only when neither its album nor its
 * artwork URI has been seen yet, so one album never fills several cells even
 * when its songs carry different artwork URIs (per-file embedded art), and
 * two different albums sharing one cover image still collapse to one cell.
 * Songs with no album id fall back to the artwork URI alone.
 * Stops once [maxCovers] distinct covers are found, because the collage
 * never shows more than that; callers must pass enough songs for the scan
 * to reach distinct artwork instead of only the first few.
 */
internal fun distinctAlbumCovers(songs: List<Song>, maxCovers: Int = 4): List<Pair<String, String>> {
    val seenAlbumIds = HashSet<Long>()
    val seenUris = HashSet<String>()
    val out = ArrayList<Pair<String, String>>()
    for (song in songs) {
        val uri = song.albumArtUriString?.takeIf { it.isNotBlank() } ?: continue
        val albumId = song.albumId
        val albumSeen = albumId > 0 && albumId in seenAlbumIds
        val uriSeen = uri in seenUris
        if (albumSeen || uriSeen) continue
        if (albumId > 0) seenAlbumIds.add(albumId)
        seenUris.add(uri)
        out.add(uri to song.title)
        if (out.size >= maxCovers) break
    }
    return out
}
