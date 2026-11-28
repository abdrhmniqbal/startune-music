package com.startune.music.presentation.viewmodel

import com.startune.music.data.model.JumpBackInItem
import com.startune.music.data.model.PlayedEntityType
import com.startune.music.data.model.Song
import com.startune.music.data.repository.PlayedEntityRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JumpBackInViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `built-in mix cards get their collage from the mix songs`() = runTest(testDispatcher) {
        val repository = mockk<PlayedEntityRepository>()
        val holder = mockk<DailyMixStateHolder>()
        val forYouItem = JumpBackInItem(
            type = PlayedEntityType.MIX,
            id = "for-you",
            title = "For You Mix",
            subtitle = null,
            artworkUri = null
        )
        val dailyItem = JumpBackInItem(
            type = PlayedEntityType.MIX,
            id = "daily",
            title = "Daily Mix",
            subtitle = null,
            artworkUri = null
        )
        val albumItem = JumpBackInItem(
            type = PlayedEntityType.ALBUM,
            id = "7",
            title = "Dreams",
            subtitle = "Fleetwood Mac",
            artworkUri = "content://album/7"
        )
        every { repository.observeJumpBackIn(4) } returns flowOf(
            listOf(forYouItem, dailyItem, albumItem)
        )
        every { holder.dailyMixSongs } returns MutableStateFlow(
            listOf(
                song(albumId = 1L, art = "content://art/1"),
                song(albumId = 1L, art = "content://art/1-other"),
                song(albumId = 2L, art = "content://art/2")
            ).toImmutableList()
        )
        every { holder.yourMixSongs } returns MutableStateFlow(
            listOf(song(albumId = 3L, art = "content://art/3")).toImmutableList()
        )

        val viewModel = JumpBackInViewModel(repository, holder)
        val items = viewModel.items.first { it.size == 3 }

        assertEquals(listOf("content://art/3"), items[0].collageArtworkUris)
        assertEquals(listOf("content://art/1", "content://art/2"), items[1].collageArtworkUris)
        assertEquals(albumItem, items[2])
    }

    @Test
    fun `a mix that already has a collage keeps it`() = runTest(testDispatcher) {
        val repository = mockk<PlayedEntityRepository>()
        val holder = mockk<DailyMixStateHolder>()
        val mixItem = JumpBackInItem(
            type = PlayedEntityType.MIX,
            id = "user-mix",
            title = "Road Trip",
            subtitle = null,
            artworkUri = null,
            collageArtworkUris = persistentListOf("content://art/9")
        )
        every { repository.observeJumpBackIn(4) } returns flowOf(listOf(mixItem))
        every { holder.dailyMixSongs } returns MutableStateFlow(persistentListOf<Song>())
        every { holder.yourMixSongs } returns MutableStateFlow(persistentListOf<Song>())

        val viewModel = JumpBackInViewModel(repository, holder)
        val items = viewModel.items.first { it.isNotEmpty() }

        assertEquals(listOf("content://art/9"), items[0].collageArtworkUris)
    }

    private fun song(albumId: Long, art: String?) =
        Song.emptySong().copy(albumId = albumId, albumArtUriString = art)
}
