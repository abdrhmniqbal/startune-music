package com.startune.music.presentation.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
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
import coil.ImageLoader
import coil.request.ImageRequest
import com.startune.music.R
import com.startune.music.data.model.Artist
import com.startune.music.data.model.MixRule
import com.startune.music.presentation.components.CoverColorPicker
import com.startune.music.presentation.components.CoverIconPicker
import com.startune.music.presentation.components.ImageCropView
import com.startune.music.presentation.components.getIconByName
import com.startune.music.presentation.components.getThemeContentColor
import com.startune.music.presentation.viewmodel.PlayerViewModel
import com.startune.music.ui.theme.RoundedSans
import com.startune.music.ui.theme.ShapeCache
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
        }
    )

@Composable
private fun mixRuleSubtitle(rule: MixRule): String =
    stringResource(
        when (rule) {
            MixRule.TOP_PLAYED -> R.string.mix_rule_top_played_subtitle
            MixRule.RECENTLY_PLAYED -> R.string.mix_rule_recently_played_subtitle
            MixRule.FORGOTTEN_FAVORITES -> R.string.mix_rule_forgotten_favorites_subtitle
            MixRule.NEW_GEMS -> R.string.mix_rule_new_gems_subtitle
            MixRule.ARTISTS -> R.string.mix_rule_artists_subtitle
        }
    )

/**
 * Create-mix form, full screen like the playlist form: a name (required,
 * typed by hand), one of the five presets, an artist picker for the Artists
 * preset, the song count, and the same Default / Image / Icon cover choice
 * as playlists. A mix cover is always shown in the cookie shape; the choice
 * only changes what is inside it.
 */
