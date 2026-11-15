package com.startune.music.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.size.Size
import com.startune.music.R
import com.startune.music.data.model.Song
import kotlinx.collections.immutable.ImmutableList

/**
 * Playlist cover collage: a 2x2 grid of square album artworks, mirroring the
 * old Startune playlist artwork. The covers are distinct albums first (see
 * [distinctAlbumCovers]); when the playlist has fewer than 4 distinct albums
 * the layout adapts instead of repeating a cover, so no artwork ever appears
 * twice.
 */
@Composable
fun PlaylistArtCollage(
    songs: ImmutableList<Song>,
    modifier: Modifier = Modifier,
) {
    val covers = remember(songs) { distinctAlbumCovers(songs) }
    if (covers.isEmpty()) {
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = stringResource(R.string.cd_playlist_artwork),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    } else if (covers.size == 1) {
        SmartImage(
            model = covers[0].first,
            contentDescription = covers[0].second,
            contentScale = ContentScale.Crop,
            targetSize = Size(256, 256),
            modifier = modifier.aspectRatio(1f)
        )
    } else if (covers.size == 2) {
        Row(modifier = modifier.aspectRatio(1f)) {
            covers.take(2).forEach { cover ->
                SmartImage(
                    model = cover.first,
                    contentDescription = cover.second,
                    contentScale = ContentScale.Crop,
                    targetSize = Size(128, 128),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    } else if (covers.size == 3) {
        Column(modifier = modifier.aspectRatio(1f)) {
            SmartImage(
                model = covers[0].first,
                contentDescription = covers[0].second,
                contentScale = ContentScale.Crop,
                targetSize = Size(256, 128),
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            Row(modifier = Modifier.weight(1f)) {
                covers.drop(1).take(2).forEach { cover ->
                    SmartImage(
                        model = cover.first,
                        contentDescription = cover.second,
                        contentScale = ContentScale.Crop,
                        targetSize = Size(128, 128),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    } else {
        Column(modifier = modifier.aspectRatio(1f)) {
            Row(modifier = Modifier.weight(1f)) {
                SmartImage(
                    model = covers[0].first,
                    contentDescription = covers[0].second,
                    contentScale = ContentScale.Crop,
                    targetSize = Size(128, 128),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                SmartImage(
                    model = covers[1].first,
                    contentDescription = covers[1].second,
                    contentScale = ContentScale.Crop,
                    targetSize = Size(128, 128),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
            Row(modifier = Modifier.weight(1f)) {
                SmartImage(
                    model = covers[2].first,
                    contentDescription = covers[2].second,
                    contentScale = ContentScale.Crop,
                    targetSize = Size(128, 128),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                SmartImage(
                    model = covers[3].first,
                    contentDescription = covers[3].second,
                    contentScale = ContentScale.Crop,
                    targetSize = Size(128, 128),
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        }
    }
}
