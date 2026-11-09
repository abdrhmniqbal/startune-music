package com.startune.music.data.notifications

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.startune.music.R
import com.startune.music.data.preferences.UserPreferencesRepository
import com.startune.music.data.worker.SyncManager
import com.startune.music.data.worker.SyncProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts a progress-bar notification while a library scan runs, and cancels
 * it when the scan finishes. Gated by the indexer-notifications setting.
 * Lives in the Application scope so it also works when the app is in the
 * background; the in-app overlay in MainActivity shows the same progress
 * while the app is open.
 */
@Singleton
class SyncNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncManager: SyncManager,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var notificationShown = false

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            combine(
                syncManager.syncProgress,
                userPreferencesRepository.indexerNotificationsEnabledFlow
            ) { progress, enabled -> progress to enabled }
                .collect { (progress, enabled) ->
                    updateNotification(progress, enabled)
                }
        }
    }

    private fun updateNotification(progress: SyncProgress, enabled: Boolean) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!enabled || !progress.isRunning || progress.isCompleted) {
            if (notificationShown) {
                notificationManager.cancel(SYNC_NOTIFICATION_ID)
                notificationShown = false
            }
            return
        }
        if (!notificationManager.areNotificationsEnabled()) return
        try {
            val builder = NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_monochrome)
                .setContentTitle(context.getString(R.string.sync_preparing_library))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            if (progress.hasProgress) {
                builder
                    .setContentText(
                        context.getString(
                            R.string.sync_scanned_count,
                            progress.currentCount,
                            progress.totalCount
                        )
                    )
                    .setProgress(progress.totalCount, progress.currentCount, false)
            } else {
                builder.setProgress(0, 0, true)
            }
            notificationManager.notify(SYNC_NOTIFICATION_ID, builder.build())
            notificationShown = true
        } catch (e: Exception) {
            Timber.e(e, "Failed to post sync progress notification")
        }
    }

    companion object {
        const val SYNC_CHANNEL_ID = "library_sync"
        const val SYNC_NOTIFICATION_ID = 2001
    }
}
