package com.startune.music.presentation.screens

import com.startune.music.presentation.navigation.navigateSafely
import com.startune.music.presentation.navigation.navigateSafelyReplacing
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.startune.music.ui.theme.ShapeCache

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
                            GenreSubViewHeader(
                                title = stringResource(R.string.top_songs_title),
                                onBackClick = { activeView = GenreDetailView.Overview }
                            )
                        }
                                                itemsIndexed(
                            items = uiState.rankedSongs,
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
                                    onMoreOptionsClick = { showSongOptionsSheet = it },
                                    onClick = {
                                        playerViewModel.showAndPlaySong(song, uiState.rankedSongs, genreDisplayName)
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
                            GenreSubViewHeader(
                                title = stringResource(R.string.library_tab_albums),
                                onBackClick = { activeView = GenreDetailView.Overview }
                            )
                        }
                        gridItems(
                            items = uiState.albums,
                            key = { album -> "genre_grid_album_${album.name}" }
                        ) { album ->
                            GenreMediaCard(
                                imageUri = album.artUri,
                                title = album.name,
                                subtitle = album.songs.firstOrNull()?.displayArtist ?: "",
                                onClick = {
                                    navController.navigateSafely(
                                        com.startune.music.presentation.navigation.Screen.AlbumDetail.createRoute(
                                            album.songs.firstOrNull()?.albumId ?: -1L
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                imageAspectRatio = 3f / 2f
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
                            GenreSubViewHeader(
                                title = stringResource(R.string.library_tab_artists),
                                onBackClick = { activeView = GenreDetailView.Overview }
                            )
                        }
                        gridItems(
                            items = uiState.artists,
                            key = { artist -> "genre_grid_artist_${artist.id}_${artist.name}" }
                        ) { artist ->
                            GenreMediaCard(
                                imageUri = artist.imageUrl,
                                title = artist.name,
                                subtitle = formatSongCount(artist.songCount),
                                onClick = {
                                    navController.navigateSafely(
                                        com.startune.music.presentation.navigation.Screen.ArtistDetail.createRoute(artist.id)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                imageAspectRatio = 1f,
                                imageShape = ShapeCache.expressiveAvatar
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
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
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

@Composable
private fun GenreAlbumsSection(
    albums: List<AlbumData>,
    onViewMoreClick: () -> Unit,
    onAlbumClick: (AlbumData) -> Unit
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
                GenreMediaCard(
                    imageUri = album.artUri,
                    title = album.name,
                    subtitle = album.songs.firstOrNull()?.displayArtist ?: "",
                    onClick = { onAlbumClick(album) },
                    modifier = Modifier.width(GenreGridCardWidth),
                    imageAspectRatio = 3f / 2f
                )
            }
        }
    }
}

@Composable
private fun GenreArtistsSection(
    artists: List<Artist>,
    onViewMoreClick: () -> Unit,
    onArtistClick: (Artist) -> Unit
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
                GenreMediaCard(
                    imageUri = artist.imageUrl,
                    title = artist.name,
                    subtitle = formatSongCount(artist.songCount),
                    onClick = { onArtistClick(artist) },
                    modifier = Modifier.width(GenreGridCardWidth),
                    imageAspectRatio = 1f,
                    imageShape = ShapeCache.expressiveAvatar
                )
            }
        }
    }
}

@Composable
private fun GenreMediaCard(
    imageUri: String?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageAspectRatio: Float = 1f,
    imageShape: Shape = RoundedCornerShape(12.dp)
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SmartImage(
            model = imageUri,
            contentDescription = null,
            shape = imageShape,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(imageAspectRatio)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
