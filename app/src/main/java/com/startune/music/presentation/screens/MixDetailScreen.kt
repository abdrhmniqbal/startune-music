package com.startune.music.presentation.screens

import com.startune.music.presentation.navigation.navigateSafely

import androidx.compose.foundation.background
import com.startune.music.presentation.components.subcomps.SelectionActionRow
import com.startune.music.presentation.components.ExpressiveEmptyState
import com.startune.music.presentation.components.subcomps.SelectionCountPill
import androidx.compose.ui.zIndex
import com.startune.music.presentation.components.rememberModalSheetState
import com.startune.music.presentation.components.SongMultiSelectionHost
import com.startune.music.data.model.MixRule
import com.startune.music.data.model.fromPlaylistSource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.foundation.layout.width
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startune.music.ui.theme.LocalStartuneDarkTheme
import com.startune.music.ui.theme.RoundedSans
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import com.startune.music.utils.traceSection
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.startune.music.R
import com.startune.music.data.model.Song
import com.startune.music.presentation.components.CollapsibleCommonTopBar
import com.startune.music.presentation.components.MediaDetailHero
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
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
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
    var showAddAllToPlaylistSheet by remember { mutableStateOf(false) }
    val toastAddedToQueue = stringResource(R.string.toast_added_to_queue)
    val toastPlayingNext = stringResource(R.string.toast_playing_next)
    val favoriteSongIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val selectedSongForInfo by playerViewModel.selectedSongForInfo.collectAsStateWithLifecycle()

    val lazyListState = rememberLazyListState()

    // Collapsing header, like the reference detail screens: the header
    // starts at 300.dp and shrinks to the bar as the list scrolls.
    val density = LocalDensity.current
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 300.dp
    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }
    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
    val collapseFraction by remember(minTopBarHeightPx, maxTopBarHeightPx) {
        derivedStateOf {
            1f - ((topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(0f, 1f)
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0

                if (!isScrollingDown && (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)) {
                    return Offset.Zero
                }

                val previousHeight = topBarHeight.value
                val newHeight = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - previousHeight

                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch { topBarHeight.snapTo(newHeight) }
                }

                val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val currentHeight = topBarHeight.value
                if (currentHeight > minTopBarHeightPx && currentHeight < maxTopBarHeightPx) {
                    val targetHeight = if (available.y > 500f) {
                        maxTopBarHeightPx
                    } else if (available.y < -500f) {
                        minTopBarHeightPx
                    } else {
                        if (currentHeight > (minTopBarHeightPx + maxTopBarHeightPx) / 2) maxTopBarHeightPx else minTopBarHeightPx
                    }

                    coroutineScope.launch {
                        topBarHeight.animateTo(
                            targetValue = targetHeight,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    }
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    // The meta line the hero and the bar subtitle share, and the cover
    // cluster's arts: the mix's distinct song arts.
    val totalDuration = remember(mixSongs) { mixSongs.sumOf { it.duration } }
    val mixMetaLine = pluralStringResource(
        R.plurals.presentation_batch_b_songs_dot_duration,
        mixSongs.size,
        mixSongs.size,
        formatDuration(totalDuration)
    )
    val albumArts = remember(mixSongs) { mixSongs.map { it.albumArtUriString }.distinct().take(3) }

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
            if (showAddAllToPlaylistSheet && mixSongs.isNotEmpty()) {
                val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()

                PlaylistBottomSheet(
                    playlistUiState = playlistUiState,
                    songs = mixSongs,
                    onDismiss = { showAddAllToPlaylistSheet = false },
                    bottomBarHeight = bottomBarHeightDp,
                    playerViewModel = playerViewModel,
                )
            }
        }


        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .background(backgroundBrush)
        ) {
            val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }
            val solidAlpha = (collapseFraction * 2f).coerceIn(0f, 1f)
            val expandedContentAlpha = 1f - solidAlpha

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
                    modifier = Modifier
                        .fillMaxSize()
                        .offset {
                            val extraHeight = (topBarHeight.value - minTopBarHeightPx).roundToInt()
                            IntOffset(0, extraHeight)
                        },
                    contentPadding = PaddingValues(
                        top = minTopBarHeight + 8.dp,
                        bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "mix_hero") {
                        MediaDetailHero(
                            title = mixTitle,
                            metaLine = mixMetaLine,
                            onShuffle = {
                                if (mixSongs.isNotEmpty()) {
                                    playerViewModel.playSongsShuffled(
                                        songsToPlay = mixSongs,
                                        queueName = mixTitle,
                                        startAtZero = true
                                    )
                                }
                            },
                            onPlay = {
                                if (mixSongs.isNotEmpty()) {
                                    playerViewModel.playSongs(
                                        songsToPlay = mixSongs,
                                        startSong = mixSongs.first(),
                                        queueName = mixTitle
                                    )
                                }
                            },
                            menuActions = persistentListOf(),
                            enabled = mixSongs.isNotEmpty()
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

                    if (mixSongs.isEmpty()) {
                        item(key = "mix_empty") {
                            ExpressiveEmptyState(
                                iconRes = R.drawable.rounded_instant_mix_24,
                                titleRes = R.string.mix_detail_empty_title,
                                subtitleRes = R.string.mix_detail_empty_subtitle,
                                modifier = Modifier.padding(top = 48.dp)
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(currentTopBarHeightDp)
                    .clipToBounds()
            ) {
                // The mix gradient stays as the header background.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundBrush)
                )

                // The mix cover, centered in the header: the stacked cluster,
                // or the custom cover image / color + icon when the mix has
                // one, fading out as the bar collapses.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .graphicsLayer { alpha = expandedContentAlpha }
                ) {
                    if (userMix?.coverImageUri == null && userMix?.coverColorArgb == null) {
                        MixThumbnailCluster(
                            artUrls = albumArts,
                            centerSize = 180.dp,
                            overlap = 65.dp,
                            thirdShapeCornerRadius = 24.dp,
                            centerThird = true
                        )
                    } else if (userMix?.coverImageUri != null) {
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(ShapeCache.expressiveAvatar)
                        ) {
                            SmartImage(
                                model = userMix.coverImageUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        val coverColor = userMix?.coverColorArgb
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .clip(ShapeCache.expressiveAvatar)
                                .background(Color(coverColor ?: 0)),
                            contentAlignment = Alignment.Center
                        ) {
                            val icon = getIconByName(userMix?.coverIconName) ?: Icons.Rounded.MusicNote
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = getThemeContentColor(coverColor ?: 0, MaterialTheme.colorScheme)
                            )
                        }
                    }
                }

                // The bar's own title only fades in as the header collapses,
                // like the reference: expanded, the hero below carries the
                // title; collapsed, the bar carries it.
                CollapsibleCommonTopBar(
                    title = mixTitle,
                    subtitle = mixMetaLine,
                    collapseFraction = collapseFraction,
                    headerHeight = currentTopBarHeightDp,
                    onBackClick = { navController.popBackStack() },
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = solidAlpha),
                    collapsedTitleStartPadding = 68.dp,
                    expandedTitleStartPadding = 24.dp,
                    collapsedTitleEndPadding = 24.dp,
                    expandedTitleEndPadding = 24.dp,
                    containerHeightRange = 112.dp to 56.dp,
                    titleStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = RoundedSans,
                        fontWeight = FontWeight.SemiBold,
                        textGeometricTransform = TextGeometricTransform(scaleX = 1.08f)
                    ),
                    titleScaleRange = 1f to 1f,
                    titleFontSizeRange = 30.sp to 18.sp,
                    maxLines = 1,
                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = solidAlpha),
                    subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = solidAlpha),
                    fadeSubtitleOnCollapse = false,
                    syncStatusBarWithContainer = false,
                    actions = {
                        if (isForYouMix || isUserMix) {
                            Box(
                                modifier = Modifier.padding(end = 12.dp, top = 4.dp)
                            ) {
                                FilledIconButton(
                                    onClick = {
                                        if (isRemoveModeEnabled) {
                                            isRemoveModeEnabled = false
                                        } else {
                                            showMixOptionsSheet = true
                                        }
                                    },
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                    )
                                ) {
                                    if (isRemoveModeEnabled) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = stringResource(R.string.confirm)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.MoreVert,
                                            contentDescription = stringResource(R.string.presentation_batch_b_more_options)
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.presentation_batch_b_mix_options_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = mixTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MixOptionsItem(
                        icon = rememberVectorPainter(Icons.Rounded.PlayArrow),
                        label = stringResource(R.string.play_playback),
                        onClick = {
                            showMixOptionsSheet = false
                            if (mixSongs.isNotEmpty()) {
                                playerViewModel.playSongs(mixSongs, mixSongs.first(), queueName = mixTitle)
                            }
                        }
                    )
                    MixOptionsItem(
                        icon = rememberVectorPainter(Icons.Rounded.Shuffle),
                        label = stringResource(R.string.shortcut_shuffle_short),
                        onClick = {
                            showMixOptionsSheet = false
                            if (mixSongs.isNotEmpty()) {
                                playerViewModel.playSongsShuffled(mixSongs, mixTitle, startAtZero = true)
                            }
                        }
                    )
                    MixOptionsItem(
                        icon = rememberVectorPainter(Icons.Rounded.SkipNext),
                        label = stringResource(R.string.detail_hero_play_next),
                        onClick = {
                            showMixOptionsSheet = false
                            if (mixSongs.isNotEmpty()) {
                                playerViewModel.addSelectedAsNext(mixSongs)
                                playerViewModel.sendToast(toastPlayingNext)
                            }
                        }
                    )
                    MixOptionsItem(
                        icon = rememberVectorPainter(Icons.AutoMirrored.Rounded.QueueMusic),
                        label = stringResource(R.string.detail_hero_add_to_queue),
                        onClick = {
                            showMixOptionsSheet = false
                            if (mixSongs.isNotEmpty()) {
                                playerViewModel.addSelectedToQueue(mixSongs)
                                playerViewModel.sendToast(toastAddedToQueue)
                            }
                        }
                    )
                    MixOptionsItem(
                        icon = rememberVectorPainter(Icons.AutoMirrored.Rounded.PlaylistAdd),
                        label = stringResource(R.string.detail_hero_add_to_playlist),
                        onClick = {
                            showMixOptionsSheet = false
                            showAddAllToPlaylistSheet = true
                        }
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