@Composable
fun CreateMixForm(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, MixRule, Int, List<Long>, String?, Int?, String?, Float, Float, Float) -> Unit,
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
    onCreate: (String, MixRule, Int, List<Long>, String?, Int?, String?, Float, Float, Float) -> Unit,
    playerViewModel: PlayerViewModel
) {
    val context = LocalContext.current
    val artists by playerViewModel.artistsFlow.collectAsStateWithLifecycle()

    var mixName by remember { mutableStateOf("") }
    var selectedRule by remember { mutableStateOf(MixRule.TOP_PLAYED) }
    var sliderValue by remember { mutableFloatStateOf(60f) }
    var selectedArtistIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var showArtistPicker by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(0) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showCropUi by remember { mutableStateOf(false) }
    var imageBitmap by remember(selectedImageUri) { mutableStateOf<ImageBitmap?>(null) }
    var cropScale by remember { mutableFloatStateOf(1f) }
    var cropOffset by remember { mutableStateOf(Offset.Zero) }
    val defaultColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
    var selectedColor by remember { mutableStateOf<Int?>(defaultColor) }
    var selectedIconName by remember { mutableStateOf<String?>("MusicNote") }

    LaunchedEffect(selectedImageUri) {
        if (selectedImageUri != null) {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(selectedImageUri)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            val drawable = result.drawable
            if (drawable is android.graphics.drawable.BitmapDrawable) {
                imageBitmap = drawable.bitmap.asImageBitmap()
            }
        } else {
            imageBitmap = null
            cropScale = 1f
            cropOffset = Offset.Zero
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            cropScale = 1f
            cropOffset = Offset.Zero
            showCropUi = true
        }
    }

    val canCreate = mixName.isNotBlank() &&
        (selectedRule != MixRule.ARTISTS || selectedArtistIds.isNotEmpty())

    BackHandler(enabled = showCropUi) {
        showCropUi = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.mix_create_title),
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
                        onClick = {
                            if (showCropUi) showCropUi = false else onDismiss()
                        }
                    ) {
                        Icon(
                            if (showCropUi) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Close,
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
            if (!showCropUi) {
                MediumExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.presentation_batch_f_create)) },
                    icon = {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                    },
                    onClick = {
                        if (!canCreate) return@MediumExtendedFloatingActionButton
                        val imageUriString = if (selectedTab == 1) selectedImageUri?.toString() else null
                        val color = if (selectedTab == 2) selectedColor else null
                        val icon = if (selectedTab == 2) selectedIconName else null
                        val scale = if (selectedTab == 1) cropScale else 1f
                        val panX = if (selectedTab == 1) cropOffset.x else 0f
                        val panY = if (selectedTab == 1) cropOffset.y else 0f
                        onCreate(
                            mixName.trim(),
                            selectedRule,
                            sliderValue.toInt(),
                            selectedArtistIds,
                            imageUriString,
                            color,
                            icon,
                            scale,
                            panX,
                            panY
                        )
                    },
                    containerColor = if (canCreate) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = if (canCreate) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
    ) { padding ->
        if (showCropUi) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                val cropBitmap = imageBitmap
                if (cropBitmap != null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(32.dp))
                        Text(
                            text = stringResource(R.string.presentation_batch_f_adjust_cover_title),
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.presentation_batch_f_adjust_cover_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(16.dp)
                                .clip(ShapeCache.expressiveAvatar)
                        ) {
                            ImageCropView(
                                imageBitmap = cropBitmap,
                                modifier = Modifier.fillMaxSize(),
                                scale = cropScale,
                                pan = cropOffset,
                                enabled = true,
                                onCrop = { scale, pan ->
                                    cropScale = scale
                                    cropOffset = pan
                                }
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { showCropUi = false },
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(24.dp)
                                .height(56.dp),
                            shape = CircleShape
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.presentation_batch_f_done))
                        }
                    }
                } else {
                    ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "mix_cover_preview_transition",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
            ) { targetTab ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (targetTab) {
                        0 -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(ShapeCache.expressiveAvatar)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    stringResource(R.string.presentation_batch_f_auto_generated_collage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        1 -> {
                            val previewBitmap = imageBitmap
                            if (previewBitmap != null) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(180.dp)
                                            .clip(ShapeCache.expressiveAvatar)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                    ) {
                                        ImageCropView(
                                            imageBitmap = previewBitmap,
                                            modifier = Modifier.fillMaxSize(),
                                            scale = cropScale,
                                            pan = cropOffset,
                                            enabled = false,
                                            onCrop = { _, _ -> }
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = { imagePickerLauncher.launch("image/*") },
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                            modifier = Modifier.height(40.dp).weight(1f)
                                        ) {
                                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.presentation_batch_f_change), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Button(
                                            onClick = { selectedImageUri = null },
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                            modifier = Modifier.height(40.dp).weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            )
                                        ) {
                                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(R.string.presentation_batch_f_remove), style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(180.dp)
                                        .clip(ShapeCache.expressiveAvatar)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        .clickable { imagePickerLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Rounded.AddPhotoAlternate,
                                            contentDescription = stringResource(R.string.presentation_batch_f_cd_add_photo),
                                            modifier = Modifier.size(56.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(12.dp))
                                        Text(stringResource(R.string.presentation_batch_f_pick_image), style = MaterialTheme.typography.titleSmall)
                                    }
                                }
                            }
                        }
                        2 -> {
                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .clip(ShapeCache.expressiveAvatar)
                                    .background(
                                        selectedColor?.let { Color(it) }
                                            ?: MaterialTheme.colorScheme.primaryContainer
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedIconName != null) {
                                    val icon = getIconByName(selectedIconName) ?: Icons.Rounded.MusicNote
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = selectedColor?.let { getThemeContentColor(it, MaterialTheme.colorScheme) }
                                            ?: MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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

                val tabs = persistentListOf(
                    stringResource(R.string.presentation_batch_f_tab_default),
                    stringResource(R.string.presentation_batch_f_tab_image),
                    stringResource(R.string.presentation_batch_f_tab_icon)
                )
                ExpressiveButtonGroup(
                    items = tabs,
                    selectedIndex = selectedTab,
                    onItemClick = { selectedTab = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                )

                AnimatedVisibility(visible = selectedTab == 2) {
                    Column(
                        modifier = Modifier.padding(top = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CoverColorPicker(
                            selectedColor = selectedColor,
                            onColorChange = { selectedColor = it }
                        )
                        CoverIconPicker(
                            selectedIconName = selectedIconName,
                            onIconChange = { selectedIconName = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.mix_preset_label),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                ) {
                    MixRule.entries.forEach { rule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRule = rule },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRule == rule,
                                onClick = { selectedRule = rule }
                            )
                            Column(modifier = Modifier.padding(start = 8.dp)) {
                                Text(
                                    text = mixRuleTitle(rule),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = mixRuleSubtitle(rule),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    InputChip(
                                        selected = false,
                                        onClick = { selectedArtistIds = selectedArtistIds - artistId },
                                        label = { Text(artistName) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.mix_detail_size_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                )
                Text(
                    text = stringResource(R.string.mix_detail_size_songs, sliderValue.toInt()),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 10f..100f,
                    steps = 17,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp)
                )
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
