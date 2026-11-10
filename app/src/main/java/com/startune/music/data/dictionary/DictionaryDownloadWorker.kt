package com.startune.music.data.dictionary

import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.startune.music.R
import com.startune.music.data.preferences.UserPreferencesRepository
import com.startune.music.utils.MultiLangRomanizer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * Downloads one dictionary pack in the background with a progress
 * notification, verifies its sha256, extracts it to internal storage, and
 * records the installed version. The zip is deleted after extraction; a
 * failed download leaves no partial pack behind.
 */
@HiltWorker
class DictionaryDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val okHttpClient: OkHttpClient,
    private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val packId = inputData.getString(KEY_PACK_ID).orEmpty()
        val url = inputData.getString(KEY_URL).orEmpty()
        val sha256 = inputData.getString(KEY_SHA256).orEmpty()
        val version = inputData.getInt(KEY_VERSION, 0)
        if (packId.isBlank() || url.isBlank() || sha256.isBlank() || version <= 0) {
            return Result.failure()
        }
        setForeground(createForegroundInfo(0f))
        return withContext(Dispatchers.IO) {
            val tmpZip = File(applicationContext.cacheDir, "dict-$packId.zip")
            val tmpDir = File(applicationContext.filesDir, "dictionaries/$packId.tmp")
            try {
                downloadZip(url, tmpZip)
                verifySha256(tmpZip, sha256)
                extractZip(tmpZip, tmpDir)
                val missing = DictionaryManager.DICTIONARY_FILE_NAMES
                    .filter { !File(tmpDir, it).isFile }
                if (missing.isNotEmpty()) {
                    throw IOException("Dictionary zip is missing files: ${missing.joinToString()}")
                }
                val packDir = File(applicationContext.filesDir, "dictionaries/$packId")
                packDir.deleteRecursively()
                if (!tmpDir.renameTo(packDir)) {
                    throw IOException("Failed to install dictionary $packId")
                }
                userPreferencesRepository.setInstalledDictionaryVersion(packId, version)
                if (packId == DictionaryManager.KUROMOJI_PACK_ID) {
                    MultiLangRomanizer.setDictionaryDirectory(packDir)
                }
                setProgress(workDataOf(PROGRESS_BYTES to tmpZip.length(), PROGRESS_TOTAL to tmpZip.length()))
                Result.success()
            } catch (e: Exception) {
                Timber.e(e, "Dictionary download failed: $packId")
                tmpDir.deleteRecursively()
                Result.failure()
            } finally {
                tmpZip.delete()
                NotificationManagerCompat.from(applicationContext)
                    .cancel(DOWNLOAD_NOTIFICATION_ID)
            }
        }
    }

    private suspend fun downloadZip(url: String, targetFile: File) {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Dictionary download failed: ${response.code}")
            }
            val body = response.body ?: throw IOException("Empty dictionary download response")
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
                            val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
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
    }

    private fun verifySha256(file: File, expectedHex: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        val actualHex = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actualHex.equals(expectedHex, ignoreCase = true)) {
            throw IOException("Dictionary sha256 mismatch")
        }
    }

    private fun extractZip(zipFile: File, targetDir: File) {
        targetDir.deleteRecursively()
        targetDir.mkdirs()
        ZipInputStream(FileInputStream(zipFile)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name.substringAfterLast('/')
                    if (name.isBlank() || name.contains("..")) {
                        throw IOException("Unsafe dictionary zip entry: ${entry.name}")
                    }
                    File(targetDir, name).outputStream().use { output ->
                        zip.copyTo(output)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun createForegroundInfo(progress: Float): ForegroundInfo {
        val notification = NotificationCompat.Builder(
            applicationContext,
            DictionaryManager.DICTIONARIES_CHANNEL_ID
        )
            .setSmallIcon(R.mipmap.ic_launcher_monochrome)
            .setContentTitle(applicationContext.getString(R.string.dictionaries_notification_title))
            .setContentText(
                applicationContext.getString(
                    R.string.dictionaries_notification_downloading,
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
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    }

    private fun updateNotification(progress: Float) {
        try {
            val notification = NotificationCompat.Builder(
                applicationContext,
                DictionaryManager.DICTIONARIES_CHANNEL_ID
            )
                .setSmallIcon(R.mipmap.ic_launcher_monochrome)
                .setContentTitle(applicationContext.getString(R.string.dictionaries_notification_title))
                .setContentText(
                    applicationContext.getString(
                        R.string.dictionaries_notification_downloading,
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
            Timber.e(e, "Failed to update dictionary download notification")
        }
    }

    companion object {
        const val KEY_PACK_ID = "dictionary_pack_id"
        const val KEY_URL = "dictionary_download_url"
        const val KEY_SHA256 = "dictionary_download_sha256"
        const val KEY_VERSION = "dictionary_download_version"
        const val PROGRESS_BYTES = "dictionary_download_bytes"
        const val PROGRESS_TOTAL = "dictionary_download_total"
        const val DOWNLOAD_NOTIFICATION_ID = 2004
        const val NOTIFY_THROTTLE_MS = 500L
    }
}
