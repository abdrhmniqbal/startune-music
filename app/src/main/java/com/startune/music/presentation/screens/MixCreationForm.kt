package com.startune.music.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.startune.music.R
import com.startune.music.data.model.Artist
import com.startune.music.data.model.MixRule
import com.startune.music.data.model.Genre
import com.startune.music.data.playlist.nlp.GenreTaxonomy
import com.startune.music.data.playlist.nlp.NlpText
import com.startune.music.presentation.viewmodel.PlayerViewModel
import com.startune.music.ui.theme.RoundedSans
import com.startune.music.ui.theme.ShapeCache
import com.startune.music.utils.GenreParser
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
private fun mixRuleTitle(rule: MixRule): String =
    stringResource(
        when (rule) {
            MixRule.TOP_PLAYED -> R.string.mix_rule_top_played_title
            MixRule.RECENTLY_PLAYED -> R.string.mix_rule_recently_played_title
            MixRule.FORGOTTEN_FAVORITES -> R.string.mix_rule_forgotten_favorites_title
            MixRule.NEW_GEMS -> R.string.mix_rule_new_gems_title
            MixRule.ARTISTS -> R.string.mix_rule_artists_title
            MixRule.GENRES -> R.string.mix_rule_genres_title
        }
    )

/** Generation modes in the order the form shows them. */
private val generationModeOrder = listOf(
    MixRule.RECENTLY_PLAYED,
    MixRule.TOP_PLAYED,
    MixRule.FORGOTTEN_FAVORITES,
    MixRule.NEW_GEMS,
    MixRule.ARTISTS,
    MixRule.GENRES
)

/** Selected artist/genre badge blue, measured from the mix reference screenshot. */
private val MixBadgeBlue = Color(0xFF004A87)

@Composable
private fun mixRuleSubtitle(rule: MixRule): String =
    stringResource(
        when (rule) {
            MixRule.TOP_PLAYED -> R.string.mix_rule_top_played_subtitle
            MixRule.RECENTLY_PLAYED -> R.string.mix_rule_recently_played_subtitle
            MixRule.FORGOTTEN_FAVORITES -> R.string.mix_rule_forgotten_favorites_subtitle
            MixRule.NEW_GEMS -> R.string.mix_rule_new_gems_subtitle
            MixRule.ARTISTS -> R.string.mix_rule_artists_subtitle
            MixRule.GENRES -> R.string.mix_rule_genres_subtitle
        }
    )

/**
 * Create-mix form, full screen like the playlist form: a name (required,
 * typed by hand), the song count, one of the six generation modes, an artist
 * picker for the By Artists mode or a comma-separated genre input for the
 * By Genres mode. There is no cover UI: the cover is always
 * auto-generated.
 */

/**
 * Initial values for editing a mix. Create mode uses the defaults.
 * The stored cover fields are kept for the callers; the form no longer shows
 * or edits a cover, so saving a mix clears them.
 */
data class MixFormInitial(
    val name: String = "",
    val rule: MixRule = MixRule.TOP_PLAYED,
    val songCount: Int = 60,
    val artistIds: List<Long> = emptyList(),
    val genres: List<String> = emptyList(),
    val coverImageUri: String? = null,
    val coverColorArgb: Int? = null,
    val coverIconName: String? = null
)

/**
 * Edit-mix form: the same full-screen form as [CreateMixForm], prefilled
 * from the mix. For the For You Mix only the size is editable (its name and
 * rule are fixed), so the name and preset sections hide.
 */
@Composable
fun EditMixForm(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, MixRule, Int, List<Long>, List<String>, String?, Int?, String?, Float, Float, Float) -> Unit,
    playerViewModel: PlayerViewModel,
    initial: MixFormInitial = MixFormInitial(),
    isForYouMix: Boolean = false
) {
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = visible

    if (transitionState.currentState || transitionState.targetState) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = slideInVertically(initialOffsetY = { it / 6 }) + fadeIn(animationSpec = tween(220)),
                exit = slideOutVertically(targetOffsetY = { it / 6 }) + fadeOut(animationSpec = tween(200)),
                label = "edit_mix_form"
            ) {
                CreateMixFormContent(
                    onDismiss = onDismiss,
                    onCreate = onSave,
                    playerViewModel = playerViewModel,
                    initial = initial,
                    isEdit = true,
                    isForYouMix = isForYouMix
                )
            }
        }
    }
}

