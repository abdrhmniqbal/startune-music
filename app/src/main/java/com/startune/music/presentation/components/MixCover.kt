package com.startune.music.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.size.Size
import com.startune.music.data.model.Song
import com.startune.music.R
import com.startune.music.ui.theme.ShapeCache
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Mix cover: the old playlist collage arrangement, before it became a square
 * 2x2 grid. The artwork count decides the layout: 1 fills the whole cover,
 * 2 stack vertically, 3 form a triangle (1 centered on top, 2 below), 4 or
 * more form a 2x2 grid. Each tile uses the cookie shape instead of a circle.
 */
@Composable
fun MixCover(
    songs: ImmutableList<Song>,
    modifier: Modifier = Modifier,
) {
    val covers = remember(songs) {
        songs.mapNotNull { song ->
            song.albumArtUriString?.takeIf { it.isNotBlank() }?.let { it to song.title }
        }.distinctBy { it.first }
    }
    val cookieShape = ShapeCache.expressiveAvatar
    if (covers.isEmpty()) {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .clip(cookieShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = stringResource(R.string.mix_cover_cd),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    } else {
        Box(modifier = modifier.aspectRatio(1f)) {
            when (covers.size) {
                1 -> {
                    SmartImage(
                        model = covers[0].first,
                        contentDescription = covers[0].second,
                        contentScale = ContentScale.Crop,
                        targetSize = Size(256, 256),
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(cookieShape)
                    )
                }
                2 -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        covers.take(2).forEach { cover ->
                            SmartImage(
                                model = cover.first,
                                contentDescription = cover.second,
                                contentScale = ContentScale.Crop,
                                targetSize = Size(128, 128),
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(cookieShape)
                            )
                        }
                    }
                }
                3 -> {
                    Layout(
                        content = {
                            covers.take(3).forEach { cover ->
                                SmartImage(
                                    model = cover.first,
                                    contentDescription = cover.second,
                                    contentScale = ContentScale.Crop,
                                    targetSize = Size(128, 128),
                                    modifier = Modifier.clip(cookieShape)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { measurables, constraints ->
                        val separation = 2.dp.toPx()
                        val itemSize = floor((constraints.maxWidth * 2f / (2f + sqrt(3f))) - separation).toInt()

                        val placeables = measurables.map {
                            it.measure(
                                constraints.copy(
                                    minWidth = itemSize, maxWidth = itemSize,
                                    minHeight = itemSize, maxHeight = itemSize
                                )
                            )
                        }

                        val l = itemSize + separation
                        val h = l * sqrt(3f) / 2f

                        val collageHeight = h + itemSize
                        val collageWidth = l + itemSize

                        val offsetX = ((constraints.maxWidth - collageWidth) / 2f).toInt()
                        val offsetY = ((constraints.maxHeight - collageHeight) / 2f).toInt()

                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeables[0].placeRelative(
                                x = (offsetX + (collageWidth - itemSize) / 2f).toInt(),
                                y = offsetY
                            )
                            placeables[1].placeRelative(
                                x = offsetX,
                                y = (offsetY + h).toInt()
                            )
                            placeables[2].placeRelative(
                                x = (offsetX + l).toInt(),
                                y = (offsetY + h).toInt()
                            )
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            covers.take(2).forEach { cover ->
                                SmartImage(
                                    model = cover.first,
                                    contentDescription = cover.second,
                                    contentScale = ContentScale.Crop,
                                    targetSize = Size(128, 128),
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(cookieShape)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            covers.drop(2).take(2).forEach { cover ->
                                SmartImage(
                                    model = cover.first,
                                    contentDescription = cover.second,
                                    contentScale = ContentScale.Crop,
                                    targetSize = Size(128, 128),
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(cookieShape)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
