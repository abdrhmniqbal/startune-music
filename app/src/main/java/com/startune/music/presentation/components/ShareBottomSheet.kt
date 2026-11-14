package com.startune.music.presentation.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.startune.music.R
import com.startune.music.data.model.Song
import com.startune.music.presentation.recap.letterboxToStory
import com.startune.music.presentation.viewmodel.ThemeStateHolder
import com.startune.music.ui.theme.RoundedSans
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

private const val INSTAGRAM_PACKAGE = "com.instagram.android"
private const val STARTUNE_REPO_URL = "https://github.com/abdrhmniqbal/startune-music"
private const val SHARE_CARD_CACHE_DIR = "share_cards"
private const val MAX_SELECTED_LYRICS = 5

/**
 * Strips LRC metadata headers and timestamps, leaving the plain lyric lines.
 * A song with no usable lines simply has no Lyrics Card tab.
 */
object LyricCleaner {
    fun clean(rawLyrics: String?): List<String> {
        if (rawLyrics.isNullOrBlank()) return emptyList()
        // Strip metadata headers like [ti:Title], [ar:Artist], or [offset:0]
        val noMeta = rawLyrics.replace(Regex("(?m)^\\[[a-zA-Z]+:.*\\]\\r?\\n?"), "")
        // Strip timestamp tags like [00:12.34], [01:23], [00:12.345] or empty brackets
        val noTimestamps = noMeta.replace(Regex("\\[\\d{2}:\\d{2}(?:\\.\\d{1,3})?\\]"), "")
        return noTimestamps.lines()
            .map { it.trim() }
            // Filter out blank lines and noise starting with bracket tags
            .filter { it.isNotEmpty() && !it.startsWith("[") && !it.startsWith("(") }
    }
}

