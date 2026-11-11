package com.startune.music.data.media

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.cache.StandaloneDatabaseProvider
import com.startune.music.data.playlist.MixBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-disk cache for streamed (online) audio. Local files never enter it: the
 * player wires this factory as the base factory of DefaultDataSource, which
 * only uses its base factory for http(s) URIs.
 *
 * Cache keys are stable: providers issue fresh per-request stream URLs
 * (Navidrome tokens), so the raw URL is not a stable key. The player resolver
 * registers every stream it resolves (resolved URL -> original cloud URI)
 * and the key factory looks that mapping up.
 *
 * A cached stream counts as expired when it has not been touched for
 * [MixBuilder.FORGOTTEN_FAVORITE_DAYS] days, the same rule as Forgotten
 * Favorites. The cache is only touched when the track plays, so untouched
 * means unplayed. SimpleCache is a singleton: two instances on one directory
 * corrupt the index.
 */
@OptIn(UnstableApi::class)
@Singleton
class StreamPlaybackCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class StreamCacheStats(
        val totalBytes: Long,
        val expiredBytes: Long,
        val expiredTrackCount: Int
    )

    companion object {
        const val DEFAULT_STREAM_CACHE_LIMIT_MB = 500
        val STREAM_CACHE_LIMIT_PRESETS_MB = listOf(0, 100, 250, 500, 1024, 2048)

        /** Snaps a stored or chosen value to the nearest preset. */
        fun sanitizeLimitMb(limitMb: Int?): Int {
            val value = limitMb ?: DEFAULT_STREAM_CACHE_LIMIT_MB
            return STREAM_CACHE_LIMIT_PRESETS_MB.minByOrNull { kotlin.math.abs(it - value) }
                ?: DEFAULT_STREAM_CACHE_LIMIT_MB
        }

        private const val CACHE_DIR_NAME = "stream_playback_cache"
        private const val DEFAULT_MAX_CACHE_BYTES =
            DEFAULT_STREAM_CACHE_LIMIT_MB * 1024L * 1024L
    }

    /** The limit is a setting (Data & Storage), not a constant: the app applies
     * the stored value at startup and on every change, so a new limit trims
     * the cache right away instead of waiting for the next stream. */
    @Volatile
    private var maxCacheBytes: Long = DEFAULT_MAX_CACHE_BYTES

    private val evictor = DynamicCacheEvictor { maxCacheBytes }

    fun setMaxCacheBytes(bytes: Long) {
        maxCacheBytes = bytes.coerceAtLeast(0L)
        evictor.trim()
    }

    private val expiryMillis =
        MixBuilder.FORGOTTEN_FAVORITE_DAYS * 24L * 60L * 60L * 1000L

    private val cacheDir: File
        get() = context.cacheDir.resolve(CACHE_DIR_NAME)

    private val stableKeys = ConcurrentHashMap<String, String>()

    private val simpleCache: SimpleCache by lazy {
        SimpleCache(
            cacheDir,
            evictor,
            StandaloneDatabaseProvider(context)
        )
    }

    fun dataSourceFactory(): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
            .setCacheKeyFactory { dataSpec ->
                stableKeys[dataSpec.uri.toString()] ?: dataSpec.uri.toString()
            }
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    fun registerStreamKey(resolvedUrl: String, stableKey: String) {
        stableKeys[resolvedUrl] = stableKey
    }

    /** Totals over the cache index. Call off the main thread. */
    fun stats(): StreamCacheStats {
        val expiryThreshold = System.currentTimeMillis() - expiryMillis
        var totalBytes = 0L
        var expiredBytes = 0L
        var expiredTrackCount = 0
        for (key in simpleCache.keys) {
            var keyExpiredBytes = 0L
            for (span in simpleCache.getCachedSpans(key)) {
                val length = span.length
                totalBytes += length
                if (span.lastTouchTimestamp < expiryThreshold) {
                    keyExpiredBytes += length
                }
            }
            if (keyExpiredBytes > 0L) {
                expiredBytes += keyExpiredBytes
                expiredTrackCount += 1
            }
        }
        return StreamCacheStats(totalBytes, expiredBytes, expiredTrackCount)
    }

    /** Removes every cached stream. Call off the main thread. */
    fun clear() {
        for (key in simpleCache.keys.toList()) {
            simpleCache.removeResource(key)
        }
    }
}
