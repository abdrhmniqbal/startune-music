package com.startune.music.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

/**
 * The app's own folder on shared storage, so every file the app writes
 * outside its private dirs lives in one place the user can find:
 * Startune/Music/Autobackup for automatic backups,
 * Startune/Music/Downloads for downloads, Startune/Music/Downloads/APKs
 * for update APKs. Writing here needs all-files access; callers fall back
 * to app-private storage when the folder is not writable.
 */
object AppFolders {
    val root: File
        get() = File(Environment.getExternalStorageDirectory(), "Startune/Music")

    val autoBackupDir: File
        get() = File(root, "Autobackup")

    val downloadsDir: File
        get() = File(root, "Downloads")

    val apkDownloadsDir: File
        get() = File(downloadsDir, "APKs")

    fun resolveAutoBackupDir(customPath: String): File =
        if (customPath.isBlank()) autoBackupDir else File(customPath)

    fun apkDownloadDir(context: Context): File {
        val preferred = apkDownloadsDir
        if ((preferred.exists() || preferred.mkdirs()) && preferred.canWrite()) {
            return preferred
        }
        return File(context.cacheDir, "updates").apply { mkdirs() }
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
