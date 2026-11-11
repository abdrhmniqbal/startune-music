package com.startune.music.utils

import android.os.Environment
import java.io.File
import java.text.Normalizer
import java.util.Locale

/** Explicit artist portraits beside an album directory, without walking the storage tree. */
internal object FolderArtistArtUtils {
    private val artworkNames = listOf("artist.jpg", "artist.png", "band.jpg", "band.png")

    /**
     * Storage roots folder art is resolved against: the data storage root the user
     * currently has set plus the library folders they currently allow. A directory
     * outside every current root is stale (storage moved, folder removed) and is
     * never searched, so a portrait path can never outlive the storage it came from.
     */
    fun resolveStorageRoots(dataStorageRoot: String, allowedDirectories: Set<String>): List<File> {
        val roots = allowedDirectories.filter { it.isNotBlank() }.map { File(it) }.toMutableList()
        roots += AppFolders.resolveRoot(dataStorageRoot)
        if (roots.isEmpty()) roots += Environment.getExternalStorageDirectory()
        return roots.mapNotNull { runCatching { it.canonicalFile }.getOrNull() }.distinct()
    }

    /** True when [directory] sits inside one of [roots] (canonical paths, root itself counts). */
    internal fun isUnderStorageRoots(directory: File, roots: List<File>): Boolean {
        val path = runCatching { directory.canonicalFile.path }.getOrNull() ?: directory.absolutePath
        return roots.any { root ->
            val rootPath = root.path.trimEnd('/')
            path == rootPath || path.startsWith("$rootPath/")
        }
    }

    fun findArtistImage(
        audioDirectoryPaths: List<String>,
        artistName: String,
        storageRoots: List<File>,
        isUsableArtwork: (File) -> Boolean = AlbumArtUtils::isUsableExternalArtwork
    ): File? {
        val normalizedArtistName = normalizeName(artistName)
        val visited = mutableSetOf<String>()
        for (path in audioDirectoryPaths) {
            val audioDirectory = canonicalDirectory(path) ?: continue
            // Conventional layouts: Artist/Album/track or Artist/Album/Disc/track.
            // Only named artist ancestors qualify, so collaborators and loose tracks do
            // not inherit another artist's portrait or an image at the storage root.
            val artistAncestors = listOfNotNull(
                audioDirectory.parentFile,
                audioDirectory.parentFile?.parentFile
            ).filter { normalizedArtistName.isNotEmpty() && normalizeName(it.name) == normalizedArtistName }
            for (directory in artistAncestors + audioDirectory) {
                if (!visited.add(directory.path)) continue
                // Folder art is relative to the data storage currently set: a directory
                // outside every current root belongs to moved or removed storage.
                if (!isUnderStorageRoots(directory, storageRoots)) continue
                if (!AlbumArtUtils.shouldTrustDirectoryArtwork(directory.name)) continue

                val candidates = runCatching {
                    directory.listFiles { file ->
                        file.name.lowercase(Locale.ROOT) in artworkNames
                    }?.sortedWith(
                        compareBy<File> { artworkNames.indexOf(it.name.lowercase(Locale.ROOT)) }
                            .thenBy { it.name }
                    )
                }.getOrNull().orEmpty()

                for (candidate in candidates) {
                    val image = runCatching { candidate.canonicalFile }.getOrNull() ?: continue
                    // A portrait symlink must not escape the directory being inspected.
                    if (image.parentFile != directory || !image.isFile || !image.canRead()) continue
                    if (runCatching { isUsableArtwork(image) }.getOrDefault(false)) return image
                }
            }
        }
        return null
    }

    private fun normalizeName(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .filter { it.isLetterOrDigit() }

    private fun canonicalDirectory(path: String): File? {
        if (path.isBlank()) return null
        val directory = File(path)
        if (!directory.isAbsolute) return null
        return runCatching { directory.canonicalFile }
            .getOrNull()
            ?.takeIf { it.isDirectory }
    }
}
