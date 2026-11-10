package com.startune.music

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.startune.music.data.dictionary.DictionaryManager
import com.startune.music.data.preferences.UserPreferencesRepository
import com.startune.music.data.media.SongMetadataEditor
import com.startune.music.data.playlist.M3uSyncCoordinator
import com.startune.music.data.repository.ArtistImageRepository
import com.startune.music.presentation.viewmodel.LibraryStateHolder
import com.startune.music.presentation.viewmodel.ThemeStateHolder
import com.startune.music.utils.AlbumArtCacheManager
import com.startune.music.utils.MultiLangRomanizer
import com.startune.music.utils.AlbumArtUtils
import com.startune.music.utils.AppLocaleManager
import com.startune.music.utils.CrashHandler
import com.startune.music.utils.MediaMetadataRetrieverPool
import com.startune.music.utils.PhoneOrientationPolicy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class StartuneApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var imageLoader: dagger.Lazy<ImageLoader>

    @Inject
    lateinit var jellyfinCoilFetcherFactory: dagger.Lazy<com.startune.music.data.image.JellyfinCoilFetcher.Factory>

    @Inject
    lateinit var navidromeCoilFetcherFactory: dagger.Lazy<com.startune.music.data.image.NavidromeCoilFetcher.Factory>

    @Inject
    lateinit var localArtworkCoilFetcherFactory: dagger.Lazy<com.startune.music.data.image.LocalArtworkCoilFetcher.Factory>

    @Inject
    lateinit var themeStateHolder: dagger.Lazy<ThemeStateHolder>

    @Inject
    lateinit var artistImageRepository: dagger.Lazy<ArtistImageRepository>

    @Inject
    lateinit var libraryStateHolder: dagger.Lazy<LibraryStateHolder>

    @Inject
    lateinit var userPreferencesRepository: dagger.Lazy<UserPreferencesRepository>

    @Inject
    lateinit var syncManager: dagger.Lazy<com.startune.music.data.worker.SyncManager>

    @Inject
    lateinit var syncNotificationManager: dagger.Lazy<com.startune.music.data.notifications.SyncNotificationManager>

    @Inject
    lateinit var m3uSyncCoordinator: dagger.Lazy<M3uSyncCoordinator>

    @Inject
    lateinit var songMetadataEditor: dagger.Lazy<SongMetadataEditor>

    @Inject
    lateinit var dictionaryManager: dagger.Lazy<DictionaryManager>

    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "startune_music_channel"
    }

    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            libraryStateHolder.get().restoreAfterTrimIfNeeded()
            m3uSyncCoordinator.get().onAppForeground()
            // Gives songs cut off by an interrupted tag save (a failed write-back, or the app
            // being killed mid-write) their edited audio back, and lists the ones the app can no
            // longer write to so the player can ask the user for access.
            startupScope.launch { songMetadataEditor.get().resumeInterruptedWriteBacks() }
        }

    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocaleManager.wrapContext(base))
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.BUILD_TYPE != "benchmark") {
            CrashHandler.install(this)
        }

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Startune Music Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    com.startune.music.data.notifications.SyncNotificationManager.SYNC_CHANNEL_ID,
                    "Library sync",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Library scan progress" }
            )
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    com.startune.music.data.backup.AutoBackupManager.AUTO_BACKUP_CHANNEL_ID,
                    "Automatic backup",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Automatic backup progress" }
            )
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    com.startune.music.data.updates.AppUpdateManager.UPDATES_CHANNEL_ID,
                    "App updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "App update availability and download progress" }
            )
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    com.startune.music.data.dictionary.DictionaryManager.DICTIONARIES_CHANNEL_ID,
                    "Dictionary downloads",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Dictionary download progress" }
            )
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)

        registerActivityLifecycleCallbacks(PhoneOrientationPolicy)

        syncManager.get().start()

        syncNotificationManager.get().start()

        m3uSyncCoordinator.get().start()

        startupScope.launch {
            val packDir = dictionaryManager.get()
                .installedPackDir(DictionaryManager.KUROMOJI_PACK_ID)
            if (packDir != null) {
                MultiLangRomanizer.setDictionaryDirectory(packDir)
            }
        }

        startupScope.launch {
            AlbumArtUtils.migrateLegacyCacheLocation(this@StartuneApplication)
            val savedLimit = runCatching {
                userPreferencesRepository.get().albumArtCacheLimitMbFlow.first()
            }.getOrNull()
            if (savedLimit != null) {
                AlbumArtCacheManager.configuredCacheLimitMb = savedLimit.toLong()
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return imageLoader.get().newBuilder()
            .components {
                add(localArtworkCoilFetcherFactory.get())
                add(jellyfinCoilFetcherFactory.get())
                add(navidromeCoilFetcherFactory.get())
            }
            .build()
    }

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        imageLoader.get().memoryCache?.trimMemory(level)

        if (
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE ||
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
            level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        ) {
            themeStateHolder.get().trimMemory(level)
        }

        if (
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
            level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN
        ) {
            artistImageRepository.get().clearCache()
            MediaMetadataRetrieverPool.clear()
        }

        libraryStateHolder.get().trimMemory(level)

        if (
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE
        ) {
            imageLoader.get().memoryCache?.clear()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

}
