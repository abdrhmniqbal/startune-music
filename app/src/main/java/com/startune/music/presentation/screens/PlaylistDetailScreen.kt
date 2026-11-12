package com.startune.music.presentation.screens

import kotlinx.collections.immutable.toImmutableList
import com.startune.music.presentation.components.PlaylistCover
import com.startune.music.presentation.components.AutoScrollingText
import androidx.compose.ui.platform.LocalWindowInfo
import android.widget.Toast
import com.startune.music.presentation.navigation.navigateSafely
import com.startune.music.presentation.navigation.navigateSafelyReplacing

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startune.music.ui.theme.LocalStartuneDarkTheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.startune.music.presentation.components.subcomps.TightWrapText
import androidx.compose.ui.unit.dp
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil.size.Size
import com.startune.music.R
import com.startune.music.data.model.Song
import com.startune.music.presentation.components.MiniPlayerHeight
import com.startune.music.presentation.components.PlaylistBottomSheet
import com.startune.music.presentation.components.QueuePlaylistSongItem
import com.startune.music.presentation.components.SongPickerBottomSheet
import com.startune.music.presentation.components.ExpressiveScrollBar
import com.startune.music.presentation.components.SmartImage
import com.startune.music.presentation.components.SongInfoBottomSheet
import com.startune.music.presentation.components.SongMultiSelectionHost
import com.startune.music.presentation.components.subcomps.SelectionActionRow
import com.startune.music.presentation.components.subcomps.SelectionCountPill
import androidx.compose.ui.zIndex
import com.startune.music.presentation.components.resolveNavBarOccupiedHeight
import com.startune.music.presentation.navigation.Screen
import com.startune.music.presentation.viewmodel.PlayerViewModel
import com.startune.music.presentation.viewmodel.PlaylistViewModel
import com.startune.music.presentation.viewmodel.CloudDownloadsViewModel
import com.startune.music.data.offline.CloudOfflineRepository
import com.startune.music.presentation.viewmodel.PlaylistViewModel.Companion.FOLDER_PLAYLIST_PREFIX
import com.startune.music.presentation.utils.LocalAppHapticsConfig
import com.startune.music.presentation.utils.getSegmentedItemShape
import com.startune.music.presentation.utils.performAppCompatHapticFeedback
import com.startune.music.presentation.viewmodel.PlaylistSongsOrderMode
import com.startune.music.utils.formatSongCount
import com.startune.music.utils.formatTotalDuration
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.startune.music.presentation.components.LibrarySortBottomSheet
import com.startune.music.data.model.SortOption
import com.startune.music.data.model.PlaylistShapeType
import com.startune.music.data.model.isMix
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.startune.music.presentation.components.rememberModalSheetState
import kotlinx.collections.immutable.persistentListOf
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import com.startune.music.presentation.components.CollapsibleCommonTopBar
import kotlin.math.roundToInt
import androidx.compose.ui.text.style.TextAlign

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    onBackClick: () -> Unit,
    onDeletePlayListClick: () -> Unit,
    playerViewModel: PlayerViewModel,
    playlistViewModel: PlaylistViewModel = hiltViewModel(),
    cloudDownloadsViewModel: CloudDownloadsViewModel = hiltViewModel(),
    navController: NavController
) {
    val uiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val playerStableState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()
    val hasCurrentSong by remember {
        derivedStateOf { playerStableState.currentSong != null }
    }
    val context = LocalContext.current
    val fallbackPlaylistName = stringResource(R.string.shortcut_playlist_short)
    val sortSongsLabel = stringResource(R.string.presentation_batch_b_sort_songs)
    val moreOptionsLabel = stringResource(R.string.presentation_batch_b_more_options)
    val confirmLabel = stringResource(R.string.confirm)
    val shuffleLabel = stringResource(R.string.shortcut_shuffle_short)
    val playItLabel = stringResource(R.string.presentation_batch_b_play_it)
    val addSongsCd = stringResource(R.string.presentation_batch_b_add_songs)
    val removeSongsCd = stringResource(R.string.presentation_batch_b_remove_songs)
    val reorderSongsCd = stringResource(R.string.presentation_batch_b_reorder_songs)
    val reorderSongCd = stringResource(R.string.presentation_batch_b_reorder_song)
    val playlistEmptyTitle = stringResource(R.string.presentation_batch_b_playlist_empty_title)
    val playlistEmptyFolder = stringResource(R.string.presentation_batch_b_playlist_empty_folder_body)
    val playlistEmptyAddHint = stringResource(R.string.presentation_batch_b_playlist_empty_add_hint)
    val playlistOptionsTitle = stringResource(R.string.presentation_batch_b_playlist_options_title)
    val editPlaylistLabel = stringResource(R.string.presentation_batch_b_edit_playlist)
    val deletePlaylistLabel = stringResource(R.string.presentation_batch_b_delete_playlist)
    val setDefaultTransitionLabel = stringResource(R.string.presentation_batch_b_set_default_transition)
    val exportPlaylistLabel = stringResource(R.string.presentation_batch_b_export_playlist)
    val downloadPlaylistLabel = stringResource(R.string.cloud_playlist_download)
    val deletePlaylistConfirmTitle = stringResource(R.string.presentation_batch_b_delete_playlist_confirm_title)
    val deletePlaylistConfirmBody = stringResource(R.string.presentation_batch_b_delete_playlist_confirm_body)
    val sortSheetTitle = stringResource(R.string.presentation_batch_b_sort_songs)
    val toastAddedToQueue = stringResource(R.string.toast_added_to_queue)
    val toastPlayingNext = stringResource(R.string.toast_playing_next)
    val currentPlaylist = uiState.currentPlaylistDetails
    val isFolderPlaylist = currentPlaylist?.id?.startsWith(FOLDER_PLAYLIST_PREFIX) == true
    val isMix = currentPlaylist?.isMix == true
    val isEditablePlaylist = !isFolderPlaylist && !isMix
    val songsInPlaylist = uiState.currentPlaylistSongs
    val cloudSongsInPlaylist = remember(songsInPlaylist) {
        CloudOfflineRepository.downloadCandidates(songsInPlaylist)
    }

    LaunchedEffect(playlistId) {
        playlistViewModel.loadPlaylistDetails(playlistId)
    }

    var showAddSongsSheet by remember { mutableStateOf(false) }

    var isReorderModeEnabled by remember { mutableStateOf(false) }
    var isRemoveModeEnabled by remember { mutableStateOf(false) }
    var showSongInfoBottomSheet by remember { mutableStateOf(false) }
    var showPlaylistOptionsSheet by remember { mutableStateOf(false) }
    var showEditPlaylistDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val m3uExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        uri?.let {
            currentPlaylist?.let { playlist ->
                playlistViewModel.exportM3u(playlist, it, context)
            }
        }
    }

    val selectedSongForInfo by playerViewModel.selectedSongForInfo.collectAsStateWithLifecycle()
    val favoriteIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val stableOnMoreOptionsClick: (Song) -> Unit = remember {
        { song ->
            playerViewModel.selectSongForInfo(song)
            showSongInfoBottomSheet = true
        }
    }
    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navBarCompactMode by playerViewModel.navBarCompactMode.collectAsStateWithLifecycle()
    val bottomBarHeightDp = resolveNavBarOccupiedHeight(systemNavBarInset, navBarCompactMode)
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }

    val multiSelectionState = playerViewModel.multiSelectionStateHolder
    val selectedSongs by multiSelectionState.selectedSongs.collectAsStateWithLifecycle()
    val isSelectionMode by multiSelectionState.isSelectionMode.collectAsStateWithLifecycle()
    val selectedSongIds by multiSelectionState.selectedSongIds.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var showMultiSelectionSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) { multiSelectionState.clearSelection() }
    DisposableEffect(Unit) { onDispose { multiSelectionState.clearSelection() } }
    // The initial position identifies an occurrence, even when the same song appears twice.
    // Move the entry with its key so dragging never changes a row's identity.
    var localReorderableEntries by remember(songsInPlaylist) {
        mutableStateOf(songsInPlaylist.withIndex().toList())
    }
    val localReorderableSongs = remember(localReorderableEntries) {
        localReorderableEntries.map { it.value }
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val appHapticsConfig = LocalAppHapticsConfig.current
    var lastMovedFrom by remember { mutableStateOf<Int?>(null) }
    var lastMovedTo by remember { mutableStateOf<Int?>(null) }

    val reorderableState = rememberReorderableLazyListState(
        lazyListState = listState,
        onMove = { from, to ->
            // The lazy list has a selection-row header item at index 0, so the
            // library's absolute lazy indices are NOT song positions: dragging
            // the last song reported index == size and crashed in removeAt.
            // Resolve by the entry key instead, like QueueBottomSheet does.
            fun resolveKeyToIndex(key: Any?): Int? {
                if (key !is Int) return null
                val index = localReorderableEntries.indexOfFirst { it.index == key }
                return index.takeIf { it >= 0 }
            }
            val fromLocalIndex = resolveKeyToIndex(from.key)
                ?: return@rememberReorderableLazyListState
            // Dropping above the first song targets the header row: that means top.
            val toLocalIndex = resolveKeyToIndex(to.key)
                ?: if (to.key == "playlist_selection_row") 0
                else return@rememberReorderableLazyListState
            if (fromLocalIndex == toLocalIndex) return@rememberReorderableLazyListState
            localReorderableEntries = localReorderableEntries.toMutableList().apply {
                add(toLocalIndex, removeAt(fromLocalIndex))
            }
            if (lastMovedFrom == null) {
                lastMovedFrom = fromLocalIndex
            }
            lastMovedTo = toLocalIndex
        }
    )

    LaunchedEffect(reorderableState.isAnyItemDragging, isEditablePlaylist) {
        if (isEditablePlaylist && !reorderableState.isAnyItemDragging && lastMovedFrom != null && lastMovedTo != null) {
            currentPlaylist?.let {
                playlistViewModel.reorderSongsInPlaylist(it.id, lastMovedFrom!!, lastMovedTo!!)
            }
            lastMovedFrom = null
            lastMovedTo = null
        } else if (!isEditablePlaylist && !reorderableState.isAnyItemDragging) {
            lastMovedFrom = null
            lastMovedTo = null
        }
    }

    val density = LocalDensity.current

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // The cover would leave no room for the songs on short (landscape phone) windows.
    val hasRoomForCover = LocalWindowInfo.current.containerDpSize.height >= 560.dp
    // Header is exactly nav + cover + title block, no slack: the 92.dp
    // title container centers the ~60.dp title block, so ~16.dp sits
    // below the subtitle (the buttons start right under the header);
    // the cover's bottom edge sits 88.dp above the bottom.
    val minTopBarHeight = 62.dp + statusBarHeight
    // Expanded header must clear the nav row above the cover: upstream's
    // TwoRowsTopAppBar puts its first row (nav + actions) above the title
    // column, so the cover starts below it. The fork's cover is positioned
    // from the bottom (88.dp padding + 168.dp cover above the ~76.dp title
    // block), so its top sits exactly below the nav row, like upstream.
    // 318.dp + status bar = nav row (62.dp + status bar) + cover + 12.dp
    // gap + title block + 16.dp below the subtitle.
    val maxTopBarHeight = if (hasRoomForCover) 318.dp + statusBarHeight else 200.dp
    // The same first songs the library thumbnail uses, so the collage matches it.
    val coverSongs = remember(currentPlaylist?.songIds, songsInPlaylist) {
        currentPlaylist?.songIds.orEmpty().take(4)
            .mapNotNull { id -> songsInPlaylist.firstOrNull { it.id == id } }
            .toImmutableList()
    }

    // Page colors come from the playlist artwork: the custom cover
    // image when set, otherwise the first song art of the collage.
    // A solid custom cover color has no image, so the page keeps
    // the default colors.
    val isDarkTheme = LocalStartuneDarkTheme.current
    val baseColorScheme = MaterialTheme.colorScheme
    val useArtworkColorForDetails by playerViewModel.useArtworkColorForDetails.collectAsStateWithLifecycle()
    val playlistArtUri = remember(currentPlaylist, coverSongs) {
        currentPlaylist?.coverImageUri?.takeIf { it.isNotBlank() }
            ?: coverSongs.firstNotNullOfOrNull { it.albumArtUriString?.takeIf { uri -> uri.isNotBlank() } }
    }
    val playlistColorSchemeFlow = remember(playlistArtUri) {
        playlistArtUri?.let { playerViewModel.themeStateHolder.getAlbumColorSchemeFlow(it) }
    }
    val playlistColorSchemePair = playlistColorSchemeFlow?.collectAsStateWithLifecycle()?.value
    val playlistColorScheme = remember(playlistColorSchemePair, isDarkTheme, baseColorScheme, useArtworkColorForDetails) {
        if (!useArtworkColorForDetails) baseColorScheme
        else playlistColorSchemePair?.let { pair -> if (isDarkTheme) pair.dark else pair.light }
            ?: baseColorScheme
    }

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

                if (!scrollingDown && (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0)) {
                    return Offset.Zero
                }

                val previousHeight = topBarHeight.value
                val newHeight = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - previousHeight

                if (consumed.roundToInt() != 0) {
                    scope.launch {
                        topBarHeight.snapTo(newHeight)
                    }
                }

                val canConsume = !(scrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsume) Offset(0f, consumed) else Offset.Zero
            }
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            val target = if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx

            if (topBarHeight.value != target) {
                scope.launch {
                    topBarHeight.animateTo(target, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }
    val solidAlpha = (collapseFraction * 2f).coerceIn(0f, 1f)
    val expandedContentAlpha = 1f - solidAlpha


    MaterialTheme(
        colorScheme = playlistColorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
        Scaffold(
            modifier = Modifier
                .nestedScroll(nestedScrollConnection),
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(currentTopBarHeightDp)
                        .clipToBounds()
                ) {
                    CollapsibleCommonTopBar(
                        title = currentPlaylist?.name ?: fallbackPlaylistName,
                        // Collapsed the title is one line with an ellipsis, not the
                        // marquee: the marquee never truncates, so a long title ran
                        // underneath the action buttons. Expanded keeps the marquee.
                        titleContent = if (collapseFraction < 0.5f) {
                            { style ->
                                AutoScrollingText(
                                    text = currentPlaylist?.name ?: fallbackPlaylistName,
                                    style = style,
                                    textAlign = TextAlign.Center,
                                    gradientEdgeColor = MaterialTheme.colorScheme.surface
                                )
                            }
                        } else {
                            null
                        },
                        // The actions row (Sort 48.dp + More 48.dp + 8.dp spacing +
                        // 12.dp end padding = 116.dp; folder playlists have only
                        // Sort = 60.dp) sits on top of the title, so the collapsed
                        // title must end before it, plus an 8.dp gap.
                        collapsedTitleEndPadding = if (isFolderPlaylist) 68.dp else 124.dp,
                        subtitle = stringResource(
                            R.string.presentation_batch_f_status_bullet_step,
                            formatSongCount(songsInPlaylist.size),
                            formatTotalDuration(songsInPlaylist)
                        ),
                        collapseFraction = collapseFraction,
                        headerHeight = currentTopBarHeightDp,
                        onBackClick = onBackClick,
                        expandedTitleStartPadding = 24.dp,
                        centerExpandedTitle = true,
                        titleFontSizeRange = 30.sp to 28.sp,
                        maxLines = if (collapseFraction < 0.5f) Int.MAX_VALUE else 1,
                        containerHeightRange = 92.dp to 56.dp,
                        actions = {
                            Row(
                                modifier = Modifier.padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        playerViewModel.showSortingSheet()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Sort,
                                        contentDescription = sortSongsLabel
                                    )
                                }
                                if (!isFolderPlaylist) {
                                    FilledTonalIconButton(
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ),
                                        onClick = {
                                            if (isReorderModeEnabled) {
                                                isReorderModeEnabled = false
                                            } else if (isRemoveModeEnabled) {
                                                isRemoveModeEnabled = false
                                            } else {
                                                showPlaylistOptionsSheet = true
                                            }
                                        }
                                    ) {
                                        if (isReorderModeEnabled || isRemoveModeEnabled) {
                                            Icon(Icons.Rounded.Check, confirmLabel)
                                        } else {
                                            Icon(Icons.Filled.MoreVert, moreOptionsLabel)
                                        }
                                    }
                                }
                            }
                        }
                    )
                    // Centered cover above the expanded title (upstream dff8a8c:
                    // 168.dp, only when expanded and the window is tall enough).
                    val playlistForCover = currentPlaylist
                    if (hasRoomForCover && playlistForCover != null) {
                        PlaylistCover(
                            playlist = playlistForCover,
                            playlistSongs = coverSongs,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 88.dp)
                                .graphicsLayer { alpha = expandedContentAlpha },
                            size = 168.dp
                        )
                    }
                }
            }
        ) { innerPadding ->
            if (uiState.isLoading && currentPlaylist == null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding()), Alignment.Center
                ) { ContainedLoadingIndicator() }
            } else if (uiState.playlistNotFound) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding()), Alignment.Center
                ) { Text(stringResource(id = R.string.playlist_not_found)) }
            } else if (currentPlaylist == null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding()), Alignment.Center
                ) { ContainedLoadingIndicator() }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    // Air between the buttons and the list block (was 6.dp /
                    // 8.dp: the buttons looked glued to the list).
                    val playbackControlBottomPadding = 16.dp
                    // Upstream Play + Shuffle row (upstream has no floating FAB in
                    // the header; the title needs the full width the FAB blocked).
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(horizontal = 20.dp)
                            .padding(bottom = playbackControlBottomPadding),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (localReorderableSongs.isNotEmpty()) {
                                    playerViewModel.playSongs(
                                        localReorderableSongs,
                                        localReorderableSongs.first(),
                                        currentPlaylist?.name ?: fallbackPlaylistName,
                                        currentPlaylist?.id,
                                    )
                                    if (playerStableState.isShuffleEnabled) playerViewModel.toggleShuffle()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(76.dp),
                            enabled = localReorderableSongs.isNotEmpty(),
                            shape = AbsoluteSmoothCornerShape(
                                cornerRadiusTL = 60.dp,
                                smoothnessAsPercentTR = 60,
                                cornerRadiusTR = 14.dp,
                                smoothnessAsPercentTL = 60,
                                cornerRadiusBL = 60.dp,
                                smoothnessAsPercentBR = 60,
                                cornerRadiusBR = 14.dp,
                                smoothnessAsPercentBL = 60
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            TightWrapText(
                                text = playItLabel,
                                modifier = Modifier.padding(end = 4.dp),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                                lineHeight = 20.sp
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                if (localReorderableSongs.isNotEmpty()) {
                                    playerViewModel.playSongsShuffled(
                                        songsToPlay = localReorderableSongs,
                                        queueName = currentPlaylist?.name ?: fallbackPlaylistName,
                                        playlistId = currentPlaylist?.id,
                                        startAtZero = true,
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(76.dp),
                            enabled = localReorderableSongs.isNotEmpty(),
                            shape = AbsoluteSmoothCornerShape(
                                cornerRadiusTL = 14.dp,
                                smoothnessAsPercentTR = 60,
                                cornerRadiusTR = 60.dp,
                                smoothnessAsPercentTL = 60,
                                cornerRadiusBL = 14.dp,
                                smoothnessAsPercentBR = 60,
                                cornerRadiusBR = 60.dp,
                                smoothnessAsPercentBL = 60
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Shuffle,
                                contentDescription = shuffleLabel,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            TightWrapText(
                                text = shuffleLabel,
                                modifier = Modifier.padding(end = 4.dp),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2,
                                lineHeight = 20.sp
                            )
                        }
                    }



                    if (localReorderableSongs.isEmpty()) {
                        Box(Modifier
                            .fillMaxSize()
                            .weight(1f), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.MusicOff, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(8.dp))
                                Text(playlistEmptyTitle, style = MaterialTheme.typography.titleMedium)
                                val emptyMessage = when {
                                    isFolderPlaylist -> playlistEmptyFolder
                                    isMix -> stringResource(R.string.presentation_batch_b_playlist_empty_mix_body)
                                    else -> playlistEmptyAddHint
                                }
                                Text(emptyMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(
                                        AbsoluteSmoothCornerShape(
                                            cornerRadiusTR = 32.dp,
                                            smoothnessAsPercentTR = 60,
                                            cornerRadiusTL = 32.dp,
                                            smoothnessAsPercentTL = 60,
                                        )
                                    )
                                    .background(color = MaterialTheme.colorScheme.surfaceContainerHigh),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(
                                    top = 12.dp,
                                    bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp,
                                    end = if (listState.canScrollForward || listState.canScrollBackward) 24.dp else 0.dp
                                ).let {
                                    PaddingValues(
                                        top = it.calculateTopPadding(),
                                        bottom = it.calculateBottomPadding(),
                                        start = it.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
                                        end = if (listState.canScrollForward || listState.canScrollBackward) 24.dp else 0.dp
                                    )
                                }
                            ) {
                                item(key = "playlist_selection_row") {
                                    Column {
                                        AnimatedVisibility(
                                            visible = isSelectionMode,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            SelectionActionRow(
                                                selectedCount = selectedSongs.size,
                                                onSelectAll = { multiSelectionState.selectAll(localReorderableSongs) },
                                                onDeselect = { multiSelectionState.clearSelection() },
                                                onOptionsClick = { showMultiSelectionSheet = true },
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )
                                        }
                                    }
                                }
                                items(
                                    localReorderableEntries,
                                    key = { it.index },
                                    contentType = { "playlist_song" }) { entry ->
                                    val song = entry.value
                                    val playbackUiState by remember(song.id, playerViewModel) {
                                        playerViewModel.stablePlayerState
                                            .map { state ->
                                                val isCurrent = state.currentSong?.id == song.id
                                                LibrarySongPlaybackUiState(
                                                    isCurrentSong = isCurrent,
                                                    isPlaying = isCurrent && state.isPlaying
                                                )
                                            }
                                            .distinctUntilChanged()
                                    }.collectAsStateWithLifecycle(initialValue = LibrarySongPlaybackUiState())
                                    ReorderableItem(
                                        state = reorderableState,
                                        key = entry.index,
                                    ) { isDragging ->
                                        val scale by animateFloatAsState(
                                            if (isDragging) 1.05f else 1f,
                                            label = "scale"
                                        )

                                        QueuePlaylistSongItem(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 0.dp)
                                                .graphicsLayer {
                                                    scaleX = scale
                                                    scaleY = scale
                                                },
                                            onClick = {
                                                if (isSelectionMode) {
                                                    multiSelectionState.toggleSelection(song)
                                                } else {
                                                    playerViewModel.playSongs(
                                                        localReorderableSongs,
                                                        song,
                                                        currentPlaylist.name,
                                                        currentPlaylist.id
                                                    )
                                                }
                                            },
                                            song = song,
                                            isCurrentSong = playbackUiState.isCurrentSong,
                                            isPlaying = playbackUiState.isPlaying,
                                            isDragging = isDragging,
                                            isSelected = selectedSongIds.contains(song.id),
                                            selectionIndex = if (isSelectionMode) multiSelectionState.getSelectionIndex(song.id) else null,
                                            isSelectionMode = isSelectionMode,
                                            onLongPress = {
                                                // In reorder mode a long-press must not
                                                // switch the row into multi-select and hide
                                                // the drag handle; dragging uses the handle.
                                                if (!isReorderModeEnabled) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    multiSelectionState.toggleSelection(song)
                                                }
                                            },
                                            onSelectionToggle = { multiSelectionState.toggleSelection(song) },
                                            onRemoveClick = {
                                                if (isEditablePlaylist) {
                                                    currentPlaylist.let {
                                                        playlistViewModel.removeSongFromPlaylist(it.id, song.id)
                                                    }
                                                }
                                            },
                                            isFromPlaylist = true,
                                            isReorderModeEnabled = isReorderModeEnabled && !isSelectionMode,
                                            isDragHandleVisible = isReorderModeEnabled && isEditablePlaylist && !isSelectionMode,
                                            isRemoveButtonVisible = isRemoveModeEnabled && isEditablePlaylist && !isSelectionMode,
                                            onMoreOptionsClick = stableOnMoreOptionsClick,
                                            dragHandle = {
                                                IconButton(
                                                    onClick = {},
                                                    modifier = Modifier
                                                        .draggableHandle(
                                                            onDragStarted = {
                                                                // Belt and braces: a drag never
                                                                // starts with a selection active.
                                                                multiSelectionState.clearSelection()
                                                                performAppCompatHapticFeedback(
                                                                    view,
                                                                    appHapticsConfig,
                                                                    HapticFeedbackConstantsCompat.GESTURE_START
                                                                )
                                                            },
                                                            onDragStopped = {
                                                                performAppCompatHapticFeedback(
                                                                    view,
                                                                    appHapticsConfig,
                                                                    HapticFeedbackConstantsCompat.GESTURE_END
                                                                )
                                                            }
                                                        )
                                                        .size(40.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.DragIndicator,
                                                        contentDescription = reorderSongCd,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
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

                            ExpressiveScrollBar(
                                listState = listState,
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(
                                        bottom = if (hasCurrentSong) MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 20.dp else WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp,
                                        end = 14.dp,
                                        top = 18.dp
                                    )
                            )
                        }
                    }
                }
            }
        }

        if (showAddSongsSheet && currentPlaylist != null && isEditablePlaylist) {
            SongPickerBottomSheet(
                initiallySelectedSongIds = currentPlaylist.songIds.toSet(),
                onDismiss = { showAddSongsSheet = false },
                onConfirm = { selectedIds ->
                    playlistViewModel.addSongsToPlaylist(currentPlaylist.id, selectedIds.toList())
                    showAddSongsSheet = false
                }
            )
        }
        if (showPlaylistOptionsSheet && !isFolderPlaylist) {
            val sheetState = rememberModalSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showPlaylistOptionsSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        // Scrolls so Export stays reachable in landscape and at large font scales.
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = playlistOptionsTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        currentPlaylist?.name?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    // One segmented group: 4dp apart, only the group's outer corners round.
                    // Our sheet adds Add / Remove / Reorder above upstream's actions.
                    val editableCount = if (isEditablePlaylist) 3 else 0
                    val optionCount = editableCount + 3 +
                        (if (cloudSongsInPlaylist.isNotEmpty()) 1 else 0) + 1
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                    if (isEditablePlaylist) {
                        PlaylistActionItem(
                            icon = rememberVectorPainter(Icons.Rounded.Add),
                            label = addSongsCd,
                            shape = getSegmentedItemShape(0, optionCount),
                            onClick = {
                                showPlaylistOptionsSheet = false
                                showAddSongsSheet = true
                            }
                        )
                        PlaylistActionItem(
                            icon = rememberVectorPainter(Icons.Filled.RemoveCircleOutline),
                            label = removeSongsCd,
                            shape = getSegmentedItemShape(1, optionCount),
                            selected = isRemoveModeEnabled,
                            onClick = {
                                showPlaylistOptionsSheet = false
                                isRemoveModeEnabled = !isRemoveModeEnabled
                            }
                        )
                        PlaylistActionItem(
                            icon = painterResource(R.drawable.drag_order_icon),
                            label = reorderSongsCd,
                            shape = getSegmentedItemShape(2, optionCount),
                            onClick = {
                                showPlaylistOptionsSheet = false
                                // Reorder never multi-selects: clear any selection
                                // before the drag handles appear.
                                if (!isReorderModeEnabled) {
                                    multiSelectionState.clearSelection()
                                }
                                isReorderModeEnabled = !isReorderModeEnabled
                            }
                        )
                    }
                    PlaylistActionItem(
                        icon = painterResource(R.drawable.rounded_edit_24),
                        label = editPlaylistLabel,
                        shape = getSegmentedItemShape(editableCount, optionCount),
                        onClick = {
                            showPlaylistOptionsSheet = false
                            showEditPlaylistDialog = true
                        }
                    )
                    PlaylistActionItem(
                        icon = painterResource(R.drawable.rounded_delete_24),
                        label = deletePlaylistLabel,
                        shape = getSegmentedItemShape(editableCount + 1, optionCount),
                        onClick = {
                            showPlaylistOptionsSheet = false
                            showDeleteConfirmation = true
                        }
                    )
                    PlaylistActionItem(
                        icon = painterResource(R.drawable.outline_graph_1_24),
                        label = setDefaultTransitionLabel,
                        shape = getSegmentedItemShape(editableCount + 2, optionCount),
                        onClick = {
                            showPlaylistOptionsSheet = false
                            navController.navigateSafely(Screen.EditTransition.createRoute(playlistId))
                        }
                    )
                    if (cloudSongsInPlaylist.isNotEmpty()) {
                        PlaylistActionItem(
                            icon = rememberVectorPainter(Icons.Rounded.CloudDownload),
                            label = downloadPlaylistLabel,
                        shape = getSegmentedItemShape(editableCount + 3, optionCount),
                            onClick = {
                                showPlaylistOptionsSheet = false
                                cloudDownloadsViewModel.downloadSelected(cloudSongsInPlaylist)
                                Toast.makeText(
                                    context,
                                    context.resources.getQuantityString(
                                        R.plurals.cloud_download_selected_started,
                                        cloudSongsInPlaylist.size,
                                        cloudSongsInPlaylist.size,
                                    ),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                        )
                    }
                    PlaylistActionItem(
                        icon = painterResource(R.drawable.rounded_attach_file_24),
                        label = exportPlaylistLabel,
                        shape = getSegmentedItemShape(optionCount - 1, optionCount),
                        onClick = {
                            showPlaylistOptionsSheet = false
                            val sanitizedName = PlaylistViewModel.sanitizeFileName(
                                currentPlaylist?.name ?: fallbackPlaylistName
                            )
                            m3uExportLauncher.launch("$sanitizedName.m3u")
                        }
                    )
                    }
                }
            }
        }
    
        if (showEditPlaylistDialog && currentPlaylist != null) {
            val initialShapeType = try {
                currentPlaylist.coverShapeType?.let { PlaylistShapeType.valueOf(it) } ?: PlaylistShapeType.Circle
            } catch (e: Exception) {
                PlaylistShapeType.Circle
            }
        
            EditPlaylistDialog(
                visible = showEditPlaylistDialog,
                currentName = currentPlaylist.name,
                currentImageUri = currentPlaylist.coverImageUri,
                currentColor = currentPlaylist.coverColorArgb,
                currentIconName = currentPlaylist.coverIconName,
                currentShapeType = initialShapeType,
                currentShapeDetail1 = currentPlaylist.coverShapeDetail1,
                currentShapeDetail2 = currentPlaylist.coverShapeDetail2,
                currentShapeDetail3 = currentPlaylist.coverShapeDetail3,
                currentShapeDetail4 = currentPlaylist.coverShapeDetail4,
                onDismiss = { showEditPlaylistDialog = false },
                onSave = { name, imageUri, color, icon, scale, panX, panY, shapeType, d1, d2, d3, d4 ->
                    playlistViewModel.updatePlaylistParameters(
                        playlistId = currentPlaylist.id,
                        name = name,
                        coverImageUri = imageUri,
                        coverColor = color,
                        coverIcon = icon,
                        cropScale = scale,
                        cropPanX = panX,
                        cropPanY = panY,
                        coverShapeType = shapeType,
                        coverShapeDetail1 = d1,
                        coverShapeDetail2 = d2,
                        coverShapeDetail3 = d3,
                        coverShapeDetail4 = d4
                    )
                    showEditPlaylistDialog = false
                }
            )
        }
        if (showDeleteConfirmation && currentPlaylist != null) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text(deletePlaylistConfirmTitle) },
                text = {
                    Text(deletePlaylistConfirmBody)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            playlistViewModel.deletePlaylist(currentPlaylist.id)
                            onDeletePlayListClick()
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

        if (showSongInfoBottomSheet && selectedSongForInfo != null) {
            val currentSong = selectedSongForInfo
            val isFavorite = remember(currentSong?.id, favoriteIds) {
                derivedStateOf {
                    currentSong?.let {
                        favoriteIds.contains(
                            it.id
                        )
                    }
                }
            }.value ?: false

            if (currentSong != null) {
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
                        playerViewModel.sendToast(toastAddedToQueue)
                    },
                    onAddNextToQueue = {
                        playerViewModel.addSongNextToQueue(currentSong)
                        showSongInfoBottomSheet = false
                        playerViewModel.sendToast(toastPlayingNext)
                    },
                    onAddToPlayList = {
                        showPlaylistBottomSheet = true;
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
                    removeFromListTrigger = {
                        playlistViewModel.removeSongFromPlaylist(playlistId, currentSong.id)
                    },
                    onRemoveFromList = {
                        playlistViewModel.removeSongFromPlaylist(playlistId, currentSong.id)
                    },
                    removeFromListLabel = stringResource(R.string.presentation_batch_e_cd_remove_from_playlist)
                )
                if (showPlaylistBottomSheet) {
                    val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()

                    PlaylistBottomSheet(
                        playlistUiState = playlistUiState,
                        songs = persistentListOf(currentSong),
                        onDismiss = {
                            showPlaylistBottomSheet = false
                        },
                        currentPlaylistId = playlistId,
                        bottomBarHeight = bottomBarHeightDp,
                        playerViewModel = playerViewModel,
                    )
                }
            }
        }

        val isSortSheetVisible by playerViewModel.isSortingSheetVisible.collectAsStateWithLifecycle()

        if (isSortSheetVisible) {
            val isManualMode = uiState.playlistSongsOrderMode is PlaylistSongsOrderMode.Manual
            val rawOption = uiState.currentPlaylistSongsSortOption
            val currentSortOption = if (isManualMode) {
                SortOption.SongDefaultOrder
            } else if (currentPlaylist != null) {
                rawOption
            } else {
                SortOption.SongTitleAZ
            }

            val songSortOptions = persistentListOf(
                SortOption.SongDefaultOrder,
                SortOption.SongTitleAZ,
                SortOption.SongTitleZA,
                SortOption.SongArtist,
                SortOption.SongArtistDesc,
                SortOption.SongAlbum,
                SortOption.SongAlbumDesc,
                SortOption.SongDateAdded,
                SortOption.SongDateAddedAsc,
                SortOption.SongDuration,
                SortOption.SongDurationAsc,
                SortOption.SongPlayCount,
                SortOption.SongPlayCountAsc
            )

            LibrarySortBottomSheet(
                title = sortSheetTitle,
                options = songSortOptions,
                selectedOption = currentSortOption,
                onDismiss = { playerViewModel.hideSortingSheet() },
                onOptionSelected = { option ->
                     playlistViewModel.sortPlaylistSongs(option)
                     playerViewModel.hideSortingSheet()
                     scope.launch {
                         kotlinx.coroutines.delay(100)
                         listState.animateScrollToItem(0)
                     }
                },
                onDirectionToggle = { option ->
                    playlistViewModel.sortPlaylistSongs(option)
                    scope.launch {
                        kotlinx.coroutines.delay(100)
                        listState.animateScrollToItem(0)
                    }
                },
                showViewToggle = false 
            )
        }

        SongMultiSelectionHost(
            playerViewModel = playerViewModel,
            multiSelectionState = multiSelectionState,
            showSheet = showMultiSelectionSheet,
            onDismiss = { showMultiSelectionSheet = false },
            onRemoveFromList = {
                playlistViewModel.removeSongsFromPlaylist(playlistId, selectedSongIds.toList())
            },
            removeFromListLabel = stringResource(R.string.presentation_batch_e_cd_remove_from_playlist)
        )
    }
}


@Composable
private fun PlaylistActionItem(
    shape: Shape,
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
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
        Spacer(modifier = Modifier.width(14.dp))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenamePlaylistDialog(currentName: String, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var newName by remember { mutableStateOf(TextFieldValue(currentName)) }
    val renameTitle = stringResource(R.string.presentation_batch_b_rename_playlist_dialog_title)
    val newNameLabel = stringResource(R.string.presentation_batch_b_new_name)
    val renameAction = stringResource(R.string.action_rename)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(renameTitle) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text(newNameLabel) },
                shape = CircleShape,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (newName.text.isNotBlank()) onRename(newName.text) },
                enabled = newName.text.isNotBlank() && newName.text != currentName
            ) { Text(renameAction, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis) } }
    )
}
