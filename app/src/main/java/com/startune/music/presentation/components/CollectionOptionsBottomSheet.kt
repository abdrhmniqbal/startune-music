package com.startune.music.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
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
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map

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
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CollectionOptionsHeader(target = target, songs = loadedSongs, playerViewModel = playerViewModel)

            // One row of three equal action cards, then full-width list rows.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MoreMenuActionCard(
                    label = stringResource(R.string.play_playback),
                    icon = Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.play_playback),
                    enabled = hasSongs,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val list = loadedSongs ?: return@MoreMenuActionCard
                        if (list.isEmpty()) return@MoreMenuActionCard
                        playerViewModel.playSongs(
                            songsToPlay = list,
                            startSong = list.first(),
                            queueName = target.queueName
                        )
                        onDismiss()
                    }
                )
                MoreMenuActionCard(
                    label = stringResource(R.string.shortcut_shuffle_short),
                    icon = Icons.Rounded.Shuffle,
                    contentDescription = stringResource(R.string.shortcut_shuffle_short),
                    enabled = hasSongs,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val list = loadedSongs ?: return@MoreMenuActionCard
                        if (list.isEmpty()) return@MoreMenuActionCard
                        playerViewModel.playSongsShuffled(
                            songsToPlay = list,
                            queueName = target.queueName
                        )
                        onDismiss()
                    }
                )
                MoreMenuActionCard(
                    label = stringResource(R.string.detail_hero_play_next),
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = stringResource(R.string.detail_hero_play_next),
                    enabled = hasSongs,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val list = loadedSongs ?: return@MoreMenuActionCard
                        if (list.isEmpty()) return@MoreMenuActionCard
                        playerViewModel.addSelectedAsNext(list)
                        playerViewModel.sendToast(playingNextMessage)
                        onDismiss()
                    }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MoreMenuListRow(
                    label = stringResource(R.string.detail_hero_add_to_queue),
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    enabled = hasSongs,
                    onClick = {
                        val list = loadedSongs ?: return@MoreMenuListRow
                        if (list.isEmpty()) return@MoreMenuListRow
                        playerViewModel.addSelectedToQueue(list)
                        playerViewModel.sendToast(addedToQueueMessage)
                        onDismiss()
                    }
                )
                MoreMenuListRow(
                    label = stringResource(R.string.detail_hero_add_to_playlist),
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    enabled = hasSongs,
                    onClick = { showPlaylistBottomSheet = true }
                )

                if (target is ItemOptionsTarget.Playlist) {
                    val playlist = target.playlist
                    val isEditable = !playlist.isMix &&
                        !playlist.id.startsWith(PlaylistViewModel.FOLDER_PLAYLIST_PREFIX)
                    if (isEditable) {
                        MoreMenuListRow(
                            label = stringResource(R.string.action_rename),
                            icon = Icons.Rounded.Edit,
                            onClick = { showRenameDialog = true }
                        )
                        MoreMenuListRow(
                            label = stringResource(R.string.action_delete),
                            icon = Icons.Rounded.Delete,
                            destructive = true,
                            onClick = { showDeleteDialog = true }
                        )
                    }
                }

                if (target is ItemOptionsTarget.Folder && onOpenFolderAsPlaylist != null) {
                    MoreMenuListRow(
                        label = stringResource(R.string.item_options_open_as_playlist),
                        icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
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
private fun CollectionOptionsHeader(
    target: ItemOptionsTarget,
    songs: List<Song>?,
    playerViewModel: PlayerViewModel
) {
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
        CollectionOptionsCover(target = target, playerViewModel = playerViewModel)
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
private fun CollectionOptionsCover(target: ItemOptionsTarget, playerViewModel: PlayerViewModel) {
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
            // Mixes keep their signature stacked cluster, exactly like the
            // library mix card; regular playlists keep the 2x2 collage the
            // rows show, observed the same way (first 4 songs) so it is
            // there from the first frame instead of a placeholder.
            if (target.playlist.isMix) {
                val mixSongs by remember(target.playlist.songIds, playerViewModel) {
                    playerViewModel.observeSongs(target.playlist.songIds)
                        .map<List<Song>, List<Song>?> { it }
                }.collectAsStateWithLifecycle(
                    initialValue = remember(target.playlist.songIds) {
                        if (target.playlist.songIds.isEmpty()) emptyList<Song>() else null
                    }
                )
                val clusterArts = remember(mixSongs) {
                    (mixSongs ?: emptyList())
                        .mapNotNull { it.albumArtUriString }
                        .distinct()
                        .take(3)
                }
                MixThumbnailCluster(
                    artUrls = clusterArts,
                    centerSize = 56.dp,
                    overlap = 20.dp,
                    borderWidth = 2.dp
                )
            } else {
                val previewSongIds = remember(target.playlist.songIds) {
                    target.playlist.songIds.take(4)
                }
                val previewSongs by remember(previewSongIds, playerViewModel) {
                    playerViewModel.observeSongs(previewSongIds)
                        .map<List<Song>, List<Song>?> { it }
                }.collectAsStateWithLifecycle(
                    initialValue = remember(previewSongIds) {
                        if (previewSongIds.isEmpty()) emptyList<Song>() else null
                    }
                )
                PlaylistCover(
                    playlist = target.playlist,
                    playlistSongs = remember(previewSongs) {
                        (previewSongs ?: emptyList()).toImmutableList()
                    },
                    size = 56.dp
                )
            }
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
