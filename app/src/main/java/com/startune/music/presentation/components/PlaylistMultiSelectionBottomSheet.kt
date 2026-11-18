package com.startune.music.presentation.components

import android.app.Activity
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Merge
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startune.music.data.model.Playlist
import com.startune.music.data.model.Song
import com.startune.music.presentation.components.subcomps.AutoSizingTextToFill
import com.startune.music.presentation.viewmodel.PlayerViewModel
import com.startune.music.ui.theme.RoundedSans
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import androidx.compose.ui.res.stringResource
import com.startune.music.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

/**
 * Bottom sheet for batch operations on multiple selected playlists.
 * Designed to match the MultiSelectionBottomSheet (songs) in layout, animation and styling.
 *
 * @param selectedPlaylists List of selected playlists
 * @param onDismiss Callback when sheet is dismissed
 * @param onDeleteAll Delete all selected playlists
 * @param onExportAll Export all selected playlists as M3U files
 * @param onMergeAll Merge all selected playlists into one
 * @param onShareAll Share all selected playlists as ZIP file
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlaylistMultiSelectionBottomSheet(
    selectedPlaylists: ImmutableList<Playlist>,
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onDeleteAll: () -> Unit,
    onExportAll: () -> Unit,
    onMergeAll: () -> Unit,
    onShareAll: () -> Unit
) {
    val sheetState = rememberModalSheetState(skipPartiallyExpanded = true)
    val evenCornerRadius = 26.dp
    val buttonShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = evenCornerRadius, smoothnessAsPercentBR = 60,
        cornerRadiusBR = evenCornerRadius, smoothnessAsPercentTL = 60,
        cornerRadiusTL = evenCornerRadius, smoothnessAsPercentBL = 60,
        cornerRadiusBL = evenCornerRadius, smoothnessAsPercentTR = 60
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Box(
            modifier = Modifier
                .animateContentSize(animationSpec = tween(durationMillis = 200))
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val stackedImageSize = 66.dp
                    val stackedOverlap = 33.dp
                    val stackedCount = selectedPlaylists.take(4).size
                    val stackedWidth = if (stackedCount > 0) {
                        (stackedImageSize - stackedOverlap) * (stackedCount - 1) + stackedImageSize
                    } else 0.dp

                    StackedPlaylistCovers(
                        playlists = remember(selectedPlaylists) { selectedPlaylists.take(4).toImmutableList() },
                        playerViewModel = playerViewModel,
                        modifier = Modifier
                            .height(74.dp)
                            .width(stackedWidth)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        // Inherits LocalContentColor, which is onSurface inside the sheet.
                        AutoSizingTextToFill(
                            text = stringResource(R.string.multi_selection_playlists_count_upper, selectedPlaylists.size),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RoundedSans,
                            minFontSize = 16.sp,
                            maxFontSizeLimit = 24.sp,
                            maxLines = 2,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.multi_selection_selected),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = RoundedSans,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        MoreMenuListRow(
                            label = stringResource(R.string.action_export),
                            icon = Icons.Rounded.FileDownload,
                            onClick = {
                                onExportAll()
                                onDismiss()
                            }
                        )
                    }
                    item {
                        MoreMenuListRow(
                            label = stringResource(R.string.action_merge),
                            icon = Icons.Rounded.Merge,
                            onClick = {
                                onMergeAll()
                                onDismiss()
                            }
                        )
                    }
                    item {
                        MoreMenuListRow(
                            label = stringResource(R.string.action_share),
                            icon = Icons.Rounded.Share,
                            onClick = {
                                onShareAll()
                                onDismiss()
                            }
                        )
                    }
                    // Destructive actions always go last, so they sit at the
                    // very bottom of the sheet.
                    item {
                        MoreMenuListRow(
                            label = stringResource(R.string.delete_action),
                            icon = Icons.Rounded.Delete,
                            destructive = true,
                            onClick = {
                                onDeleteAll()
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Displays stacked playlist cover images with overlap effect for the bottom sheet header.
 * Mirrors the StackedAlbumArts component from MultiSelectionBottomSheet.
 */
@Composable
private fun StackedPlaylistCovers(
    playlists: ImmutableList<Playlist>,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier
) {
    val imageSize = 66.dp
    val overlap = 33.dp
    val borderWidth = 3.dp
    val borderColor = MaterialTheme.colorScheme.surfaceContainerLow

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        playlists.forEachIndexed { index, playlist ->
            val offsetX = index * (imageSize.value - overlap.value)

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.dp.roundToPx(), 0) }
                    .zIndex((playlists.size - index).toFloat())
                    .size(imageSize)
                    .background(borderColor, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(borderWidth)
                        .clip(CircleShape)
                ) {
                    // The real cover: a custom image, a custom color, or the
                    // auto collage from the playlist's songs. A playlist with
                    // no custom cover used to fall back to its initial letter
                    // here even though the list row shows its collage.
                    StackedPlaylistCoverImage(
                        playlist = playlist,
                        playerViewModel = playerViewModel,
                        size = imageSize - borderWidth * 2,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * One playlist's real cover for the stacked header. Loads the playlist's
 * songs (first 16, like the list row) so PlaylistCover can build the auto
 * collage when the playlist has no custom cover or color.
 */
@Composable
private fun StackedPlaylistCoverImage(
    playlist: Playlist,
    playerViewModel: PlayerViewModel,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val previewSongIds = remember(playlist.songIds) { playlist.songIds.take(16) }
    val initialValue = remember(previewSongIds) {
        if (previewSongIds.isEmpty()) emptyList<Song>() else null
    }
    val playlistSongs by remember(previewSongIds, playerViewModel) {
        playerViewModel.observeSongs(previewSongIds).map<List<Song>, List<Song>?> { it }
    }.collectAsStateWithLifecycle(initialValue = initialValue)

    PlaylistCover(
        playlist = playlist,
        playlistSongs = remember(playlistSongs) { (playlistSongs ?: emptyList()).toImmutableList() },
        size = size,
        applyCoverShape = false,
        modifier = modifier
    )
}
