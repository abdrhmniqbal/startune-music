package com.startune.music.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

/**
 * The app's own folder on shared storage, so every file the app writes
 * outside its private dirs lives in one place the user can find. The user
 * picks one Data Storage root in settings (blank means the default
 * Startune/Music); everything else derives from it: <root>/Autobackup for
 * automatic backups, <root>/Downloads for downloaded songs,
 * <root>/Downloads/APKs for update APKs. Writing here needs all-files
 * access; callers fall back to app-private storage when the folder is not
 * writable.
 */
object AppFolders {
    val defaultRoot: File
        get() = File(Environment.getExternalStorageDirectory(), "Startune/Music")

    fun resolveRoot(customPath: String): File =
        if (customPath.isBlank()) defaultRoot else File(customPath)

    fun autoBackupDir(root: File): File = File(root, "Autobackup")

    fun downloadsDir(root: File): File = File(root, "Downloads")

    fun apkDownloadsDir(root: File): File = File(downloadsDir(root), "APKs")

    fun apkDownloadDir(context: Context, dataRoot: String): File {
        val preferred = apkDownloadsDir(resolveRoot(dataRoot))
        if ((preferred.exists() || preferred.mkdirs()) && preferred.canWrite()) {
            return preferred
        }
        return File(context.cacheDir, "updates").apply { mkdirs() }
    }

    fun songDownloadsDir(context: Context, dataRoot: String): File {
        val preferred = downloadsDir(resolveRoot(dataRoot))
        if ((preferred.exists() || preferred.mkdirs()) && preferred.canWrite()) {
            return preferred
        }
        return File(context.filesDir, "cloud_downloads").apply { mkdirs() }
    }

    fun pathFromTreeUri(uri: Uri): String? {
        return try {
            val treeId = DocumentsContract.getTreeDocumentId(uri)
            val separator = treeId.indexOf(':')
            if (separator < 0) return null
            if (treeId.substring(0, separator) != "primary") return null
            File(
                Environment.getExternalStorageDirectory(),
                treeId.substring(separator + 1)
            ).absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
