package com.startune.music.data.lastfm

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for the Startune Last.fm proxy service
 * (app-services: POST /api/lastfm/..., key and secret held server-side).
 *
 * The proxy contract mirrors the old Startune (Expo) client: session login with
 * username/password, now-playing on track start, one scrobble per call, and
 * artist info for bio + artwork.
 */
interface LastFmApiService {

    @POST("api/lastfm/session")
    suspend fun createSession(@Body body: LastFmSessionRequest): Response<LastFmSessionResponse>

    @POST("api/lastfm/now-playing")
    suspend fun updateNowPlaying(@Body body: LastFmNowPlayingRequest): Response<Unit>

    @POST("api/lastfm/scrobble")
    suspend fun scrobble(@Body body: LastFmScrobbleRequest): Response<Unit>

    @POST("api/lastfm/artist-info")
    suspend fun getArtistInfo(@Body body: LastFmArtistInfoRequest): Response<LastFmArtistInfoResponse>
}
