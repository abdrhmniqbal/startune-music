package com.startune.music.data.media

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheEvictor
import androidx.media3.datasource.cache.CacheSpan
import java.util.TreeSet

/**
 * Least-recently-used evictor whose limit can change at runtime. It mirrors
 * media3's LeastRecentlyUsedCacheEvictor, whose max is final and which only
 * evicts on cache writes; this one reads the limit from [maxBytesProvider]
 * on every eviction pass, and [trim] applies the current limit right now so
 * a settings change takes effect without restarting the app or the player.
 */
@OptIn(UnstableApi::class)
class DynamicCacheEvictor(
    private val maxBytesProvider: () -> Long
) : CacheEvictor {
    private val leastRecentlyUsed = TreeSet<CacheSpan> { lhs, rhs -> compare(lhs, rhs) }
    private var currentSize = 0L
    private var cache: Cache? = null

    override fun requiresCacheSpanTouches(): Boolean = true

    override fun onCacheInitialized() {
        // Nothing to seed: spans arrive through onSpanAdded during the load.
    }

    override fun onStartFile(cache: Cache, key: String, position: Long, length: Long) {
        this.cache = cache
        if (length != C.LENGTH_UNSET) {
            synchronized(this) { evictCache(cache, length) }
        }
    }

    override fun onSpanAdded(cache: Cache, span: CacheSpan) {
        this.cache = cache
        synchronized(this) {
            leastRecentlyUsed.add(span)
            currentSize += span.length
            evictCache(cache, 0)
        }
    }

    override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
        synchronized(this) {
            leastRecentlyUsed.remove(span)
            currentSize -= span.length
        }
    }

    override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) {
        onSpanRemoved(cache, oldSpan)
        onSpanAdded(cache, newSpan)
    }

    /** Applies the current limit right now, evicting the oldest spans first. */
    fun trim() {
        val cached = cache ?: return
        synchronized(this) { evictCache(cached, 0) }
    }

    private fun evictCache(cache: Cache, requiredSpace: Long) {
        while (currentSize + requiredSpace > maxBytesProvider() &&
            leastRecentlyUsed.isNotEmpty()
        ) {
            cache.removeSpan(leastRecentlyUsed.first())
        }
    }

    companion object {
        private fun compare(lhs: CacheSpan, rhs: CacheSpan): Int {
            val lastTouchTimestampDelta = lhs.lastTouchTimestamp - rhs.lastTouchTimestamp
            if (lastTouchTimestampDelta == 0L) {
                // Use the standard compareTo method as a tie-break.
                return lhs.compareTo(rhs)
            }
            return if (lhs.lastTouchTimestamp < rhs.lastTouchTimestamp) -1 else 1
        }
    }
}
