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
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class LastFmSessionResponse(
    @SerializedName("session") val session: LastFmSession? = null
)

data class LastFmSession(
    @SerializedName("name") val name: String? = null,
    @SerializedName("key") val key: String? = null
)

data class LastFmNowPlayingRequest(
    @SerializedName("sessionKey") val sessionKey: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("track") val track: String,
    @SerializedName("album") val album: String? = null,
    /** Track duration in seconds. */
    @SerializedName("duration") val duration: Int? = null
)

data class LastFmScrobbleRequest(
    @SerializedName("sessionKey") val sessionKey: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("track") val track: String,
    /** Epoch seconds of when the listen started. */
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("album") val album: String? = null
)

data class LastFmArtistInfoRequest(
    @SerializedName("artist") val artist: String,
    @SerializedName("lang") val lang: String? = null,
    @SerializedName("username") val username: String? = null
)

data class LastFmArtistInfoResponse(
    @SerializedName("artist") val artist: LastFmArtistInfo? = null
)

data class LastFmArtistInfo(
    @SerializedName("name") val name: String? = null,
    @SerializedName("mbid") val mbid: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("image") val image: List<LastFmImage> = emptyList(),
    @SerializedName("bio") val bio: LastFmBio? = null
)

data class LastFmImage(
    @SerializedName("#text") val text: String? = null,
    @SerializedName("size") val size: String? = null
)

data class LastFmBio(
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("content") val content: String? = null
)

/** Scrobble configuration, mirroring the old Startune defaults. */
data class LastFmScrobbleConfig(
    val isEnabled: Boolean = false,
    val nowPlayingEnabled: Boolean = true,
    val minimumTrackDurationSeconds: Int = DEFAULT_MIN_TRACK_DURATION_SECONDS,
    val maxScrobbleDelaySeconds: Int = DEFAULT_MAX_SCROBBLE_DELAY_SECONDS
) {
    companion object {
        const val DEFAULT_MIN_TRACK_DURATION_SECONDS = 30
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
    data object Timeout : LastFmLoginResult
    data object NoConnection : LastFmLoginResult
    data class ServerError(val httpCode: Int) : LastFmLoginResult
    data object TransientError : LastFmLoginResult
}

/** Parsed artist bio + artwork from artist.getInfo, ready to cache in Room. */
data class LastFmArtistBio(
    val bio: String,
    val imageUrl: String?
)
