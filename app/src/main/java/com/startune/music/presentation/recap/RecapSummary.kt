@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.startune.music.presentation.recap

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.startune.music.R
import java.text.NumberFormat
import kotlin.random.Random

/** Closing slide: a shareable card of the year's highlights, plus what to do next. */
@Composable
internal fun RecapSummarySlide(
    slide: RecapSlide.Summary,
    palette: RecapPalette,
    reveal: () -> Float,
    contentPadding: PaddingValues,
    scrollState: ScrollState,
    actions: RecapSummaryActions
) {
    val cardLayer = rememberGraphicsLayer()
    RecapSlideColumn(contentPadding = contentPadding, scrollState = scrollState, horizontalAlignment = Alignment.CenterHorizontally) {
        RecapKicker(
            text = stringResource(recapSummaryKickerRes(slide.range)),
            color = palette.content,
            textAlign = TextAlign.Center,
            modifier = Modifier.recapReveal(reveal, order = 0)
        )
        Spacer(Modifier.height(4.dp))
        // The captured area includes a margin of page colour, so the shared image has no
        // transparent corners for messengers to turn black.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .recapReveal(reveal, order = 1)
                .drawWithContent {
                    cardLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(cardLayer)
                }
                .background(palette.background)
                .padding(12.dp)
        ) {
            RecapSummaryCard(slide = slide, palette = palette)
        }
        Spacer(Modifier.height(12.dp))
        // Stacked rather than side by side: translated labels do not fit half a phone's width.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .recapReveal(reveal, order = 3),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { actions.onShare(cardLayer, palette.background) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.content,
                    contentColor = palette.background
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.recap_share),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (slide.canPlayTopSongs) {
                FilledTonalButton(
                    onClick = actions.onPlayTopSongs,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = palette.content.copy(alpha = 0.14f),
                        contentColor = palette.content
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.recap_play_top_songs),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = actions.onReplay,
            colors = ButtonDefaults.textButtonColors(contentColor = palette.content),
            modifier = Modifier.recapReveal(reveal, order = 4)
        ) {
            Icon(Icons.Rounded.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.recap_replay))
        }
    }
}

