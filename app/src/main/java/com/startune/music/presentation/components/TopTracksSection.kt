package com.startune.music.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.startune.music.R
import com.startune.music.data.stats.PlaybackStatsRepository
import kotlin.math.abs
import kotlinx.collections.immutable.ImmutableList

private const val TOP_TRACKS_PAGE_SIZE = 5
private const val TOP_TRACKS_HOME_LIMIT = 15

/**
 * Home Top Tracks: plain list (title + ranked rows) with the slideable
 * pager, 15 tracks in pages of 5. The playing track shows the same
 * equalizer indicator as song lists elsewhere.
 */
@Composable
fun TopTracksSection(
    tracks: ImmutableList<PlaybackStatsRepository.SongPlaybackSummary>,
    currentSongId: String?,
    isPlaying: Boolean,
    onTrackClick: (PlaybackStatsRepository.SongPlaybackSummary) -> Unit,
    onOpenAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topTracks = remember(tracks) { tracks.take(TOP_TRACKS_HOME_LIMIT) }
    if (topTracks.isEmpty()) return
    val pages = remember(topTracks) { topTracks.chunked(TOP_TRACKS_PAGE_SIZE) }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val contentColor = MaterialTheme.colorScheme.onSurface
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.top_songs_title),
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                FilledIconButton(
                    modifier = Modifier
                        .height(40.dp)
                        .width(64.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    onClick = onOpenAllClick
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 0.dp),
                pageSpacing = 24.dp,
                verticalAlignment = Alignment.Top
            ) { page ->
                val pageOffset = abs(page - pagerState.currentPage - pagerState.currentPageOffsetFraction)
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pages[page].forEachIndexed { index, track ->
                        key(track.songId) {
                            RankedSongRow(
                                rank = page * TOP_TRACKS_PAGE_SIZE + index + 1,
                                title = track.title,
                                artist = track.artist,
                                artUri = track.albumArtUri,
                                isCurrentSong = currentSongId == track.songId,
                                isPlaying = isPlaying,
                                onClick = { onTrackClick(track) },
                                entranceProgress = (1f - pageOffset * (1f + index * 0.15f)).coerceIn(0f, 1f)
                            )
                        }
                    }
                }
            }
    }
}
