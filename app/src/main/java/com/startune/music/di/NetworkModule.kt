package com.startune.music.di

import com.startune.music.data.lastfm.LastFmApiService
import com.startune.music.data.lastfm.LastFmEndpoint
import com.startune.music.data.listenbrainz.ListenBrainzApiService
import com.startune.music.data.listenbrainz.ListenBrainzEndpoint
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.io.IOException
import javax.inject.Singleton
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @ListenBrainzRetrofit
    fun provideListenBrainzRetrofit(
        okHttpClient: OkHttpClient,
        endpoint: ListenBrainzEndpoint
    ): Retrofit {
        val client = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                val rewritten = endpoint.rewrite(request.url)
                if (rewritten == request.url) {
                    chain.proceed(request)
                } else {
                    chain.proceed(request.newBuilder().url(rewritten).build())
                }
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(ListenBrainzEndpoint.DEFAULT_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideListenBrainzApiService(@ListenBrainzRetrofit retrofit: Retrofit): ListenBrainzApiService {
        return retrofit.create(ListenBrainzApiService::class.java)
    }

    /**
     * Last.fm proxy Retrofit. Requests go to the primary domain; on transport failure
     * (IOException — never on an HTTP error, so a scrobble is never submitted twice)
     * the request is retried once against the Vercel fallback host, same path.
     */
    @Provides
    @Singleton
    @LastFmRetrofit
    fun provideLastFmRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val fallback = LastFmEndpoint.FALLBACK_BASE_URL.toHttpUrl()
        val client = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request()
                try {
                    chain.proceed(request)
                } catch (e: IOException) {
                    val fallbackUrl = request.url.newBuilder()
                        .scheme(fallback.scheme)
                        .host(fallback.host)
                        .port(fallback.port)
                        .build()
                    chain.proceed(request.newBuilder().url(fallbackUrl).build())
                }
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(LastFmEndpoint.PRIMARY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideLastFmApiService(@LastFmRetrofit retrofit: Retrofit): LastFmApiService {
        return retrofit.create(LastFmApiService::class.java)
    }
}
