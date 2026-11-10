package com.startune.music.data.worker

import com.startune.music.data.jellyfin.JellyfinRepository
import com.startune.music.data.navidrome.NavidromeRepository
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class CloudSyncCoordinator @Inject constructor(
    private val jellyfinRepository: JellyfinRepository,
    private val navidromeRepository: NavidromeRepository
) {
    val needsActiveCloudSync: Boolean
        get() = false

    suspend fun syncUnifiedCloudLibraries() {
        syncJellyfinCache()
        syncNavidromeCache()
    }

    private suspend fun syncJellyfinCache() {
        if (!jellyfinRepository.isLoggedIn) return

        try {
            jellyfinRepository.syncUnifiedLibrarySongsFromJellyfin()
        } catch (error: Exception) {
            Timber.tag(TAG).e(error, "Failed to sync Jellyfin cached data")
        }
    }

    private suspend fun syncNavidromeCache() {
        if (!navidromeRepository.isLoggedIn) return

        try {
            navidromeRepository.syncUnifiedLibrarySongsFromNavidrome()
        } catch (error: Exception) {
            Timber.tag(TAG).e(error, "Failed to sync Navidrome cached data")
        }
    }

    private companion object {
        private const val TAG = "CloudSyncCoordinator"
    }
}
