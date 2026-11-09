@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.startune.music.presentation.screens

import com.startune.music.presentation.navigation.navigateSafely
import com.startune.music.presentation.navigation.navigateSafelyReplacing
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.startune.music.ui.theme.LocalStartuneDarkTheme
import com.startune.music.ui.theme.RoundedSans
import com.startune.music.ui.theme.StartuneStatusBarStyle
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.startune.music.data.model.Artist
import com.startune.music.data.model.Album
import com.startune.music.data.model.Song
import com.startune.music.data.model.SortOption
import com.startune.music.presentation.components.CollapsibleCommonTopBar
import com.startune.music.presentation.components.ExpressiveScrollBar
import com.startune.music.presentation.components.LibrarySortBottomSheet
import com.startune.music.presentation.components.MiniPlayerHeight
import com.startune.music.presentation.components.PlaylistBottomSheet
import com.startune.music.presentation.components.SmartImageCompactListTargetSize
import com.startune.music.presentation.components.SmartImage
import com.startune.music.presentation.components.SongInfoBottomSheet
import com.startune.music.presentation.components.resolveNavBarOccupiedHeight
import com.startune.music.presentation.navigation.Screen
import com.startune.music.presentation.viewmodel.ArtistDetailViewModel
import com.startune.music.presentation.viewmodel.ArtistAlbumSection
import com.startune.music.presentation.viewmodel.PlayerViewModel
import com.startune.music.presentation.viewmodel.PlaylistViewModel
import com.startune.music.utils.formatSongCount
import com.startune.music.utils.shapes.RoundedStarShape
import kotlinx.coroutines.launch
import com.startune.music.presentation.components.subcomps.EnhancedSongListItem
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import androidx.compose.ui.res.stringResource
import com.startune.music.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val UseSharedCollapsibleTopBarProbe = true

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArtistDetailScreen(
    artistId: String,
    navController: NavController,
    playerViewModel: PlayerViewModel,
    viewModel: ArtistDetailViewModel = hiltViewModel(),
    playlistViewModel: PlaylistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stablePlayerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()
    

    val lazyListState = rememberLazyListState()
    val favoriteIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val navBarCompactMode by playerViewModel.navBarCompactMode.collectAsStateWithLifecycle()
    var showSongInfoBottomSheet by remember { mutableStateOf(false) }
    val selectedSongForInfo by playerViewModel.selectedSongForInfo.collectAsStateWithLifecycle()
    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBarHeightDp = resolveNavBarOccupiedHeight(systemNavBarInset, navBarCompactMode)
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = LocalStartuneDarkTheme.current
    val baseColorScheme = MaterialTheme.colorScheme

    val artistColorSchemePair by viewModel.artistColorScheme.collectAsStateWithLifecycle()
    val artistColorScheme = remember(artistColorSchemePair, isDarkTheme) {
        artistColorSchemePair?.let { pair -> if (isDarkTheme) pair.dark else pair.light }
            ?: baseColorScheme
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setCustomImage(it) }
    }

    LaunchedEffect(Unit) {
        playerViewModel.collapsePlayerSheet()
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 300.dp

    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }
    val headerImageRequestSize = remember(
        configuration.screenWidthDp,
        density.density,
        maxTopBarHeightPx
    ) {
        Size(
            width = with(density) { configuration.screenWidthDp.dp.roundToPx() },
            height = maxTopBarHeightPx.roundToInt()
        )
    }

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
                    coroutineScope.launch {
                        topBarHeight.snapTo(newHeight)
                    }
                }

                val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return super.onPostFling(consumed, available)
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0

            val targetValue = if (shouldExpand && canExpand) {
                maxTopBarHeightPx
            } else {
                minTopBarHeightPx
            }

            if (topBarHeight.value != targetValue) {
                coroutineScope.launch {
                    topBarHeight.animateTo(targetValue, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = artistColorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.nestedScroll(nestedScrollConnection)) {
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
                    val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }

                    val albumSections = uiState.albumSections
                    val featuredOnSections = uiState.featuredOnSections
                    val popularTracks = uiState.popularTracks
                    var activeView by remember { mutableStateOf(ArtistDetailView.Overview) }
                    var trackSortOption by remember { mutableStateOf<SortOption>(SortOption.SongTitleAZ) }
                    var showSortSheet by remember { mutableStateOf(false) }
                    val sortedTracks = remember(songs, trackSortOption) { sortArtistSongs(songs, trackSortOption) }
                    val tracksListState = rememberLazyListState()
                    val albumsGridState = rememberLazyGridState()
                    BackHandler(enabled = activeView != ArtistDetailView.Overview) {
                        activeView = ArtistDetailView.Overview
                    }

                    val showScrollBar by remember {
                        derivedStateOf {
                            collapseFraction > 0.95f &&
                                (lazyListState.canScrollForward || lazyListState.canScrollBackward)
                        }
                    }

                    when (activeView) {
                        ArtistDetailView.Overview -> {
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset {
                                        val extraHeight =
                                            (topBarHeight.value - minTopBarHeightPx).roundToInt()
                                        IntOffset(0, extraHeight)
                                    },
                                contentPadding = PaddingValues(
                                    top = minTopBarHeight + 8.dp,
                                    start = 16.dp,
                                    end = if (showScrollBar) 24.dp else 16.dp,
                                    bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (songs.isNotEmpty()) {
                                    item(
                                        key = "artist_tracks_header",
                                        contentType = "artist_section_header"
                                    ) {
                                        ArtistSectionHeader(
                                            title = stringResource(R.string.artist_tracks_title),
                                            onViewMoreClick = { activeView = ArtistDetailView.Tracks }
                                        )
                                    }
                                    item(
                                        key = "artist_tracks_actions",
                                        contentType = "artist_section_actions"
                                    ) {
                                        ArtistPlayShuffleRow(
                                            onPlay = {
                                                val firstSong = songs.firstOrNull() ?: return@ArtistPlayShuffleRow
                                                playerViewModel.playSongs(songs, firstSong, artist.name)
                                            },
                                            onShuffle = {
                                                playerViewModel.playSongsShuffled(
                                                    songs,
                                                    artist.name,
                                                    startAtZero = true
                                                )
                                            }
                                        )
                                    }
                                    itemsIndexed(
                                        items = popularTracks,
                                        key = { _, song -> "artist_popular_${song.id}" },
                                        contentType = { _, _ -> "artist_popular_song" }
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
                                                onMoreOptionsClick = {
                                                    playerViewModel.selectSongForInfo(song)
                                                    showSongInfoBottomSheet = true
                                                },
                                                onClick = { playerViewModel.showAndPlaySong(song, songs) }
                                            )
                                        }
                                    }
                                    item(
                                        key = "artist_tracks_spacer",
                                        contentType = "artist_section_spacer"
                                    ) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }

                                if (albumSections.isNotEmpty()) {
                                    item(
                                        key = "artist_albums_header",
                                        contentType = "artist_section_header"
                                    ) {
                                        ArtistSectionHeader(
                                            title = stringResource(R.string.library_tab_albums),
                                            onViewMoreClick = { activeView = ArtistDetailView.Albums }
                                        )
                                    }
                                    item(
                                        key = "artist_albums_row",
                                        contentType = "artist_albums_row"
                                    ) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            contentPadding = PaddingValues(vertical = 8.dp)
                                        ) {
                                            items(
                                                items = albumSections,
                                                key = { section -> "artist_album_${section.albumId}" },
                                                contentType = { "artist_album_card" }
                                            ) { section ->
                                                ArtistAlbumCard(
                                                    section = section,
                                                    artistName = artist.name,
                                                    viewModel = viewModel,
                                                    modifier = Modifier.width(180.dp),
                                                    onClick = {
                                                        navController.navigateSafely(
                                                            Screen.AlbumDetail.createRoute(section.albumId)
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    item(
                                        key = "artist_albums_spacer",
                                        contentType = "artist_section_spacer"
                                    ) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }

                                if (featuredOnSections.isNotEmpty()) {
                                    item(
                                        key = "artist_featured_header",
                                        contentType = "artist_section_header"
                                    ) {
                                        ArtistSectionHeader(
                                            title = stringResource(R.string.artist_featured_on_title),
                                            onViewMoreClick = { activeView = ArtistDetailView.FeaturedOn }
                                        )
                                    }
                                    item(
                                        key = "artist_featured_row",
                                        contentType = "artist_albums_row"
                                    ) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            contentPadding = PaddingValues(vertical = 8.dp)
                                        ) {
                                            items(
                                                items = featuredOnSections,
                                                key = { section -> "artist_featured_${section.albumId}" },
                                                contentType = { "artist_album_card" }
                                            ) { section ->
                                                ArtistAlbumCard(
                                                    section = section,
                                                    artistName = artist.name,
                                                    viewModel = viewModel,
                                                    modifier = Modifier.width(180.dp),
                                                    onClick = {
                                                        navController.navigateSafely(
                                                            Screen.AlbumDetail.createRoute(section.albumId)
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    item(
                                        key = "artist_featured_spacer",
                                        contentType = "artist_section_spacer"
                                    ) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }

                                val artistBio = artist.bio
                                if (!artistBio.isNullOrBlank()) {
                                    item(
                                        key = "artist_bio",
                                        contentType = "artist_bio"
                                    ) {
                                        ArtistBioSection(bio = artistBio)
                                    }
                                }
                            }
                        }
                        ArtistDetailView.Tracks -> {
                            LazyColumn(
                                state = tracksListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset {
                                        val extraHeight =
                                            (topBarHeight.value - minTopBarHeightPx).roundToInt()
                                        IntOffset(0, extraHeight)
                                    },
                                contentPadding = PaddingValues(
                                    top = minTopBarHeight + 8.dp,
                                    start = 16.dp,
                                    end = if (showScrollBar) 24.dp else 16.dp,
                                    bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item(
                                    key = "artist_all_tracks_header",
                                    contentType = "artist_subview_header"
                                ) {
                                    ArtistSubViewHeader(
                                        title = stringResource(R.string.artist_all_tracks_title),
                                        onBackClick = { activeView = ArtistDetailView.Overview },
                                        onSortClick = { showSortSheet = true }
                                    )
                                }
                                item(
                                    key = "artist_all_tracks_actions",
                                    contentType = "artist_section_actions"
                                ) {
                                    ArtistPlayShuffleRow(
                                        onPlay = {
                                            val firstSong = sortedTracks.firstOrNull() ?: return@ArtistPlayShuffleRow
                                            playerViewModel.playSongs(sortedTracks, firstSong, artist.name)
                                        },
                                        onShuffle = {
                                            playerViewModel.playSongsShuffled(
                                                sortedTracks,
                                                artist.name,
                                                startAtZero = true
                                            )
                                        }
                                    )
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
                                            onMoreOptionsClick = {
                                                playerViewModel.selectSongForInfo(song)
                                                showSongInfoBottomSheet = true
                                            },
                                            onClick = { playerViewModel.showAndPlaySong(song, sortedTracks) }
                                        )
                                    }
                                }
                            }
                        }
                        ArtistDetailView.Albums, ArtistDetailView.FeaturedOn -> {
                            val gridSections = if (activeView == ArtistDetailView.FeaturedOn) featuredOnSections else albumSections
                            val gridTitle = if (activeView == ArtistDetailView.FeaturedOn) {
                                stringResource(R.string.artist_featured_on_title)
                            } else {
                                stringResource(R.string.library_tab_albums)
                            }
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                state = albumsGridState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset {
                                        val extraHeight =
                                            (topBarHeight.value - minTopBarHeightPx).roundToInt()
                                        IntOffset(0, extraHeight)
                                    },
                                contentPadding = PaddingValues(
                                    top = minTopBarHeight + 8.dp,
                                    start = 16.dp,
                                    end = 16.dp,
                                    bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item(span = { GridItemSpan(2) }) {
                                    ArtistSubViewHeader(
                                        title = gridTitle,
                                        onBackClick = { activeView = ArtistDetailView.Overview }
                                    )
                                }
                                gridItems(
                                    items = gridSections,
                                    key = { section -> "artist_grid_album_${section.albumId}" },
                                    contentType = { "artist_album_card" }
                                ) { section ->
                                    ArtistAlbumCard(
                                        section = section,
                                        artistName = artist.name,
                                        viewModel = viewModel,
                                        onClick = {
                                            navController.navigateSafely(
                                                Screen.AlbumDetail.createRoute(section.albumId)
                                            )
                                        }
                                    )
                                }
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

                    if (collapseFraction > 0.95f) {
                        ExpressiveScrollBar(
                            listState = lazyListState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(
                                    top = minTopBarHeight + 12.dp,
                                    bottom = MiniPlayerHeight + systemNavBarInset + 8.dp
                                )
                        )
                    }

                    if (UseSharedCollapsibleTopBarProbe) {
                        SharedArtistTopBarProbe(
                            artist = artist,
                            effectiveImageUrl = uiState.effectiveImageUrl,
                            songsCount = songs.size,
                            collapseFraction = collapseFraction,
                            headerHeight = currentTopBarHeightDp,
                            headerImageRequestSize = headerImageRequestSize,
                            hasCustomImage = !artist.customImageUri.isNullOrBlank(),
                            onBackPressed = {
                                if (activeView != ArtistDetailView.Overview) {
                                    activeView = ArtistDetailView.Overview
                                } else {
                                    navController.popBackStack()
                                }
                            },
                            onPlayClick = {
                                if (songs.isNotEmpty()) {
                                    playerViewModel.playSongsShuffled(
                                        songs,
                                        artist.name,
                                        startAtZero = true
                                    )
                                }
                            },
                            onChangeImage = { imagePickerLauncher.launch("image/*") },
                            onClearCustomImage = { viewModel.clearCustomImage() }
                        )
                    } else {
                        CustomCollapsingTopBar(
                            artist = artist,
                            effectiveImageUrl = uiState.effectiveImageUrl,
                            hasCustomImage = !artist.customImageUri.isNullOrBlank(),
                            songsCount = songs.size,
                            collapseFraction = collapseFraction,
                            headerHeight = currentTopBarHeightDp,
                            headerImageRequestSize = headerImageRequestSize,
                            onBackPressed = {
                                if (activeView != ArtistDetailView.Overview) {
                                    activeView = ArtistDetailView.Overview
                                } else {
                                    navController.popBackStack()
                                }
                            },
                            onPlayClick = {
                                if (songs.isNotEmpty()) {
                                    playerViewModel.playSongsShuffled(
                                        songs,
                                        artist.name,
                                        startAtZero = true
                                    )
                                }
                            },
                            onChangeImage = { imagePickerLauncher.launch("image/*") },
                            onClearCustomImage = { viewModel.clearCustomImage() }
                        )
                    }
                }
            }
        }
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
    }
}


@Composable
private fun SharedArtistTopBarProbe(
    artist: Artist,
    effectiveImageUrl: String?,
    songsCount: Int,
    collapseFraction: Float,
    headerHeight: Dp,
    headerImageRequestSize: Size,
    hasCustomImage: Boolean,
    onBackPressed: () -> Unit,
    onPlayClick: () -> Unit,
    onChangeImage: () -> Unit,
    onClearCustomImage: () -> Unit
) {
    var showImageMenu by remember { mutableStateOf(false) }
    val surfaceColor = MaterialTheme.colorScheme.surface
    val statusBarColor =
        if (LocalStartuneDarkTheme.current) Color.Black.copy(alpha = 0.6f)
        else Color.White.copy(alpha = 0.4f)
    val solidAlpha = (collapseFraction * 2f).coerceIn(0f, 1f)
    val expandedContentAlpha = 1f - solidAlpha
    val displayUrl = effectiveImageUrl?.takeIf { it.isNotBlank() }
    val headerOverlayBrush = remember(surfaceColor, expandedContentAlpha) {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                surfaceColor.copy(alpha = 0.24f * expandedContentAlpha),
                surfaceColor.copy(alpha = 0.84f * expandedContentAlpha),
                surfaceColor
            )
        )
    }
    val statusBarBrush = remember(statusBarColor) {
        Brush.verticalGradient(colors = listOf(statusBarColor, Color.Transparent))
    }
    val expandedStatusBarFallback = remember(statusBarColor, surfaceColor) {
        statusBarColor.compositeOver(surfaceColor)
    }
    val fallbackStatusBarColor = remember(expandedStatusBarFallback, surfaceColor, solidAlpha) {
        lerpColor(expandedStatusBarFallback, surfaceColor, solidAlpha)
    }
    val titleVerticalBias = lerp(1f, -1f, collapseFraction)
    val shuffleAlignment = BiasAlignment(horizontalBias = 1f, verticalBias = titleVerticalBias)

    StartuneStatusBarStyle(color = fallbackStatusBarColor)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .clipToBounds()
    ) {
        if (expandedContentAlpha > 0.01f) {
            if (!displayUrl.isNullOrEmpty()) {
                SmartImage(
                    model = displayUrl,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    targetSize = headerImageRequestSize,
                    allowHardware = true,
                    crossfadeDurationMillis = 0,
                    alpha = expandedContentAlpha,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                MusicIconPattern(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = expandedContentAlpha }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(headerOverlayBrush)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(statusBarBrush)
                .align(Alignment.TopCenter)
        )

        CollapsibleCommonTopBar(
            title = artist.name,
            subtitle = formatSongCount(songsCount),
            collapseFraction = collapseFraction,
            headerHeight = headerHeight,
            onBackClick = onBackPressed,
            containerColor = surfaceColor.copy(alpha = solidAlpha),
            collapsedTitleStartPadding = 68.dp,
            expandedTitleStartPadding = 24.dp,
            collapsedTitleEndPadding = 88.dp,
            expandedTitleEndPadding = 136.dp,
            containerHeightRange = 112.dp to 56.dp,
            titleStyle = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = RoundedSans,
                fontWeight = FontWeight.SemiBold,
                textGeometricTransform = TextGeometricTransform(scaleX = 1.08f)
            ),
            titleScaleRange = 1f to 1f,
            titleFontSizeRange = 30.sp to 18.sp,
            maxLines = if (collapseFraction < 0.5f) 2 else 1,
            collapsedSubtitleMaxLines = 1,
            expandedSubtitleMaxLines = 2,
            contentColor = MaterialTheme.colorScheme.onSurface,
            subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant,
            fadeSubtitleOnCollapse = false,
            syncStatusBarWithContainer = false,
            actions = {
                Box(
                    modifier = Modifier.padding(end = 12.dp, top = 4.dp)
                ) {
                    FilledIconButton(
                        onClick = { showImageMenu = true },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.presentation_batch_d_edit_artist_image_cd)
                        )
                    }

                    DropdownMenu(
                        expanded = showImageMenu,
                        onDismissRequest = { showImageMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.presentation_batch_d_change_photo)) },
                            leadingIcon = {
                                Icon(Icons.Rounded.AddAPhoto, contentDescription = null)
                            },
                            onClick = {
                                showImageMenu = false
                                onChangeImage()
                            }
                        )
                        if (hasCustomImage) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.presentation_batch_d_reset_to_default)) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Delete, contentDescription = null)
                                },
                                onClick = {
                                    showImageMenu = false
                                    onClearCustomImage()
                                }
                            )
                        }
                    }
                }
            }
        )

        LargeExtendedFloatingActionButton(
            onClick = onPlayClick,
            shape = RoundedStarShape(sides = 8, curve = 0.05, rotation = 0f),
            modifier = Modifier
                .align(shuffleAlignment)
                .statusBarsPadding()
                .padding(end = 16.dp)
                .graphicsLayer {
                    scaleX = expandedContentAlpha
                    scaleY = expandedContentAlpha
                    alpha = expandedContentAlpha
                }
        ) {
            Icon(Icons.Rounded.Shuffle, contentDescription = stringResource(R.string.presentation_batch_d_cd_shuffle_play_artist))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CustomCollapsingTopBar(
    artist: Artist,
    effectiveImageUrl: String?,
    hasCustomImage: Boolean,
    songsCount: Int,
    collapseFraction: Float,
    headerHeight: Dp,
    headerImageRequestSize: Size,
    onBackPressed: () -> Unit,
    onPlayClick: () -> Unit,
    onChangeImage: () -> Unit,
    onClearCustomImage: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val statusBarColor = if (LocalStartuneDarkTheme.current) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.4f)

    val fabScale = 1f - collapseFraction
    val backgroundAlpha = collapseFraction
    val headerContentAlpha = 1f - (collapseFraction * 2).coerceAtMost(1f)
    val showExpandedArtwork = headerContentAlpha > 0.01f
    val headerOverlayBrush = remember(surfaceColor, headerContentAlpha) {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                surfaceColor.copy(alpha = 0.30f * headerContentAlpha),
                surfaceColor.copy(alpha = 0.90f * headerContentAlpha),
                surfaceColor.copy(alpha = headerContentAlpha)
            )
        )
    }
    val statusBarBrush = remember(statusBarColor) {
        Brush.verticalGradient(
            colors = listOf(
                statusBarColor,
                Color.Transparent
            )
        )
    }
    val solidAlpha = (collapseFraction * 2f).coerceIn(0f, 1f)
    val expandedStatusBarFallback = remember(statusBarColor, surfaceColor) {
        statusBarColor.compositeOver(surfaceColor)
    }
    val fallbackStatusBarColor = remember(expandedStatusBarFallback, surfaceColor, solidAlpha) {
        lerpColor(expandedStatusBarFallback, surfaceColor, solidAlpha)
    }

    val titleScale = lerp(1f, 0.75f, collapseFraction)
    val titlePaddingStart = lerp(24.dp, 58.dp, collapseFraction)
    val titleMaxLines = if(collapseFraction < 0.5f) 2 else 1
    val titleVerticalBias = lerp(1f, -1f, collapseFraction)
    val animatedTitleAlignment = BiasAlignment(horizontalBias = -1f, verticalBias = titleVerticalBias)
    val titleContainerHeight = lerp(88.dp, 56.dp, collapseFraction)
    val yOffsetCorrection = lerp( (titleContainerHeight / 2) - 64.dp, 0.dp, collapseFraction)


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .clipToBounds()
    ) {
        StartuneStatusBarStyle(color = fallbackStatusBarColor)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .background(surfaceColor.copy(alpha = backgroundAlpha))
        ) {
            if (showExpandedArtwork) {
                val displayUrl = effectiveImageUrl?.takeIf { it.isNotBlank() }
                if (!displayUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(displayUrl)
                            .size(headerImageRequestSize)
                            .allowHardware(true)
                            .crossfade(false)
                            .build(),
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        alpha = headerContentAlpha,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MusicIconPattern(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = headerContentAlpha }
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(headerOverlayBrush)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(statusBarBrush)
                    .align(Alignment.TopCenter)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                FilledIconButton(
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 4.dp),
                    onClick = onBackPressed,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.auth_cd_back))
                }

                if (collapseFraction < 0.5f) {
                    var showImageMenu by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp, top = 4.dp)
                            .graphicsLayer { alpha = 1f - (collapseFraction * 4).coerceAtMost(1f) }
                    ) {
                        SmallFloatingActionButton(
                            onClick = { showImageMenu = true },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.presentation_batch_d_edit_artist_image_cd))
                        }
                        DropdownMenu(
                            expanded = showImageMenu,
                            onDismissRequest = { showImageMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.presentation_batch_d_change_photo)) },
                                leadingIcon = { Icon(Icons.Rounded.AddAPhoto, contentDescription = null) },
                                onClick = {
                                    showImageMenu = false
                                    onChangeImage()
                                }
                            )
                            if (hasCustomImage) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.presentation_batch_d_reset_to_default)) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                                    onClick = {
                                        showImageMenu = false
                                        onClearCustomImage()
                                    }
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(animatedTitleAlignment)
                        .height(titleContainerHeight)
                        .fillMaxWidth()
                        .offset(y = yOffsetCorrection)
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = titlePaddingStart, end = 120.dp)
                            .graphicsLayer {
                                scaleX = titleScale
                                scaleY = titleScale
                            },
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 26.sp,
                                textGeometricTransform = TextGeometricTransform(scaleX = 1.2f),
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = titleMaxLines,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = formatSongCount(songsCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                LargeExtendedFloatingActionButton(
                    onClick = onPlayClick,
                    shape = RoundedStarShape(sides = 8, curve = 0.05, rotation = 0f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .graphicsLayer {
                            scaleX = fabScale
                            scaleY = fabScale
                            alpha = fabScale
                        }
                ) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = stringResource(R.string.cd_shuffle_play_album))
                }
            }
        }
    }
}

