package com.startune.music.data.model

import androidx.compose.runtime.Immutable

@Immutable
enum class MixRule(
    val storageKey: String
) {
    TOP_PLAYED(storageKey = "top_played"),
    RECENTLY_PLAYED(storageKey = "recently_played"),
    FORGOTTEN_FAVORITES(storageKey = "forgotten_favorites"),
    NEW_GEMS(storageKey = "new_gems"),
    ARTISTS(storageKey = "artists"),
    GENRES(storageKey = "genres");

    companion object {
        fun fromStorageKey(key: String?): MixRule? {
            if (key.isNullOrBlank()) return null
            return entries.firstOrNull { it.storageKey == key }
        }
    }
}

/**
 * Source prefix written for mixes created after the Smart Playlist rename.
 * Rows with the legacy "SMART" source are rewritten to this prefix by
 * MIGRATION_9_10; a bare "SMART" source (no rule key) stays recognized by
 * [isMixSource] but has no rule to refresh from.
 */
const val MIX_PLAYLIST_SOURCE_LEGACY = "SMART"
const val MIX_PLAYLIST_SOURCE_PREFIX = "MIX:"

fun MixRule.toMixSource(): String = "$MIX_PLAYLIST_SOURCE_PREFIX$storageKey"

fun MixRule.Companion.fromPlaylistSource(source: String): MixRule? {
    if (!source.startsWith(MIX_PLAYLIST_SOURCE_PREFIX)) return null
    return fromStorageKey(source.removePrefix(MIX_PLAYLIST_SOURCE_PREFIX))
}

fun isMixSource(source: String): Boolean =
    source == MIX_PLAYLIST_SOURCE_LEGACY || source.startsWith(MIX_PLAYLIST_SOURCE_PREFIX)

val Playlist.isMix: Boolean
    get() = isMixSource(source)
