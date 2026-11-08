package com.startune.music.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.startune.music.R
import com.startune.music.data.model.Song
import com.startune.music.presentation.components.subcomps.PlayingEqIcon
import com.startune.music.presentation.model.RecentlyPlayedSongUiModel
import kotlinx.collections.immutable.ImmutableList

private const val HomeRecentlyPlayedLimit = 10
internal const val RecentlyPlayedSectionMinSongsToShow = 4
private val HomeRecentlyPlayedCardWidth = 144.dp
private val HomeRecentlyPlayedCardSpacing = 12.dp
private val HomeRecentlyPlayedDefaultContentPadding = PaddingValues(horizontal = 8.dp)

@Composable
fun RecentlyPlayedSection(
    songs: ImmutableList<RecentlyPlayedSongUiModel>,
    onSongClick: (Song) -> Unit,
    onOpenAllClick: () -> Unit,
    currentSongId: String? = null,
    isPlaying: Boolean = true,
    contentPadding: PaddingValues = HomeRecentlyPlayedDefaultContentPadding,
    modifier: Modifier = Modifier
) {
    val visibleSongs = remember(songs) { songs.take(HomeRecentlyPlayedLimit) }
    if (visibleSongs.size < RecentlyPlayedSectionMinSongsToShow) return

    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(start = 6.dp),
                text = stringResource(R.string.presentation_batch_g_recently_played_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            FilledIconButton(
                modifier = Modifier
                    .height(40.dp)
                    .width(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.secondary
                ),
                onClick = onOpenAllClick,
                enabled = visibleSongs.size >= RecentlyPlayedSectionMinSongsToShow
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        LazyRow(
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(HomeRecentlyPlayedCardSpacing)
        ) {
            items(visibleSongs, key = { it.song.id }) { item ->
                RecentlyPlayedGridCard(
                    item = item,
                    isCurrentSong = currentSongId == item.song.id,
                    isPlaying = isPlaying,
                    onClick = { onSongClick(item.song) }
                )
            }
        }
    }
}

@Composable
private fun RecentlyPlayedGridCard(
    item: RecentlyPlayedSongUiModel,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(HomeRecentlyPlayedCardWidth)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SmartImage(
            model = item.song.albumArtUriString,
            contentDescription = null,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(HomeRecentlyPlayedCardWidth)
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.song.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentSong) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isCurrentSong) {
                    PlayingEqIcon(
                        modifier = Modifier.size(width = 18.dp, height = 16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        isPlaying = isPlaying
                    )
                }
            }
            Text(
                text = item.song.displayArtist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
