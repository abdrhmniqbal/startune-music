package com.startune.music.data.updates

import android.os.Build
import com.google.gson.Gson
import com.startune.music.BuildConfig
import com.google.gson.reflect.TypeToken
import com.startune.music.utils.VersionCompare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class GitHubAsset(
    val name: String?,
    val browser_download_url: String?
)

data class GitHubRelease(
    val tag_name: String?,
    val name: String?,
    val body: String?,
    val html_url: String?,
    val draft: Boolean?,
    val prerelease: Boolean?,
    val published_at: String?,
    val assets: List<GitHubAsset>?
)

data class AppUpdateInfo(
    val currentVersion: String,
    val newVersion: String,
    val releaseName: String,
    val body: String,
    val htmlUrl: String,
    val downloadUrl: String,
    val apkFileName: String,
    val prerelease: Boolean
)

/**
 * Reads release history for our own repo. The changelog reads CHANGELOG.md
 * (raw file, not the releases API): the API allows only 60 unauthenticated
 * requests per hour per IP, and on a shared mobile IP that fails with a
 * dead error screen, while the raw file is served from a CDN with no such
 * limit. The changelog starts at v2.0.0: older releases (the old Expo app)
 * are excluded, and so are pre-releases, except the installed version's
 * own notes: a beta user must see their own version's changelog, otherwise
 * the list is empty until the first stable release. Update checks still
 * use the API: they need release metadata and APK asset URLs, which only
 * the API provides. Update checks pick the newest release newer than the
 * installed version, with the APK asset matching this device's ABI.
 */
@Singleton
class GitHubReleasesRepository @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()
    private val releasesType = object : TypeToken<List<GitHubRelease>>() {}.type

    suspend fun fetchReleases(): List<GitHubRelease> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(RELEASES_URL)
            .header("Accept", "application/vnd.github+json")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GitHub releases request failed: ${response.code}")
            }
            val body = response.body?.string()
                ?: throw IOException("Empty GitHub releases response")
            gson.fromJson<List<GitHubRelease>>(body, releasesType) ?: emptyList()
        }
    }

    suspend fun getReleaseNotes(): List<GitHubRelease> {
        return try {
            parseChangelog(fetchChangelogMarkdown())
                .filter { VersionCompare.compareVersions(it.tag_name!!, CHANGELOG_MIN_VERSION) >= 0 }
                // Stable releases only, except the installed version's own
                // notes: a pre-release's notes never appear in the changelog
                // for other versions (the tag itself carries -beta/-rc/...).
                .filter {
                    !VersionCompare.isPreviewReleaseVersion(it.tag_name!!) ||
                        it.tag_name == "v${BuildConfig.VERSION_NAME}"
                }
                .sortedWith { left, right ->
                    val byVersion = VersionCompare.compareVersions(
                        right.tag_name ?: "",
                        left.tag_name ?: ""
                    )
                    if (byVersion != 0) byVersion
                    else (right.published_at ?: "").compareTo(left.published_at ?: "")
                }
        } catch (e: Exception) {
            Timber.e(e, "Failed to load release notes")
            throw e
        }
    }

    private suspend fun fetchChangelogMarkdown(): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(CHANGELOG_RAW_URL)
            .header("Accept", "text/plain")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Changelog request failed: ${response.code}")
            }
            response.body?.string()?.takeIf { it.isNotBlank() }
                ?: throw IOException("Empty changelog response")
        }
    }

    suspend fun checkForUpdate(
        currentVersion: String,
        includePrereleases: Boolean
    ): AppUpdateInfo? {
        val eligible = fetchReleases()
            .filter { it.draft != true }
            .filter { it.prerelease != true || includePrereleases }
            .filter {
                !it.tag_name.isNullOrBlank() &&
                    VersionCompare.isNewerVersion(it.tag_name!!, currentVersion)
            }
        val latest = eligible.maxWithOrNull { left, right ->
            VersionCompare.compareVersions(left.tag_name ?: "", right.tag_name ?: "")
        } ?: return null
        return toUpdateInfo(latest, currentVersion)
    }

    private fun toUpdateInfo(release: GitHubRelease, currentVersion: String): AppUpdateInfo {
        val tag = release.tag_name ?: ""
        val asset = resolveApkAsset(release)
        return AppUpdateInfo(
            currentVersion = currentVersion,
            newVersion = tag,
            releaseName = release.name?.takeIf { it.isNotBlank() } ?: tag,
            body = release.body ?: "",
            htmlUrl = release.html_url ?: "",
            downloadUrl = asset?.browser_download_url ?: "",
            apkFileName = asset?.name?.takeIf { it.isNotBlank() }
                ?: "startune-music-$tag.apk",
            prerelease = release.prerelease == true
        )
    }

    private fun resolveApkAsset(release: GitHubRelease): GitHubAsset? {
        val apkAssets = release.assets.orEmpty()
            .filter { it.name?.endsWith(".apk", ignoreCase = true) == true }
        if (apkAssets.isEmpty()) return null
        for (abi in Build.SUPPORTED_ABIS) {
            apkAssets.firstOrNull {
                it.name?.contains(abi, ignoreCase = true) == true
            }?.let { return it }
        }
        apkAssets.firstOrNull {
            it.name?.contains("universal", ignoreCase = true) == true
        }?.let { return it }
        return apkAssets.first()
    }

    companion object {
        const val RELEASES_URL =
            "https://api.github.com/repos/abdrhmniqbal/startune-music/releases"
        const val CHANGELOG_RAW_URL =
            "https://raw.githubusercontent.com/abdrhmniqbal/startune-music/main/CHANGELOG.md"
        const val CHANGELOG_MIN_VERSION = "2.0.0-beta"

        private val CHANGELOG_SECTION =
            Regex("^## \\[([^]]+)](?:\\s*-\\s*(\\d{4}-\\d{2}-\\d{2}))?")

        /**
         * Parses CHANGELOG.md (Keep a Changelog format) into release entries
         * the changelog UI already renders. Pure function, unit tested in
         * ChangelogParserTest.
         */
        internal fun parseChangelog(markdown: String): List<GitHubRelease> {
            val entries = mutableListOf<GitHubRelease>()
            var version: String? = null
            var date = ""
            val body = StringBuilder()
            fun flush() {
                val current = version ?: return
                if (current.equals("Unreleased", ignoreCase = true)) return
                val text = body.toString().trim()
                if (text.isEmpty()) return
                entries.add(
                    GitHubRelease(
                        tag_name = "v$current",
                        name = current,
                        body = text,
                        html_url = "https://github.com/abdrhmniqbal/startune-music/releases/tag/v$current",
                        draft = false,
                        prerelease = VersionCompare.isPreviewReleaseVersion(current),
                        published_at = date.takeIf { it.isNotBlank() }?.let { "${it}T00:00:00Z" },
                        assets = emptyList()
                    )
                )
            }
            for (line in markdown.lineSequence()) {
                val header = CHANGELOG_SECTION.matchEntire(line)
                if (header != null) {
                    flush()
                    version = header.groupValues[1]
                    date = header.groupValues[2]
                    body.clear()
                } else if (version != null) {
                    body.appendLine(line)
                }
            }
            flush()
            return entries
        }
    }
}
