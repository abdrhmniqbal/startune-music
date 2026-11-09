package com.startune.music.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startune.music.data.model.Song
import com.startune.music.presentation.viewmodel.MultiSelectionStateHolder
import com.startune.music.presentation.viewmodel.PlayerViewModel
import com.startune.music.presentation.viewmodel.PlaylistViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Shared host for the song batch-action sheet outside the Library.
 * Wires [MultiSelectionBottomSheet] to the standard [PlayerViewModel] batch actions
 * and opens [PlaylistBottomSheet] for Add to playlist, so detail, search and
 * history screens do not each copy the same ten-callback call site.
 *
 * Batch edit and cloud download are library-only and stay hidden here
 * ([MultiSelectionBottomSheet] hides them when their callbacks are null).
 *
 * [onActionTaken] runs after any batch action completes, so a screen that
 * resolved a category selection (albums, artists) into songs can clear
 * that category selection at the same moment the song selection clears.
 */
@Composable
fun SongMultiSelectionHost(
    playerViewModel: PlayerViewModel,
    multiSelectionState: MultiSelectionStateHolder,
    showSheet: Boolean,
    onDismiss: () -> Unit,
    bottomBarHeight: Dp = MiniPlayerHeight,
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    onActionTaken: () -> Unit = {}
) {
    val selectedSongs by multiSelectionState.selectedSongs.collectAsStateWithLifecycle()
    val favoriteSongIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()

    var showPlaylistPicker by remember { mutableStateOf(false) }
    var pickerSongs by remember { mutableStateOf<ImmutableList<Song>>(persistentListOf()) }

    if (showSheet && selectedSongs.isNotEmpty()) {
        MultiSelectionBottomSheet(
            selectedSongs = selectedSongs,
            favoriteSongIds = favoriteSongIds,
            onDismiss = onDismiss,
            onPlayAll = {
                playerViewModel.playSelectedSongs(selectedSongs)
                multiSelectionState.clearSelection()
                onActionTaken()
            },
            onAddToQueue = {
                playerViewModel.addSelectedToQueue(selectedSongs)
                multiSelectionState.clearSelection()
                onActionTaken()
            },
            onPlayNext = {
                playerViewModel.addSelectedAsNext(selectedSongs)
                multiSelectionState.clearSelection()
                onActionTaken()
            },
            onAddToPlaylist = {
                pickerSongs = selectedSongs
                multiSelectionState.clearSelection()
                onActionTaken()
                showPlaylistPicker = true
            },
            onToggleLikeAll = { shouldLike ->
                if (shouldLike) {
                    playerViewModel.likeSelectedSongs(selectedSongs)
                } else {
                    playerViewModel.unlikeSelectedSongs(selectedSongs)
                }
                multiSelectionState.clearSelection()
                onActionTaken()
            },
            onShareAll = {
                playerViewModel.shareSelectedAsZip(selectedSongs)
                multiSelectionState.clearSelection()
                onActionTaken()
            },
            onDeleteAll = { deleteActivity, onComplete ->
                playerViewModel.deleteSelectedFromDevice(deleteActivity, selectedSongs) {
                    multiSelectionState.clearSelection()
                    onActionTaken()
                    onComplete(true)
                }
            }
        )
    }

    if (showPlaylistPicker && pickerSongs.isNotEmpty()) {
        PlaylistBottomSheet(
            playlistUiState = playlistUiState,
            songs = pickerSongs,
            onDismiss = {
                showPlaylistPicker = false
                pickerSongs = persistentListOf()
            },
            bottomBarHeight = bottomBarHeight,
            playerViewModel = playerViewModel,
            playlistViewModel = playlistViewModel
        )
    }
}