/**
 * One share-card look for the whole app: the song's album color scheme,
 * fixed dark, solid colors, Material Expressive shapes. There is no theme
 * picker and no light/dark toggle; the card always looks like this.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShareBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    onShareAudioFile: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val entryPoint = remember(appContext) {
        EntryPointAccessors.fromApplication(appContext, ShareBottomSheetEntryPoint::class.java)
    }
    val themeStateHolder = entryPoint.themeStateHolder()
    val albumColorSchemeState by themeStateHolder
        .getAlbumColorSchemeFlow(song.albumArtUriString.orEmpty())
        .collectAsState()
    // The card is always the dark album scheme; the app theme is the fallback
    // when the album has no extracted colors yet.
    val cardScheme = albumColorSchemeState?.dark ?: MaterialTheme.colorScheme

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val sheetState = rememberModalSheetState(skipPartiallyExpanded = true)

    // Card mode: 0 = Song Card, 1 = Lyrics Card
    var selectedCardMode by remember { mutableStateOf(0) }

    val cleanedLyrics = remember(song.lyrics) { LyricCleaner.clean(song.lyrics) }
    val hasLyrics = remember(cleanedLyrics) { cleanedLyrics.isNotEmpty() }
    val selectedLyrics = remember { mutableStateListOf<String>() }

    // Start with the first 3 lines so the lyrics card has a preview immediately.
    LaunchedEffect(cleanedLyrics) {
        if (selectedLyrics.isEmpty() && cleanedLyrics.isNotEmpty()) {
            selectedLyrics.addAll(cleanedLyrics.take(3))
        }
    }

    var isCapturing by remember { mutableStateOf(false) }
    val cardLayer = rememberGraphicsLayer()

    val instagramInstalled = remember { isPackageInstalled(context, INSTAGRAM_PACKAGE) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val cardShape = AbsoluteSmoothCornerShape(
        cornerRadiusTR = 24.dp, smoothnessAsPercentBR = 60,
        cornerRadiusBR = 24.dp, smoothnessAsPercentTL = 60,
        cornerRadiusTL = 24.dp, smoothnessAsPercentBL = 60,
        cornerRadiusBL = 24.dp, smoothnessAsPercentTR = 60
    )

    // Record the card as drawn, centre it on a 9:16 story canvas, and hand
    // the finished PNG to the action. Runs on the UI scope so the layer read
    // happens on the frame it was recorded.
    fun captureAndShare(action: suspend (Bitmap) -> Unit) {
        isCapturing = true
        scope.launch {
            try {
                val bitmap = captureStoryBitmap(cardLayer, cardScheme.surfaceContainerLowest)
                action(bitmap)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.share_card_capture_failed),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isCapturing = false
            }
        }
    }

    val maxLinesMessage = stringResource(R.string.share_card_max_lines)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                // Custom drag handle, in the column like the upstream sheet.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                }
                // ── Header ──────────────────────────────────────────────────
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.share_sheet_title),
                    fontFamily = RoundedSans,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))

                // ── Card Mode Tabs (Song / Lyrics) ──────────────────────────
                if (hasLyrics) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            stringResource(R.string.share_card_tab_song),
                            stringResource(R.string.share_card_tab_lyrics)
                        ).forEachIndexed { index, label ->
                            val isSelected = selectedCardMode == index
                            val bgColor by animateColorAsState(
                                targetValue = if (isSelected) primaryColor else Color.Transparent,
                                animationSpec = tween(250),
                                label = "tabColor$index"
                            )
                            val textColor by animateColorAsState(
                                targetValue = if (isSelected) onPrimaryColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(250),
                                label = "tabTextColor$index"
                            )
                            val tabScale by animateFloatAsState(
                                targetValue = if (isSelected) 1.03f else 1f,
                                label = "tabScale$index"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .graphicsLayer {
                                        scaleX = tabScale
                                        scaleY = tabScale
                                    }
                                    .clip(CircleShape)
                                    .background(bgColor)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedCardMode = index
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = RoundedSans,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // ── 9:16 Card Preview ───────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = Pair(selectedCardMode, selectedLyrics.toList()),
                        transitionSpec = {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                        },
                        label = "cardPreview"
                    ) { (mode, lyricsList) ->
                        ShareableCard(
                            modifier = Modifier
                                .fillMaxWidth(0.70f)
                                .drawWithContent {
                                    cardLayer.record { this@drawWithContent.drawContent() }
                                    drawLayer(cardLayer)
                                },
                            song = song,
                            isLyricsMode = mode == 1,
                            selectedLyrics = lyricsList,
                            cardScheme = cardScheme,
                            cardShape = cardShape
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // ── Lyric Selector (only in Lyrics Card mode) ─────────────
                if (selectedCardMode == 1 && cleanedLyrics.isNotEmpty()) {
                    LyricLineSelector(
                        lines = cleanedLyrics,
                        selectedLines = selectedLyrics,
                        onToggleLine = { line ->
                            if (selectedLyrics.contains(line)) {
                                selectedLyrics.remove(line)
                            } else {
                                if (selectedLyrics.size < MAX_SELECTED_LYRICS) {
                                    selectedLyrics.add(line)
                                } else {
                                    Toast.makeText(context, maxLinesMessage, Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        primaryColor = primaryColor,
                        haptic = haptic
                    )
                    Spacer(Modifier.height(16.dp))
                }

                // ── Share Actions ───────────────────────────────────────────
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    // Instagram Story
                    if (instagramInstalled) {
                        item {
                            ShareActionChip(
                                icon = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_instagram),
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = Color.Unspecified
                                    )
                                },
                                label = stringResource(R.string.share_action_instagram),
                                containerColor = Color(0xFFE1306C).copy(alpha = 0.12f),
                                contentColor = Color(0xFFE1306C),
                                onClick = {
                                    captureAndShare { bitmap ->
                                        val file = saveBitmapToCache(context, bitmap)
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.provider",
                                            file
                                        )
                                        shareToInstagramStory(
                                            context = context,
                                            imageUri = uri,
                                            topColorHex = cardScheme.primaryContainer.toInstagramHex(),
                                            bottomColorHex = cardScheme.surfaceContainerLowest.toInstagramHex()
                                        )
                                    }
                                }
                            )
                        }
                    }

                    // Save Card
                    item {
                        ShareActionChip(
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = stringResource(R.string.share_action_download_card),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = {
                                captureAndShare { bitmap ->
                                    val saved = saveCardToPictures(context, bitmap, song)
                                    Toast.makeText(
                                        context,
                                        context.getString(
                                            if (saved) R.string.share_card_saved
                                            else R.string.share_card_capture_failed
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }

                    // Share More
                    item {
                        val shareText = stringResource(
                            R.string.share_text_format,
                            song.title,
                            song.displayArtist,
                            STARTUNE_REPO_URL
                        )
                        ShareActionChip(
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = stringResource(R.string.share_action_more_apps),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = {
                                captureAndShare { bitmap ->
                                    val file = saveBitmapToCache(context, bitmap)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(
                                            shareIntent,
                                            context.getString(R.string.share_sheet_chooser_title)
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                // ── Share the audio file itself ───────────────────────────
                MoreMenuListRow(
                    label = stringResource(R.string.share_card_share_audio_file),
                    icon = Icons.Rounded.AudioFile,
                    onClick = {
                        onDismiss()
                        onShareAudioFile()
                    },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.navigationBarsPadding())
            }

            // Capture overlay: drawn inside the sheet, over its content.
            if (isCapturing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Shareable Card (9:16) — one solid look: the dark album scheme, no
// gradients, no glass, no bloom, no vignette.
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun ShareableCard(
    modifier: Modifier = Modifier,
    song: Song,
    isLyricsMode: Boolean,
    selectedLyrics: List<String>,
    cardScheme: ColorScheme,
    cardShape: Shape
) {
    val cardRatio = 9f / 16f

    Box(
        modifier = modifier
            .aspectRatio(cardRatio)
            .shadow(elevation = 16.dp, shape = cardShape, clip = true)
            .clip(cardShape)
            .background(cardScheme.surfaceContainerLowest)
    ) {
        // ── Foreground Content Column ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            if (!isLyricsMode) {
                // ── Song Mini Card (solid primaryContainer) ─────────────
                Card(
                    modifier = Modifier.fillMaxWidth(0.88f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardScheme.primaryContainer
                    )
                ) {
                    SongMiniCard(song = song, cardScheme = cardScheme)
                }
            } else {
                // ── Lyrics Panel (solid primaryContainer) ───────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardScheme.primaryContainer)
                        .padding(14.dp)
                ) {
                    LyricsPanel(song = song, selectedLyrics = selectedLyrics, cardScheme = cardScheme)
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── STARTUNE MUSIC watermark pill ───────────────────────────
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardScheme.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "STARTUNE MUSIC",
                    fontFamily = RoundedSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = cardScheme.onSurface
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Song Mini Card — inner content on the card's primaryContainer.
// Layout: flush full-width art → title → artist → thin wavy progress bar.
// No controls. No branding inside this card.
// ────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SongMiniCard(
    song: Song,
    cardScheme: ColorScheme
) {
    val formattedDuration = remember(song.duration) {
        val totalSecs = song.duration / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        String.format("%02d:%02d", mins, secs)
    }
    val formattedProgress = remember(song.duration) {
        val progressSecs = (song.duration * 0.4f / 1000).toLong()
        val mins = progressSecs / 60
        val secs = progressSecs % 60
        String.format("%02d:%02d", mins, secs)
    }

    val density = LocalDensity.current
    val stroke = remember(density) {
        Stroke(width = with(density) { 3.dp.toPx() }, cap = StrokeCap.Round)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        // Album Art — flush to all edges, top corners rounded 12dp.
        SmartImage(
            model = song.albumArtUriString,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    )
                )
        )

        // Info Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Song Title
            Text(
                text = song.title,
                fontFamily = RoundedSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                color = cardScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Artist
            Text(
                text = song.displayArtist,
                fontFamily = RoundedSans,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                color = cardScheme.onPrimaryContainer.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Thin Wavy Progress Bar + Timestamps (static 40% position)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formattedProgress,
                    fontFamily = RoundedSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 8.sp,
                    color = cardScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
                LinearWavyProgressIndicator(
                    progress = { 0.4f },
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp),
                    color = cardScheme.primary,
                    trackColor = cardScheme.primary.copy(alpha = 0.22f),
                    stroke = stroke,
                    trackStroke = stroke,
                    wavelength = 12.dp,
                    amplitude = { 0.5f },
                    waveSpeed = 4.dp
                )
                Text(
                    text = formattedDuration,
                    fontFamily = RoundedSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 8.sp,
                    color = cardScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Lyrics Panel — content inside the solid panel on the lyrics card.
// Layout: thumbnail header → divider → dynamic-font lyric lines.
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun LyricsPanel(
    song: Song,
    selectedLyrics: List<String>,
    cardScheme: ColorScheme
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Header: small thumbnail + title + artist
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SmartImage(
                model = song.albumArtUriString,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .shadow(4.dp, RoundedCornerShape(8.dp), clip = true)
                    .clip(RoundedCornerShape(8.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontFamily = RoundedSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    color = cardScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.displayArtist,
                    fontFamily = RoundedSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = cardScheme.onPrimaryContainer.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(
            color = cardScheme.onPrimaryContainer.copy(alpha = 0.15f),
            thickness = 1.dp
        )
        Spacer(Modifier.height(12.dp))

        // Lyric lines — dynamic font size shrinks as more lines are selected
        if (selectedLyrics.isEmpty()) {
            Text(
                text = stringResource(R.string.share_card_select_lyrics_hint),
                fontFamily = RoundedSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = cardScheme.onPrimaryContainer.copy(alpha = 0.38f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            )
        } else {
            val fontSize = when (selectedLyrics.size) {
                1    -> 22.sp
                2    -> 18.sp
                3    -> 15.sp
                4    -> 13.sp
                else -> 11.sp
            }
            val lineHeight = when (selectedLyrics.size) {
                1    -> 28.sp
                2    -> 24.sp
                3    -> 20.sp
                4    -> 17.sp
                else -> 15.sp
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                selectedLyrics.forEach { line ->
                    Text(
                        text = line,
                        fontFamily = RoundedSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        color = cardScheme.onPrimaryContainer,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Scrollable Lyric Multi-Selector
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun LyricLineSelector(
    lines: List<String>,
    selectedLines: List<String>,
    onToggleLine: (String) -> Unit,
    primaryColor: Color,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.share_card_select_lyrics_title),
            fontFamily = RoundedSans,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(lines) { line ->
                    val isSelected = selectedLines.contains(line)
                    val bgSelectedColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent
                    val borderSelectedColor = if (isSelected) primaryColor else Color.Transparent
                    val textWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    val textColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgSelectedColor)
                            .border(1.dp, borderSelectedColor, RoundedCornerShape(10.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleLine(line)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = line,
                            fontFamily = RoundedSans,
                            fontWeight = textWeight,
                            fontSize = 15.sp,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Share Action Chip
// ────────────────────────────────────────────────────────────────────────────
@Composable
private fun ShareActionChip(
    icon: @Composable () -> Unit,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(containerColor)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                icon()
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.width(64.dp)
        )
    }
}

// ────────────────────────────────────────────────────────────────────────────
// Capture and file helpers
// ────────────────────────────────────────────────────────────────────────────

/** Records what [layer] last drew and centres it on a 9:16 story canvas. */
private suspend fun captureStoryBitmap(layer: GraphicsLayer, background: Color): Bitmap {
    val captured = layer.toImageBitmap().asAndroidBitmap()
    // Recorded layers come back as hardware bitmaps, which cannot be encoded directly.
    val bitmap = if (captured.config == Bitmap.Config.HARDWARE) {
        captured.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        captured
    }
    return letterboxToStory(bitmap, background)
}

