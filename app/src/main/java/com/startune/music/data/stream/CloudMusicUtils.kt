package com.startune.music.data.stream

import com.startune.music.utils.maskUnsplitValues
import com.startune.music.utils.restoreMaskedValues

/**
 * Shared data class for bulk sync operations across cloud music repositories.
 */
data class BulkSyncResult(
    val playlistCount: Int,
    val syncedSongCount: Int,
    val failedPlaylistCount: Int
)

/**
 * Shared utility functions for cloud music repositories.
 */
object CloudMusicUtils {

    /** Disc numbers are 1-based; a missing/zero disc is stored as null. */
    fun normalizeDiscNumber(rawDiscNumber: Int): Int? = rawDiscNumber.takeIf { it > 0 }

    /** Split a raw artist string like "A, B & C" into individual names. */
    fun parseArtistNames(rawArtist: String, unsplitArtists: List<String> = emptyList()): List<String> {
        if (rawArtist.isBlank()) return listOf("Unknown Artist")
        val (masked, placeholders) = maskUnsplitValues(rawArtist, unsplitArtists)
        val parsed = restoreMaskedValues(
            masked.split(Regex("\\s*[,/&;+、]\\s*"))
                .map { it.trim() }
                .filter { it.isNotBlank() },
            placeholders
        ).distinct()
        return if (parsed.isEmpty()) listOf("Unknown Artist") else parsed
    }
}
