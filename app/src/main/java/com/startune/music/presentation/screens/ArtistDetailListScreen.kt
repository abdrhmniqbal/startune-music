package com.startune.music.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.startune.music.R
import com.startune.music.presentation.viewmodel.ArtistAlbumSection
import com.startune.music.data.model.SortOption
import com.startune.music.presentation.components.CollapsibleCommonTopBar
import com.startune.music.presentation.components.ExpressiveScrollBar
import com.startune.music.presentation.components.LibrarySortBottomSheet
import com.startune.music.presentation.components.MiniPlayerHeight
import com.startune.music.presentation.components.PlaylistBottomSheet
import com.startune.music.presentation.components.subcomps.SelectionActionRow
import com.startune.music.presentation.components.subcomps.SelectionCountPill
import com.startune.music.presentation.components.SongInfoBottomSheet
import com.startune.music.presentation.components.SongMultiSelectionHost
import com.startune.music.presentation.components.resolveNavBarOccupiedHeight
import com.startune.music.presentation.navigation.Screen
import com.startune.music.presentation.navigation.navigateSafely
import com.startune.music.presentation.navigation.navigateSafelyReplacing
import com.startune.music.presentation.viewmodel.ArtistDetailViewModel
import com.startune.music.presentation.viewmodel.PlayerViewModel
import com.startune.music.presentation.viewmodel.PlaylistViewModel
import com.startune.music.ui.theme.LocalStartuneDarkTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArtistDetailListScreen(
    artistId: String,
    view: ArtistDetailView,
    navController: NavController,
    playerViewModel: PlayerViewModel,
    viewModel: ArtistDetailViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val useArtworkColorForDetails by playerViewModel.useArtworkColorForDetails.collectAsStateWithLifecycle()
    val favoriteIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val navBarCompactMode by playerViewModel.navBarCompactMode.collectAsStateWithLifecycle()
    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBarHeightDp = resolveNavBarOccupiedHeight(systemNavBarInset, navBarCompactMode)

    val multiSelectionState = playerViewModel.multiSelectionStateHolder
    val selectedSongs by multiSelectionState.selectedSongs.collectAsStateWithLifecycle()
    val isSelectionMode by multiSelectionState.isSelectionMode.collectAsStateWithLifecycle()
    val selectedSongIds by multiSelectionState.selectedSongIds.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var showMultiSelectionSheet by remember { mutableStateOf(false) }
    var showSongInfoBottomSheet by remember { mutableStateOf(false) }
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    val selectedSongForInfo by playerViewModel.selectedSongForInfo.collectAsStateWithLifecycle()

    var selectedAlbumSections by remember { mutableStateOf<ImmutableList<ArtistAlbumSection>>(persistentListOf()) }
    val selectedAlbumSectionIds = remember(selectedAlbumSections) { selectedAlbumSections.map { it.albumId }.toSet() }
    val isAlbumSelectionMode = selectedAlbumSections.isNotEmpty()

    var trackSortOption by remember { mutableStateOf<SortOption>(SortOption.SongTitleAZ) }
    var showSortSheet by remember { mutableStateOf(false) }
    var albumSortOption by remember { mutableStateOf<SortOption>(SortOption.AlbumReleaseYear) }
    var showAlbumSortSheet by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { multiSelectionState.clearSelection() } }
    BackHandler(enabled = isSelectionMode || isAlbumSelectionMode) {
        multiSelectionState.clearSelection()
        selectedAlbumSections = persistentListOf()
    }
    LaunchedEffect(Unit) { playerViewModel.collapsePlayerSheet() }

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
    val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }

    val lazyListState = rememberLazyListState()
    val albumsGridState = rememberLazyGridState()
    val isGridView = view != ArtistDetailView.Tracks
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val scrollingDown = delta < 0
                val firstVisibleItemIndex = if (isGridView) albumsGridState.firstVisibleItemIndex else lazyListState.firstVisibleItemIndex
                val firstVisibleItemScrollOffset = if (isGridView) albumsGridState.firstVisibleItemScrollOffset else lazyListState.firstVisibleItemScrollOffset
                if (!scrollingDown && (firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > 0)) {
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
    val isScrollInProgress = if (isGridView) albumsGridState.isScrollInProgress else lazyListState.isScrollInProgress
    LaunchedEffect(isScrollInProgress) {
        if (!isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand = if (isGridView) {
                albumsGridState.firstVisibleItemIndex == 0 && albumsGridState.firstVisibleItemScrollOffset == 0
            } else {
                lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
            }
            val target = if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx
            if (topBarHeight.value != target) {
                coroutineScope.launch {
                    topBarHeight.animateTo(target, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    val showScrollBar by remember {
        derivedStateOf {
            collapseFraction > 0.95f && (
                if (isGridView) albumsGridState.canScrollForward || albumsGridState.canScrollBackward
                else lazyListState.canScrollForward || lazyListState.canScrollBackward
            )
        }
    }

    val isDarkTheme = LocalStartuneDarkTheme.current
    val baseColorScheme = MaterialTheme.colorScheme
    val artistColorSchemePair by viewModel.artistColorScheme.collectAsStateWithLifecycle()
    val artistColorScheme = remember(artistColorSchemePair, isDarkTheme, useArtworkColorForDetails) {
        if (!useArtworkColorForDetails) baseColorScheme
        else artistColorSchemePair?.let { pair -> if (isDarkTheme) pair.dark else pair.light }
            ?: baseColorScheme
    }

    MaterialTheme(
        colorScheme = artistColorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .nestedScroll(nestedScrollConnection)
        ) {
            when {
                uiState.isLoading && uiState.artist == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ContainedLoadingIndicator()
                    }
                }
                uiState.error != null && uiState.artist == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            FilledTonalButton(onClick = { viewModel.retry() }) {
                                Text(stringResource(R.string.library_retry))
                            }
                        }
                    }
                }
                uiState.artist != null -> {
                    val artist = uiState.artist!!
                    val songs = uiState.songs
                    val albumSections = uiState.albumSections
                    val featuredOnSections = uiState.featuredOnSections
                    val sortedTracks = remember(songs, trackSortOption, uiState.playCounts) { sortArtistSongs(songs, trackSortOption, uiState.playCounts) }
                    val sortedAlbumSections = remember(albumSections, albumSortOption) { sortAlbumSections(albumSections, albumSortOption, artist.name) }
                    val sortedFeaturedOnSections = remember(featuredOnSections, albumSortOption) { sortAlbumSections(featuredOnSections, albumSortOption, artist.name) }
                    val gridSections = if (view == ArtistDetailView.FeaturedOn) sortedFeaturedOnSections else sortedAlbumSections
                    val title = when (view) {
                        ArtistDetailView.Tracks -> stringResource(R.string.artist_all_songs_title)
                        ArtistDetailView.FeaturedOn -> stringResource(R.string.artist_featured_on_title)
                        else -> stringResource(R.string.library_tab_albums)
                    }

                    if (view == ArtistDetailView.Tracks) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = currentTopBarHeightDp + 8.dp,
                                start = 16.dp,
                                end = if (showScrollBar) 24.dp else 16.dp,
                                bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSelectionMode) {
                                item(key = "artist_list_selection_row") {
                                    SelectionActionRow(
                                        selectedCount = selectedSongs.size,
                                        onSelectAll = { multiSelectionState.selectAll(sortedTracks) },
                                        onDeselect = { multiSelectionState.clearSelection() },
                                        onOptionsClick = { showMultiSelectionSheet = true }
                                    )
                                }
                            }
                            itemsIndexed(
                                items = sortedTracks,
                                key = { _, song -> "artist_track_${song.id}" },
                                contentType = { _, _ -> "artist_track_song" }
                            ) { index, song ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = (index + 1).toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(28.dp)
                                    )
                                    LibraryPlaybackAwareSongItem(
                                        modifier = Modifier.weight(1f),
                                        song = song,
                                        playerViewModel = playerViewModel,
                                        showAlbumArt = true,
                                        isSelected = selectedSongIds.contains(song.id),
                                        selectionIndex = if (isSelectionMode) multiSelectionState.getSelectionIndex(song.id) else null,
                                        isSelectionMode = isSelectionMode,
                                        onLongPress = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            multiSelectionState.toggleSelection(song)
                                        },
                                        onMoreOptionsClick = {
                                            playerViewModel.selectSongForInfo(song)
                                            showSongInfoBottomSheet = true
                                        },
                                        onClick = {
                                            if (isSelectionMode) {
                                                multiSelectionState.toggleSelection(song)
                                            } else {
                                                playerViewModel.showAndPlaySong(song, sortedTracks)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = albumsGridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = currentTopBarHeightDp + 8.dp,
                                start = 16.dp,
                                end = 16.dp,
                                bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isAlbumSelectionMode) {
                                item(span = { GridItemSpan(2) }) {
                                    SelectionActionRow(
                                        selectedCount = selectedAlbumSections.size,
                                        onSelectAll = {
                                            multiSelectionState.clearSelection()
                                            selectedAlbumSections = (selectedAlbumSections + gridSections.filterNot { section -> selectedAlbumSectionIds.contains(section.albumId) }).toImmutableList()
                                        },
                                        onDeselect = {
                                            selectedAlbumSections = persistentListOf()
                                            multiSelectionState.clearSelection()
                                        },
                                        onOptionsClick = {
                                            val albumSongs = selectedAlbumSections.flatMap { it.songs }.distinctBy { it.id }
                                            if (albumSongs.isNotEmpty()) {
                                                multiSelectionState.replaceSelection(albumSongs)
                                                showMultiSelectionSheet = true
                                            }
                                        }
                                    )
                                }
                            }
                            items(
                                items = gridSections,
                                key = { section -> "artist_grid_album_${section.albumId}" },
                                contentType = { "artist_album_card" }
                            ) { section ->
                                ArtistAlbumCard(
                                    section = section,
                                    artistName = artist.name,
                                    viewModel = viewModel,
                                    useArtworkColor = useArtworkColorForDetails,
                                    isSelected = selectedAlbumSectionIds.contains(section.albumId),
                                    selectionIndex = if (isAlbumSelectionMode) {
                                        selectedAlbumSections.indexOfFirst { it.albumId == section.albumId }.takeIf { it >= 0 }?.plus(1)
                                    } else null,
                                    isSelectionMode = isAlbumSelectionMode,
                                    onLongPress = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        multiSelectionState.clearSelection()
                                        selectedAlbumSections = if (selectedAlbumSectionIds.contains(section.albumId)) {
                                            selectedAlbumSections.filterNot { it.albumId == section.albumId }.toImmutableList()
                                        } else {
                                            (selectedAlbumSections + section).toImmutableList()
                                        }
                                    },
                                    onSelectionToggle = {
                                        multiSelectionState.clearSelection()
                                        selectedAlbumSections = if (selectedAlbumSectionIds.contains(section.albumId)) {
                                            selectedAlbumSections.filterNot { it.albumId == section.albumId }.toImmutableList()
                                        } else {
                                            (selectedAlbumSections + section).toImmutableList()
                                        }
                                    },
                                    onClick = {
                                        navController.navigateSafely(
                                            Screen.AlbumDetail.createRoute(section.albumId)
                                        )
                                    }
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
                        CollapsibleCommonTopBar(
                            title = title,
                            collapseFraction = collapseFraction,
                            headerHeight = currentTopBarHeightDp,
                            onBackClick = { navController.popBackStack() },
                            expandedTitleEndPadding = 136.dp,
                            collapsedTitleEndPadding = 136.dp,
                            actions = {
                                Row(
                                    modifier = Modifier.padding(end = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (view == ArtistDetailView.Tracks) showSortSheet = true else showAlbumSortSheet = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                                            contentDescription = stringResource(R.string.presentation_batch_d_sort_by)
                                        )
                                    }
                                }
                            }
                        )
                    }

                    if (collapseFraction > 0.95f) {
                        ExpressiveScrollBar(
                            listState = if (view == ArtistDetailView.Tracks) lazyListState else null,
                            gridState = if (view == ArtistDetailView.Tracks) null else albumsGridState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(
                                    top = minTopBarHeight + 12.dp,
                                    bottom = MiniPlayerHeight + systemNavBarInset + 8.dp
                                )
                        )
                    }

                    SelectionCountPill(
                        selectedCount = selectedSongs.size + selectedAlbumSections.size,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = currentTopBarHeightDp)
                            .zIndex(1f)
                    )
                }
            }
        }

        if (showSortSheet) {
            LibrarySortBottomSheet(
                title = stringResource(R.string.presentation_batch_d_sort_by),
                options = SortOption.SONGS.toImmutableList(),
                selectedOption = trackSortOption,
                onDismiss = { showSortSheet = false },
                onOptionSelected = {
                    trackSortOption = it
                    showSortSheet = false
                }
            )
        }

        if (showAlbumSortSheet) {
            LibrarySortBottomSheet(
                title = stringResource(R.string.presentation_batch_d_sort_by),
                options = SortOption.ALBUMS.toImmutableList(),
                selectedOption = albumSortOption,
                onDismiss = { showAlbumSortSheet = false },
                onOptionSelected = {
                    albumSortOption = it
                    showAlbumSortSheet = false
                }
            )
        }

        if (showSongInfoBottomSheet && selectedSongForInfo != null) {
            val currentSong = selectedSongForInfo
            val isFavorite = remember(currentSong?.id, favoriteIds) {
                currentSong?.let { favoriteIds.contains(it.id) } ?: false
            }
            if (currentSong != null) {
                val removeFromListTrigger = remember(uiState.songs) {
                    {
                        viewModel.removeSongFromAlbumSection(currentSong.id)
                    }
                }
                SongInfoBottomSheet(
                    song = currentSong,
                    isFavorite = isFavorite,
                    onToggleFavorite = {
                        playerViewModel.toggleFavoriteSpecificSong(currentSong)
                    },
                    onDismiss = { showSongInfoBottomSheet = false },
                    onPlaySong = {
                        playerViewModel.showAndPlaySong(currentSong)
                        showSongInfoBottomSheet = false
                    },
                    onAddToQueue = {
                        playerViewModel.addSongToQueue(currentSong)
                        showSongInfoBottomSheet = false
                    },
                    onAddNextToQueue = {
                        playerViewModel.addSongNextToQueue(currentSong)
                        showSongInfoBottomSheet = false
                    },
                    onAddToPlayList = {
                        showPlaylistBottomSheet = true
                    },
                    onDeleteFromDevice = playerViewModel::deleteFromDevice,
                    onNavigateToAlbum = {
                        navController.navigateSafelyReplacing(
                            route = Screen.AlbumDetail.createRoute(currentSong.albumId),
                            patternToPop = Screen.AlbumDetail.route
                        )
                        showSongInfoBottomSheet = false
                    },
                    onNavigateToArtist = {
                        navController.navigateSafelyReplacing(
                            route = Screen.ArtistDetail.createRoute(currentSong.artistId),
                            patternToPop = Screen.ArtistDetail.route
                        )
                        showSongInfoBottomSheet = false
                    },
                    onNavigateToArtistById = { artistId ->
                        navController.navigateSafelyReplacing(
                            route = Screen.ArtistDetail.createRoute(artistId),
                            patternToPop = Screen.ArtistDetail.route
                        )
                        showSongInfoBottomSheet = false
                    },
                    onNavigateToGenre = {
                        currentSong.genre?.let {
                            navController.navigateSafelyReplacing(
                                route = Screen.GenreDetail.createRoute(java.net.URLEncoder.encode(it, "UTF-8")),
                                patternToPop = Screen.GenreDetail.route
                            )
                        }
                        showSongInfoBottomSheet = false
                    },
                    onEditSong = { newTitle, newArtist, newAlbum, newAlbumArtist, newComposer, newGenre, newLyrics, newTrackNumber, newDiscNumber, replayGainTrackGainDb, replayGainAlbumGainDb, coverArtUpdate, customMetadataChanges ->
                        playerViewModel.editSongMetadata(
                            currentSong,
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
                    removeFromListTrigger = removeFromListTrigger
                )
                if (showPlaylistBottomSheet) {
                    val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
                    PlaylistBottomSheet(
                        playlistUiState = playlistUiState,
                        songs = persistentListOf(currentSong),
                        onDismiss = { showPlaylistBottomSheet = false },
                        bottomBarHeight = bottomBarHeightDp,
                        playerViewModel = playerViewModel,
                    )
                }
            }
        }

        SongMultiSelectionHost(
            playerViewModel = playerViewModel,
            multiSelectionState = multiSelectionState,
            showSheet = showMultiSelectionSheet,
            onDismiss = { showMultiSelectionSheet = false },
            onActionTaken = { selectedAlbumSections = persistentListOf() }
        )
    }
}

