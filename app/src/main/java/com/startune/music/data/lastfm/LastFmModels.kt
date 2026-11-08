package com.startune.music.data.lastfm

import com.google.gson.annotations.SerializedName

/**
 * Proxy base URLs. The primary is Iqbal's custom domain; the fallback is the same
 * app-services deployment on Vercel, used only when the primary is unreachable
 * (for example when the domain lapses). The fallback is tried only on transport
 * failure, never on an HTTP error, so a scrobble is never submitted twice.
 */
object LastFmEndpoint {
    const val PRIMARY_BASE_URL = "https://app-services.startune.web.id/"
    const val FALLBACK_BASE_URL = "https://startune-services.vercel.app/"
}

data class LastFmSessionRequest(
    val username: String,
    val password: String
)

data class LastFmSessionResponse(
    val session: LastFmSession? = null
)

data class LastFmSession(
    val name: String? = null,
    val key: String? = null
)

data class LastFmNowPlayingRequest(
    val sessionKey: String,
    val artist: String,
    val track: String,
    val album: String? = null,
    /** Track duration in seconds. */
    val duration: Int? = null
)

data class LastFmScrobbleRequest(
    val sessionKey: String,
    val artist: String,
    val track: String,
    /** Epoch seconds of when the listen started. */
    val timestamp: Long,
    val album: String? = null
)

data class LastFmArtistInfoRequest(
    val artist: String,
    val lang: String? = null,
    val username: String? = null
)

data class LastFmArtistInfoResponse(
    val artist: LastFmArtistInfo? = null
)

data class LastFmArtistInfo(
    val name: String? = null,
    val mbid: String? = null,
    val url: String? = null,
    val image: List<LastFmImage> = emptyList(),
    val bio: LastFmBio? = null
)

data class LastFmImage(
    @SerializedName("#text") val text: String? = null,
    val size: String? = null
)

data class LastFmBio(
    val summary: String? = null,
    val content: String? = null
)

/** Scrobble configuration, mirroring the old Startune defaults. */
data class LastFmScrobbleConfig(
    val isEnabled: Boolean = false,
    val nowPlayingEnabled: Boolean = true,
    val minimumTrackDurationSeconds: Int = DEFAULT_MIN_TRACK_DURATION_SECONDS,
    val scrobbleDelayPercent: Int = DEFAULT_SCROBBLE_DELAY_PERCENT,
    val maxScrobbleDelaySeconds: Int = DEFAULT_MAX_SCROBBLE_DELAY_SECONDS
) {
    companion object {
        const val DEFAULT_MIN_TRACK_DURATION_SECONDS = 30
        const val DEFAULT_SCROBBLE_DELAY_PERCENT = 30
        const val DEFAULT_MAX_SCROBBLE_DELAY_SECONDS = 240
    }
}

data class LastFmAccountState(
    val isConnected: Boolean = false,
    val userName: String? = null,
    val needsReauth: Boolean = false,
    val config: LastFmScrobbleConfig = LastFmScrobbleConfig()
)

sealed interface LastFmSubmitResult {
    data object Success : LastFmSubmitResult
    data object AuthFailed : LastFmSubmitResult
    data object InvalidPayload : LastFmSubmitResult
    data class TransientError(val retryAfterSeconds: Long? = null) : LastFmSubmitResult
}

sealed interface LastFmLoginResult {
    data class Success(val userName: String) : LastFmLoginResult
    data object InvalidCredentials : LastFmLoginResult
    data object TransientError : LastFmLoginResult
}

/** Parsed artist bio + artwork from artist.getInfo, ready to cache in Room. */
data class LastFmArtistBio(
    val bio: String,
    val imageUrl: String?
)
