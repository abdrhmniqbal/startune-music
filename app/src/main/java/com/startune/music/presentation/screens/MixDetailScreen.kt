package com.startune.music.presentation.screens

import com.startune.music.presentation.navigation.navigateSafely

import androidx.compose.foundation.background
import com.startune.music.presentation.components.subcomps.SelectionActionRow
import com.startune.music.presentation.components.subcomps.SelectionCountPill
import androidx.compose.ui.zIndex
import com.startune.music.presentation.components.rememberModalSheetState
import com.startune.music.presentation.components.SongMultiSelectionHost
import com.startune.music.data.model.MixRule
import com.startune.music.data.model.fromPlaylistSource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.layout.width
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startune.music.ui.theme.LocalStartuneDarkTheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.startune.music.utils.traceSection
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.startune.music.R
import com.startune.music.data.model.Song
import com.startune.music.presentation.components.MiniPlayerHeight
import com.startune.music.presentation.components.PlaylistBottomSheet
import com.startune.music.presentation.components.SmartImage
import com.startune.music.presentation.components.SongInfoBottomSheet
import com.startune.music.presentation.components.ExpressiveScrollBar
import com.startune.music.presentation.components.SaveQueueAsPlaylistSheet
import com.startune.music.presentation.components.getIconByName
import com.startune.music.presentation.components.getThemeContentColor
import com.startune.music.presentation.components.resolveNavBarOccupiedHeight
import com.startune.music.presentation.navigation.Screen
import com.startune.music.presentation.viewmodel.MainViewModel
import com.startune.music.presentation.viewmodel.MixIds
import com.startune.music.presentation.viewmodel.PlayerViewModel
import com.startune.music.presentation.viewmodel.PlaylistViewModel
import com.startune.music.utils.formatDuration
import com.startune.music.ui.theme.ShapeCache
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.startune.music.presentation.components.subcomps.EnhancedSongListItem
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.startune.music.presentation.components.MixThumbnailCluster
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MixDetailScreen(
    mixId: String,
    mainViewModel: MainViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel,
    navController: NavController,
) = traceSection("MixDetailScreen.Composition") {
    val isForYouMix = mixId == MixIds.FOR_YOU
    val isUserMix = !isForYouMix && mixId != MixIds.DAILY
    val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val userMix = if (isUserMix) playlistUiState.currentPlaylistDetails else null
    val mixTitle = when {
        isForYouMix -> stringResource(R.string.mix_for_you_title)
        isUserMix -> userMix?.name ?: ""
        else -> stringResource(R.string.presentation_batch_b_daily_mix_title)
    }
    val mixSongs: ImmutableList<Song> = when {
        isForYouMix -> playerViewModel.yourMixSongs.collectAsStateWithLifecycle().value
        isUserMix -> playlistUiState.currentPlaylistSongs.toImmutableList()
        else -> playerViewModel.dailyMixSongs.collectAsStateWithLifecycle().value
    }

    // Page colors come from the mix artwork: the custom cover image
    // when set, otherwise the first song art of the header collage.
    // A solid custom cover color has no image, so the page keeps
    // the default colors.
    val isDarkTheme = LocalStartuneDarkTheme.current
    val baseColorScheme = MaterialTheme.colorScheme
    val useArtworkColorForDetails by playerViewModel.useArtworkColorForDetails.collectAsStateWithLifecycle()
    val mixArtUri = remember(userMix, mixSongs) {
        userMix?.coverImageUri?.takeIf { it.isNotBlank() }
            ?: mixSongs.firstNotNullOfOrNull { it.albumArtUriString?.takeIf { uri -> uri.isNotBlank() } }
    }
    val mixColorSchemeFlow = remember(mixArtUri) {
        mixArtUri?.let { playerViewModel.themeStateHolder.getAlbumColorSchemeFlow(it) }
    }
    val mixColorSchemePair = mixColorSchemeFlow?.collectAsStateWithLifecycle()?.value
    val mixColorScheme = remember(mixColorSchemePair, isDarkTheme, baseColorScheme, useArtworkColorForDetails) {
        if (!useArtworkColorForDetails) baseColorScheme
        else mixColorSchemePair?.let { pair -> if (isDarkTheme) pair.dark else pair.light }
            ?: baseColorScheme
    }
    val shuffleLabel = stringResource(R.string.shortcut_shuffle_short)
    val playlistCreatedToast = stringResource(R.string.playlist_created_songs_added_toast)
    val forYouMixSongCount by playerViewModel.forYouMixSongCount.collectAsStateWithLifecycle()

    val multiSelectionState = playerViewModel.multiSelectionStateHolder
    val selectedSongs by multiSelectionState.selectedSongs.collectAsStateWithLifecycle()
    val isSelectionMode by multiSelectionState.isSelectionMode.collectAsStateWithLifecycle()
    val selectedSongIds by multiSelectionState.selectedSongIds.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var showMultiSelectionSheet by remember { mutableStateOf(false) }
    var showMixOptionsSheet by remember { mutableStateOf(false) }
    var isRemoveModeEnabled by remember { mutableStateOf(false) }
    var showEditMixForm by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showSaveAsPlaylistSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) { multiSelectionState.clearSelection() }
    DisposableEffect(Unit) { onDispose { multiSelectionState.clearSelection() } }

    LaunchedEffect(mixId) {
        if (isUserMix) playlistViewModel.loadPlaylistDetails(mixId)
    }
    val currentSongId by remember { playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged() }.collectAsStateWithLifecycle(initialValue = null)
    val isPlaying by remember { playerViewModel.stablePlayerState.map { it.isPlaying }.distinctUntilChanged() }.collectAsStateWithLifecycle(initialValue = false)
    val isShuffleEnabled by remember { playerViewModel.stablePlayerState.map { it.isShuffleEnabled }.distinctUntilChanged() }.collectAsStateWithLifecycle(initialValue = false)
    val navBarCompactMode by playerViewModel.navBarCompactMode.collectAsStateWithLifecycle()
    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBarHeightDp = resolveNavBarOccupiedHeight(systemNavBarInset, navBarCompactMode)
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    val favoriteSongIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val selectedSongForInfo by playerViewModel.selectedSongForInfo.collectAsStateWithLifecycle()

    val lazyListState = rememberLazyListState()

    var showSongInfoSheet by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = mixColorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
        val surfaceContainer = MaterialTheme.colorScheme.surface
        val headerColor = MaterialTheme.colorScheme.primary
        val backgroundBrush = remember(surfaceContainer, headerColor) {
            Brush.verticalGradient(
                colors = listOf(
                    headerColor.copy(alpha = 0.25f),
                    surfaceContainer.copy(alpha = 0.5f),
                    surfaceContainer
                ),
                endY = 1200f
            )
        }

        if (showSongInfoSheet && selectedSongForInfo != null) {
            val song = selectedSongForInfo!!
            val removeFromListTrigger = remember(mixSongs) {
                {
                    when {
                        isForYouMix -> playerViewModel.removeFromYourMix(song.id)
                        isUserMix -> playlistViewModel.removeSongFromMix(mixId, song.id)
                        else -> playerViewModel.removeFromDailyMix(song.id)
                    }
                }
            }
            SongInfoBottomSheet(
                song = song,
                isFavorite = favoriteSongIds.contains(song.id),
                onToggleFavorite = { playerViewModel.toggleFavoriteSpecificSong(song) },
                onDismiss = { showSongInfoSheet = false },
                onPlaySong = {
                    playerViewModel.showAndPlaySong(song, mixSongs, mixTitle, isVoluntaryPlay = false)
                    showSongInfoSheet = false
                },
                onAddToQueue = {
                    playerViewModel.addSongToQueue(song)
                    showSongInfoSheet = false
                },
                onAddNextToQueue = {
                    playerViewModel.addSongNextToQueue(song)
                    showSongInfoSheet = false
                },
                onAddToPlayList = {
                    showPlaylistBottomSheet = true;
                },
                onDeleteFromDevice = playerViewModel::deleteFromDevice,
                onNavigateToAlbum = {
                    navController.navigateSafely(Screen.AlbumDetail.createRoute(song.albumId))
                    showSongInfoSheet = false
                },
                onNavigateToArtist = {
                    navController.navigateSafely(Screen.ArtistDetail.createRoute(song.artistId))
                    showSongInfoSheet = false
                },
                onNavigateToArtistById = { artistId ->
                    navController.navigateSafely(Screen.ArtistDetail.createRoute(artistId))
                    showSongInfoSheet = false
                },
                onNavigateToGenre = {
                    song.genre?.let {
                        navController.navigateSafely(Screen.GenreDetail.createRoute(java.net.URLEncoder.encode(it, "UTF-8")))
                    }
                    showSongInfoSheet = false
                },
                onEditSong = { newTitle, newArtist, newAlbum, newAlbumArtist, newComposer, newGenre, newLyrics, newTrackNumber, newDiscNumber, replayGainTrackGainDb, replayGainAlbumGainDb, coverArtUpdate, customMetadataChanges ->
                    playerViewModel.editSongMetadata(
                        song,
                        newTitle,
                        newArtist,
                        newAlbum,
                        newAlbumArtist,
                        newComposer,
                        newGenre,
                        newLyrics,
                        newTrackNumber,
                        newDiscNumber,
                        replayGainTrackGainDb,
                        replayGainAlbumGainDb,
                        coverArtUpdate,
                        customMetadataChanges
                    )
                },
                removeFromListTrigger = removeFromListTrigger,
                onRemoveFromList = {
                    when {
                        isForYouMix -> playerViewModel.removeFromYourMix(song.id)
                        isUserMix -> playlistViewModel.removeSongFromMix(mixId, song.id)
                        else -> playerViewModel.removeFromDailyMix(song.id)
                    }
                },
                removeFromListLabel = stringResource(R.string.mix_remove_from_mix)
            )

            if (showPlaylistBottomSheet) {
                val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()

                PlaylistBottomSheet(
                    playlistUiState = playlistUiState,
                    songs = persistentListOf(song),
                    onDismiss = { showPlaylistBottomSheet = false },
                    bottomBarHeight = bottomBarHeightDp,
                    playerViewModel = playerViewModel,
                )
            }
        }


        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        ) {
            if (mixSongs.isEmpty() && (!isUserMix || playlistUiState.isLoading)) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ContainedLoadingIndicator()
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "mix_header") {
                        ExpressiveMixDetailHeader(
                            title = mixTitle,
                            songs = mixSongs,
                            scrollState = lazyListState,
                            coverImageUri = userMix?.coverImageUri,
                            coverColorArgb = userMix?.coverColorArgb,
                            coverIconName = userMix?.coverIconName,
                            shuffleLabel = shuffleLabel,
                            onShuffleClick = {
                                if (mixSongs.isNotEmpty()) {
                                    playerViewModel.playSongsShuffled(
                                        songsToPlay = mixSongs,
                                        queueName = mixTitle,
                                        startAtZero = true,
                                    )
                                }
                            }
                        )
                    }

                    item(key = "mix_selection_row") {
                        AnimatedVisibility(
                            visible = isSelectionMode,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            SelectionActionRow(
                                selectedCount = selectedSongs.size,
                                onSelectAll = { multiSelectionState.selectAll(mixSongs) },
                                onDeselect = { multiSelectionState.clearSelection() },
                                onOptionsClick = { showMultiSelectionSheet = true },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    items(mixSongs, key = { it.id }) { song ->
                        EnhancedSongListItem(
                            modifier = Modifier
                                .padding(horizontal = 16.dp),
                            song = song,
                            isCurrentSong = currentSongId == song.id,
                            isPlaying = currentSongId == song.id && isPlaying,
                            isSelected = selectedSongIds.contains(song.id),
                            selectionIndex = if (isSelectionMode) multiSelectionState.getSelectionIndex(song.id) else null,
                            isSelectionMode = isSelectionMode,
                            onLongPress = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                multiSelectionState.toggleSelection(song)
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    multiSelectionState.toggleSelection(song)
                                } else {
                                    playerViewModel.showAndPlaySong(song, mixSongs, mixTitle, isVoluntaryPlay = false)
                                }
                            },
                            onMoreOptionsClick = {
                                playerViewModel.selectSongForInfo(song)
                                showSongInfoSheet = true
                            },
                            onRemoveClick = {
                                when {
                                    isForYouMix -> playerViewModel.removeFromYourMix(song.id)
                                    isUserMix -> playlistViewModel.removeSongFromMix(mixId, song.id)
                                    else -> playerViewModel.removeFromDailyMix(song.id)
                                }
                            },
                            isRemoveButtonVisible = isRemoveModeEnabled && !isSelectionMode
                        )
                    }
                }
            }

            FilledIconButton(
                onClick = { navController.popBackStack() },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 10.dp, top = 8.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.auth_cd_back)
                )
            }

            if (isForYouMix || isUserMix) {
                FilledIconButton(
                    onClick = {
                        if (isRemoveModeEnabled) {
                            isRemoveModeEnabled = false
                        } else {
                            showMixOptionsSheet = true
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(end = 10.dp, top = 8.dp)
                        .clip(CircleShape)
                ) {
                    if (isRemoveModeEnabled) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.confirm)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.presentation_batch_b_more_options)
                        )
                    }
                }
            }

            SelectionCountPill(
                selectedCount = selectedSongs.size,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(1f)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(80.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.surfaceContainerLowest.copy(0.5f),
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            )
                        )
                    )
            ) {

            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .height(50.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceContainerLowest.copy(0.5f),
                                Color.Transparent,
                            )
                        )
                    )
            ) {

            }

            ExpressiveScrollBar(
                listState = lazyListState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(
                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp,
                        bottom = if (currentSongId != null) MiniPlayerHeight + systemNavBarInset + 20.dp else systemNavBarInset + 16.dp,
                        end = 14.dp
                    )
            )
        }

        if (showMixOptionsSheet && (isForYouMix || isUserMix)) {
            val sheetState = rememberModalSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showMixOptionsSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = mixTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                    MixOptionsItem(
                        icon = painterResource(R.drawable.rounded_edit_24),
                        label = stringResource(R.string.mix_edit_title),
                        onClick = {
                            showMixOptionsSheet = false
                            showEditMixForm = true
                        }
                    )
                    MixOptionsItem(
                        icon = rememberVectorPainter(Icons.Filled.RemoveCircleOutline),
                        label = stringResource(R.string.presentation_batch_b_remove_songs),
                        selected = isRemoveModeEnabled,
                        onClick = {
                            showMixOptionsSheet = false
                            isRemoveModeEnabled = !isRemoveModeEnabled
                        }
                    )
                    MixOptionsItem(
                        icon = painterResource(R.drawable.outline_graph_1_24),
                        label = stringResource(R.string.presentation_batch_b_set_default_transition),
                        onClick = {
                            showMixOptionsSheet = false
                            navController.navigateSafely(Screen.EditTransition.createRoute(mixId))
                        }
                    )
                    MixOptionsItem(
                        icon = rememberVectorPainter(Icons.Filled.LibraryAdd),
                        label = stringResource(R.string.presentation_batch_e_action_save_as_playlist),
                        onClick = {
                            showMixOptionsSheet = false
                            showSaveAsPlaylistSheet = true
                        }
                    )
                    if (isUserMix) {
                        MixOptionsItem(
                            icon = painterResource(R.drawable.rounded_delete_24),
                            label = stringResource(R.string.mix_delete_title),
                            onClick = {
                                showMixOptionsSheet = false
                                showDeleteConfirmation = true
                            }
                        )
                    }
                }
            }
        }

        if (showSaveAsPlaylistSheet) {
            SaveQueueAsPlaylistSheet(
                songs = mixSongs,
                defaultName = mixTitle,
                onDismiss = { showSaveAsPlaylistSheet = false },
                onConfirm = { name, selectedIds ->
                    val orderedSelection = mixSongs
                        .filter { selectedIds.contains(it.id) }
                        .map { it.id }
                    if (orderedSelection.isNotEmpty()) {
                        playlistViewModel.createPlaylist(
                            name = name,
                            songIds = orderedSelection
                        )
                        playerViewModel.sendToast(playlistCreatedToast)
                    }
                    showSaveAsPlaylistSheet = false
                }
            )
        }

        if (showEditMixForm && isUserMix && userMix != null) {
            EditMixForm(
                visible = true,
                initial = MixFormInitial(
                    name = userMix.name,
                    rule = MixRule.fromPlaylistSource(userMix.source) ?: MixRule.TOP_PLAYED,
                    songCount = userMix.mixSongCount ?: 60,
                    artistIds = userMix.mixArtistIds ?: emptyList(),
                    genres = userMix.mixGenres ?: emptyList(),
                    coverImageUri = userMix.coverImageUri,
                    coverColorArgb = userMix.coverColorArgb,
                    coverIconName = userMix.coverIconName
                ),
                onDismiss = { showEditMixForm = false },
                onSave = { name, rule, count, artistIds, genres, imageUri, color, icon, scale, panX, panY ->
                    playlistViewModel.updateMix(
                        playlistId = mixId,
                        name = name,
                        rule = rule,
                        songCount = count,
                        artistIds = artistIds,
                        genres = genres,
                        coverImageUri = imageUri,
                        coverColor = color,
                        coverIcon = icon,
                        cropScale = scale,
                        cropPanX = panX,
                        cropPanY = panY
                    )
                    showEditMixForm = false
                },
                playerViewModel = playerViewModel
            )
        }

        if (showEditMixForm && isForYouMix) {
            EditMixForm(
                visible = true,
                isForYouMix = true,
                initial = MixFormInitial(songCount = forYouMixSongCount),
                onDismiss = { showEditMixForm = false },
                onSave = { _, _, count, _, _, _, _, _, _, _, _ ->
                    playerViewModel.setForYouMixSongCount(count)
                    showEditMixForm = false
                },
                playerViewModel = playerViewModel
            )
        }

        if (showDeleteConfirmation && isUserMix && userMix != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text(stringResource(R.string.mix_delete_confirm_title)) },
                text = { Text(stringResource(R.string.mix_delete_confirm_body)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            playlistViewModel.deletePlaylist(mixId)
                            navController.popBackStack()
                            showDeleteConfirmation = false
                        }
                    ) {
                        Text(stringResource(R.string.delete_action), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            )
        }

        SongMultiSelectionHost(
            playerViewModel = playerViewModel,
            multiSelectionState = multiSelectionState,
            showSheet = showMultiSelectionSheet,
            onDismiss = { showMultiSelectionSheet = false },
            onRemoveFromList = {
                val ids = selectedSongs.map { it.id }
                when {
                    isForYouMix -> playerViewModel.removeSongsFromYourMix(ids)
                    isUserMix -> playlistViewModel.removeSongsFromMix(mixId, ids)
                    else -> playerViewModel.removeSongsFromDailyMix(ids)
                }
            },
            removeFromListLabel = stringResource(R.string.mix_remove_from_mix)
        )
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ExpressiveMixDetailHeader(
    title: String,
    songs: ImmutableList<Song>,
    scrollState: LazyListState,
    coverImageUri: String? = null,
    coverColorArgb: Int? = null,
    coverIconName: String? = null,
    shuffleLabel: String,
    onShuffleClick: () -> Unit
) = traceSection("ExpressiveMixDetailHeader.Composition") {
    val albumArts = remember(songs) { songs.map { it.albumArtUriString }.distinct().take(3) }
    val totalDuration = remember(songs) { songs.sumOf { it.duration } }

    val parallaxOffset by remember { derivedStateOf { if (scrollState.firstVisibleItemIndex == 0) scrollState.firstVisibleItemScrollOffset * 0.5f else 0f } }

    val headerAlpha by remember {
        derivedStateOf {
            (1f - (scrollState.firstVisibleItemScrollOffset / 600f)).coerceIn(0f, 1f)
        }
    }

    val titleStyle = rememberMixDetailTitleStyle()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .graphicsLayer {
                translationY = parallaxOffset
                alpha = headerAlpha
            }
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            if (coverImageUri == null && coverColorArgb == null) {
            MixThumbnailCluster(
                artUrls = albumArts,
                centerSize = 220.dp,
                overlap = 80.dp,
                thirdShapeCornerRadius = 30.dp,
                centerThird = true
            )
            } else {
                when {
                    coverImageUri != null -> {
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(ShapeCache.expressiveAvatar)
                        ) {
                            SmartImage(
                                model = coverImageUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    coverColorArgb != null -> {
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(ShapeCache.expressiveAvatar)
                                .background(Color(coverColorArgb)),
                            contentAlignment = Alignment.Center
                        ) {
                            val icon = getIconByName(coverIconName) ?: Icons.Rounded.MusicNote
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = getThemeContentColor(coverColorArgb, MaterialTheme.colorScheme)
                            )
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surface
                        ),
                        startY = 0f,
                        endY = 900f
                    )
                )
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.Absolute.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(start = 6.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = Int.MAX_VALUE
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    modifier = Modifier.padding(start = 3.dp),
                    text = pluralStringResource(
                        R.plurals.presentation_batch_b_songs_dot_duration,
                        songs.size,
                        songs.size,
                        formatDuration(totalDuration)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
            LargeExtendedFloatingActionButton(
                onClick = onShuffleClick,
                modifier = Modifier.padding(start = 12.dp),
                // Cookie shape (Cookie9Sided), the same shape the mix cover
                // tiles use. The old RoundedStarShape clipped this wide
                // icon+text button (dbe137f); a RoundedPolygonShape scales
                // to the full bounds, so icon + label render fully.
                shape = ShapeCache.expressiveAvatar
            ) {
                Icon(Icons.Rounded.Shuffle, contentDescription = shuffleLabel)
                Spacer(Modifier.width(8.dp))
                Text(shuffleLabel)
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun rememberMixDetailTitleStyle(): TextStyle {
    return remember {
        TextStyle(
            fontFamily = FontFamily(
                Font(
                    resId = R.font.gflex_variable,
                    variationSettings = FontVariation.Settings(
                        FontVariation.weight(436),
                        FontVariation.width(102f),
                        FontVariation.Setting("ROND", 100f),
                        FontVariation.Setting("XTRA", 520f),
                        FontVariation.Setting("YOPQ", 90f),
                        FontVariation.Setting("YTLC", 505f)
                    )
                )
            ),
            fontWeight = FontWeight(760),
            fontSize = 44.sp,
        )
    }
}

@Composable
private fun MixOptionsItem(
    icon: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
