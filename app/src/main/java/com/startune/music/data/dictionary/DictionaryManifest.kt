package com.startune.music.data.dictionary

import kotlinx.serialization.Serializable

/** A dictionary manifest: the list of packs the app can download. The file
 * lives in the startune-dictionaries repo; the app fetches it when the
 * Dictionaries screen opens and falls back to [DictionaryManager.BAKED_IN_PACKS]
 * when offline. */
@Serializable
data class DictionaryManifest(
    val version: Int = 1,
    val packs: List<DictionaryPack> = emptyList()
)

@Serializable
data class DictionaryPack(
    val id: String,
    val name: String,
    val description: String = "",
    val version: Int,
    val downloadSizeBytes: Long,
    val installedSizeBytes: Long,
    val sha256: String,
    val url: String
)
