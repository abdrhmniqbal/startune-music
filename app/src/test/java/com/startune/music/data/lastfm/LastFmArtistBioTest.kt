package com.startune.music.data.lastfm

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.jupiter.api.Test

class LastFmArtistBioTest {

    @Test
    fun `detects the Lastfm disambiguation bio`() {
        assertThat(
            isDisambiguationBio(
                "There are multiple artists called Bibi:\n\n1. BiBi is a sub-unit of group idol μ's."
            )
        ).isTrue()
    }

    @Test
    fun `ignores leading whitespace and case when detecting a disambiguation bio`() {
        assertThat(isDisambiguationBio("  there are multiple artists called Bibi:"))
            .isTrue()
    }

    @Test
    fun `does not flag a normal bio as disambiguation`() {
        assertThat(
            isDisambiguationBio(
                "BIBI (비비) is a South Korean solo singer-songwriter and rapper " +
                    "currently under Feel GHood Music."
            )
        ).isFalse()
    }

    @Test
    fun `does not flag an empty bio as disambiguation`() {
        assertThat(isDisambiguationBio("")).isFalse()
    }

    @Test
    fun `mbid request serializes without the artist name`() {
        val json = Gson().toJson(LastFmArtistInfoRequest(mbid = "21c93d2d-dc10-4f8f-ae91-7285eff37c2f"))

        assertThat(json).contains("\"mbid\":\"21c93d2d-dc10-4f8f-ae91-7285eff37c2f\"")
        assertThat(json).doesNotContain("artist")
    }

    @Test
    fun `name request serializes without an mbid`() {
        val json = Gson().toJson(LastFmArtistInfoRequest(artist = "BIBI"))

        assertThat(json).contains("\"artist\":\"BIBI\"")
        assertThat(json).doesNotContain("mbid")
    }
}
