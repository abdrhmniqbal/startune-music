package com.startune.music.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startune.music.data.model.JumpBackInItem
import com.startune.music.data.model.PlayedEntityType
import com.startune.music.data.repository.PlayedEntityRepository
import com.startune.music.presentation.components.distinctAlbumCovers
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Home "Jump back in": the four most recently played albums, artists,
 * playlists, and mixes, latest first.
 *
 * Built-in mixes have no playlist row, so the repository cannot resolve their
 * artwork; the mix songs live here in the presentation layer instead, and this
 * ViewModel fills the collage from them. A built-in mix keeps the recorded
 * name and an empty collage until its songs load.
 */
@HiltViewModel
class JumpBackInViewModel @Inject constructor(
    playedEntityRepository: PlayedEntityRepository,
    dailyMixStateHolder: DailyMixStateHolder
) : ViewModel() {
    val items: StateFlow<ImmutableList<JumpBackInItem>> =
        combine(
            playedEntityRepository.observeJumpBackIn(JUMP_BACK_IN_LIMIT),
            dailyMixStateHolder.dailyMixSongs,
            dailyMixStateHolder.yourMixSongs
        ) { items, dailyMixSongs, yourMixSongs ->
            items.map { item ->
                if (item.type != PlayedEntityType.MIX || item.collageArtworkUris.isNotEmpty()) {
                    item
                } else {
                    val mixSongs = when (item.id) {
                        MixIds.DAILY -> dailyMixSongs
                        MixIds.FOR_YOU -> yourMixSongs
                        else -> null
                    } ?: return@map item
                    item.copy(
                        collageArtworkUris = distinctAlbumCovers(mixSongs)
                            .map { it.first }
                            .toImmutableList()
                    )
                }
            }.toImmutableList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), persistentListOf())

    companion object {
        const val JUMP_BACK_IN_LIMIT = 4
    }
}
