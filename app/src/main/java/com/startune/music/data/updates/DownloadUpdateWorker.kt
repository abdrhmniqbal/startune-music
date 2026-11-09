package com.startune.music.data.updates

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.startune.music.R
import com.startune.music.utils.AppFolders
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Downloads an update APK in the background with a progress notification.
 * The file lands in cacheDir/updates/; the manager posts a tap-to-install
 * notification when it finishes.
 */
@HiltWorker
class DownloadUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL).orEmpty()
        val version = inputData.getString(KEY_VERSION).orEmpty()
        val fileName = inputData.getString(KEY_FILE_NAME).orEmpty()
        if (url.isBlank() || version.isBlank() || fileName.isBlank()) {
            return Result.failure()
        }
        setForeground(createForegroundInfo(0f))
        return withContext(Dispatchers.IO) {
            try {
                val updatesDir = AppFolders.apkDownloadDir(applicationContext)
                val targetFile = File(updatesDir, fileName)
                val request = Request.Builder().url(url).build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("Update download failed: ${response.code}")
                    }
                    val body = response.body
                        ?: throw IOException("Empty update download response")
                    val totalBytes = body.contentLength()
                    var downloadedBytes = 0L
                    var lastNotifiedAt = 0L
                    body.byteStream().use { input ->
                        targetFile.outputStream().use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                output.write(buffer, 0, read)
                                downloadedBytes += read
                                val now = System.currentTimeMillis()
                                if (totalBytes > 0 && now - lastNotifiedAt >= NOTIFY_THROTTLE_MS) {
                                    lastNotifiedAt = now
                                    val progress =
                                        downloadedBytes.toFloat() / totalBytes.toFloat()
                                    setProgress(
                                        workDataOf(
                                            PROGRESS_BYTES to downloadedBytes,
                                            PROGRESS_TOTAL to totalBytes
                                        )
                                    )
                                    updateNotification(progress)
                                }
                            }
                            output.flush()
                        }
                    }
                }
                setProgress(
                    workDataOf(
                        PROGRESS_BYTES to targetFile.length(),
                        PROGRESS_TOTAL to targetFile.length()
                    )
                )
                Result.success(workDataOf(KEY_FILE_PATH to targetFile.absolutePath))
            } catch (e: Exception) {
                Timber.e(e, "Update download failed")
                Result.failure()
            }
        }
    }

    private fun createForegroundInfo(progress: Float): ForegroundInfo {
        val notification = NotificationCompat.Builder(
            applicationContext,
            AppUpdateManager.UPDATES_CHANNEL_ID
        )
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(applicationContext.getString(R.string.update_download_title))
            .setContentText(
                applicationContext.getString(
                    R.string.update_downloading,
                    (progress * 100).toInt()
                )
            )
            .setProgress(100, (progress * 100).toInt(), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
        return ForegroundInfo(
            DOWNLOAD_NOTIFICATION_ID,
            notification,
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun updateNotification(progress: Float) {
        try {
            val notification = NotificationCompat.Builder(
                applicationContext,
                AppUpdateManager.UPDATES_CHANNEL_ID
            )
                .setSmallIcon(R.mipmap.ic_launcher_monochrome)
                .setContentTitle(applicationContext.getString(R.string.update_download_title))
                .setContentText(
                    applicationContext.getString(
                        R.string.update_downloading,
                        (progress * 100).toInt()
                    )
                )
                .setProgress(100, (progress * 100).toInt(), false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .build()
            NotificationManagerCompat.from(applicationContext)
                .notify(DOWNLOAD_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update download notification")
        }
    }

    companion object {
        const val KEY_URL = "update_download_url"
        const val KEY_VERSION = "update_download_version"
        const val KEY_FILE_NAME = "update_download_file_name"
        const val KEY_FILE_PATH = "update_download_file_path"
        const val PROGRESS_BYTES = "update_download_bytes"
        const val PROGRESS_TOTAL = "update_download_total"
        const val DOWNLOAD_NOTIFICATION_ID = 2003
        const val NOTIFY_THROTTLE_MS = 500L
    }
}
