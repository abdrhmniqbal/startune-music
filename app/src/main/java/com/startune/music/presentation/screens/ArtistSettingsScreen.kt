package com.startune.music.presentation.screens

import com.startune.music.presentation.navigation.navigateSafely

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.startune.music.R
import com.startune.music.presentation.components.CollapsibleCommonTopBar
import com.startune.music.presentation.components.RescanRequiredBanner
import com.startune.music.presentation.components.ExpressiveTopBarContent
import com.startune.music.presentation.settings.search.settingHighlight
import com.startune.music.presentation.settings.search.LocalSettingsHighlightTarget
import com.startune.music.presentation.viewmodel.ArtistSettingsViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ArtistSettingsScreen(
    navController: NavController,
    highlightKey: String? = null,
    viewModel: ArtistSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    // LazyColumn index of the Library org section below that settings search can point into.
    // (Upstream's artist-page section does not exist in the fork.)
    val artistSettingsLibraryOrgItemIndex = 3

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 180.dp

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
        }
    }

    // The sections live in separate lazy items, and on a short or landscape screen the lower
    // ones start below the viewport, so they are never composed and their row cannot scroll
    // itself into view. Bring the target's item in first; the row then clears the bars itself.
    // Skipped once the highlight was consumed, so returning here or rotating does not scroll.
    val highlightPending = LocalSettingsHighlightTarget.current != null
    LaunchedEffect(highlightKey) {
        if (!highlightPending) return@LaunchedEffect
        val targetIndex = when (highlightKey) {
            "item_artist_group_by_album_artist" -> artistSettingsLibraryOrgItemIndex
            else -> return@LaunchedEffect
        }
        delay(200)
        lazyListState.animateScrollToItem(targetIndex)
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0

            val targetValue = if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx

            if (topBarHeight.value != targetValue) {
                coroutineScope.launch {
                    topBarHeight.animateTo(targetValue, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .nestedScroll(nestedScrollConnection)
            .fillMaxSize()
    ) {
        val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = currentTopBarHeightDp + 8.dp, bottom = 100.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = uiState.rescanRequired,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    RescanRequiredBanner(
                        title = stringResource(R.string.presentation_batch_g_artist_rescan_banner_title),
                        body = stringResource(R.string.presentation_batch_g_artist_rescan_banner_body),
                        rescanText = stringResource(R.string.presentation_batch_g_artist_rescan),
                        scanningText = stringResource(R.string.presentation_batch_g_artist_rescan_scanning),
                        isResyncing = uiState.isResyncing,
                        onRescanClick = { viewModel.rescanLibrary() }
                    )
                }
            }

            item {
                SettingsSection(
                    title = stringResource(R.string.presentation_batch_g_artist_section_multi_parsing),
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier.clip(shape = RoundedCornerShape(24.dp)),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        SettingsItem(
                            title = stringResource(R.string.presentation_batch_g_artist_item_char_delimiters),
                            subtitle = stringResource(
                                R.string.presentation_batch_g_artist_item_char_delimiters_current,
                                uiState.artistDelimiters.joinToString(", ")
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = stringResource(R.string.presentation_batch_g_artist_cd_configure),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.settingHighlight("item_artist_char_delimiters", highlightKey),
                            onClick = {
                                navController.navigateSafely("delimiter_config")
                            }
                        )

                        SettingsItem(
                            title = stringResource(R.string.presentation_batch_g_artist_item_word_delimiters),
                            subtitle = if (uiState.wordDelimiters.isEmpty()) {
                                stringResource(R.string.presentation_batch_g_artist_word_none)
                            } else {
                                val preview = uiState.wordDelimiters.take(5).joinToString(", ") +
                                    if (uiState.wordDelimiters.size > 5) "..." else ""
                                stringResource(
                                    R.string.presentation_batch_g_artist_word_current_prefix,
                                    preview
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = stringResource(R.string.presentation_batch_g_artist_cd_configure),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.settingHighlight("item_artist_word_delimiters", highlightKey),
                            onClick = {
                                navController.navigateSafely("word_delimiter_config")
                            }
                        )

                        SwitchSettingItem(
                            title = stringResource(R.string.presentation_batch_g_artist_extract_from_title),
                            subtitle = stringResource(R.string.presentation_batch_g_artist_extract_from_title_sub),
                            checked = uiState.extractArtistsFromTitle,
                            onCheckedChange = { viewModel.setExtractArtistsFromTitle(it) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.LibraryMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            modifier = Modifier.settingHighlight("item_artist_extract_from_title", highlightKey)
                        )

                        SettingsItem(
                            title = stringResource(R.string.presentation_batch_g_artist_item_unsplit_artists),
                            subtitle = if (uiState.unsplitArtists.isEmpty()) {
                                stringResource(R.string.presentation_batch_g_artist_word_none)
                            } else {
                                val preview = uiState.unsplitArtists.take(5).joinToString(", ") +
                                    if (uiState.unsplitArtists.size > 5) "..." else ""
                                stringResource(
                                    R.string.presentation_batch_g_artist_word_current_prefix,
                                    preview
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = stringResource(R.string.presentation_batch_g_artist_cd_configure),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                navController.navigateSafely("unsplit_artists_config")
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(
                    title = stringResource(R.string.presentation_batch_g_genre_section),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Column(
                        modifier = Modifier.clip(shape = RoundedCornerShape(24.dp)),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        SettingsItem(
                            title = stringResource(R.string.presentation_batch_g_genre_item_split_symbols),
                            subtitle = stringResource(
                                R.string.presentation_batch_g_artist_item_char_delimiters_current,
                                uiState.genreDelimiters.joinToString(", ")
                            ),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = stringResource(R.string.presentation_batch_g_artist_cd_configure),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                navController.navigateSafely("genre_delimiter_config")
                            }
                        )

                        SettingsItem(
                            title = stringResource(R.string.presentation_batch_g_genre_item_unsplit_genres),
                            subtitle = if (uiState.unsplitGenres.isEmpty()) {
                                stringResource(R.string.presentation_batch_g_artist_word_none)
                            } else {
                                val preview = uiState.unsplitGenres.take(5).joinToString(", ") +
                                    if (uiState.unsplitGenres.size > 5) "..." else ""
                                stringResource(
                                    R.string.presentation_batch_g_artist_word_current_prefix,
                                    preview
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = stringResource(R.string.presentation_batch_g_artist_cd_configure),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            onClick = {
                                navController.navigateSafely("unsplit_genres_config")
                            }
                        )
                    }
                }
            }

            item {
                SettingsSection(
                    title = stringResource(R.string.presentation_batch_g_artist_section_library_org),
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    Column(modifier = Modifier.clip(shape = RoundedCornerShape(24.dp))) {
                        SwitchSettingItem(
                            title = stringResource(R.string.presentation_batch_g_artist_group_by_album_artist),
                            subtitle = stringResource(R.string.presentation_batch_g_artist_group_by_album_artist_sub),
                            checked = uiState.groupByAlbumArtist,
                            onCheckedChange = { viewModel.setGroupByAlbumArtist(it) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Album,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            modifier = Modifier.settingHighlight("item_artist_group_by_album_artist", highlightKey)
                        )

                        SwitchSettingItem(
                            title = stringResource(R.string.presentation_batch_g_artist_case_sensitive),
                            subtitle = stringResource(R.string.presentation_batch_g_artist_case_sensitive_sub),
                            checked = uiState.caseSensitiveArtists,
                            onCheckedChange = { viewModel.setCaseSensitiveArtists(it) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        )
                    }
                }
            }

            item {
                InfoCard(
                    title = stringResource(R.string.presentation_batch_g_artist_info_title),
                    content = stringResource(R.string.presentation_batch_g_artist_info_body)
                )
            }

            item {
                ExamplesCard(
                    examples = persistentListOf(
                        stringResource(R.string.presentation_batch_g_artist_ex_1_in) to stringResource(R.string.presentation_batch_g_artist_ex_1_out),
                        stringResource(R.string.presentation_batch_g_artist_ex_2_in) to stringResource(R.string.presentation_batch_g_artist_ex_2_out),
                        stringResource(R.string.presentation_batch_g_artist_ex_3_in) to stringResource(R.string.presentation_batch_g_artist_ex_3_out),
                        stringResource(R.string.presentation_batch_g_artist_ex_4_in) to stringResource(R.string.presentation_batch_g_artist_ex_4_out),
                        stringResource(R.string.presentation_batch_g_artist_ex_5_in) to stringResource(R.string.presentation_batch_g_artist_ex_5_out)
                    )
                )
            }
        }
        CollapsibleCommonTopBar(
            title = stringResource(R.string.setcat_artists_title),
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeightDp,
            onBackClick = { navController.popBackStack() },
            expandedTitleStartPadding = 20.dp
        )
    }
}



@Composable
private fun InfoCard(
    title: String,
    content: String
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ExamplesCard(
    examples: ImmutableList<Pair<String, String>>
) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.presentation_batch_g_artist_examples_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            examples.forEach { (input, output) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.presentation_batch_h_artist_example_bullet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = input,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.presentation_batch_g_artist_example_arrow),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = output,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
