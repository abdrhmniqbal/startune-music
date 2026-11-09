package com.startune.music.data.updates

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.startune.music.BuildConfig
import com.startune.music.MainActivity
import com.startune.music.R
import com.startune.music.data.preferences.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns in-app update state: the pending update sheet, the background APK
 * download, and the once-per-version availability notification.
 * Notifications only fire when the app-update notification pref is on.
 * Dismissing a version ("Don't remind me") only silences that version; the
 * next release notifies again.
 */
@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val releasesRepository: GitHubReleasesRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    sealed interface DownloadState {
        data object Idle : DownloadState
        data class Downloading(val progress: Float) : DownloadState
        data class Downloaded(val file: File) : DownloadState
        data object Failed : DownloadState
    }

    sealed interface ManualCheckResult {
        data class UpdateAvailable(val info: AppUpdateInfo) : ManualCheckResult
        data object UpToDate : ManualCheckResult
        data object Error : ManualCheckResult
    }

    private val _pendingUpdate = MutableStateFlow<AppUpdateInfo?>(null)
    val pendingUpdate: StateFlow<AppUpdateInfo?> = _pendingUpdate.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    val currentVersion: String get() = BuildConfig.VERSION_NAME

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var startupCheckDone = false
    private var downloadObserverJob: Job? = null

    fun checkOnStartup() {
        if (startupCheckDone) return
        startupCheckDone = true
        scope.launch {
            if (!userPreferencesRepository.appUpdateNotificationsEnabledFlow.first()) {
                return@launch
            }
            val includePrereleases = userPreferencesRepository.includePrereleasesFlow.first()
            val info = runCatching {
                releasesRepository.checkForUpdate(currentVersion, includePrereleases)
            }.getOrNull() ?: return@launch
            val lastNotified = userPreferencesRepository.lastNotifiedUpdateVersionFlow.first()
            val dismissed = userPreferencesRepository.dismissedUpdateVersionFlow.first()
            if (info.newVersion == lastNotified || info.newVersion == dismissed) {
                return@launch
            }
            _pendingUpdate.value = info
            notifyUpdateAvailable(info)
            userPreferencesRepository.setLastNotifiedUpdateVersion(info.newVersion)
        }
    }

    suspend fun checkManually(): ManualCheckResult {
        val includePrereleases = userPreferencesRepository.includePrereleasesFlow.first()
        return try {
            val info = releasesRepository.checkForUpdate(currentVersion, includePrereleases)
            if (info == null) {
                ManualCheckResult.UpToDate
            } else {
                _pendingUpdate.value = info
                ManualCheckResult.UpdateAvailable(info)
            }
        } catch (e: Exception) {
            Timber.e(e, "Manual update check failed")
            ManualCheckResult.Error
        }
    }

    fun dismissPendingUpdate(dontRemind: Boolean) {
        val info = _pendingUpdate.value
        if (dontRemind && info != null) {
            scope.launch {
                userPreferencesRepository.setDismissedUpdateVersion(info.newVersion)
            }
        }
        _pendingUpdate.value = null
    }

    fun enqueueDownload(info: AppUpdateInfo) {
        if (info.downloadUrl.isBlank()) return
        _downloadState.value = DownloadState.Downloading(0f)
        val request = OneTimeWorkRequestBuilder<DownloadUpdateWorker>()
            .setInputData(
                workDataOf(
                    DownloadUpdateWorker.KEY_URL to info.downloadUrl,
                    DownloadUpdateWorker.KEY_VERSION to info.newVersion,
                    DownloadUpdateWorker.KEY_FILE_NAME to info.apkFileName
                )
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            DOWNLOAD_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
        observeDownload(request.id, info)
    }

    private fun observeDownload(workId: java.util.UUID, info: AppUpdateInfo) {
        downloadObserverJob?.cancel()
        downloadObserverJob = scope.launch {
            WorkManager.getInstance(context).getWorkInfoByIdFlow(workId).collect { workInfo ->
                if (workInfo == null) return@collect
                when (workInfo.state) {
                    WorkInfo.State.RUNNING -> {
                        val bytes = workInfo.progress.getLong(
                            DownloadUpdateWorker.PROGRESS_BYTES, 0L
                        )
                        val total = workInfo.progress.getLong(
                            DownloadUpdateWorker.PROGRESS_TOTAL, 0L
                        )
                        if (total > 0) {
                            _downloadState.value =
                                DownloadState.Downloading(bytes.toFloat() / total.toFloat())
                        }
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val path = workInfo.outputData.getString(
                            DownloadUpdateWorker.KEY_FILE_PATH
                        )
                        val file = path?.let { File(it) }
                        if (file != null && file.exists()) {
                            _downloadState.value = DownloadState.Downloaded(file)
                            notifyDownloadComplete(info, file)
                        } else {
                            _downloadState.value = DownloadState.Failed
                        }
                    }
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                        _downloadState.value = DownloadState.Failed
                    }
                    else -> Unit
                }
            }
        }
    }

    fun installDownloadedApk(file: File): Boolean {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                false
            } else {
                context.startActivity(installIntent)
                true
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to start APK install")
            false
        }
    }

    private fun notifyUpdateAvailable(info: AppUpdateInfo) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, UPDATES_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.update_notification_title))
            .setContentText(
                context.getString(
                    R.string.update_notification_body,
                    info.newVersion,
                    info.currentVersion
                )
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(UPDATE_AVAILABLE_NOTIFICATION_ID, notification)
    }

    private fun notifyDownloadComplete(info: AppUpdateInfo, file: File) {
        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) return
        val installUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(installUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, UPDATES_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(context.getString(R.string.update_downloaded_title))
            .setContentText(
                context.getString(
                    R.string.update_downloaded_body,
                    info.newVersion
                )
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(
            DownloadUpdateWorker.DOWNLOAD_NOTIFICATION_ID,
            notification
        )
    }

    companion object {
        const val UPDATES_CHANNEL_ID = "app_updates"
        const val UPDATE_AVAILABLE_NOTIFICATION_ID = 2002
        const val DOWNLOAD_WORK_NAME = "app_update_download"
    }
}
