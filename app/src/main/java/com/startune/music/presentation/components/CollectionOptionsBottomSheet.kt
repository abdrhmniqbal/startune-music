package com.startune.music.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.startune.music.R
import com.startune.music.data.model.ItemOptionsTarget
import com.startune.music.data.model.MusicFolder
import com.startune.music.data.model.Song
import com.startune.music.data.model.isMix
import com.startune.music.presentation.screens.RenamePlaylistDialog
import com.startune.music.presentation.viewmodel.PlayerViewModel
import com.startune.music.presentation.viewmodel.PlaylistViewModel
import com.startune.music.utils.formatSongCount
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * The per-item more menu for a list row that is not a song. One sheet serves
 * albums, artists, playlists, genres and folders: a header with the item's
 * cover and title, then Play, Shuffle, Play next, Add to queue and Add to
 * playlist for its songs. Playlists add Rename and Delete (editable ones
 * only); folders add Open as playlist. Songs keep their own sheet
 * (SongInfoBottomSheet).
 */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionOptionsBottomSheet(
    target: ItemOptionsTarget,
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    onOpenFolderAsPlaylist: ((MusicFolder) -> Unit)? = null
) {
    val sheetState = rememberModalSheetState(skipPartiallyExpanded = true)

    var songs by remember(target) { mutableStateOf<List<Song>?>(null) }
    LaunchedEffect(target) {
        songs = try {
            playerViewModel.loadSongsForOptionsTarget(target)
        } catch (e: Exception) {
            emptyList()
        }
    }
    val loadedSongs = songs
    val hasSongs = !loadedSongs.isNullOrEmpty()

    var showPlaylistBottomSheet by remember(target) { mutableStateOf(false) }
    var showRenameDialog by remember(target) { mutableStateOf(false) }
    var showDeleteDialog by remember(target) { mutableStateOf(false) }

    val addedToQueueMessage = stringResource(R.string.toast_added_to_queue)
    val playingNextMessage = stringResource(R.string.toast_playing_next)
    val playlistViewModel: PlaylistViewModel = hiltViewModel()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CollectionOptionsHeader(target = target, songs = loadedSongs)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CollectionActionButton(
                        label = stringResource(R.string.play_playback),
                        icon = Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.play_playback),
                        enabled = hasSongs,
                        primary = true,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val list = loadedSongs ?: return@CollectionActionButton
                            if (list.isEmpty()) return@CollectionActionButton
                            playerViewModel.playSongs(
                                songsToPlay = list,
                                startSong = list.first(),
                                queueName = target.queueName
                            )
                            onDismiss()
                        }
                    )
                    CollectionActionButton(
                        label = stringResource(R.string.shortcut_shuffle_short),
                        icon = Icons.Rounded.Shuffle,
                        contentDescription = stringResource(R.string.shortcut_shuffle_short),
                        enabled = hasSongs,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val list = loadedSongs ?: return@CollectionActionButton
                            if (list.isEmpty()) return@CollectionActionButton
                            playerViewModel.playSongsShuffled(
                                songsToPlay = list,
                                queueName = target.queueName
                            )
                            onDismiss()
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CollectionActionButton(
                        label = stringResource(R.string.detail_hero_play_next),
                        icon = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.detail_hero_play_next),
                        enabled = hasSongs,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val list = loadedSongs ?: return@CollectionActionButton
                            if (list.isEmpty()) return@CollectionActionButton
                            playerViewModel.addSelectedAsNext(list)
                            playerViewModel.sendToast(playingNextMessage)
                            onDismiss()
                        }
                    )
                    CollectionActionButton(
                        label = stringResource(R.string.detail_hero_add_to_queue),
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = stringResource(R.string.detail_hero_add_to_queue),
                        enabled = hasSongs,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val list = loadedSongs ?: return@CollectionActionButton
                            if (list.isEmpty()) return@CollectionActionButton
                            playerViewModel.addSelectedToQueue(list)
                            playerViewModel.sendToast(addedToQueueMessage)
                            onDismiss()
                        }
                    )
                }

                CollectionActionButton(
                    label = stringResource(R.string.detail_hero_add_to_playlist),
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = stringResource(R.string.detail_hero_add_to_playlist),
                    enabled = hasSongs,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showPlaylistBottomSheet = true }
                )

                if (target is ItemOptionsTarget.Playlist) {
                    val playlist = target.playlist
                    val isEditable = !playlist.isMix &&
                        !playlist.id.startsWith(PlaylistViewModel.FOLDER_PLAYLIST_PREFIX)
                    if (isEditable) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CollectionActionButton(
                                label = stringResource(R.string.action_rename),
                                icon = Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.action_rename),
                                modifier = Modifier.weight(1f),
                                onClick = { showRenameDialog = true }
                            )
                            CollectionActionButton(
                                label = stringResource(R.string.action_delete),
                                icon = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                destructive = true,
                                modifier = Modifier.weight(1f),
                                onClick = { showDeleteDialog = true }
                            )
                        }
                    }
                }

                if (target is ItemOptionsTarget.Folder && onOpenFolderAsPlaylist != null) {
                    CollectionActionButton(
                        label = stringResource(R.string.item_options_open_as_playlist),
                        icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                        contentDescription = stringResource(R.string.item_options_open_as_playlist),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onOpenFolderAsPlaylist(target.folder)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }

    if (showPlaylistBottomSheet && hasSongs) {
        val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
        val navBarCompactMode by playerViewModel.navBarCompactMode.collectAsStateWithLifecycle()
        val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        PlaylistBottomSheet(
            playlistUiState = playlistUiState,
            songs = loadedSongs!!.toImmutableList(),
            onDismiss = { showPlaylistBottomSheet = false },
            bottomBarHeight = resolveNavBarOccupiedHeight(systemNavBarInset, navBarCompactMode),
            playerViewModel = playerViewModel,
            playlistViewModel = playlistViewModel
        )
    }

    if (showRenameDialog && target is ItemOptionsTarget.Playlist) {
        RenamePlaylistDialog(
            currentName = target.playlist.name,
            onDismiss = { showRenameDialog = false },
            onRename = { newName ->
                playlistViewModel.renamePlaylist(target.playlist.id, newName)
                showRenameDialog = false
                onDismiss()
            }
        )
    }

    if (showDeleteDialog && target is ItemOptionsTarget.Playlist) {
        DeletePlaylistsConfirmationDialog(
            count = 1,
            onConfirm = {
                playlistViewModel.deletePlaylist(target.playlist.id)
                showDeleteDialog = false
                onDismiss()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun CollectionOptionsHeader(target: ItemOptionsTarget, songs: List<Song>?) {
    val songCount = songs?.size
    val subtitle: String? = when (target) {
        is ItemOptionsTarget.Album -> {
            val artist = target.album.artist.takeIf { it.isNotBlank() }
            val count = formatSongCount(target.album.songCount)
            if (artist != null) "$artist • $count" else count
        }
        is ItemOptionsTarget.Artist -> formatSongCount(target.artist.songCount)
        is ItemOptionsTarget.Playlist -> formatSongCount(target.playlist.songIds.size)
        is ItemOptionsTarget.Genre -> songCount?.let { formatSongCount(it) }
        is ItemOptionsTarget.Folder -> formatSongCount(target.folder.totalSongCount)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CollectionOptionsCover(target = target, songs = songs)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = target.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CollectionOptionsCover(target: ItemOptionsTarget, songs: List<Song>?) {
    when (target) {
        is ItemOptionsTarget.Album -> {
            SmartImage(
                model = target.album.albumArtUriString,
                contentDescription = stringResource(R.string.cd_album_art_for_title, target.album.title),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
        }
        is ItemOptionsTarget.Artist -> {
            val imageUrl = target.artist.effectiveImageUrl
            if (!imageUrl.isNullOrEmpty()) {
                SmartImage(
                    model = imageUrl,
                    contentDescription = target.artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_artist_24),
                        contentDescription = stringResource(R.string.presentation_batch_d_cd_artist),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        is ItemOptionsTarget.Playlist -> {
            PlaylistCover(
                playlist = target.playlist,
                playlistSongs = songs?.toImmutableList() ?: persistentListOf(),
                size = 56.dp
            )
        }
        is ItemOptionsTarget.Genre -> {
            val colorScheme = MaterialTheme.colorScheme
            val rainbowPalette = com.startune.music.ui.theme.LocalRainbowPalette.current
            val themeColor = remember(target.genre, colorScheme, rainbowPalette) {
                com.startune.music.ui.theme.GenreThemeUtils.getGenreThemeColor(
                    genre = target.genre,
                    scheme = colorScheme,
                    fallbackGenreId = target.genre.id,
                    storedPalette = rainbowPalette
                )
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(themeColor.container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = themeColor.onContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        is ItemOptionsTarget.Folder -> {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_folder),
                    contentDescription = stringResource(R.string.presentation_batch_d_cd_folder),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun CollectionActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val colors = when {
        destructive -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
        primary -> ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        else -> ButtonDefaults.filledTonalButtonColors()
    }
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        colors = colors,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
        modifier = modifier.heightIn(min = 56.dp)
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The trailing more button shared by every non-song list row. Same size and
 * shape as the song row's button; hidden while the list is in selection mode.
 */
@Composable
fun ItemMoreOptionsButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier.size(36.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = stringResource(R.string.presentation_batch_g_list_cd_more_for_title, title),
            modifier = Modifier.size(24.dp)
        )
    }
}