@Composable
private fun MusicIconPattern(modifier: Modifier = Modifier) {
    val color1 = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
    val color2 = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    Box(modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null, tint = color1,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 76.dp, y = 72.dp)
                .size(60.dp)
                .graphicsLayer { rotationZ = -12f }
        )
        Icon(
            imageVector = Icons.Default.GraphicEq,
            contentDescription = null, tint = color1,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 4.dp, y = 78.dp)
                .size(50.dp)
                .graphicsLayer { rotationZ = 18f }
        )
        Icon(
            imageVector = Icons.Rounded.Album,
            contentDescription = null, tint = color2,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 4.dp, y = (-70).dp)
                .size(70.dp)
                .graphicsLayer { rotationZ = 12f }
        )
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = null, tint = color1,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = 14.dp, y = (-6).dp)
                .size(60.dp)
                .graphicsLayer { rotationZ = 14f }
        )
        Icon(
            imageVector = Icons.Rounded.SurroundSound,
            contentDescription = null, tint = color2,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (-24).dp, y = 32.dp)
                .size(80.dp)
                .graphicsLayer { rotationZ = 10f }
        )
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null, tint = color1,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-18).dp, y = (-138).dp)
                .size(45.dp)
                .graphicsLayer { rotationZ = -18f }
        )
        Icon(
            imageVector = Icons.Rounded.Headphones,
            contentDescription = null, tint = color2,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 72.dp, y = 34.dp)
                .size(45.dp)
                .graphicsLayer { rotationZ = -8f }
        )
    }
}


