package com.startune.music.data.backup

import android.net.Uri
import com.startune.music.data.backup.model.BackupSection
import com.startune.music.data.preferences.UserPreferencesRepository
import com.startune.music.utils.AppFolders
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Automatic backups, mirroring the old app: at most once per configured
 * interval, checked at app startup, writing a .starbk export of all backup
 * sections into the backup folder (Startune/Music/Autobackup by default).
 * Failures are logged, never surfaced: a missed backup must not interrupt
 * opening the app.
 */
@Singleton
class AutoBackupManager @Inject constructor(
    private val backupManager: BackupManager,
    private val userPreferencesRepository: UserPreferencesRepository
) {
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
        }
    }
}
