package com.startune.music.data.updates

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GitHubReleasesRepositoryTest {

    private val repository = GitHubReleasesRepository(OkHttpClient())

    private fun release(tag: String, prerelease: Boolean = false) = GitHubRelease(
        tag_name = tag,
        name = tag.removePrefix("v"),
        body = "Notes for $tag.",
        html_url = "https://github.com/abdrhmniqbal/startune-music/releases/tag/$tag",
        draft = false,
        prerelease = prerelease,
        published_at = "2026-11-15T00:00:00Z",
        assets = emptyList()
    )

    @Test
    fun newestEligiblePicksNewestStableNewerThanCurrent() {
        val releases = listOf(
            release("v2.0.0"),
            release("v2.0.0-beta", prerelease = true),
            release("v1.0.5")
        )
        val latest = repository.newestEligible(releases, "2.0.0-beta", includePrereleases = false)
        assertEquals("v2.0.0", latest?.tag_name)
    }

    @Test
    fun newestEligibleExcludesPrereleasesUnlessOptedIn() {
        val releases = listOf(
            release("v2.0.1-beta", prerelease = true),
            release("v2.0.0")
        )
        assertNull(
            repository.newestEligible(releases, "2.0.0", includePrereleases = false)
        )
        val latest = repository.newestEligible(releases, "2.0.0", includePrereleases = true)
        assertEquals("v2.0.1-beta", latest?.tag_name)
    }

    @Test
    fun newestEligibleReturnsNullWhenUpToDate() {
        val releases = listOf(release("v2.0.0"), release("v1.0.5"))
        assertNull(
            repository.newestEligible(releases, "2.0.0", includePrereleases = false)
        )
    }

    @Test
    fun changelogUpdateBuildsDownloadUrlFromReleasePattern() {
        val info = repository.toUpdateInfoFromChangelog(
            release("v2.0.0"),
            currentVersion = "2.0.0-beta",
            abi = "arm64-v8a"
        )
        assertEquals("2.0.0-beta", info.currentVersion)
        assertEquals("v2.0.0", info.newVersion)
        assertEquals("2.0.0", info.releaseName)
        assertEquals("Notes for v2.0.0.", info.body)
        assertEquals(
            "https://github.com/abdrhmniqbal/startune-music/releases/tag/v2.0.0",
            info.htmlUrl
        )
        assertEquals(
            "https://github.com/abdrhmniqbal/startune-music/releases/download/v2.0.0/startune-music-2.0.0-arm64-v8a.apk",
            info.downloadUrl
        )
        assertEquals("startune-music-2.0.0-arm64-v8a.apk", info.apkFileName)
        assertEquals(false, info.prerelease)
    }

    @Test
    fun changelogUpdateKeepsPrereleaseSuffixInUrl() {
        val info = repository.toUpdateInfoFromChangelog(
            release("v2.0.1-beta", prerelease = true),
            currentVersion = "2.0.0",
            abi = "armeabi-v7a"
        )
        assertEquals("v2.0.1-beta", info.newVersion)
        assertEquals(
            "https://github.com/abdrhmniqbal/startune-music/releases/download/v2.0.1-beta/startune-music-2.0.1-beta-armeabi-v7a.apk",
            info.downloadUrl
        )
        assertEquals("startune-music-2.0.1-beta-armeabi-v7a.apk", info.apkFileName)
        assertEquals(true, info.prerelease)
    }
}