@Composable
private fun RecapSummaryCard(slide: RecapSlide.Summary, palette: RecapPalette) {
    val locale = LocalConfiguration.current.locales[0]
    val minutes = remember(slide.totalMs, locale) {
        NumberFormat.getIntegerInstance(locale).format(recapListenedMinutes(slide.totalMs))
    }
    val ink = palette.onAccent
    val topArtist = slide.topArtists.firstOrNull()
    // The monogram fallback must stand out on the accent card, so it is drawn in the ink.
    val heroPalette = remember(palette) { palette.copy(accent = palette.onAccent, onAccent = palette.accent) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(palette.accent)
    ) {
        RecapSummaryBackdrop(slide = slide, ink = ink)
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero: the top artist's photo beside the title, big enough to anchor the poster.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (topArtist != null) {
                    RecapArtwork(
                        model = topArtist.imageUri,
                        shape = MaterialShapes.Cookie9Sided.toShape(),
                        palette = heroPalette,
                        monogram = rememberMonogram(topArtist.name),
                        targetSizePx = 256,
                        modifier = Modifier.size(112.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(MaterialShapes.Cookie9Sided.toShape())
                                .background(ink),
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.recap_intro_kicker),
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = rememberRecapFlexFont(weight = 680, width = 110f),
                            color = ink.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    RecapHeadline(
                        text = recapSummaryTitle(slide.range, slide.year, slide.endDate, slide.isSoFar),
                        color = ink,
                        maxFontSize = 30.sp,
                        minFontSize = 20.sp,
                        weight = 880
                    )
                }
            }
            // Headline stats, side by side: minutes listened next to the top genre.
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.recap_summary_minutes),
                        style = MaterialTheme.typography.labelLarge,
                        color = ink.copy(alpha = 0.78f)
                    )
                    RecapHeadline(
                        text = minutes,
                        color = ink,
                        maxFontSize = 52.sp,
                        minFontSize = 28.sp,
                        maxLines = 1,
                        weight = 950
                    )
                }
                slide.topGenre?.let { genre ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.recap_summary_top_genre),
                            style = MaterialTheme.typography.labelLarge,
                            color = ink.copy(alpha = 0.78f)
                        )
                        Text(
                            text = genre,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = rememberRecapFlexFont(weight = 800, width = 108f),
                            color = ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            // Ranked lists, side by side so the card stays a poster: every row is a rank plus
            // one line of name, so long names ellipsize instead of wrapping mid-word.
            if (slide.topArtists.isNotEmpty() || slide.topSongs.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (slide.topArtists.isNotEmpty()) {
                        SummaryRankColumn(
                            heading = stringResource(R.string.recap_summary_top_artists),
                            names = slide.topArtists.map { it.name },
                            ink = ink,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (slide.topSongs.isNotEmpty()) {
                        SummaryRankColumn(
                            heading = stringResource(R.string.recap_summary_top_songs),
                            names = slide.topSongs.map { it.title },
                            ink = ink,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.recap_summary_footer),
                style = MaterialTheme.typography.labelMedium,
                fontFamily = rememberRecapFlexFont(weight = 700, width = 110f),
                color = ink.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            }
    }
}

/**
 * Decorative shapes behind the poster content, so the card reads as expressive rather
 * than a clean flat panel. The shapes are drawn from the slide and the ink colour, so
 * they stay put across recompositions and change only when the recap itself changes.
 * They sit at the card's edges, partly outside it, at a low alpha so the text on top
 * keeps its contrast.
 */
@Composable
private fun RecapSummaryBackdrop(slide: RecapSlide.Summary, ink: Color) {
    // toShape() reads the density, so it runs here in the composable body, not inside
    // the remember block below.
    val cookieShapes = listOf(
        MaterialShapes.Cookie9Sided.toShape(),
        MaterialShapes.Cookie6Sided.toShape(),
        CircleShape
    )
    val shapes = remember(slide.range, slide.year, slide.endDate, ink) {
        val random = Random(slide.year * 31L + slide.range.ordinal * 7L + slide.endDate.dayOfYear + ink.hashCode())
        listOf(
            BackdropShape(
                shape = cookieShapes[random.nextInt(cookieShapes.size)],
                size = (176 + random.nextInt(48)).dp,
                alignment = Alignment.TopEnd,
                offsetX = (48 + random.nextInt(24)).dp,
                offsetY = (-56 + random.nextInt(16)).dp,
                rotation = random.nextFloat() * 360f,
                alpha = 0.07f + random.nextFloat() * 0.05f
            ),
            BackdropShape(
                shape = cookieShapes[random.nextInt(cookieShapes.size)],
                size = (144 + random.nextInt(48)).dp,
                alignment = Alignment.BottomStart,
                offsetX = (-64 + random.nextInt(24)).dp,
                offsetY = (40 + random.nextInt(24)).dp,
                rotation = random.nextFloat() * 360f,
                alpha = 0.07f + random.nextFloat() * 0.05f
            ),
            BackdropShape(
                shape = cookieShapes[random.nextInt(cookieShapes.size)],
                size = (96 + random.nextInt(40)).dp,
                alignment = Alignment.CenterEnd,
                offsetX = (56 + random.nextInt(24)).dp,
                offsetY = (8 + random.nextInt(24)).dp,
                rotation = random.nextFloat() * 360f,
                alpha = 0.06f + random.nextFloat() * 0.04f
            )
        )
    }
    Box(modifier = Modifier.fillMaxSize()) {
        shapes.forEach { shape ->
            Box(
                modifier = Modifier
                    .align(shape.alignment)
                    .offset(shape.offsetX, shape.offsetY)
                    .size(shape.size)
                    .rotate(shape.rotation)
                    .clip(shape.shape)
                    .background(ink.copy(alpha = shape.alpha))
            )
        }
    }
}

private data class BackdropShape(
    val shape: Shape,
    val size: Dp,
    val alignment: Alignment,
    val offsetX: Dp,
    val offsetY: Dp,
    val rotation: Float,
    val alpha: Float
)

/** One ranked list on the summary poster: a heading plus rank-and-name rows, one line each. */
@Composable
private fun SummaryRankColumn(
    heading: String,
    names: List<String>,
    ink: Color,
    modifier: Modifier = Modifier
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = heading,
            style = MaterialTheme.typography.labelLarge,
            color = ink.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        names.forEachIndexed { index, name ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = NumberFormat.getIntegerInstance(locale).format(index + 1),
                    modifier = Modifier.widthIn(min = 16.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = rememberRecapFlexFont(weight = 820, width = 110f),
                    color = ink.copy(alpha = if (index == 0) 1f else 0.7f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = rememberRecapFlexFont(weight = if (index == 0) 760 else 660, width = 104f),
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** Whether a graphics layer has anything to capture; a never-drawn layer has no size. */
internal val GraphicsLayer.hasContent: Boolean
    get() = size.width > 0 && size.height > 0