@Composable
private fun ArtistBioSection(bio: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.artist_info_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = bio,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private enum class ArtistDetailView {
    Overview, Tracks, Albums, FeaturedOn
}

private fun sortArtistSongs(songs: List<Song>, sortOption: SortOption): List<Song> =
    when (sortOption) {
        SortOption.SongDefaultOrder -> songs
        SortOption.SongTitleAZ -> songs.sortedBy { it.title.lowercase() }
        SortOption.SongTitleZA -> songs.sortedByDescending { it.title.lowercase() }
        SortOption.SongArtist -> songs.sortedBy { it.displayArtist.lowercase() }
        SortOption.SongArtistDesc -> songs.sortedByDescending { it.displayArtist.lowercase() }
        SortOption.SongAlbum -> songs.sortedBy { it.album.lowercase() }
        SortOption.SongAlbumDesc -> songs.sortedByDescending { it.album.lowercase() }
        SortOption.SongDateAdded -> songs.sortedByDescending { it.dateAdded }
        SortOption.SongDateAddedAsc -> songs.sortedBy { it.dateAdded }
        SortOption.SongDuration -> songs.sortedByDescending { it.duration }
        SortOption.SongDurationAsc -> songs.sortedBy { it.duration }
        else -> songs
    }

@Composable
private fun ArtistSectionHeader(
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
private fun ArtistSubViewHeader(
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
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.auth_cd_back)
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
            IconButton(onClick = onSortClick) {
                Icon(
                    imageVector = Icons.Filled.Sort,
                    contentDescription = stringResource(R.string.presentation_batch_d_sort_by)
                )
            }
        }
    }
}

