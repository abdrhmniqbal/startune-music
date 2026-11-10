package com.startune.music.presentation.screens

import com.startune.music.presentation.navigation.navigateSafely

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.startune.music.utils.traceSection
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.Constraints
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
import com.startune.music.presentation.components.getIconByName
import com.startune.music.presentation.components.getThemeContentColor
import com.startune.music.presentation.components.threeShapeSwitch
import com.startune.music.presentation.components.resolveNavBarOccupiedHeight
import com.startune.music.presentation.navigation.Screen
import com.startune.music.presentation.viewmodel.MainViewModel
import com.startune.music.presentation.viewmodel.MixIds
import com.startune.music.presentation.viewmodel.PlayerViewModel
import com.startune.music.presentation.viewmodel.PlaylistViewModel
import com.startune.music.utils.formatDuration
import com.startune.music.utils.shapes.RoundedStarShape
import com.startune.music.ui.theme.ShapeCache
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import com.startune.music.presentation.components.subcomps.EnhancedSongListItem
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    val playItLabel = stringResource(R.string.presentation_batch_b_play_it)
    val shuffleLabel = stringResource(R.string.shortcut_shuffle_short)
    val forYouMixSongCount by playerViewModel.forYouMixSongCount.collectAsStateWithLifecycle()
    val mixSizeCount = if (isForYouMix) forYouMixSongCount else (userMix?.mixSongCount ?: 60)
    var showMixSizeDialog by remember { mutableStateOf(false) }

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
            removeFromListTrigger = removeFromListTrigger
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
                        coverIconName = userMix?.coverIconName
                    )
                }

                item(key = "play_shuffle_buttons") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (mixSongs.isNotEmpty()) {
                                    playerViewModel.playSongs(mixSongs, mixSongs.first(), mixTitle)
                                    if (isShuffleEnabled) playerViewModel.toggleShuffle()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(76.dp),
                            enabled = mixSongs.isNotEmpty(),
                            shape = RoundedCornerShape(
                                topStart = 60.dp,
                                topEnd = 14.dp,
                                bottomStart = 60.dp,
                                bottomEnd = 14.dp
                            )
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.cd_play), modifier = Modifier.size(
                                ButtonDefaults.IconSize))
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(playItLabel)
                        }
                        FilledTonalButton(
                            onClick = {
                                if (mixSongs.isNotEmpty()) {
                                    playerViewModel.playSongsShuffled(
                                        songsToPlay = mixSongs,
                                        queueName = mixTitle,
                                        startAtZero = true,
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(76.dp),
                            enabled = mixSongs.isNotEmpty(),
                            shape = RoundedCornerShape(
                                topStart = 14.dp,
                                topEnd = 60.dp,
                                bottomStart = 14.dp,
                                bottomEnd = 60.dp
                            )
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = shuffleLabel, modifier = Modifier.size(
                                ButtonDefaults.IconSize))
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(shuffleLabel)
                        }
                    }
                }

                if (isForYouMix || isUserMix) {
                    item(key = "mix_size") {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showMixSizeDialog = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(16.dp).fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.mix_detail_size_title),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = stringResource(R.string.mix_detail_size_songs, mixSizeCount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(mixSongs, key = { it.id }) { song ->
                    EnhancedSongListItem(
                        modifier = Modifier
                            .padding(horizontal = 16.dp),
                        song = song,
                        isCurrentSong = currentSongId == song.id,
                        isPlaying = currentSongId == song.id && isPlaying,
                        onClick = { playerViewModel.showAndPlaySong(song, mixSongs, mixTitle, isVoluntaryPlay = false) },
                        onMoreOptionsClick = {
                            playerViewModel.selectSongForInfo(song)
                            showSongInfoSheet = true
                        }
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
    }

    if (showMixSizeDialog) {
        MixSizeDialog(
            currentCount = mixSizeCount,
            onDismiss = { showMixSizeDialog = false },
            onConfirm = { count ->
                showMixSizeDialog = false
                if (isForYouMix) playerViewModel.setForYouMixSongCount(count)
                else playlistViewModel.setMixSongCount(mixId, count)
            }
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
    coverIconName: String? = null
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (coverImageUri == null && coverColorArgb == null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy((-80).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                albumArts.forEachIndexed { index, artUrl ->
                    val size = when (index) {
                        0 -> 180.dp
                        1 -> 220.dp
                        2 -> 180.dp
                        else -> 150.dp
                    }
                    val rotation = when (index) {
                        0 -> -15f
                        1 -> 0f
                        2 -> 15f
                        else -> 0f
                    }
                    val shape = threeShapeSwitch(index, thirdShapeCornerRadius = 30.dp)

                    if (index == 2) {
                        Box(
                            modifier = Modifier.layout { measurable, constraints ->
                                val placeable = measurable.measure(
                                    Constraints.fixed(width = size.roundToPx(), height = size.roundToPx())
                                )
                                layout(constraints.maxWidth, placeable.height) {
                                    val xOffset = (constraints.maxWidth - placeable.width) / 2
                                    placeable.placeRelative(xOffset, 0)
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .graphicsLayer { rotationZ = rotation }
                                    .clip(shape)
                            ) {
                                SmartImage(
                                    model = artUrl ?: R.drawable.rounded_album_24,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(size)
                                .graphicsLayer { rotationZ = rotation }
                                .clip(shape)
                        ) {
                            SmartImage(
                                model = artUrl ?: R.drawable.rounded_album_24,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
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
                    .padding(start = 6.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = title,
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
private fun MixSizeDialog(
    currentCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var sliderValue by remember(currentCount) { mutableFloatStateOf(currentCount.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mix_detail_size_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.mix_detail_size_songs, sliderValue.toInt()),
                    style = MaterialTheme.typography.titleMedium
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 10f..100f,
                    steps = 17
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(sliderValue.toInt()) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
