package com.startune.music.data.musicbrainz

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.jupiter.api.Test

class MusicBrainzArtistSearchTest {

    private val bibiJson = """
        {
          "artists": [
            {
              "id": "2c89e930-7769-403f-8145-f215bb8fb5ba",
              "name": "Bibi Blocksberg",
              "type": "Character",
              "country": "DE",
              "disambiguation": "fictitious artist",
              "score": 100
            },
            {
              "id": "21c93d2d-dc10-4f8f-ae91-7285eff37c2f",
              "name": "BIBI",
              "type": "Person",
              "country": "KR",
              "disambiguation": "South Korean singer",
              "score": 95
            },
            {
              "id": "ec387e78-d510-447b-be78-4945b55fe708",
              "name": "BiBi",
              "type": "Group",
              "country": "JP",
              "disambiguation": "subgroup of fictional Japanese idol group μ's",
              "score": 90
            },
            {
              "id": "01cfc4db-3407-476c-bfe2-757228bac8ba",
              "name": "Bibi",
              "type": "Person",
              "country": "FR",
              "score": 85
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `parses artists with optional fields missing`() {
        val artists = MusicBrainzApiService.parseArtistSearchResponse(JSONObject(bibiJson))

        assertThat(artists).hasSize(4)
        assertThat(artists[1].mbid).isEqualTo("21c93d2d-dc10-4f8f-ae91-7285eff37c2f")
        assertThat(artists[1].name).isEqualTo("BIBI")
        assertThat(artists[1].type).isEqualTo("Person")
        assertThat(artists[1].country).isEqualTo("KR")
        assertThat(artists[1].disambiguation).isEqualTo("South Korean singer")
        assertThat(artists[1].score).isEqualTo(95)
        assertThat(artists[3].disambiguation).isNull()
    }

    @Test
    fun `skips artists without an id or a name`() {
        val artists = MusicBrainzApiService.parseArtistSearchResponse(
            JSONObject("""{"artists": [{"id": "", "name": "Nope"}, {"id": "x", "name": ""}, {"id": "y", "name": "Kept"}]}""")
        )

        assertThat(artists.map { it.mbid }).containsExactly("y")
    }

    @Test
    fun `repository keeps only real performers`() = runTest {
        val api = mockk<MusicBrainzApiService>()
        coEvery { api.searchArtists(any()) } returns
            MusicBrainzApiService.parseArtistSearchResponse(JSONObject(bibiJson))
        val repository = MusicBrainzRepository(api, mockk())

        val candidates = repository.searchArtists("BIBI")

        assertThat(candidates.map { it.name }).containsExactly("BIBI", "BiBi", "Bibi")
            .inOrder()
    }
}
