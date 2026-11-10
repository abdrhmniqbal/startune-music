package com.startune.music.data.worker

import com.startune.music.utils.extractArtistsFromTitle
import com.startune.music.utils.splitArtistsByDelimiters

internal fun collectArtistNames(
    rawArtistName: String,
    title: String,
    artistDelimiters: List<String>,
    wordDelimiters: List<String> = emptyList(),
    unsplitArtists: List<String> = emptyList(),
    extractFromTitle: Boolean = true,
    caseSensitive: Boolean = false
): List<String> {
    return collectArtistNames(
        rawArtistNames = listOf(rawArtistName),
        title = title,
        artistDelimiters = artistDelimiters,
        wordDelimiters = wordDelimiters,
        unsplitArtists = unsplitArtists,
        extractFromTitle = extractFromTitle,
        caseSensitive = caseSensitive
    )
}

/**
 * Splits every ARTIST field before de-duplicating the result. The de-duplication is
 * case-insensitive unless [caseSensitive] is true, in which case names that differ only
 * by letter case (for example LiSA and Lisa) stay separate.
 * The physical tag order is retained, so the first value remains the primary artist.
 */
internal fun collectArtistNames(
    rawArtistNames: List<String>,
    title: String,
    artistDelimiters: List<String>,
    wordDelimiters: List<String> = emptyList(),
    unsplitArtists: List<String> = emptyList(),
    extractFromTitle: Boolean = true,
    caseSensitive: Boolean = false
): List<String> {
    val splitFromArtist = mutableListOf<String>()
    rawArtistNames.forEach { rawArtistName ->
        rawArtistName
            .splitArtistsByDelimiters(artistDelimiters, wordDelimiters, unsplitArtists)
            .forEach { artistName ->
                if (splitFromArtist.none { it.equals(artistName, ignoreCase = !caseSensitive) }) {
                    splitFromArtist += artistName
                }
            }
    }
    if (!extractFromTitle) {
        return splitFromArtist
    }

    val (_, titleArtists) = title.extractArtistsFromTitle(artistDelimiters, wordDelimiters)
    if (titleArtists.isEmpty()) {
        return splitFromArtist
    }

    val combined = splitFromArtist.toMutableList()
    titleArtists.forEach { titleArtist ->
        if (combined.none { it.equals(titleArtist, ignoreCase = !caseSensitive) }) {
            combined.add(titleArtist)
        }
    }
    return combined
}

internal fun choosePreferredArtistName(
    localArtistName: String,
    mediaStoreArtistName: String,
    artistDelimiters: List<String>,
    wordDelimiters: List<String> = emptyList()
): String {
    val localTrimmed = localArtistName.trim()
    val mediaTrimmed = mediaStoreArtistName.trim()

    if (localTrimmed.isBlank()) return mediaStoreArtistName
    if (mediaTrimmed.isBlank()) return localArtistName

    val localArtists = localTrimmed.splitArtistsByDelimiters(artistDelimiters, wordDelimiters)
    val mediaArtists = mediaTrimmed.splitArtistsByDelimiters(artistDelimiters, wordDelimiters)

    return when {
        mediaArtists.size > localArtists.size -> mediaStoreArtistName
        localArtists.size > mediaArtists.size -> localArtistName
        mediaTrimmed.length > localTrimmed.length -> mediaStoreArtistName
        localTrimmed.length > mediaTrimmed.length -> localArtistName
        else -> mediaStoreArtistName
    }
}
