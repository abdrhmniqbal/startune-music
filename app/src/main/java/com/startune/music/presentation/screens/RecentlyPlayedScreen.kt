package com.startune.music.presentation.screens

import com.startune.music.presentation.navigation.navigateSafely

import com.startune.music.utils.traceSection
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.startune.music.R
import com.startune.music.data.model.Song
import com.startune.music.data.model.SortOption
import com.startune.music.data.stats.StatsTimeRange
import com.startune.music.presentation.components.LibrarySortBottomSheet
import com.startune.music.presentation.components.MiniPlayerHeight
import com.startune.music.presentation.components.PlaylistBottomSheet
import com.startune.music.presentation.components.RecentlyPlayedRangeSelector
import com.startune.music.presentation.components.SongInfoBottomSheet
import com.startune.music.presentation.components.SongMultiSelectionHost
import com.startune.music.presentation.components.subcomps.SelectionActionRow
import com.startune.music.presentation.components.subcomps.SelectionCountPill
import androidx.compose.ui.zIndex
import com.startune.music.presentation.components.subcomps.EnhancedSongListItem
import com.startune.music.presentation.navigation.Screen
import com.startune.music.presentation.model.RecentlyPlayedSongUiModel
import com.startune.music.presentation.model.collectRecentlyPlayedSongIds
import com.startune.music.presentation.model.mapRecentlyPlayedSongs
import com.startune.music.presentation.viewmodel.PlaylistViewModel
import com.startune.music.presentation.viewmodel.PlayerViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.LargeExtendedFloatingActionButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.util.lerp
import com.startune.music.presentation.components.CollapsibleCommonTopBar
import com.startune.music.utils.shapes.RoundedStarShape
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RecentlyPlayedScreen(
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    navController: NavController
) = traceSection("RecentlyPlayedScreen.Composition") {
    val context = LocalContext.current
    val queueRecentlyPlayed = stringResource(R.string.presentation_batch_b_queue_recently_played)
    val playbackHistory by playerViewModel.playbackHistory.collectAsStateWithLifecycle()
    val currentSongId by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.currentSong?.id }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = null)
    val isPlaying by remember(playerViewModel.stablePlayerState) {
        playerViewModel.stablePlayerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    val favoriteSongIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val selectedSongForInfo by playerViewModel.selectedSongForInfo.collectAsStateWithLifecycle()
    val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()

    var selectedRange by rememberSaveable { mutableStateOf(StatsTimeRange.WEEK) }
    var sortOption by remember { mutableStateOf<SortOption>(SortOption.SongDefaultOrder) }
    var showSortSheet by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 62.dp + statusBarHeight
    val maxTopBarHeight = 176.dp

    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
    var collapseFraction by remember { mutableStateOf(0f) }

    LaunchedEffect(topBarHeight.value) {
        collapseFraction = 1f - ((topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(0f, 1f)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val scrollingDown = delta < 0

                if (!scrollingDown && (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)) {
                    return Offset.Zero
                }

                val previousHeight = topBarHeight.value
                val newHeight = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - previousHeight

                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch {
                        topBarHeight.snapTo(newHeight)
                    }
                }

                val canConsume = !(scrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsume) Offset(0f, consumed) else Offset.Zero
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
            val target = if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx

            if (topBarHeight.value != target) {
                coroutineScope.launch {
                    topBarHeight.animateTo(target, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }
    val solidAlpha = (collapseFraction * 2f).coerceIn(0f, 1f)
    val expandedContentAlpha = 1f - solidAlpha
    val titleVerticalBias = lerp(1f, -1f, collapseFraction)
    val shuffleAlignment = BiasAlignment(horizontalBias = 1f, verticalBias = titleVerticalBias)
    var showSongInfoBottomSheet by remember { mutableStateOf(false) }
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }

    val multiSelectionState = playerViewModel.multiSelectionStateHolder
    val selectedSongs by multiSelectionState.selectedSongs.collectAsStateWithLifecycle()
    val isSelectionMode by multiSelectionState.isSelectionMode.collectAsStateWithLifecycle()
    val selectedSongIds by multiSelectionState.selectedSongIds.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var showMultiSelectionSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) { multiSelectionState.clearSelection() }
    DisposableEffect(Unit) { onDispose { multiSelectionState.clearSelection() } }
    val bottomBarHeightDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val recentSongIds = remember(playbackHistory, selectedRange) {
        collectRecentlyPlayedSongIds(
            playbackHistory = playbackHistory,
            range = selectedRange,
            maxItems = Int.MAX_VALUE
        )
    }
    val recentSongsInitialValue: List<Song>? = remember(recentSongIds) {
        if (recentSongIds.isEmpty()) emptyList<Song>() else null
    }
    val recentlyPlayedSourceSongs by remember(recentSongIds, playerViewModel) {
        playerViewModel.observeSongs(recentSongIds)
            .map<List<Song>, List<Song>?> { it }
    }.collectAsStateWithLifecycle(initialValue = recentSongsInitialValue)

    val recentlyPlayedSongs = remember(playbackHistory, recentlyPlayedSourceSongs, selectedRange) {
        val sourceSongs = recentlyPlayedSourceSongs ?: return@remember persistentListOf()
        mapRecentlyPlayedSongs(
            playbackHistory = playbackHistory,
            songs = sourceSongs,
            range = selectedRange,
            maxItems = Int.MAX_VALUE
        ).toImmutableList()
    }
    val groupedSongs = remember(recentlyPlayedSongs, selectedRange, context) {
        groupRecentlyPlayedSongs(
            context = context,
            songs = recentlyPlayedSongs,
            range = selectedRange
        )
    }
    val queueSongs = remember(recentlyPlayedSongs) {
        recentlyPlayedSongs.map { it.song }.toImmutableList()
    }
    val isDefaultOrder = sortOption == SortOption.SongDefaultOrder
    val displaySongs = remember(queueSongs, sortOption) {
        when (sortOption) {
            SortOption.SongDefaultOrder -> queueSongs
            else -> sortArtistSongs(queueSongs, sortOption).toImmutableList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(nestedScrollConnection)
    ) {
        if (recentlyPlayedSourceSongs == null) {
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
                contentPadding = PaddingValues(
                    top = currentTopBarHeightDp + 8.dp,
                    bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                item(key = "recently_played_range_selector") {
                    AnimatedContent(
                        targetState = isSelectionMode,
                        label = "ActionRowModeSwitch",
                        transitionSpec = {
                            (slideInHorizontally { -it } + fadeIn()) togetherWith
                                (slideOutHorizontally { it } + fadeOut())
                        }
                    ) { inSelectionMode ->
                        if (inSelectionMode) {
                            SelectionActionRow(
                                selectedCount = selectedSongs.size,
                                onSelectAll = { multiSelectionState.selectAll(displaySongs) },
                                onDeselect = { multiSelectionState.clearSelection() },
                                onOptionsClick = { showMultiSelectionSheet = true },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        } else {
                            RecentlyPlayedRangeSelector(
                                selected = selectedRange,
                                onRangeSelected = { selectedRange = it },
                                modifier = Modifier.padding(horizontal = 0.dp)
                            )
                        }
                    }
                }

                if (recentlyPlayedSongs.isNotEmpty()) {
                }

                if (recentlyPlayedSongs.isEmpty()) {
                    item(key = "recently_played_empty") {
                        RecentlyPlayedEmptyState(
                            range = selectedRange,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else if (isDefaultOrder) {
                    groupedSongs.forEachIndexed { groupIndex, group ->
                        item(key = "recently_played_time_${groupIndex}_${group.key}") {
                            RecentlyPlayedTimestampDivider(
                                label = group.label,
                                isHourBucket = group.isHourBucket,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        items(group.songs, key = { songUi -> songUi.song.id }, contentType = { "recently_played_song" }) { item ->
                            EnhancedSongListItem(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                song = item.song,
                                isCurrentSong = currentSongId == item.song.id,
                                isPlaying = currentSongId == item.song.id && isPlaying,
                                isSelected = selectedSongIds.contains(item.song.id),
                                selectionIndex = if (isSelectionMode) multiSelectionState.getSelectionIndex(item.song.id) else null,
                                isSelectionMode = isSelectionMode,
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    multiSelectionState.toggleSelection(item.song)
                                },
                                onClick = {
                                    if (isSelectionMode) {
                                        multiSelectionState.toggleSelection(item.song)
                                    } else {
                                        playerViewModel.playSongs(
                                            songsToPlay = displaySongs,
                                            startSong = item.song,
                                            queueName = queueRecentlyPlayed
                                        )
                                    }
                                },
                                onMoreOptionsClick = { song ->
                                    playerViewModel.selectSongForInfo(song)
                                    showSongInfoBottomSheet = true
                                }
                            )
                        }
                    }
                } else {
                    itemsIndexed(displaySongs, key = { _, song -> song.id }, contentType = { _, _ -> "recently_played_song" }) { _, song ->
                        EnhancedSongListItem(
                            modifier = Modifier.padding(horizontal = 16.dp),
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
                                    playerViewModel.playSongs(
                                        songsToPlay = displaySongs,
                                        startSong = song,
                                        queueName = queueRecentlyPlayed
                                    )
                                }
                            },
                            onMoreOptionsClick = { selected ->
                                playerViewModel.selectSongForInfo(selected)
                                showSongInfoBottomSheet = true
                            }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(currentTopBarHeightDp)
                .clipToBounds()
        ) {
            CollapsibleCommonTopBar(
                title = stringResource(R.string.presentation_batch_b_recently_played_title),
                collapseFraction = collapseFraction,
                headerHeight = currentTopBarHeightDp,
                onBackClick = { navController.popBackStack() },
                expandedTitleEndPadding = 136.dp,
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showSortSheet = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = stringResource(R.string.presentation_batch_d_sort_by)
                            )
                        }
                    }
                }
            )
            if (displaySongs.isNotEmpty()) {
                LargeExtendedFloatingActionButton(
                    onClick = {
                        playerViewModel.playSongsShuffled(
                            songsToPlay = displaySongs,
                            queueName = queueRecentlyPlayed,
                            startAtZero = true
                        )
                    },
                    shape = RoundedStarShape(sides = 8, curve = 0.05, rotation = 0f),
                    modifier = Modifier
                        .align(shuffleAlignment)
                        .statusBarsPadding()
                        .padding(end = 16.dp)
                        .graphicsLayer {
                            alpha = expandedContentAlpha
                            scaleX = expandedContentAlpha
                            scaleY = expandedContentAlpha
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shuffle,
                        contentDescription = stringResource(R.string.shortcut_shuffle_short)
                    )
                }
            }
        }

        if (showSongInfoBottomSheet && selectedSongForInfo != null) {
            val song = selectedSongForInfo!!
            SongInfoBottomSheet(
                song = song,
                isFavorite = favoriteSongIds.contains(song.id),
                onToggleFavorite = {
                    playerViewModel.toggleFavoriteSpecificSong(song)
                },
                onDismiss = {
                    showSongInfoBottomSheet = false
                    showPlaylistBottomSheet = false
                },
                onPlaySong = {
                    if (displaySongs.isNotEmpty()) {
                        playerViewModel.playSongs(displaySongs, song, queueRecentlyPlayed)
                    }
                    showSongInfoBottomSheet = false
                },
                onAddToQueue = {
                    playerViewModel.addSongToQueue(song)
                    showSongInfoBottomSheet = false
                },
                onAddNextToQueue = {
                    playerViewModel.addSongNextToQueue(song)
                    showSongInfoBottomSheet = false
                },
                onAddToPlayList = {
                    showPlaylistBottomSheet = true
                },
                onDeleteFromDevice = playerViewModel::deleteFromDevice,
                onNavigateToAlbum = {
                    navController.navigateSafely(Screen.AlbumDetail.createRoute(song.albumId))
                    showSongInfoBottomSheet = false
                },
                onNavigateToArtist = {
                    navController.navigateSafely(Screen.ArtistDetail.createRoute(song.artistId))
                    showSongInfoBottomSheet = false
                },
                onNavigateToArtistById = { artistId ->
                    navController.navigateSafely(Screen.ArtistDetail.createRoute(artistId))
                    showSongInfoBottomSheet = false
                },
                onNavigateToGenre = {
                    song.genre?.let {
                        navController.navigateSafely(Screen.GenreDetail.createRoute(java.net.URLEncoder.encode(it, "UTF-8")))
                    }
                    showSongInfoBottomSheet = false
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
                removeFromListTrigger = {}
            )

            if (showPlaylistBottomSheet) {
                PlaylistBottomSheet(
                    playlistUiState = playlistUiState,
                    songs = persistentListOf(song),
                    onDismiss = { showPlaylistBottomSheet = false },
                    bottomBarHeight = bottomBarHeightDp,
                    playerViewModel = playerViewModel,
                )
            }
        }

        SongMultiSelectionHost(
            playerViewModel = playerViewModel,
            multiSelectionState = multiSelectionState,
            showSheet = showMultiSelectionSheet,
            onDismiss = { showMultiSelectionSheet = false }
        )

        SelectionCountPill(
            selectedCount = selectedSongs.size,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = currentTopBarHeightDp)
                .zIndex(1f)
        )

        if (showSortSheet) {
            LibrarySortBottomSheet(
                title = stringResource(R.string.presentation_batch_d_sort_by),
                options = SortOption.SONGS.filter { it != SortOption.SongPlayCount && it != SortOption.SongPlayCountAsc }.toImmutableList(),
                selectedOption = sortOption,
                onDismiss = { showSortSheet = false },
                onOptionSelected = {
                    sortOption = it
                    showSortSheet = false
                }
            )
        }
    }
}

@Composable
private fun RecentlyPlayedTimestampDivider(
    label: String,
    isHourBucket: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val railColor = if (isHourBucket) colors.primary else colors.secondary
    val chipContainer = if (isHourBucket) {
        colors.primaryContainer.copy(alpha = 0.78f)
    } else {
        colors.secondaryContainer.copy(alpha = 0.78f)
    }
    val chipContent = if (isHourBucket) colors.onPrimaryContainer else colors.onSecondaryContainer

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            railColor.copy(alpha = 0f),
                            railColor.copy(alpha = 0.50f)
                        )
                    )
                )
        )
        Surface(
            color = chipContainer,
            shape = AbsoluteSmoothCornerShape(
                cornerRadiusTL = 22.dp,
                smoothnessAsPercentTR = 60,
                cornerRadiusTR = 22.dp,
                smoothnessAsPercentBR = 60,
                cornerRadiusBL = 22.dp,
                smoothnessAsPercentBL = 60,
                cornerRadiusBR = 22.dp,
                smoothnessAsPercentTL = 60
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(railColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = chipContent
                )
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            railColor.copy(alpha = 0.50f),
                            railColor.copy(alpha = 0f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun RecentlyPlayedEmptyState(
    range: StatsTimeRange,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AbsoluteSmoothCornerShape(
            cornerRadiusTL = 26.dp,
            smoothnessAsPercentTR = 60,
            cornerRadiusTR = 26.dp,
            smoothnessAsPercentBR = 60,
            cornerRadiusBL = 26.dp,
            smoothnessAsPercentBL = 60,
            cornerRadiusBR = 26.dp,
            smoothnessAsPercentTL = 60
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.presentation_batch_b_recent_empty_title,
                    range.displayName.lowercase()
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.presentation_batch_b_recent_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class TimestampGroup(
    val key: String,
    val label: String,
    val isHourBucket: Boolean,
    val songs: List<RecentlyPlayedSongUiModel>
)

private fun groupRecentlyPlayedSongs(
    context: android.content.Context,
    songs: List<RecentlyPlayedSongUiModel>,
    range: StatsTimeRange
): List<TimestampGroup> {
    if (songs.isEmpty()) return emptyList()
    val zoneId = ZoneId.systemDefault()
    val sorted = songs.sortedByDescending { it.lastPlayedTimestamp }

    val groups = mutableListOf<TimestampGroup>()
    var currentBucketKey: String? = null
    var currentLabel: String? = null
    var currentIsHourBucket = false
    var bucket = mutableListOf<RecentlyPlayedSongUiModel>()

    sorted.forEach { item ->
        val timeBucket = resolveTimestampBucket(
            context = context,
            timestamp = item.lastPlayedTimestamp,
            range = range,
            zoneId = zoneId
        )
        if (currentBucketKey == null || currentBucketKey == timeBucket.key) {
            currentBucketKey = timeBucket.key
            currentLabel = timeBucket.label
            currentIsHourBucket = timeBucket.isHourBucket
            bucket += item
        } else {
            groups += TimestampGroup(
                key = currentBucketKey,
                label = currentLabel!!,
                isHourBucket = currentIsHourBucket,
                songs = bucket
            )
            currentBucketKey = timeBucket.key
            currentLabel = timeBucket.label
            currentIsHourBucket = timeBucket.isHourBucket
            bucket = mutableListOf(item)
        }
    }

    if (bucket.isNotEmpty() && currentLabel != null && currentBucketKey != null) {
        groups += TimestampGroup(
            key = currentBucketKey,
            label = currentLabel,
            isHourBucket = currentIsHourBucket,
            songs = bucket
        )
    }

    return groups
}

private data class TimestampBucket(
    val key: String,
    val label: String,
    val isHourBucket: Boolean
)

private fun resolveTimestampBucket(
    context: android.content.Context,
    timestamp: Long,
    range: StatsTimeRange,
    zoneId: ZoneId
): TimestampBucket {
    val safeTimestamp = timestamp.coerceAtLeast(0L)
    val safeNow = System.currentTimeMillis().coerceAtLeast(0L)
    val zonedDateTime = Instant.ofEpochMilli(safeTimestamp).atZone(zoneId)
    val date = zonedDateTime.toLocalDate()
    val nowDate = Instant.ofEpochMilli(safeNow).atZone(zoneId).toLocalDate()

    if (range == StatsTimeRange.DAY) {
        val hourStart = zonedDateTime
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        return TimestampBucket(
            key = hourStart.toInstant().toEpochMilli().toString(),
            label = hourStart.format(DateTimeFormatter.ofPattern("h a", Locale.getDefault())),
            isHourBucket = true
        )
    }

    return when {
        date == nowDate -> {
            TimestampBucket(
                key = date.toString(),
                label = context.getString(R.string.presentation_batch_b_date_today),
                isHourBucket = false
            )
        }
        date == nowDate.minusDays(1) -> {
            TimestampBucket(
                key = date.toString(),
                label = context.getString(R.string.presentation_batch_b_date_yesterday),
                isHourBucket = false
            )
        }
        range == StatsTimeRange.YEAR || range == StatsTimeRange.ALL -> {
            TimestampBucket(
                key = date.toString(),
                label = date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())),
                isHourBucket = false
            )
        }
        else -> {
            TimestampBucket(
                key = date.toString(),
                label = date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())),
                isHourBucket = false
            )
        }
    }
}
