package com.startune.music.data.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChangelogParserTest {

    private val sample = """
        # Changelog

        All notable changes to this project will be documented in this file.

        ## [Unreleased]

        ## [2.0.0-beta] - 2026-11-11

        First beta of the Kotlin rewrite.

        ### Added
        - Local playback with Top Songs
        - Navidrome and Jellyfin libraries

        ## [1.8.11] - 2026-10-31

        ### ✨ What's New
        - Custom Proxy Support.

        ## [1.0.0] - 2026-06-30

        First stable release.
    """.trimIndent()

    @Test
    fun parsesVersionsDatesAndBodies() {
        val entries = GitHubReleasesRepository.parseChangelog(sample)
        assertEquals(
            listOf("v2.0.0-beta", "v1.8.11", "v1.0.0"),
            entries.map { it.tag_name }
        )
        assertEquals("2026-11-11T00:00:00Z", entries[0].published_at)
        assertTrue(entries[0].body!!.contains("First beta of the Kotlin rewrite."))
        assertTrue(entries[0].body!!.contains("- Local playback with Top Songs"))
        assertEquals(true, entries[0].prerelease)
        assertEquals(false, entries[1].prerelease)
    }

    @Test
    fun skipsUnreleasedAndEmptySections() {
        val entries = GitHubReleasesRepository.parseChangelog(
            "## [Unreleased]\n\n## [0.1.0] - 2026-03-12\n"
        )
        assertTrue(entries.isEmpty())
    }
}
