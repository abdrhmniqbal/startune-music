package com.startune.music.data.backup

import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.startune.music.R
import com.startune.music.data.backup.model.BackupSection
import com.startune.music.data.preferences.UserPreferencesRepository
import com.startune.music.utils.AppFolders
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Automatic backups, mirroring the old app: at most once per configured
 * interval, writing a .starbk export of all backup sections into the backup
 * folder (Startune/Music/Autobackup by default). A periodic WorkManager worker
 * runs the check in the background so backups happen even when the app is
 * rarely opened; the app-startup check remains as a second trigger. While a
 * backup runs, an ongoing notification is shown (unless the user turned
 * backup notifications off). Failures are logged, never surfaced: a missed
 * backup must not interrupt opening the app.
 */
@Singleton
class AutoBackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager,
    private val backupManager: BackupManager,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    /**
     * Makes the periodic worker match the current setting: scheduled at the
     * configured interval while auto-backup is on, cancelled while it is off.
     * Called at app startup and whenever the setting changes; the UPDATE
     * policy also re-schedules with a new interval.
     */
    suspend fun ensureScheduled() {
        val enabled = userPreferencesRepository.autoBackupEnabledFlow.first()
        if (!enabled) {
            workManager.cancelUniqueWork(AUTO_BACKUP_WORK_NAME)
            return
        }
        val intervalHours =
            userPreferencesRepository.autoBackupIntervalHoursFlow.first().coerceAtLeast(1).toLong()
        workManager.enqueueUniquePeriodicWork(
            AUTO_BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<AutoBackupWorker>(intervalHours, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder().setRequiresStorageNotLow(true).build()
                )
                .build(),
        )
    }

    suspend fun runCheck(force: Boolean = false): Boolean {
        val enabled = userPreferencesRepository.autoBackupEnabledFlow.first()
        if (!enabled && !force) return false
        val intervalHours = userPreferencesRepository.autoBackupIntervalHoursFlow.first()
        val lastRunMs = userPreferencesRepository.autoBackupLastRunMsFlow.first()
        val now = System.currentTimeMillis()
        if (!force && now - lastRunMs < intervalHours * 3_600_000L) return false

        val dataRoot = userPreferencesRepository.dataStorageRootFlow.first()
        val targetDir = AppFolders.autoBackupDir(AppFolders.resolveRoot(dataRoot))
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            Timber.e("Auto-backup failed: cannot create ${targetDir.absolutePath}")
            return false
        }
        val file = File(targetDir, "autobackup-$now.starbk")
        val notificationsEnabled =
            userPreferencesRepository.autoBackupNotificationsEnabledFlow.first()
        if (notificationsEnabled) showBackupNotification()
        return try {
            backupManager.export(
                uri = Uri.fromFile(file),
                sections = BackupSection.defaultSelection,
                onProgress = {}
            ).getOrThrow()
            userPreferencesRepository.setAutoBackupLastRunMs(now)
            true
        } catch (e: Exception) {
            Timber.e(e, "Auto-backup failed")
            false
        } finally {
            if (notificationsEnabled) hideBackupNotification()
        }
    }

    private fun showBackupNotification() {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, AUTO_BACKUP_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.auto_backup_notification_title))
            .setContentText(context.getString(R.string.auto_backup_notification_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(0, 0, true)
            .build()
        notificationManager.notify(AUTO_BACKUP_NOTIFICATION_ID, notification)
    }

    private fun hideBackupNotification() {
        NotificationManagerCompat.from(context).cancel(AUTO_BACKUP_NOTIFICATION_ID)
    }

    companion object {
        const val AUTO_BACKUP_CHANNEL_ID = "auto_backup"
        const val AUTO_BACKUP_WORK_NAME = "auto_backup_periodic"
        private const val AUTO_BACKUP_NOTIFICATION_ID = 4201
    }
}
