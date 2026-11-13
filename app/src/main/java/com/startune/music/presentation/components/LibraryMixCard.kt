package com.startune.music.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.map
import com.startune.music.R
import com.startune.music.data.model.Playlist
import com.startune.music.data.model.Song
import com.startune.music.presentation.components.subcomps.EnhancedSongListItem
import com.startune.music.presentation.utils.getSegmentedItemShape
import com.startune.music.presentation.viewmodel.PlayerViewModel
import com.startune.music.ui.theme.RoundedSans
import com.startune.music.utils.formatSongCount
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

/**
 * A mix in the Library MIXES tab: header (thumbnail cluster, name, song
 * count, chevron) toggles an animated preview of its first 3 songs; the
 * See all footer is always visible and opens the mix detail page.
 */
@Composable
fun LibraryMixCard(
    playlist: Playlist,
    playerViewModel: PlayerViewModel,
    onSeeAllClick: () -> Unit,
    onMoreOptionsClick: (Song) -> Unit,
    onMixMoreOptionsClick: ((Playlist) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val mixSongs by remember(playlist.songIds, playerViewModel) {
        playerViewModel.observeSongs(playlist.songIds)
            .map<List<Song>, List<Song>?> { it }
    }.collectAsStateWithLifecycle(
        initialValue = remember(playlist.songIds) {
            if (playlist.songIds.isEmpty()) emptyList<Song>() else null
        }
    )
    val allSongs = remember(mixSongs) { (mixSongs ?: emptyList()).toImmutableList() }
    val previewSongs = remember(allSongs) { allSongs.take(3) }
    val clusterArts = remember(allSongs) {
        allSongs.mapNotNull { it.albumArtUriString }.distinct().take(3)
    }
    val stablePlayerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()
    var expanded by rememberSaveable(playlist.id) { mutableStateOf(true) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "mixCardChevronRotation"
    )

    Card(
        shape = AbsoluteSmoothCornerShape(
            cornerRadiusBR = 30.dp,
            smoothnessAsPercentTL = 60,
            cornerRadiusTR = 30.dp,
            smoothnessAsPercentTR = 60,
            cornerRadiusBL = 30.dp,
            smoothnessAsPercentBL = 60,
            cornerRadiusTL = 30.dp,
            smoothnessAsPercentBR = 60
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.elevatedCardElevation(0.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MixThumbnailCluster(
                    artUrls = clusterArts,
                    centerSize = 56.dp,
                    overlap = 20.dp,
                    borderWidth = 2.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = playlist.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = RoundedSans),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatSongCount(playlist.songIds.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = chevronRotation },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onMixMoreOptionsClick != null) {
                    ItemMoreOptionsButton(
                        title = playlist.name,
                        onClick = { onMixMoreOptionsClick?.invoke() }
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(durationMillis = 300)) +
                    fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 300)) +
                    fadeOut(animationSpec = tween(durationMillis = 300))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, start = 8.dp, end = 8.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    previewSongs.forEachIndexed { index, song ->
                        EnhancedSongListItem(
                            song = song,
                            isCurrentSong = stablePlayerState.currentSong?.id == song.id,
                            isPlaying = stablePlayerState.isPlaying &&
                                stablePlayerState.currentSong?.id == song.id,
                            containerColorOverride = MaterialTheme.colorScheme.surfaceContainerLow,
                            onMoreOptionsClick = onMoreOptionsClick,
                            customShape = getSegmentedItemShape(index, previewSongs.size),
                            onClick = {
                                playerViewModel.showAndPlaySong(
                                    song = song,
                                    contextSongs = allSongs,
                                    queueName = playlist.name,
                                    isVoluntaryPlay = false
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            FilledTonalButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
                onClick = onSeeAllClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.Transparent
                ),
                shape = AbsoluteSmoothCornerShape(
                    cornerRadiusTL = 10.dp,
                    cornerRadiusTR = 10.dp,
                    smoothnessAsPercentTL = 70,
                    smoothnessAsPercentTR = 70,
                    cornerRadiusBL = 60.dp,
                    cornerRadiusBR = 60.dp,
                    smoothnessAsPercentBL = 70,
                    smoothnessAsPercentBR = 70
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.mix_see_all),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        painter = painterResource(R.drawable.rounded_arrow_forward_24),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
