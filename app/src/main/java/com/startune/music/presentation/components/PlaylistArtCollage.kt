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
 * old Startune playlist artwork. Up to 4 distinct covers are used, repeated
 * cyclically when the playlist has fewer.
 */
@Composable
fun PlaylistArtCollage(
    songs: ImmutableList<Song>,
    modifier: Modifier = Modifier,
) {
    val covers = remember(songs) {
        val distinct = songs.mapNotNull { song ->
            song.albumArtUriString?.takeIf { it.isNotBlank() }?.let { it to song.title }
        }.distinctBy { it.first }
        if (distinct.isEmpty()) emptyList() else List(4) { index -> distinct[index % distinct.size] }
    }
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