@Composable
private fun ArtistPlayShuffleRow(
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LargeExtendedFloatingActionButton(
            onClick = onPlay,
            shape = RoundedStarShape(sides = 8, curve = 0.05, rotation = 0f)
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.play_playback)
            )
        }
        LargeExtendedFloatingActionButton(
            onClick = onShuffle,
            shape = RoundedStarShape(sides = 8, curve = 0.05, rotation = 0f)
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = stringResource(R.string.shortcut_shuffle_short)
            )
        }
    }
}

@Composable
private fun ArtistAlbumCard(
    section: ArtistAlbumSection,
    artistName: String,
    viewModel: ArtistDetailViewModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val album = remember(section, artistName) {
        Album(
            id = section.albumId,
            title = section.title,
            artist = artistName,
            year = section.year ?: 0,
            dateAdded = 0,
            albumArtUriString = section.albumArtUriString,
            songCount = section.songs.size,
            albumArtist = section.songs.firstNotNullOfOrNull { it.albumArtist }
        )
    }
    val albumColorSchemeFlow = remember(album.albumArtUriString) {
        viewModel.themeStateHolder.getAlbumColorSchemeFlow(album.albumArtUriString ?: "")
    }
    Box(modifier = modifier) {
        AlbumGridItemRedesigned(
            album = album,
            albumColorSchemePairFlow = albumColorSchemeFlow,
            onClick = onClick
        )
    }
}
