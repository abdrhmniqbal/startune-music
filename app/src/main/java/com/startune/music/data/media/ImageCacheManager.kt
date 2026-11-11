package com.startune.music.data.media

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import coil.memory.MemoryCache
import com.startune.music.data.image.RemoteArtworkCache
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun clearAllCoverArtCaches() {
        val imageLoader = context.imageLoader
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }

    @OptIn(ExperimentalCoilApi::class)
    fun invalidateCoverArtCaches(vararg uriStrings: String?) {
        val imageLoader = context.imageLoader
        val memoryCache = imageLoader.memoryCache
        val diskCache = imageLoader.diskCache
        if (memoryCache == null && diskCache == null) return

        val knownSizeSuffixes = listOf(null, "128x128", "150x150", "168x168", "256x256", "300x300", "512x512", "600x600", "800x800")

        uriStrings.mapNotNull { it?.takeIf(String::isNotBlank) }.forEach { baseUri ->
            if (com.startune.music.utils.LocalArtworkUri.isLocalArtworkUri(baseUri)) {
                com.startune.music.utils.LocalArtworkUri.parseSongId(baseUri)?.let { songId ->
                    com.startune.music.utils.AlbumArtUtils.clearCacheForSong(context, songId)
                }
            }

            knownSizeSuffixes.forEach { suffix ->
                val cacheKey = suffix?.let { "${baseUri}_${it}" } ?: baseUri
                memoryCache?.remove(MemoryCache.Key(cacheKey))
                diskCache?.remove(cacheKey)
            }
        }
    }

    /**
     * Total bytes of cached artwork: Coil's disk cache plus the remote-cover
     * files (Jellyfin/Navidrome) stored directly in cacheDir. Call off the
     * main thread.
     */
    fun artworkCacheSizeBytes(): Long {
        val coilSize = context.imageLoader.diskCache?.size ?: 0L
        val remoteSize = context.cacheDir.listFiles()
            ?.filter { RemoteArtworkCache.isCachedImage(it) }
            ?.sumOf { it.length() }
            ?: 0L
        return coilSize + remoteSize
    }

    /** Deletes the remote-cover files. Pair with [clearAllCoverArtCaches]. */
    fun clearRemoteArtworkCache() {
        context.cacheDir.listFiles()
            ?.filter { RemoteArtworkCache.isCachedImage(it) }
            ?.forEach { it.delete() }
    }

}
