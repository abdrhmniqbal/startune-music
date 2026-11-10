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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeExtendedFloatingActionButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.startune.music.R
import com.startune.music.presentation.viewmodel.AlbumData
import com.startune.music.data.model.Artist
import com.startune.music.data.model.Song
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
import com.startune.music.presentation.viewmodel.GenreDetailViewModel
import com.startune.music.presentation.viewmodel.PlayerViewModel
import com.startune.music.presentation.viewmodel.PlaylistViewModel
import com.startune.music.utils.shapes.RoundedStarShape
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GenreDetailListScreen(
    genreId: String,
    view: GenreDetailView,
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    viewModel: GenreDetailViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteSongIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val navBarCompactMode by playerViewModel.navBarCompactMode.collectAsStateWithLifecycle()
    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBarHeightDp = resolveNavBarOccupiedHeight(systemNavBarInset, navBarCompactMode)
    val genreDisplayName = uiState.genre?.name
        ?: java.net.URLDecoder.decode(genreId, "UTF-8")
            .replace("_", " ")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    val multiSelectionState = playerViewModel.multiSelectionStateHolder
    val selectedSongs by multiSelectionState.selectedSongs.collectAsStateWithLifecycle()
    val isSelectionMode by multiSelectionState.isSelectionMode.collectAsStateWithLifecycle()
    val selectedSongIds by multiSelectionState.selectedSongIds.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var showMultiSelectionSheet by remember { mutableStateOf(false) }
    var showSongOptionsSheet by remember { mutableStateOf<Song?>(null) }
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    val toastAddedToQueue = stringResource(R.string.toast_added_to_queue)
    val toastPlayingNext = stringResource(R.string.toast_playing_next)

    var selectedAlbums by remember { mutableStateOf<ImmutableList<AlbumData>>(persistentListOf()) }
    val selectedAlbumNames = remember(selectedAlbums) { selectedAlbums.map { it.name }.toSet() }
    val isAlbumSelectionMode = selectedAlbums.isNotEmpty()
    var selectedArtists by remember { mutableStateOf<ImmutableList<Artist>>(persistentListOf()) }
    val selectedArtistIds = remember(selectedArtists) { selectedArtists.map { it.id }.toSet() }
    val isArtistSelectionMode = selectedArtists.isNotEmpty()
    val toggleAlbumSelection: (AlbumData) -> Unit = { album ->
        multiSelectionState.clearSelection()
        selectedAlbums = if (selectedAlbumNames.contains(album.name)) {
            selectedAlbums.filterNot { it.name == album.name }.toImmutableList()
        } else {
            (selectedAlbums + album).toImmutableList()
        }
    }
    val toggleArtistSelection: (Artist) -> Unit = { artist ->
        multiSelectionState.clearSelection()
        selectedArtists = if (selectedArtistIds.contains(artist.id)) {
            selectedArtists.filterNot { it.id == artist.id }.toImmutableList()
        } else {
            (selectedArtists + artist).toImmutableList()
        }
    }

    var songsSortOption by remember { mutableStateOf<SortOption>(SortOption.SongPlayCount) }
    var showSongsSortSheet by remember { mutableStateOf(false) }
    var albumsSortOption by remember { mutableStateOf<SortOption>(SortOption.AlbumReleaseYear) }
    var showAlbumsSortSheet by remember { mutableStateOf(false) }
    var artistsSortOption by remember { mutableStateOf<SortOption>(SortOption.ArtistNameAZ) }
    var showArtistsSortSheet by remember { mutableStateOf(false) }
    val sortedRankedSongs = remember(uiState.rankedSongs, songsSortOption) {
        when (songsSortOption) {
            SortOption.SongPlayCount -> uiState.rankedSongs
            SortOption.SongPlayCountAsc -> uiState.rankedSongs.reversed()
            else -> sortArtistSongs(uiState.rankedSongs, songsSortOption)
        }
    }
    val sortedAlbums = remember(uiState.albums, albumsSortOption) { sortGenreAlbums(uiState.albums, albumsSortOption) }
    val sortedArtists = remember(uiState.artists, artistsSortOption) { sortGenreArtists(uiState.artists, artistsSortOption) }

    DisposableEffect(Unit) { onDispose { multiSelectionState.clearSelection() } }
    BackHandler(enabled = isSelectionMode || isAlbumSelectionMode || isArtistSelectionMode) {
        multiSelectionState.clearSelection()
        selectedAlbums = persistentListOf()
        selectedArtists = persistentListOf()
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
    val solidAlpha = (collapseFraction * 2f).coerceIn(0f, 1f)
    val expandedContentAlpha = 1f - solidAlpha
    val titleVerticalBias = lerp(1f, -1f, collapseFraction)
    val shuffleAlignment = BiasAlignment(horizontalBias = 1f, verticalBias = titleVerticalBias)

    val songsListState = rememberLazyListState()
    val albumsGridState = rememberLazyGridState()
    val artistsGridState = rememberLazyGridState()
    val isGridView = view != GenreDetailView.Songs
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val scrollingDown = delta < 0
                val firstVisibleItemIndex = when (view) {
                    GenreDetailView.Songs -> songsListState.firstVisibleItemIndex
                    GenreDetailView.Albums -> albumsGridState.firstVisibleItemIndex
                    else -> artistsGridState.firstVisibleItemIndex
                }
                val firstVisibleItemScrollOffset = when (view) {
                    GenreDetailView.Songs -> songsListState.firstVisibleItemScrollOffset
                    GenreDetailView.Albums -> albumsGridState.firstVisibleItemScrollOffset
                    else -> artistsGridState.firstVisibleItemScrollOffset
                }
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
    val isScrollInProgress = when (view) {
        GenreDetailView.Songs -> songsListState.isScrollInProgress
        GenreDetailView.Albums -> albumsGridState.isScrollInProgress
        else -> artistsGridState.isScrollInProgress
    }
    LaunchedEffect(isScrollInProgress) {
        if (!isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand = when (view) {
                GenreDetailView.Songs -> songsListState.firstVisibleItemIndex == 0 && songsListState.firstVisibleItemScrollOffset == 0
                GenreDetailView.Albums -> albumsGridState.firstVisibleItemIndex == 0 && albumsGridState.firstVisibleItemScrollOffset == 0
                else -> artistsGridState.firstVisibleItemIndex == 0 && artistsGridState.firstVisibleItemScrollOffset == 0
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
            collapseFraction > 0.95f && when (view) {
                GenreDetailView.Songs -> songsListState.canScrollForward || songsListState.canScrollBackward
                GenreDetailView.Albums -> albumsGridState.canScrollForward || albumsGridState.canScrollBackward
                else -> artistsGridState.canScrollForward || artistsGridState.canScrollBackward
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(nestedScrollConnection)
    ) {
        when {
            (uiState.isLoadingSongs || uiState.isLoadingGenreName) && uiState.genre == null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ContainedLoadingIndicator()
                }
            }
            uiState.error != null && uiState.genre == null -> {
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
            uiState.genre != null -> {
                val title = when (view) {
                    GenreDetailView.Songs -> stringResource(R.string.top_songs_title)
                    GenreDetailView.Albums -> stringResource(R.string.library_tab_albums)
                    else -> stringResource(R.string.library_tab_artists)
                }

                if (view == GenreDetailView.Songs) {
                    LazyColumn(
                        state = songsListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = currentTopBarHeightDp + 8.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isSelectionMode) {
                            item(key = "genre_list_selection_row") {
                                SelectionActionRow(
                                    selectedCount = selectedSongs.size,
                                    onSelectAll = { multiSelectionState.selectAll(uiState.rankedSongs) },
                                    onDeselect = { multiSelectionState.clearSelection() },
                                    onOptionsClick = { showMultiSelectionSheet = true }
                                )
                            }
                        }
                        itemsIndexed(
                            items = sortedRankedSongs,
                            key = { _, song -> "genre_ranked_${song.id}" }
                        ) { index, song ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    isSelected = selectedSongIds.contains(song.id),
                                    selectionIndex = if (isSelectionMode) multiSelectionState.getSelectionIndex(song.id) else null,
                                    isSelectionMode = isSelectionMode,
                                    onLongPress = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        multiSelectionState.toggleSelection(song)
                                    },
                                    onMoreOptionsClick = { showSongOptionsSheet = it },
                                    onClick = {
                                        if (isSelectionMode) {
                                            multiSelectionState.toggleSelection(song)
                                        } else {
                                            playerViewModel.showAndPlaySong(song, sortedRankedSongs, genreDisplayName)
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else if (view == GenreDetailView.Albums) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = albumsGridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = currentTopBarHeightDp + 8.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isAlbumSelectionMode) {
                            item(span = { GridItemSpan(2) }) {
                                SelectionActionRow(
                                    selectedCount = selectedAlbums.size,
                                    onSelectAll = {
                                        multiSelectionState.clearSelection()
                                        selectedAlbums = (selectedAlbums + uiState.albums.filterNot { selectedAlbumNames.contains(it.name) }).toImmutableList()
                                    },
                                    onDeselect = {
                                        selectedAlbums = persistentListOf()
                                        multiSelectionState.clearSelection()
                                    },
                                    onOptionsClick = {
                                        val songs = selectedAlbums.flatMap { it.songs }.distinctBy { it.id }
                                        if (songs.isNotEmpty()) {
                                            multiSelectionState.replaceSelection(songs)
                                            showMultiSelectionSheet = true
                                        }
                                    }
                                )
                            }
                        }
                        items(
                            items = sortedAlbums,
                            key = { album -> "genre_grid_album_${album.name}" }
                        ) { album ->
                            val albumColorSchemeFlow = remember(album.artUri) {
                                playerViewModel.themeStateHolder.getAlbumColorSchemeFlow(album.artUri ?: "")
                            }
                            AlbumGridItemRedesigned(
                                album = albumModelFromAlbumData(album),
                                albumColorSchemePairFlow = albumColorSchemeFlow,
                                onClick = {
                                    navController.navigateSafely(
                                        Screen.AlbumDetail.createRoute(
                                            album.songs.firstOrNull()?.albumId ?: -1L
                                        )
                                    )
                                },
                                isSelectionMode = isAlbumSelectionMode,
                                isSelected = selectedAlbumNames.contains(album.name),
                                selectionIndex = if (isAlbumSelectionMode) {
                                    selectedAlbums.indexOfFirst { it.name == album.name }.takeIf { it >= 0 }?.plus(1)
                                } else null,
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    toggleAlbumSelection(album)
                                },
                                onSelectionToggle = { toggleAlbumSelection(album) }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = artistsGridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = currentTopBarHeightDp + 8.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isArtistSelectionMode) {
                            item(span = { GridItemSpan(2) }) {
                                SelectionActionRow(
                                    selectedCount = selectedArtists.size,
                                    onSelectAll = {
                                        multiSelectionState.clearSelection()
                                        selectedArtists = (selectedArtists + uiState.artists.filterNot { selectedArtistIds.contains(it.id) }).toImmutableList()
                                    },
                                    onDeselect = {
                                        selectedArtists = persistentListOf()
                                        multiSelectionState.clearSelection()
                                    },
                                    onOptionsClick = {
                                        val snapshot = selectedArtists.toList()
                                        val snapshotIds = snapshot.map { it.id }
                                        coroutineScope.launch {
                                            val songs = playerViewModel.resolveArtistSongsForBatchActions(snapshot)
                                            if (songs.isNotEmpty() && selectedArtists.map { it.id } == snapshotIds) {
                                                multiSelectionState.replaceSelection(songs)
                                                showMultiSelectionSheet = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        items(
                            items = sortedArtists,
                            key = { artist -> "genre_grid_artist_${artist.id}_${artist.name}" }
                        ) { artist ->
                            ArtistGridItem(
                                artist = artist,
                                onClick = {
                                    navController.navigateSafely(
                                        Screen.ArtistDetail.createRoute(artist.id)
                                    )
                                },
                                isSelectionMode = isArtistSelectionMode,
                                isSelected = selectedArtistIds.contains(artist.id),
                                selectionIndex = if (isArtistSelectionMode) {
                                    selectedArtists.indexOfFirst { it.id == artist.id }.takeIf { it >= 0 }?.plus(1)
                                } else null,
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    toggleArtistSelection(artist)
                                },
                                onSelectionToggle = { toggleArtistSelection(artist) }
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
                                        when (view) {
                                            GenreDetailView.Songs -> showSongsSortSheet = true
                                            GenreDetailView.Albums -> showAlbumsSortSheet = true
                                            else -> showArtistsSortSheet = true
                                        }
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
                    if (view == GenreDetailView.Songs && uiState.rankedSongs.isNotEmpty()) {
                        LargeExtendedFloatingActionButton(
                            onClick = {
                                playerViewModel.playSongsShuffled(sortedRankedSongs, genreDisplayName, startAtZero = true)
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

                if (collapseFraction > 0.95f) {
                    ExpressiveScrollBar(
                        listState = if (view == GenreDetailView.Songs) songsListState else null,
                        gridState = when (view) {
                            GenreDetailView.Albums -> albumsGridState
                            GenreDetailView.Artists -> artistsGridState
                            else -> null
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(
                                top = minTopBarHeight + 12.dp,
                                bottom = MiniPlayerHeight + systemNavBarInset + 8.dp
                            )
                    )
                }

                SelectionCountPill(
                    selectedCount = when (view) {
                        GenreDetailView.Songs -> selectedSongs.size
                        GenreDetailView.Albums -> selectedAlbums.size
                        GenreDetailView.Artists -> selectedArtists.size
                        GenreDetailView.Overview -> 0
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = currentTopBarHeightDp)
                        .zIndex(1f)
                )
            }
        }
        if (uiState.isLoadingSongs) {
            ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }

    if (showSongsSortSheet) {
        LibrarySortBottomSheet(
            title = stringResource(R.string.presentation_batch_d_sort_by),
            options = SortOption.SONGS.toImmutableList(),
            selectedOption = songsSortOption,
            onDismiss = { showSongsSortSheet = false },
            onOptionSelected = {
                songsSortOption = it
                showSongsSortSheet = false
            }
        )
    }

    if (showAlbumsSortSheet) {
        LibrarySortBottomSheet(
            title = stringResource(R.string.presentation_batch_d_sort_by),
            options = SortOption.ALBUMS.toImmutableList(),
            selectedOption = albumsSortOption,
            onDismiss = { showAlbumsSortSheet = false },
            onOptionSelected = {
                albumsSortOption = it
                showAlbumsSortSheet = false
            }
        )
    }

    if (showArtistsSortSheet) {
        LibrarySortBottomSheet(
            title = stringResource(R.string.presentation_batch_d_sort_by),
            options = SortOption.ARTISTS.toImmutableList(),
            selectedOption = artistsSortOption,
            onDismiss = { showArtistsSortSheet = false },
            onOptionSelected = {
                artistsSortOption = it
                showArtistsSortSheet = false
            }
        )
    }

    showSongOptionsSheet?.let { song ->
        val isFavorite = favoriteSongIds.contains(song.id)
        SongInfoBottomSheet(
            song = song,
            isFavorite = isFavorite,
            onToggleFavorite = {
                playerViewModel.toggleFavoriteSpecificSong(song)
            },
            onDismiss = { showSongOptionsSheet = null },
            onPlaySong = {
                playerViewModel.showAndPlaySong(song, sortedRankedSongs, genreDisplayName)
                showSongOptionsSheet = null
            },
            onAddToQueue = {
                playerViewModel.addSongToQueue(song)
                showSongOptionsSheet = null
                playerViewModel.sendToast(toastAddedToQueue)
            },
            onAddNextToQueue = {
                playerViewModel.addSongNextToQueue(song)
                showSongOptionsSheet = null
                playerViewModel.sendToast(toastPlayingNext)
            },
            onAddToPlayList = {
                showPlaylistBottomSheet = true
            },
            onDeleteFromDevice = playerViewModel::deleteFromDevice,
            onNavigateToAlbum = {
                navController.navigateSafelyReplacing(
                    route = Screen.AlbumDetail.createRoute(song.albumId),
                    patternToPop = Screen.AlbumDetail.route
                )
                showSongOptionsSheet = null
            },
            onNavigateToArtist = {
                navController.navigateSafelyReplacing(
                    route = Screen.ArtistDetail.createRoute(song.artistId),
                    patternToPop = Screen.ArtistDetail.route
                )
                showSongOptionsSheet = null
            },
            onNavigateToArtistById = { artistId ->
                navController.navigateSafelyReplacing(
                    route = Screen.ArtistDetail.createRoute(artistId),
                    patternToPop = Screen.ArtistDetail.route
                )
                showSongOptionsSheet = null
            },
            onNavigateToGenre = {
                song.genre?.let {
                    navController.navigateSafelyReplacing(
                        route = Screen.GenreDetail.createRoute(java.net.URLEncoder.encode(it, "UTF-8")),
                        patternToPop = Screen.GenreDetail.route
                    )
                }
                showSongOptionsSheet = null
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
                bottomBarHeight = 0.dp,
                playerViewModel = playerViewModel
            )
        }
    }

    SongMultiSelectionHost(
        playerViewModel = playerViewModel,
        multiSelectionState = multiSelectionState,
        showSheet = showMultiSelectionSheet,
        onDismiss = { showMultiSelectionSheet = false },
        onActionTaken = {
            selectedAlbums = persistentListOf()
            selectedArtists = persistentListOf()
        }
    )
}
