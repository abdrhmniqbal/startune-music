package com.startune.music.presentation.screens

import com.startune.music.presentation.navigation.navigateSafely
import com.startune.music.presentation.components.SongMultiSelectionHost
import com.startune.music.presentation.components.LibrarySortBottomSheet
import com.startune.music.presentation.components.subcomps.SelectionActionRow
import com.startune.music.presentation.components.subcomps.SelectionCountPill
import com.startune.music.presentation.navigation.navigateSafelyReplacing
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import com.startune.music.R
import com.startune.music.data.model.Artist
import com.startune.music.data.model.SortOption
import com.startune.music.data.model.Album
import com.startune.music.data.model.Song
import com.startune.music.data.preferences.AlbumArtPaletteStyle
import com.startune.music.presentation.components.ExpressiveTopBarContent
import com.startune.music.presentation.components.MiniPlayerHeight
import com.startune.music.presentation.components.SmartImage
import com.startune.music.presentation.components.subcomps.PlayingEqIcon
import com.startune.music.utils.shapes.RoundedStarShape
import com.startune.music.presentation.components.SongInfoBottomSheet
import com.startune.music.presentation.screens.QuickFillDialog
import com.startune.music.presentation.viewmodel.GenreDetailViewModel
import com.startune.music.presentation.viewmodel.AlbumData
import com.startune.music.presentation.viewmodel.PlayerViewModel
import com.startune.music.ui.theme.LocalStartuneDarkTheme
import com.startune.music.utils.formatSongCount
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.toImmutableList
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GenreDetailScreen(
    navController: NavHostController,
    genreId: String,
    decodedGenreId: String = java.net.URLDecoder.decode(genreId, "UTF-8"),
    playerViewModel: PlayerViewModel,
    viewModel: GenreDetailViewModel = hiltViewModel(),
    playlistViewModel: com.startune.music.presentation.viewmodel.PlaylistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stablePlayerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()
    val favoriteSongIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val libraryGenres by playerViewModel.genres.collectAsStateWithLifecycle()
    
    val density = LocalDensity.current
    val darkMode = LocalStartuneDarkTheme.current

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val songsListState = rememberLazyListState()
    val albumsGridState = rememberLazyGridState()
    val artistsGridState = rememberLazyGridState()
    var activeView by remember { mutableStateOf(GenreDetailView.Overview) }
    BackHandler(enabled = activeView != GenreDetailView.Overview) {
        activeView = GenreDetailView.Overview
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val minTopBarHeight = 58.dp + statusBarHeight
    val maxTopBarHeight = 200.dp
    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
    val collapseFraction by remember(minTopBarHeightPx, maxTopBarHeightPx) {
        derivedStateOf {
            1f - ((topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(0f, 1f)
        }
    }
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

    val defaultContainer = MaterialTheme.colorScheme.surfaceVariant
    val defaultOnContainer = MaterialTheme.colorScheme.onSurfaceVariant
    val themeGenre = uiState.genre
    val themeColor = remember(themeGenre, decodedGenreId, darkMode, defaultContainer, defaultOnContainer) {
        if (themeGenre != null) {
            com.startune.music.ui.theme.GenreThemeUtils.getGenreThemeColor(
                genre = themeGenre,
                isDark = darkMode,
                fallbackGenreId = decodedGenreId
            )
        } else {
            com.startune.music.ui.theme.GenreThemeColor(
                defaultContainer,
                defaultOnContainer
            )
        }
    }
    
    val startColor = themeColor.container
    val contentColor = themeColor.onContainer
    
    val initialDisplayName = remember(decodedGenreId) {
        decodedGenreId
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
    val genreDisplayName = themeGenre?.name ?: uiState.genre?.name ?: initialDisplayName
    
    val toastAddedToQueue = stringResource(R.string.toast_added_to_queue)
    val toastPlayingNext = stringResource(R.string.toast_playing_next)

    var showSongOptionsSheet by remember { mutableStateOf<Song?>(null) }
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }

    val multiSelectionState = playerViewModel.multiSelectionStateHolder
    val selectedSongs by multiSelectionState.selectedSongs.collectAsStateWithLifecycle()
    val isSelectionMode by multiSelectionState.isSelectionMode.collectAsStateWithLifecycle()
    val selectedSongIds by multiSelectionState.selectedSongIds.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var showMultiSelectionSheet by remember { mutableStateOf(false) }
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

    BackHandler(enabled = isSelectionMode || isAlbumSelectionMode || isArtistSelectionMode) {
        multiSelectionState.clearSelection()
        selectedAlbums = persistentListOf()
        selectedArtists = persistentListOf()
    }
    LaunchedEffect(activeView) {
        multiSelectionState.clearSelection()
        selectedAlbums = persistentListOf()
        selectedArtists = persistentListOf()
    }
    DisposableEffect(Unit) { onDispose { multiSelectionState.clearSelection() } }
    var showQuickFillDialog by remember { mutableStateOf(false) }

    val isUnknownGenre = remember(decodedGenreId) {
        decodedGenreId.equals("unknown", ignoreCase = true) || decodedGenreId.equals("unknown genre", ignoreCase = true)
    }
    
    val customGenres by playerViewModel.customGenres.collectAsStateWithLifecycle()
    val customGenreIcons by playerViewModel.customGenreIcons.collectAsStateWithLifecycle()
    val genrePaletteStyle by playerViewModel.albumArtPaletteStyle.collectAsStateWithLifecycle(
        initialValue = AlbumArtPaletteStyle.default
    )
    val isMiniPlayerVisible = stablePlayerState.currentSong != null
    val fabBottomPadding by animateDpAsState(
        targetValue = if (isMiniPlayerVisible) MiniPlayerHeight + systemNavBarInset + 16.dp else systemNavBarInset + 16.dp,
        label = "fabPadding"
    )

    val baseColorScheme = MaterialTheme.colorScheme

    val genreColorScheme = remember(themeGenre, decodedGenreId, darkMode, genrePaletteStyle) {
        com.startune.music.ui.theme.GenreThemeUtils.getGenreDetailColorScheme(
            genre = themeGenre,
            fallbackGenreId = decodedGenreId,
            isDark = darkMode,
            paletteStyle = genrePaletteStyle
        )
    }

    MaterialTheme(colorScheme = genreColorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }
            val solidAlpha = (collapseFraction * 2f).coerceIn(0f, 1f)
            val expandedContentAlpha = 1f - solidAlpha
            val titleVerticalBias = lerp(1f, -1f, collapseFraction)
            val shuffleAlignment = BiasAlignment(horizontalBias = 1f, verticalBias = titleVerticalBias)

            AnimatedContent(
                targetState = activeView,
                transitionSpec = {
                    (fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 6 }) togetherWith
                        (fadeOut(tween(200)) + slideOutHorizontally(tween(200)) { -it / 6 })
                },
                label = "genre_detail_view"
            ) { view ->
                when (view) {
                GenreDetailView.Overview -> {
                    LazyColumn(
                        state = lazyListState,
                        contentPadding = PaddingValues(
                            top = minTopBarHeight + 8.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = fabBottomPadding + 16.dp
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .offset {
                                val extraHeight = (topBarHeight.value - minTopBarHeightPx).roundToInt()
                                IntOffset(0, extraHeight)
                            },
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        if (uiState.songs.isEmpty()) {
                            item(key = "genre_empty") {
                                GenreEmptyState()
                            }
                        } else {
                            if (uiState.rankedSongs.isNotEmpty()) {
                                item(key = "genre_top_songs") {
                                    GenreTopSongsCarousel(
                                        songs = uiState.rankedSongs.take(GENRE_TOP_SONGS_PREVIEW),
                                        currentSongId = stablePlayerState.currentSong?.id,
                                        isPlaying = stablePlayerState.isPlaying,
                                        onSongClick = { song ->
                                            playerViewModel.showAndPlaySong(song, uiState.rankedSongs, genreDisplayName)
                                        },
                                        onViewMoreClick = { activeView = GenreDetailView.Songs }
                                    )
                                }
                            }
                            if (uiState.randomAlbums.isNotEmpty()) {
                                item(key = "genre_albums") {
                                    GenreAlbumsSection(
                                        albums = uiState.randomAlbums,
                                        onViewMoreClick = { activeView = GenreDetailView.Albums },
                                        playerViewModel = playerViewModel,
                                        onAlbumClick = { album ->
                                            navController.navigateSafely(
                                                com.startune.music.presentation.navigation.Screen.AlbumDetail.createRoute(
                                                    album.songs.firstOrNull()?.albumId ?: -1L
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                            if (uiState.randomArtists.isNotEmpty()) {
                                item(key = "genre_artists") {
                                    GenreArtistsSection(
                                        artists = uiState.randomArtists,
                                        onViewMoreClick = { activeView = GenreDetailView.Artists },
                                        playerViewModel = playerViewModel,
                                        onArtistClick = { artist ->
                                            navController.navigateSafely(
                                                com.startune.music.presentation.navigation.Screen.ArtistDetail.createRoute(artist.id)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                GenreDetailView.Songs -> {
                    LazyColumn(
                        state = songsListState,
                        contentPadding = PaddingValues(
                            top = minTopBarHeight + 8.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = fabBottomPadding + 16.dp
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .offset {
                                val extraHeight = (topBarHeight.value - minTopBarHeightPx).roundToInt()
                                IntOffset(0, extraHeight)
                            },
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "genre_songs_header") {
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
                                        onSelectAll = { multiSelectionState.selectAll(uiState.rankedSongs) },
                                        onDeselect = { multiSelectionState.clearSelection() },
                                        onOptionsClick = { showMultiSelectionSheet = true }
                                    )
                                } else {
                                    GenreSubViewHeader(
                                        title = stringResource(R.string.top_songs_title),
                                        onBackClick = { activeView = GenreDetailView.Overview },
                                        onSortClick = { showSongsSortSheet = true }
                                    )
                                }
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
                }
                GenreDetailView.Albums -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = albumsGridState,
                        contentPadding = PaddingValues(
                            top = minTopBarHeight + 8.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = fabBottomPadding + 16.dp
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .offset {
                                val extraHeight = (topBarHeight.value - minTopBarHeightPx).roundToInt()
                                IntOffset(0, extraHeight)
                            },
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(span = { GridItemSpan(2) }) {
                            AnimatedContent(
                                targetState = isAlbumSelectionMode,
                                label = "ActionRowModeSwitch",
                                transitionSpec = {
                                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                                        (slideOutHorizontally { it } + fadeOut())
                                }
                            ) { inSelectionMode ->
                                if (inSelectionMode) {
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
                                } else {
                                    GenreSubViewHeader(
                                        title = stringResource(R.string.library_tab_albums),
                                        onBackClick = { activeView = GenreDetailView.Overview },
                                        onSortClick = { showAlbumsSortSheet = true }
                                    )
                                }
                            }
                        }
                        gridItems(
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
                                        com.startune.music.presentation.navigation.Screen.AlbumDetail.createRoute(
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
                }
                GenreDetailView.Artists -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = artistsGridState,
                        contentPadding = PaddingValues(
                            top = minTopBarHeight + 8.dp,
                            start = 16.dp,
                            end = 16.dp,
                            bottom = fabBottomPadding + 16.dp
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .offset {
                                val extraHeight = (topBarHeight.value - minTopBarHeightPx).roundToInt()
                                IntOffset(0, extraHeight)
                            },
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item(span = { GridItemSpan(2) }) {
                            AnimatedContent(
                                targetState = isArtistSelectionMode,
                                label = "ActionRowModeSwitch",
                                transitionSpec = {
                                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                                        (slideOutHorizontally { it } + fadeOut())
                                }
                            ) { inSelectionMode ->
                                if (inSelectionMode) {
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
                                } else {
                                    GenreSubViewHeader(
                                        title = stringResource(R.string.library_tab_artists),
                                        onBackClick = { activeView = GenreDetailView.Overview },
                                        onSortClick = { showArtistsSortSheet = true }
                                    )
                                }
                            }
                        }
                        gridItems(
                            items = sortedArtists,
                            key = { artist -> "genre_grid_artist_${artist.id}_${artist.name}" }
                        ) { artist ->
                            ArtistGridItem(
                                artist = artist,
                                onClick = {
                                    navController.navigateSafely(
                                        com.startune.music.presentation.navigation.Screen.ArtistDetail.createRoute(artist.id)
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
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(currentTopBarHeightDp)
                    .clipToBounds()
            ) {
                GenreCollapsibleTopBar(
                    title = genreDisplayName,
                    collapseFraction = collapseFraction,
                    headerHeight = currentTopBarHeightDp,
                    onBackPressed = {
                        if (activeView != GenreDetailView.Overview) {
                            activeView = GenreDetailView.Overview
                        } else {
                            navController.popBackStack()
                        }
                    },
                    startColor = startColor,
                    contentColor = contentColor,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    collapsedContentColor = MaterialTheme.colorScheme.onSurface
                )
                if (activeView == GenreDetailView.Songs && uiState.rankedSongs.isNotEmpty()) {
                    LargeExtendedFloatingActionButton(
                        onClick = {
                            playerViewModel.playSongsShuffled(uiState.rankedSongs, genreDisplayName, startAtZero = true)
                        },
                        shape = RoundedStarShape(sides = 8, curve = 0.05),
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

            if (isUnknownGenre) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = fabBottomPadding + 26.dp, end = 16.dp)
                        .zIndex(10f)
                ) {
                    MediumFloatingActionButton(
                        onClick = { showQuickFillDialog = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = AbsoluteSmoothCornerShape(24.dp, 60)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoFixHigh,
                            contentDescription = stringResource(R.string.genre_quick_fill),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            MaterialTheme(colorScheme = baseColorScheme) {
                QuickFillDialog(
                    visible = showQuickFillDialog,
                    songs = remember(uiState.songs) { uiState.songs.toImmutableList() },
                    customGenres = customGenres,
                    customGenreIcons = customGenreIcons,
                    onDismiss = { showQuickFillDialog = false },
                    onApply = { songs, genre ->
                        playerViewModel.batchEditGenre(songs, genre)
                        showQuickFillDialog = false
                    },
                    onAddCustomGenre = { genre, iconRes ->
                        playerViewModel.addCustomGenre(genre, iconRes)
                    }
                )
            }
        
            showSongOptionsSheet?.let { song ->
                val isFavorite = favoriteSongIds.contains(song.id)

                MaterialTheme(
                    colorScheme = genreColorScheme,
                    typography = MaterialTheme.typography,
                    shapes = MaterialTheme.shapes
                ) {
                    SongInfoBottomSheet(
                        song = song,
                        isFavorite = isFavorite,
                        onToggleFavorite = {
                            playerViewModel.toggleFavoriteSpecificSong(song)
                        },
                        onDismiss = { showSongOptionsSheet = null },
                        onPlaySong = {
                            playerViewModel.showAndPlaySong(song, uiState.rankedSongs, genreDisplayName)
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
                                route = com.startune.music.presentation.navigation.Screen.AlbumDetail.createRoute(song.albumId),
                                patternToPop = com.startune.music.presentation.navigation.Screen.AlbumDetail.route
                            )
                            showSongOptionsSheet = null
                        },
                        onNavigateToArtist = {
                            navController.navigateSafelyReplacing(
                                route = com.startune.music.presentation.navigation.Screen.ArtistDetail.createRoute(song.artistId),
                                patternToPop = com.startune.music.presentation.navigation.Screen.ArtistDetail.route
                            )
                            showSongOptionsSheet = null
                        },
                        onNavigateToArtistById = { artistId ->
                            navController.navigateSafelyReplacing(
                                route = com.startune.music.presentation.navigation.Screen.ArtistDetail.createRoute(artistId),
                                patternToPop = com.startune.music.presentation.navigation.Screen.ArtistDetail.route
                            )
                            showSongOptionsSheet = null
                        },
                        onNavigateToGenre = {
                            song.genre?.let {
                                navController.navigateSafelyReplacing(
                                    route = com.startune.music.presentation.navigation.Screen.GenreDetail.createRoute(java.net.URLEncoder.encode(it, "UTF-8")),
                                    patternToPop = com.startune.music.presentation.navigation.Screen.GenreDetail.route
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

                if (showPlaylistBottomSheet) {
                    com.startune.music.presentation.components.PlaylistBottomSheet(
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

        SelectionCountPill(
            selectedCount = when (activeView) {
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
        
            if (uiState.isLoadingSongs) {
                ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun GenreCollapsibleTopBar(
    title: String,
    collapseFraction: Float,
    headerHeight: Dp,
    onBackPressed: () -> Unit,
    startColor: Color,
    containerColor: Color,
    contentColor: Color,
    collapsedContentColor: Color
) {
    val solidAlpha = (collapseFraction * 2f).coerceIn(0f, 1f)
    val animatedContentColor = androidx.compose.ui.graphics.lerp(
        start = contentColor,
        stop = collapsedContentColor,
        fraction = solidAlpha
    )

    val gradientAlpha = 0.8f * (1f - solidAlpha)
    
    val verticalGradient = remember(startColor, gradientAlpha) {
        Brush.verticalGradient(
            colors = listOf(
                startColor.copy(alpha = gradientAlpha),
                startColor.copy(alpha = 0f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .zIndex(5f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(containerColor.copy(alpha = solidAlpha)) 
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(verticalGradient)
        )

        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
             FilledIconButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 4.dp)
                    .zIndex(10f),
                onClick = onBackPressed,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = animatedContentColor.copy(alpha = 0.1f),
                    contentColor = animatedContentColor
                )
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.auth_cd_back), tint = animatedContentColor)
            }

            ExpressiveTopBarContent(
                title = title,
                collapseFraction = collapseFraction,
                modifier = Modifier.fillMaxSize(),
                collapsedTitleStartPadding = 68.dp,
                expandedTitleStartPadding = 20.dp,
                maxLines = 1,
                contentColor = animatedContentColor
            )
        }
    }
}



private enum class GenreDetailView {
    Overview, Songs, Albums, Artists
}

private const val GENRE_TOP_SONGS_PREVIEW = 25
private const val GENRE_TOP_SONGS_PAGE_SIZE = 5
private val GenreGridCardWidth = 144.dp

@Composable
private fun GenreEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Filled.MusicOff,
            null,
            Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.genre_empty_title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            stringResource(R.string.genre_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GenreSectionHeader(
    title: String,
    onViewMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLargeEmphasized,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        FilledIconButton(
            modifier = Modifier
                .height(40.dp)
                .width(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.secondary
            ),
            onClick = onViewMoreClick
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun GenreSubViewHeader(
    title: String,
    onBackClick: () -> Unit,
    onSortClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledIconButton(
                modifier = Modifier
                    .height(40.dp)
                    .width(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.secondary
                ),
                onClick = onBackClick
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.auth_cd_back),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (onSortClick != null) {
            FilledTonalIconButton(onClick = onSortClick) {
                Icon(
                    imageVector = Icons.Filled.Sort,
                    contentDescription = stringResource(R.string.presentation_batch_d_sort_by)
                )
            }
        }
    }
}

@Composable
private fun GenreTopSongsCarousel(
    songs: List<Song>,
    currentSongId: String?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onViewMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return
    val pages = remember(songs) { songs.chunked(GENRE_TOP_SONGS_PAGE_SIZE) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.top_songs_title),
                style = MaterialTheme.typography.titleLargeEmphasized,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            FilledIconButton(
                modifier = Modifier
                    .height(40.dp)
                    .width(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.secondary
                ),
                onClick = onViewMoreClick
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            pageSpacing = 24.dp,
            verticalAlignment = Alignment.Top
        ) { page ->
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                pages[page].forEachIndexed { index, song ->
                    key(song.id) {
                        GenreTopSongRow(
                            rank = page * GENRE_TOP_SONGS_PAGE_SIZE + index + 1,
                            song = song,
                            isCurrentSong = currentSongId == song.id,
                            isPlaying = isPlaying,
                            onClick = { onSongClick(song) }
                        )
                    }
                }
            }
        }
    
    }
}

@Composable
private fun GenreTopSongRow(
    rank: Int,
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GenreRankBadge(
            rank = rank,
            accentColor = MaterialTheme.colorScheme.primary,
            accentOnColor = MaterialTheme.colorScheme.onPrimary,
            highlighted = rank == 1
        )
        SmartImage(
            model = song.albumArtUriString,
            contentDescription = song.title,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrentSong) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.displayArtist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isCurrentSong) {
            PlayingEqIcon(
                modifier = Modifier.size(width = 18.dp, height = 16.dp),
                color = MaterialTheme.colorScheme.primary,
                isPlaying = isPlaying
            )
        }
    }
}

@Composable
private fun GenreRankBadge(
    rank: Int,
    accentColor: Color,
    accentOnColor: Color,
    highlighted: Boolean
) {
    val containerColor = if (highlighted) {
        accentColor
    } else {
        accentColor.copy(alpha = 0.24f)
    }
    val contentColor = if (highlighted) {
        accentOnColor
    } else {
        accentColor
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 34.dp, minHeight = 34.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun sortGenreAlbums(
    albums: List<AlbumData>,
    sortOption: SortOption
): List<AlbumData> =
    when (sortOption) {
        SortOption.AlbumTitleAZ -> albums.sortedWith(compareBy<AlbumData> { it.name.lowercase() }.thenBy { it.songs.firstOrNull()?.albumId ?: -1L })
        SortOption.AlbumTitleZA -> albums.sortedWith(compareByDescending<AlbumData> { it.name.lowercase() }.thenBy { it.songs.firstOrNull()?.albumId ?: -1L })
        SortOption.AlbumArtist -> albums.sortedWith(compareBy<AlbumData> { it.songs.firstOrNull()?.displayArtist?.lowercase() ?: "" }.thenBy { it.name.lowercase() })
        SortOption.AlbumArtistDesc -> albums.sortedWith(compareByDescending<AlbumData> { it.songs.firstOrNull()?.displayArtist?.lowercase() ?: "" }.thenBy { it.name.lowercase() })
        SortOption.AlbumReleaseYear -> albums.sortedWith(compareByDescending<AlbumData> { it.songs.maxOfOrNull { song -> song.year } ?: 0 }.thenBy { it.name.lowercase() })
        SortOption.AlbumReleaseYearAsc -> albums.sortedWith(compareBy<AlbumData> { it.songs.maxOfOrNull { song -> song.year } ?: 0 }.thenBy { it.name.lowercase() })
        SortOption.AlbumDateAdded -> albums.sortedWith(compareByDescending<AlbumData> { it.songs.maxOfOrNull { song -> song.dateAdded } ?: 0 }.thenBy { it.name.lowercase() })
        SortOption.AlbumSizeAsc -> albums.sortedWith(compareBy<AlbumData> { it.songs.size }.thenBy { it.name.lowercase() })
        SortOption.AlbumSizeDesc -> albums.sortedWith(compareByDescending<AlbumData> { it.songs.size }.thenBy { it.name.lowercase() })
        else -> albums
    }

private fun sortGenreArtists(
    artists: List<Artist>,
    sortOption: SortOption
): List<Artist> =
    when (sortOption) {
        SortOption.ArtistNameAZ -> artists.sortedWith(compareBy<Artist> { it.name.lowercase() }.thenBy { it.id })
        SortOption.ArtistNameZA -> artists.sortedWith(compareByDescending<Artist> { it.name.lowercase() }.thenBy { it.id })
        SortOption.ArtistNumSongsDesc -> artists.sortedWith(compareByDescending<Artist> { it.songCount }.thenBy { it.name.lowercase() }.thenBy { it.id })
        SortOption.ArtistNumSongsAsc -> artists.sortedWith(compareBy<Artist> { it.songCount }.thenBy { it.name.lowercase() }.thenBy { it.id })
        else -> artists
    }

private fun albumModelFromAlbumData(album: AlbumData): Album {
    val firstSong = album.songs.firstOrNull()
    return Album(
        id = firstSong?.albumId ?: -1L,
        title = album.name,
        artist = firstSong?.displayArtist ?: "",
        year = firstSong?.year ?: 0,
        dateAdded = album.songs.maxOfOrNull { it.dateAdded } ?: 0,
        albumArtUriString = album.artUri,
        songCount = album.songs.size,
        albumArtist = album.songs.firstNotNullOfOrNull { it.albumArtist }
    )
}

@Composable
private fun GenreAlbumsSection(
    albums: List<AlbumData>,
    onViewMoreClick: () -> Unit,
    onAlbumClick: (AlbumData) -> Unit,
    playerViewModel: PlayerViewModel
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GenreSectionHeader(
            title = stringResource(R.string.library_tab_albums),
            onViewMoreClick = onViewMoreClick
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albums, key = { album -> "genre_album_${album.name}" }) { album ->
                val albumColorSchemeFlow = remember(album.artUri) {
                    playerViewModel.themeStateHolder.getAlbumColorSchemeFlow(album.artUri ?: "")
                }
                Box(modifier = Modifier.width(GenreGridCardWidth)) {
                    AlbumGridItemRedesigned(
                        album = albumModelFromAlbumData(album),
                        albumColorSchemePairFlow = albumColorSchemeFlow,
                        onClick = { onAlbumClick(album) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GenreArtistsSection(
    artists: List<Artist>,
    onViewMoreClick: () -> Unit,
    onArtistClick: (Artist) -> Unit,
    playerViewModel: PlayerViewModel
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GenreSectionHeader(
            title = stringResource(R.string.library_tab_artists),
            onViewMoreClick = onViewMoreClick
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(artists, key = { artist -> "genre_artist_${artist.id}_${artist.name}" }) { artist ->
                Box(modifier = Modifier.width(GenreGridCardWidth)) {
                    ArtistGridItem(
                        artist = artist,
                        onClick = { onArtistClick(artist) }
                    )
                }
            }
        }
    }
}
