package com.startune.music.data.lastfm

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import com.startune.music.data.database.LastFmDao
import com.startune.music.data.database.LastFmPendingScrobbleEntity
import com.startune.music.data.worker.LastFmFlushWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the Last.fm account (session key in EncryptedSharedPreferences, mirroring the
 * ListenBrainz credential pattern) and all proxy submission paths.
 *
 * Scrobbling is opt-in: with no stored session, every path here is a no-op.
 * The API key and secret never leave the proxy; the app only sends the session key.
 */
@Singleton
class LastFmRepository @Inject constructor(
    private val api: LastFmApiService,
    private val lastFmDao: LastFmDao,
    private val workManager: WorkManager,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LastFmRepo"
        private const val PREFS_NAME = "lastfm_prefs"
        private const val KEY_SESSION = "session_key"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_AUTH_INVALID = "auth_invalid"
        private const val KEY_SCROBBLE_ENABLED = "scrobble_enabled"
        private const val KEY_NOW_PLAYING_ENABLED = "now_playing_enabled"
        private const val KEY_MIN_DURATION_S = "min_duration_s"
        private const val KEY_DELAY_PERCENT = "delay_percent"
        private const val KEY_MAX_DELAY_S = "max_delay_s"
        private const val MAX_QUEUE_SIZE = 3000
    }

    private val prefs: SharedPreferences = createCredentialPrefs()

    /** Serializes queue mutation against disconnect so consent revocation is a clean boundary. */
    private val queueMutex = Mutex()

    @Volatile
    private var cachedSessionKey: String? = prefs.getString(KEY_SESSION, null)

    private fun readConfig(): LastFmScrobbleConfig = LastFmScrobbleConfig(
        isEnabled = prefs.getBoolean(KEY_SCROBBLE_ENABLED, false),
        nowPlayingEnabled = prefs.getBoolean(KEY_NOW_PLAYING_ENABLED, true),
        minimumTrackDurationSeconds = prefs.getInt(
            KEY_MIN_DURATION_S,
            LastFmScrobbleConfig.DEFAULT_MIN_TRACK_DURATION_SECONDS
        ),
        scrobbleDelayPercent = prefs.getInt(
            KEY_DELAY_PERCENT,
            LastFmScrobbleConfig.DEFAULT_SCROBBLE_DELAY_PERCENT
        ),
        maxScrobbleDelaySeconds = prefs.getInt(
            KEY_MAX_DELAY_S,
            LastFmScrobbleConfig.DEFAULT_MAX_SCROBBLE_DELAY_SECONDS
        )
    )

    private val _accountState = MutableStateFlow(
        LastFmAccountState(
            isConnected = cachedSessionKey != null,
            userName = prefs.getString(KEY_USER_NAME, null),
            needsReauth = prefs.getBoolean(KEY_AUTH_INVALID, false),
            config = readConfig()
        )
    )
    val accountState: StateFlow<LastFmAccountState> = _accountState.asStateFlow()

    val pendingScrobbleCount = lastFmDao.countFlow()

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun createCredentialPrefs(): SharedPreferences = try {
        createEncryptedPrefs()
    } catch (e: Exception) {
        Timber.e(e, "$TAG: EncryptedSharedPreferences unreadable, deleting and recreating")
        context.deleteSharedPreferences(PREFS_NAME)
        try {
            createEncryptedPrefs()
        } catch (e2: Exception) {
            Timber.e(e2, "$TAG: Encrypted prefs unavailable, falling back to plain")
            context.getSharedPreferences("${PREFS_NAME}_plain", Context.MODE_PRIVATE)
        }
    }

    fun hasSession(): Boolean = cachedSessionKey != null

    /** True when a session is stored and not known to be invalid. */
    fun isAuthorized(): Boolean = cachedSessionKey != null && !_accountState.value.needsReauth

    suspend fun login(username: String, password: String): LastFmLoginResult {
        return try {
            val response = api.createSession(LastFmSessionRequest(username, password))
            if (!response.isSuccessful) {
                return if (response.code() == 403) {
                    LastFmLoginResult.InvalidCredentials
                } else {
                    LastFmLoginResult.TransientError
                }
            }
            val session = response.body()?.session
            val key = session?.key
            if (key.isNullOrBlank()) return LastFmLoginResult.TransientError
            val name = session.name?.takeIf { it.isNotBlank() } ?: username
            cachedSessionKey = key
            prefs.edit {
                putString(KEY_SESSION, key)
                putString(KEY_USER_NAME, name)
                putBoolean(KEY_AUTH_INVALID, false)
            }
            _accountState.value = _accountState.value.copy(
                isConnected = true,
                userName = name,
                needsReauth = false
            )
            LastFmLoginResult.Success(name)
        } catch (e: Exception) {
            Timber.d(e, "$TAG: Login failed, will retry")
            LastFmLoginResult.TransientError
        }
    }

    suspend fun disconnect() {
        queueMutex.withLock {
            cachedSessionKey = null
            prefs.edit {
                remove(KEY_SESSION)
                remove(KEY_USER_NAME)
                putBoolean(KEY_AUTH_INVALID, false)
            }
            _accountState.value = _accountState.value.copy(
                isConnected = false,
                userName = null,
                needsReauth = false
            )
            lastFmDao.clear()
        }
    }

    fun setScrobbleEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SCROBBLE_ENABLED, enabled) }
        _accountState.value = _accountState.value.copy(config = readConfig())
    }

    fun setNowPlayingEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOW_PLAYING_ENABLED, enabled) }
        _accountState.value = _accountState.value.copy(config = readConfig())
    }

    fun setScrobbleThresholds(
        minimumTrackDurationSeconds: Int,
        scrobbleDelayPercent: Int,
        maxScrobbleDelaySeconds: Int
    ) {
        prefs.edit {
            putInt(KEY_MIN_DURATION_S, minimumTrackDurationSeconds.coerceIn(0, 600))
            putInt(KEY_DELAY_PERCENT, scrobbleDelayPercent.coerceIn(5, 100))
            putInt(KEY_MAX_DELAY_S, maxScrobbleDelaySeconds.coerceIn(30, 1800))
        }
        _accountState.value = _accountState.value.copy(config = readConfig())
    }

    suspend fun submitNowPlaying(
        trackName: String,
        artistName: String,
        releaseName: String?,
        durationMs: Long?
    ): LastFmSubmitResult {
        val sessionKey = cachedSessionKey ?: return LastFmSubmitResult.AuthFailed
        return try {
            val response = api.updateNowPlaying(
                LastFmNowPlayingRequest(
                    sessionKey = sessionKey,
                    artist = artistName,
                    track = trackName,
                    album = releaseName?.takeIf { it.isNotBlank() },
                    duration = durationMs?.takeIf { it > 0 }?.let { (it / 1000).toInt() }
                )
            )
            response.toSubmitResult()
        } catch (e: Exception) {
            Timber.d(e, "$TAG: Now playing failed, will retry")
            LastFmSubmitResult.TransientError()
        }
    }

    suspend fun submitScrobble(
        scrobble: LastFmPendingScrobbleEntity
    ): LastFmSubmitResult {
        val sessionKey = cachedSessionKey ?: return LastFmSubmitResult.AuthFailed
        return try {
            val response = api.scrobble(
                LastFmScrobbleRequest(
                    sessionKey = sessionKey,
                    artist = scrobble.artistName,
                    track = scrobble.trackName,
                    timestamp = scrobble.listenedAtMs / 1000,
                    album = scrobble.releaseName?.takeIf { it.isNotBlank() }
                )
            )
            response.toSubmitResult()
        } catch (e: Exception) {
            Timber.d(e, "$TAG: Scrobble failed, will retry")
            LastFmSubmitResult.TransientError()
        }
    }

    /**
     * Inserts under the same lock [disconnect] takes, so a scrobble admitted before a
     * disconnect can never land in the queue after consent was revoked.
     * Returns false when disconnected.
     */
    suspend fun enqueueScrobble(scrobble: LastFmPendingScrobbleEntity): Boolean {
        return queueMutex.withLock {
            if (cachedSessionKey == null) return@withLock false
            lastFmDao.insert(scrobble)
            val overflow = lastFmDao.count() - MAX_QUEUE_SIZE
            if (overflow > 0) {
                lastFmDao.deleteOldest(overflow)
                Timber.w("Last.fm queue capped at %d, dropped %d oldest scrobbles", MAX_QUEUE_SIZE, overflow)
            }
            true
        }
    }

    fun scheduleFlush() {
        workManager.enqueueUniqueWork(
            LastFmFlushWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            LastFmFlushWorker.request()
        )
    }

    fun scheduleFlushAfter(delaySeconds: Long) {
        workManager.enqueueUniqueWork(
            LastFmFlushWorker.WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            LastFmFlushWorker.request(initialDelaySeconds = delaySeconds)
        )
    }

    /**
     * Fetches artist bio + artwork through the proxy. The caller caches the result in
     * Room so repeated artist views cost no proxy usage.
     */
    suspend fun fetchArtistBio(artistName: String): LastFmArtistBio? {
        return try {
            val response = api.getArtistInfo(LastFmArtistInfoRequest(artist = artistName))
            if (!response.isSuccessful) return null
            val artist = response.body()?.artist ?: return null
            val rawBio = artist.bio?.content?.takeIf { it.isNotBlank() }
                ?: artist.bio?.summary?.takeIf { it.isNotBlank() }
                ?: return null
            val bio = cleanBio(rawBio) ?: return null
            val imageUrl = artist.image
                .filter { !it.text.isNullOrBlank() }
                .maxByOrNull { imageSizeRank(it.size) }
                ?.text
            LastFmArtistBio(bio = bio, imageUrl = imageUrl)
        } catch (e: Exception) {
            Timber.d(e, "$TAG: Artist info failed for $artistName")
            null
        }
    }

    private fun <T> Response<T>.toSubmitResult(): LastFmSubmitResult {
        return when {
            isSuccessful -> LastFmSubmitResult.Success
            code() == 403 -> {
                markAuthInvalid()
                LastFmSubmitResult.AuthFailed
            }
            code() == 400 -> LastFmSubmitResult.InvalidPayload
            else -> LastFmSubmitResult.TransientError()
        }
    }

    private fun markAuthInvalid() {
        prefs.edit { putBoolean(KEY_AUTH_INVALID, true) }
        _accountState.value = _accountState.value.copy(needsReauth = true)
    }

    private fun imageSizeRank(size: String?): Int = when (size) {
        "mega" -> 5
        "extralarge" -> 4
        "large" -> 3
        "medium" -> 2
        "small" -> 1
        else -> 0
    }

    /**
     * Last.fm bios are HTML; the summary ends with a "Read more on Last.fm" link.
     * Strips tags and entities down to plain text.
     */
    private fun cleanBio(raw: String): String? {
        val noTags = raw.replace(Regex("<[^>]*>"), " ")
        val decoded = noTags
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
        val cleaned = decoded
            .replace(Regex("Read more on Last\\.fm\\.?\\s*$"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        return cleaned.takeIf { it.isNotBlank() }
    }
}
