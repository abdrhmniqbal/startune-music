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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.startune.music.R
import java.text.NumberFormat

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
            text = stringResource(R.string.recap_summary_kicker),
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
                onClick = { actions.onShare(cardLayer) },
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(palette.accent)
            .padding(20.dp),
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
                    text = stringResource(
                        if (slide.isSoFar) R.string.recap_summary_title_so_far else R.string.recap_summary_title,
                        slide.year
                    ),
                    color = ink,
                    maxFontSize = 30.sp,
                    minFontSize = 20.sp,
                    weight = 880
                )
            }
        }
        Column {
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
        slide.topGenre?.let { genre ->
            Column {
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