/** Saves a captured card PNG in the cache and returns the file. */
private suspend fun saveBitmapToCache(context: Context, bitmap: Bitmap): File =
    withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, SHARE_CARD_CACHE_DIR).also { it.mkdirs() }
        val file = File(cacheDir, "startune_share_${System.currentTimeMillis()}.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file
    }

/** Saves a captured card PNG to Pictures/Startune via MediaStore. */
private suspend fun saveCardToPictures(
    context: Context,
    bitmap: Bitmap,
    song: Song
): Boolean = withContext(Dispatchers.IO) {
    try {
        val values = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "Startune_${song.title.take(20)}_${System.currentTimeMillis()}.png"
            )
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/Startune"
            )
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ) ?: return@withContext false
        context.contentResolver.openOutputStream(uri)?.use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) return@withContext false
        } ?: return@withContext false
        true
    } catch (e: Exception) {
        false
    }
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

private fun Color.toInstagramHex(): String {
    return String.format("#%06X", 0xFFFFFF and this.toArgb())
}

private fun shareToInstagramStory(
    context: Context,
    imageUri: Uri,
    topColorHex: String? = null,
    bottomColorHex: String? = null
) {
    val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
        type = "image/png"
        putExtra("interactive_asset_uri", imageUri)
        putExtra("content_url", STARTUNE_REPO_URL)
        if (topColorHex != null) {
            putExtra("top_background_color", topColorHex)
        }
        if (bottomColorHex != null) {
            putExtra("bottom_background_color", bottomColorHex)
        }
        `package` = INSTAGRAM_PACKAGE
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            `package` = INSTAGRAM_PACKAGE
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(fallback)
        } catch (ex: Exception) {
            Toast.makeText(
                context,
                context.getString(R.string.share_card_instagram_unavailable),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ShareBottomSheetEntryPoint {
    fun themeStateHolder(): ThemeStateHolder
}
