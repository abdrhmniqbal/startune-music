package com.startune.music.data.updates

import android.os.Build
import com.google.gson.Gson
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
 * Reads release history from the GitHub releases API for our own repo.
 * The changelog starts at v2.0.0: older releases (the old Expo app) are
 * excluded, and so are pre-releases: a stable release's changelog never
 * lists beta notes. Update checks pick the newest release newer than the
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
            fetchReleases()
                .filter { it.draft != true && !it.tag_name.isNullOrBlank() }
                // Stable releases only: a pre-release's notes never appear in
                // the changelog, even if its GitHub release was not flagged
                // as a pre-release (the tag itself carries -beta/-rc/...).
                .filter {
                    it.prerelease != true &&
                        !VersionCompare.isPreviewReleaseVersion(it.tag_name!!)
                }
                .filter { VersionCompare.compareVersions(it.tag_name!!, CHANGELOG_MIN_VERSION) >= 0 }
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
        const val CHANGELOG_MIN_VERSION = "2.0.0-beta"
    }
}
