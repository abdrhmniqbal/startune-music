package com.startune.music.data.worker

import com.startune.music.data.jellyfin.JellyfinRepository
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class CloudSyncCoordinator @Inject constructor(
    private val jellyfinRepository: JellyfinRepository
) {
    val needsActiveCloudSync: Boolean
        get() = false

    suspend fun syncUnifiedCloudLibraries() {
        syncJellyfinCache()
    }

    private suspend fun syncJellyfinCache() {
        if (!jellyfinRepository.isLoggedIn) return

        try {
            jellyfinRepository.syncUnifiedLibrarySongsFromJellyfin()
        } catch (error: Exception) {
            Timber.tag(TAG).e(error, "Failed to sync Jellyfin cached data")
        }
    }

    private companion object {
        private const val TAG = "CloudSyncCoordinator"
    }
}
