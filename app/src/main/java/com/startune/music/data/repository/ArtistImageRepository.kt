package com.startune.music.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import com.startune.music.data.database.MusicDao
import com.startune.music.data.network.deezer.DeezerApiService
import com.startune.music.data.preferences.UserPreferencesRepository
import com.startune.music.utils.NetworkRetryUtils
import com.startune.music.utils.isRetryableNetworkError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import androidx.core.graphics.scale
import com.startune.music.data.network.deezer.DeezerArtist
import com.startune.music.data.network.deezer.DeezerArtistCandidate
import java.io.ByteArrayOutputStream
import java.net.URL

/**
 * Repository for fetching and caching artist images from Deezer API.
 * Uses both in-memory LRU cache and Room database for persistent storage.
 */
@Singleton
class ArtistImageRepository @Inject constructor(
    private val deezerApiService: DeezerApiService,
    private val musicDao: MusicDao,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    companion object {
        private const val TAG = "ArtistImageRepository"
        private const val CACHE_SIZE = 100
        private const val PREFETCH_CONCURRENCY = 3
        private val deezerSizeRegex = Regex("/\\d{2,4}x\\d{2,4}([\\-.])")
        private const val NETWORK_RETRY_ATTEMPTS = 3
        private const val NETWORK_RETRY_INITIAL_DELAY_MS = 500L
        private const val MAX_CUSTOM_IMAGE_SOURCE_BYTES = 24L * 1024L * 1024L
        private const val MAX_CUSTOM_IMAGE_DIMENSION_PX = 16_384
        private const val MAX_CUSTOM_IMAGE_TOTAL_PIXELS = 80_000_000L
        private const val TARGET_CUSTOM_IMAGE_MAX_DIMENSION_PX = 2_048
        private const val TARGET_CUSTOM_IMAGE_MAX_PIXELS = 4_194_304L

        // Deezer placeholder silhouettes, ported from old Startune. A candidate
        // whose picture is one of these has no real artist photo.
        private val DEEZER_DEFAULT_AVATAR_HASHES = listOf(
            "e23066163f21176a822b54001ee648a2",
            "270b9a0569709219d84e115ceba415f9",
            "d41d8cd98f00b204e9800998ecf8427e"
        )

        internal fun calculateCustomImageSampleSize(width: Int, height: Int): Int {
            var sampleSize = 1
            while (
                width / sampleSize > TARGET_CUSTOM_IMAGE_MAX_DIMENSION_PX ||
                height / sampleSize > TARGET_CUSTOM_IMAGE_MAX_DIMENSION_PX ||
                (width.toLong() / sampleSize) * (height.toLong() / sampleSize) > TARGET_CUSTOM_IMAGE_MAX_PIXELS
            ) {
                sampleSize = sampleSize shl 1
            }
            return sampleSize.coerceAtLeast(1)
        }
    }

    private val memoryCache = LruCache<String, String>(CACHE_SIZE)
    
    private val fetchMutex = Mutex()
    private val pendingFetches = mutableSetOf<String>()
    
    private val prefetchSemaphore = Semaphore(PREFETCH_CONCURRENCY)
    
    private val failedFetches = mutableSetOf<String>()

    /**
     * Get artist image URL, fetching from Deezer if not cached.
     * @param artistName Name of the artist
     * @param artistId Room database ID of the artist (for caching)
     * @return Image URL or null if not found
     */
    suspend fun getArtistImageUrl(artistName: String, artistId: Long): String? {
        if (artistName.isBlank()) return null
        if (!userPreferencesRepository.externalArtistImagesEnabledFlow.first()) return null

        val caseSensitiveArtists = userPreferencesRepository.caseSensitiveArtistsFlow.first()
        val cacheKey = artistCacheKey(artistName, caseSensitiveArtists)

        memoryCache.get(cacheKey)?.let { cachedUrl ->
            return cachedUrl
        }
        
        if (failedFetches.contains(cacheKey)) {
            return null
        }

        val (resolvedArtistId, dbCachedUrl) = withContext(Dispatchers.IO) {
            val canonicalArtistId = if (caseSensitiveArtists) {
                musicDao.getArtistIdByName(artistName.trim()) ?: artistId
            } else {
                musicDao.getArtistIdByNormalizedName(artistName) ?: artistId
            }
            val cachedUrl = musicDao.getArtistImageUrl(canonicalArtistId)
                ?: if (caseSensitiveArtists) null else musicDao.getArtistImageUrlByNormalizedName(artistName)
            canonicalArtistId to cachedUrl
        }
        if (!dbCachedUrl.isNullOrEmpty()) {
            val upgradedDbUrl = upgradeToHighResDeezerUrl(dbCachedUrl)
            memoryCache.put(cacheKey, upgradedDbUrl)
            if (upgradedDbUrl != dbCachedUrl) {
                withContext(Dispatchers.IO) {
                    musicDao.updateArtistImageUrl(resolvedArtistId, upgradedDbUrl)
                }
            }
            return upgradedDbUrl
        }

        return fetchAndCacheArtistImage(artistName, resolvedArtistId, cacheKey)
    }

    private fun artistCacheKey(artistName: String, caseSensitive: Boolean): String {
        val trimmed = artistName.trim()
        return if (caseSensitive) trimmed else trimmed.lowercase()
    }

    /**
     * Prefetch artist images for a list of artists in background.
     * Useful for batch loading when displaying artist lists.
     */
    suspend fun prefetchArtistImages(artists: List<Pair<Long, String>>) = withContext(Dispatchers.IO) {
        if (!userPreferencesRepository.externalArtistImagesEnabledFlow.first()) return@withContext

        val caseSensitiveArtists = userPreferencesRepository.caseSensitiveArtistsFlow.first()
        artists.chunked(PREFETCH_CONCURRENCY * 4).forEach { chunk ->
            chunk.map { (artistId, artistName) ->
                async {
                    try {
                        val cacheKey = artistCacheKey(artistName, caseSensitiveArtists)
                        if (memoryCache.get(cacheKey) == null && !failedFetches.contains(cacheKey)) {
                            prefetchSemaphore.withPermit {
                                getArtistImageUrl(artistName, artistId)
                            }
                        } else {
                            Timber.tag(TAG).d("Skipping prefetch for $artistName")
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Timber.tag(TAG).w("Failed to prefetch image for $artistName: ${e.message}")
                    }
                }
            }.awaitAll()
        }
    }
    
    private suspend fun fetchAndCacheArtistImage(
        artistName: String,
        artistId: Long,
        normalizedName: String
    ): String? {
        fetchMutex.withLock {
            if (pendingFetches.contains(normalizedName)) {
                return null
            }
            pendingFetches.add(normalizedName)
        }

        return try {
            withContext(Dispatchers.IO) {
                val response = withNetworkRetry("deezer_search:$artistName") {
                    deezerApiService.searchArtist(artistName, limit = 10)
                }
                val deezerArtist = selectArtistCandidate(
                    preferRealPhotos(response.data),
                    artistName
                )

                if (deezerArtist != null) {
                    val imageUrl = (
                        deezerArtist.pictureXl
                            ?: deezerArtist.pictureBig
                            ?: deezerArtist.pictureMedium
                            ?: deezerArtist.picture
                        )?.let(::upgradeToHighResDeezerUrl)

                    if (!imageUrl.isNullOrEmpty()) {
                        memoryCache.put(normalizedName, imageUrl)
                        
                        musicDao.updateArtistImageUrl(artistId, imageUrl)

                        Timber.tag(TAG).d("Fetched and cached image for $artistName: $imageUrl")
                        imageUrl
                    } else {
                        null
                    }
                } else {
                    Timber.tag(TAG).d("No Deezer artist found for: $artistName")
                    failedFetches.add(normalizedName)
                    null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e("Error fetching artist image for $artistName: ${e.message}")
            if(e !is java.net.SocketTimeoutException) {
                failedFetches.add(normalizedName)
            }
            null
        } finally {
            fetchMutex.withLock {
                pendingFetches.remove(normalizedName)
            }
        }
    }

    private suspend fun <T> withNetworkRetry(
        operationName: String,
        maxAttempts: Int = NETWORK_RETRY_ATTEMPTS,
        initialDelayMs: Long = NETWORK_RETRY_INITIAL_DELAY_MS,
        block: suspend () -> T
    ): T {
        return NetworkRetryUtils.withNetworkRetry(
            operationName = operationName,
            maxAttempts = maxAttempts,
            initialDelayMs = initialDelayMs,
            shouldRetry = { throwable -> throwable.isRetryableNetworkError() },
            onRetry = { attempt, attempts, throwable ->
                Timber.tag(TAG)
                    .d("Retrying $operationName after failure ($attempt/$attempts): ${throwable.message}")
            },
            block = block
        )
    }
    
    /**
     * Search Deezer for artists matching [query], for the artwork picker.
     * Returns candidates with a real photo, in Deezer's relevance order.
     */
    suspend fun searchDeezerArtists(query: String, limit: Int = 25): List<DeezerArtistCandidate> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        return try {
            val response = withNetworkRetry("deezer_search:$trimmed") {
                deezerApiService.searchArtist(trimmed, limit)
            }
            response.data.mapNotNull { artist ->
                val picture = bestPictureUrl(artist)?.let(::upgradeToHighResDeezerUrl)
                if (picture.isNullOrEmpty() || isDeezerDefaultAvatar(picture)) {
                    null
                } else {
                    DeezerArtistCandidate(
                        id = artist.id,
                        name = artist.name,
                        pictureUrl = picture,
                        fanCount = artist.fanCount,
                        albumCount = artist.albumCount
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e("Error searching Deezer artists for $trimmed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Pick the best Deezer match for an artist name: exact name, then the same
     * name ignoring case and punctuation, then the first result. Ported from
     * old Startune's selectArtistCandidate.
     */
    private fun selectArtistCandidate(candidates: List<DeezerArtist>, artistName: String): DeezerArtist? {
        if (candidates.isEmpty()) return null
        val normalized = normalizeArtistName(artistName)
        return candidates.firstOrNull { it.name == artistName }
            ?: candidates.firstOrNull { normalizeArtistName(it.name) == normalized }
            ?: candidates.firstOrNull()
    }

    private fun normalizeArtistName(value: String): String =
        value.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }

    private fun preferRealPhotos(candidates: List<DeezerArtist>): List<DeezerArtist> {
        val withRealPhoto = candidates.filter { !isDeezerDefaultAvatar(bestPictureUrl(it)) }
        return withRealPhoto.ifEmpty { candidates }
    }

    private fun bestPictureUrl(artist: DeezerArtist): String? =
        artist.pictureXl ?: artist.pictureBig ?: artist.pictureMedium ?: artist.picture

    private fun isDeezerDefaultAvatar(url: String?): Boolean {
        if (url.isNullOrEmpty()) return true
        if (
            url.contains("/artist//") ||
            url.contains("/artist/default") ||
            url.contains("default_artist")
        ) {
            return true
        }
        return DEEZER_DEFAULT_AVATAR_HASHES.any { url.contains(it) }
    }

    /**
     * Clear all cached images. Useful for debugging or forced refresh.
     */
    fun clearCache() {
        memoryCache.evictAll()
        failedFetches.clear()
    }

    /**
     * Returns the effective image URL for an artist:
     * - If a custom (user-set) image exists in DB → returns that path
     * - Otherwise falls back to the Deezer URL (fetching from API if needed)
     */
    suspend fun getEffectiveArtistImageUrl(artistId: Long, artistName: String): String? {
        val customUri = withContext(Dispatchers.IO) { musicDao.getArtistCustomImage(artistId) }
        if (!customUri.isNullOrBlank()) return customUri
        return getArtistImageUrl(artistName, artistId)
    }

    /**
     * Saves a user-selected image as the artist's custom image.
     *
     * The content URI is resolved immediately and the bitmap is written to
     * internal storage (filesDir/artist_art_<id>.jpg). This avoids depending
     * on a content URI that may expire once the photo-picker dismisses.
     *
     * @param context Application context (used for contentResolver and filesDir)
     * @param artistId The artist's database row ID
     * @param sourceUri URI returned by the system photo-picker
     * @return The internal file path on success, null on failure
     */
    suspend fun setCustomArtistImage(context: Context, artistId: Long, sourceUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = decodeCustomArtistBitmap(context, sourceUri) ?: return@withContext null
                persistCustomArtistBitmap(context, artistId, bitmap)
            } catch (e: Exception) {
                Timber.tag(TAG)
                    .e("Failed to save custom artist image for id=$artistId: ${e.message}")
                null
            }
        }
    }

    /**
     * Download a Deezer picture and store it as the artist's custom image,
     * same file and DB column as the photo picker, so a picked Deezer photo
     * sticks until Reset to default. Returns the internal path on success.
     */
    suspend fun setCustomArtistImageFromUrl(context: Context, artistId: Long, imageUrl: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val bytes = downloadBytes(imageUrl, MAX_CUSTOM_IMAGE_SOURCE_BYTES) ?: return@withContext null
                val bitmap = decodeCustomArtistBitmap(bytes) ?: return@withContext null
                persistCustomArtistBitmap(context, artistId, bitmap)
            } catch (e: Exception) {
                Timber.tag(TAG)
                    .e("Failed to save Deezer artist image for id=$artistId: ${e.message}")
                null
            }
        }
    }

    private suspend fun persistCustomArtistBitmap(context: Context, artistId: Long, bitmap: Bitmap): String? {
        val scaledBitmap = scaleBitmapIfNeeded(bitmap)
        try {
            val destFile = File(context.filesDir, "artist_art_${artistId}.jpg")
            FileOutputStream(destFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            val internalPath = destFile.absolutePath

            musicDao.updateArtistCustomImage(artistId, internalPath)

            Timber.tag(TAG).d("Custom artist image saved: $internalPath")
            return internalPath
        } finally {
            if (scaledBitmap !== bitmap) {
                bitmap.recycle()
            }
            scaledBitmap.recycle()
        }
    }

    private fun downloadBytes(imageUrl: String, maxBytes: Long): ByteArray? {
        val connection = URL(imageUrl).openConnection().apply {
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        connection.getInputStream().use { input ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            var total = 0L
            while (true) {
                val read = input.read(chunk)
                if (read == -1) break
                total += read
                if (total > maxBytes) {
                    Timber.tag(TAG).w("Rejected Deezer artist image larger than allowed source size: $total")
                    return null
                }
                buffer.write(chunk, 0, read)
            }
            return buffer.toByteArray()
        }
    }

    private fun checkCustomImageBounds(width: Int, height: Int): Boolean {
        if (width <= 0 || height <= 0) {
            Timber.tag(TAG)
                .w("Rejected custom artist image with invalid bounds: ${width}x${height}")
            return false
        }
        if (width > MAX_CUSTOM_IMAGE_DIMENSION_PX || height > MAX_CUSTOM_IMAGE_DIMENSION_PX) {
            Timber.tag(TAG)
                .w("Rejected custom artist image with oversized bounds: ${width}x${height}")
            return false
        }
        if (width.toLong() * height.toLong() > MAX_CUSTOM_IMAGE_TOTAL_PIXELS) {
            Timber.tag(TAG)
                .w("Rejected custom artist image with excessive pixel count: ${width}x${height}")
            return false
        }
        return true
    }

    private fun decodeCustomArtistBitmap(context: Context, sourceUri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(sourceUri)?.lowercase()
        if (mimeType != null && !mimeType.startsWith("image/")) {
            Timber.tag(TAG).w("Rejected custom artist image with unsupported MIME type: $mimeType")
            return null
        }

        runCatching { resolver.openAssetFileDescriptor(sourceUri, "r") }.getOrNull()?.use { descriptor ->
            val declaredLength = descriptor.length
            if (declaredLength > MAX_CUSTOM_IMAGE_SOURCE_BYTES) {
                Timber.tag(TAG)
                    .w("Rejected custom artist image larger than allowed source size: $declaredLength")
                return null
            }
        }

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        
        resolver.openInputStream(sourceUri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, bounds)
        }

        val width = bounds.outWidth
        val height = bounds.outHeight
        if (!checkCustomImageBounds(width, height)) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateCustomImageSampleSize(width, height)
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        return try {
            resolver.openInputStream(sourceUri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            }
        } catch (oom: OutOfMemoryError) {
            Timber.tag(TAG).e(oom, "Failed to decode custom artist image due to OOM")
            null
        }
    }

    private fun decodeCustomArtistBitmap(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val width = bounds.outWidth
        val height = bounds.outHeight
        if (!checkCustomImageBounds(width, height)) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateCustomImageSampleSize(width, height)
            inPreferredConfig = Bitmap.Config.RGB_565
        }

        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
        } catch (oom: OutOfMemoryError) {
            Timber.tag(TAG).e(oom, "Failed to decode custom artist image due to OOM")
            null
        }
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val longestEdge = max(bitmap.width, bitmap.height)
        if (longestEdge <= TARGET_CUSTOM_IMAGE_MAX_DIMENSION_PX) {
            return bitmap
        }

        val scale = TARGET_CUSTOM_IMAGE_MAX_DIMENSION_PX.toFloat() / longestEdge.toFloat()
        val scaledWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val scaledHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return bitmap.scale(scaledWidth, scaledHeight)
    }

    /**
     * Removes the user's custom artist image, reverting to the Deezer URL.
     *
     * @param context Application context
     * @param artistId The artist's database row ID
     */
    suspend fun clearCustomArtistImage(context: Context, artistId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val destFile = File(context.filesDir, "artist_art_${artistId}.jpg")
                if (destFile.exists()) {
                    destFile.delete()
                    Timber.tag(TAG).d("Deleted custom artist image file: ${destFile.absolutePath}")
                }
                musicDao.updateArtistCustomImage(artistId, null)
            } catch (e: Exception) {
                Timber.tag(TAG)
                    .e("Failed to clear custom artist image for id=$artistId: ${e.message}")
            }
        }
    }

    private fun upgradeToHighResDeezerUrl(url: String): String {
        if (!url.contains("dzcdn.net/images/artist")) return url
        return deezerSizeRegex.replace(url, "/1000x1000$1")
    }
}