@Composable
fun CreateMixForm(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, MixRule, Int, List<Long>, List<String>, String?, Int?, String?, Float, Float, Float) -> Unit,
    playerViewModel: PlayerViewModel
) {
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = visible

    if (transitionState.currentState || transitionState.targetState) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            AnimatedVisibility(
                visibleState = transitionState,
                enter = slideInVertically(initialOffsetY = { it / 6 }) + fadeIn(animationSpec = tween(220)),
                exit = slideOutVertically(targetOffsetY = { it / 6 }) + fadeOut(animationSpec = tween(200)),
                label = "create_mix_form"
            ) {
                CreateMixFormContent(
                    onDismiss = onDismiss,
                    onCreate = onCreate,
                    playerViewModel = playerViewModel
                )
            }
        }
    }
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CreateMixFormContent(
    onDismiss: () -> Unit,
    onCreate: (String, MixRule, Int, List<Long>, List<String>, String?, Int?, String?, Float, Float, Float) -> Unit,
    playerViewModel: PlayerViewModel,
    initial: MixFormInitial = MixFormInitial(),
    isEdit: Boolean = false,
    isForYouMix: Boolean = false
) {
    val context = LocalContext.current
    val artists by playerViewModel.artistsFlow.collectAsStateWithLifecycle()

    var mixName by remember { mutableStateOf(initial.name) }
    var selectedRule by remember { mutableStateOf(initial.rule) }
    var sliderValue by remember { mutableFloatStateOf(initial.songCount.toFloat()) }
    var selectedArtistIds by remember { mutableStateOf<List<Long>>(initial.artistIds) }
    var showArtistPicker by remember { mutableStateOf(false) }
    var genreInput by remember { mutableStateOf("") }
    var selectedGenres by remember { mutableStateOf<List<String>>(initial.genres) }
    var modeExpanded by remember { mutableStateOf(false) }
    val libraryGenres by playerViewModel.genres.collectAsStateWithLifecycle()
    val onGenreInputChange: (String) -> Unit = { newText ->
        val (resolved, remaining) = resolveGenreInput(newText, libraryGenres)
        if (resolved.isNotEmpty()) {
            val updated = selectedGenres.toMutableList()
            resolved.forEach { if (it !in updated) updated.add(it) }
            selectedGenres = updated
        }
        genreInput = remaining
    }
    val resolveRemainingGenres: () -> Unit = {
        val (resolved, remaining) = resolveGenreInput(genreInput, libraryGenres, resolveLast = true)
        if (resolved.isNotEmpty()) {
            val updated = selectedGenres.toMutableList()
            resolved.forEach { if (it !in updated) updated.add(it) }
            selectedGenres = updated
        }
        genreInput = remaining
    }
    // Genres typed but not yet chipped (the trailing token) still count for
    // validation and saving, so a trailing genre is never silently lost.
    val effectiveGenres = remember(selectedGenres, genreInput, libraryGenres) {
        val (resolved, _) = resolveGenreInput(genreInput, libraryGenres, resolveLast = true)
        val updated = selectedGenres.toMutableList()
        resolved.forEach { if (it !in updated) updated.add(it) }
        updated
    }




    val canCreate = isForYouMix || (mixName.isNotBlank() &&
        (selectedRule != MixRule.ARTISTS || selectedArtistIds.isNotEmpty()) &&
        (selectedRule != MixRule.GENRES || effectiveGenres.isNotEmpty()))


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(if (isEdit) R.string.mix_edit_title else R.string.mix_create_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 24.sp,
                            textGeometricTransform = TextGeometricTransform(scaleX = 1.2f),
                        ),
                        fontFamily = RoundedSans,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 6.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = onDismiss
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.presentation_batch_f_cd_back_or_cancel)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            MediumExtendedFloatingActionButton(
                text = { Text(stringResource(if (isEdit) R.string.action_save else R.string.presentation_batch_f_create)) },
                icon = {
                    Icon(Icons.Rounded.Check, contentDescription = null)
                },
                onClick = {
                    if (!canCreate) return@MediumExtendedFloatingActionButton
                    onCreate(
                        mixName.trim(),
                        selectedRule,
                        sliderValue.toInt(),
                        selectedArtistIds,
                        effectiveGenres,
                        null,
                        null,
                        null,
                        1f,
                        0f,
                        0f
                    )
                },
                containerColor = if (canCreate) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (canCreate) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!isForYouMix) {
                OutlinedTextField(
                    value = mixName,
                    onValueChange = { mixName = it },
                    label = { Text(stringResource(R.string.mix_name_label)) },
                    placeholder = { Text(stringResource(R.string.mix_name_placeholder)) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                }


                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.mix_playlist_size_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = stringResource(R.string.mix_playlist_size_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = stringResource(R.string.mix_detail_size_songs, sliderValue.toInt()),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            valueRange = 10f..100f,
                            steps = 17,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(10, 20, 30, 40, 50).forEach { count ->
                                if (sliderValue.toInt() == count) {
                                    Button(
                                        onClick = { sliderValue = count.toFloat() },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text(count.toString()) }
                                } else {
                                    FilledTonalButton(
                                        onClick = { sliderValue = count.toFloat() },
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) { Text(count.toString()) }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!isForYouMix) {
                ExposedDropdownMenuBox(
                    expanded = modeExpanded,
                    onExpandedChange = { modeExpanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                ) {
                    OutlinedTextField(
                        value = mixRuleTitle(selectedRule),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.mix_generation_mode_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) },
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = modeExpanded,
                        onDismissRequest = { modeExpanded = false }
                    ) {
                        generationModeOrder.forEach { rule ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(text = mixRuleTitle(rule), style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = mixRuleSubtitle(rule),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = { selectedRule = rule; modeExpanded = false },
                                trailingIcon = {
                                    if (selectedRule == rule) {
                                        Icon(Icons.Rounded.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }
                }

                AnimatedVisibility(visible = selectedRule == MixRule.ARTISTS) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.mix_artists_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (selectedArtistIds.isEmpty()) {
                            Text(
                                text = stringResource(R.string.mix_artists_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedArtistIds.forEach { artistId ->
                                    val artistName = artists.firstOrNull { it.id == artistId }?.name
                                        ?: return@forEach
                                    MixBadgeChip(
                                        text = artistName,
                                        onRemove = { selectedArtistIds = selectedArtistIds - artistId }
                                    )
                                }
                            }
                        }
                        FilledTonalButton(
                            onClick = { showArtistPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.mix_artists_add))
                        }
                    }
                }

                AnimatedVisibility(visible = selectedRule == MixRule.GENRES) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.mix_genres_label),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = genreInput,
                            onValueChange = { onGenreInputChange(it) },
                            label = { Text(stringResource(R.string.mix_genres_label)) },
                            placeholder = { Text(stringResource(R.string.mix_genres_placeholder)) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { resolveRemainingGenres() })
                        )
                        if (selectedGenres.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedGenres.forEach { genre ->
                                    MixBadgeChip(
                                        text = genre,
                                        onRemove = { selectedGenres = selectedGenres - genre }
                                    )
                                }
                            }
                        }
                        Text(
                            text = stringResource(R.string.mix_genres_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(100.dp))
            }
        }
    }

    if (showArtistPicker) {
        ArtistPickerBottomSheet(
            artists = artists,
            selectedArtistIds = selectedArtistIds,
            onToggleArtist = { artistId ->
                selectedArtistIds = if (selectedArtistIds.contains(artistId)) {
                    selectedArtistIds - artistId
                } else {
                    selectedArtistIds + artistId
                }
            },
            onDismiss = { showArtistPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistPickerBottomSheet(
    artists: ImmutableList<Artist>,
    selectedArtistIds: List<Long>,
    onToggleArtist: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredArtists = remember(artists, query) {
        if (query.isBlank()) artists
        else artists.filter { it.name.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.mix_artists_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.mix_artists_search_placeholder)) },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.clickable { query = "" }
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
            Spacer(Modifier.height(12.dp))
            if (artists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.mix_artists_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (filteredArtists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.search_no_results_for_query, query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredArtists, key = { it.id }) { artist ->
                        ArtistListItem(
                            artist = artist,
                            onClick = {},
                            isSelectionMode = true,
                            isSelected = selectedArtistIds.contains(artist.id),
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            onSelectionToggle = { onToggleArtist(artist.id) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.presentation_batch_f_done))
            }
        }
    }
}

/**
 * A selected artist/genre badge: the blue chip from the mix reference, white
 * text, and a dismiss X.
 */
@Composable
private fun MixBadgeChip(text: String, onRemove: () -> Unit) {
    Surface(
        color = MixBadgeBlue,
        contentColor = Color.White,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = null, Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Splits comma-separated genre text and resolves each completed token
 * against the library genres with the NLP ([GenreTaxonomy] alias fallback).
 * Returns the newly resolved canonical names and the text that stays in the
 * field (unresolved completed tokens plus the trailing incomplete token,
 * unless [resolveLast] also resolves it, e.g. on Done). Separators never
 * become chips: only resolved genres do.
 */
private fun resolveGenreInput(
    text: String,
    libraryGenres: List<Genre>,
    resolveLast: Boolean = false
): Pair<List<String>, String> {
    val segments = text.split(",")
    val completed = if (resolveLast) segments else segments.dropLast(1)
    val trailing = if (resolveLast) "" else segments.lastOrNull() ?: ""
    val resolved = mutableListOf<String>()
    val remaining = mutableListOf<String>()
    completed.forEach { raw ->
        val token = raw.trim()
        if (token.isEmpty()) return@forEach
        resolveGenreToken(token, libraryGenres)?.let { resolved.add(it) } ?: remaining.add(token)
    }
    if (trailing.isNotBlank()) remaining.add(trailing.trim())
    return resolved to remaining.joinToString(", ")
}

/**
 * One typed genre token to its canonical library name, or the token itself
 * when the NLP taxonomy knows it as a genre word, or null when it matches
 * nothing. Exact loose-key match first ("K-Pop" for "kpop"), then a
 * containing match for longer tokens ("Alternative Rock" for "rock").
 */
private fun resolveGenreToken(token: String, libraryGenres: List<Genre>): String? {
    val key = GenreParser.looseKey(token)
    if (key.isEmpty()) return null
    libraryGenres.firstOrNull { GenreParser.looseKey(it.name) == key }?.let { return it.name }
    if (key.length >= 3) {
        libraryGenres.firstOrNull { other ->
            val otherKey = GenreParser.looseKey(other.name)
            otherKey.contains(key) || key.contains(otherKey)
        }?.let { return it.name }
    }
    if (GenreTaxonomy.resolve(NlpText.normalize(token)) != null) return token
    return null
}
