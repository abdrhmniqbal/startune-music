package com.startune.music.presentation.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import coil.compose.AsyncImage
import com.startune.music.R
import com.startune.music.data.model.JumpBackInItem
import com.startune.music.data.model.PlaybackSource
import com.startune.music.data.model.PlayedEntityType
import com.startune.music.utils.resolvePlaylistCoverContentColor
import kotlinx.collections.immutable.ImmutableList

/**
 * Home "Jump back in": the four most recently played albums, artists,
 * playlists, and mixes, as horizontal cards in a 2x2 grid, like Spotify.
 * The section is hidden entirely until the first entity has been recorded.
 *
 * The artwork follows the entity type: an album shows its square cover, an
 * artist shows a circle, a playlist or mix shows its custom cover when it has
 * one and a 2x2 grid of its songs' album art otherwise. The card of the
 * entity the player is currently playing from is highlighted like a playing
 * track: its corners round out and its colors move to the primary container.
 */
@Composable
fun JumpBackInSection(
    items: ImmutableList<JumpBackInItem>,
    onItemClick: (JumpBackInItem) -> Unit,
    modifier: Modifier = Modifier,
    currentPlaybackSource: PlaybackSource? = null
) {
    if (items.isEmpty()) return
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.jump_back_in_title),
            style = MaterialTheme.typography.titleLargeEmphasized,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { item ->
                        JumpBackInCard(
                            item = item,
                            isCurrentSource = currentPlaybackSource?.historyType == item.type &&
                                currentPlaybackSource?.historyId == item.id,
                            onClick = { onItemClick(item) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun JumpBackInCard(
    item: JumpBackInItem,
    isCurrentSource: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The playing-track highlight: corners round out and the colors move to
    // the primary container, animated the same way as a playing song row: one
    // animated progress, with radius and colors lerped from it.
    val colors = MaterialTheme.colorScheme
    val transition = updateTransition(isCurrentSource, label = "JumpBackInCardHighlight")
    val highlightProgress by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 400) },
        label = "highlightProgress"
    ) { highlighted -> if (highlighted) 1f else 0f }
    val cornerRadius = lerpDp(12.dp, 32.dp, highlightProgress)
    val containerColor = lerpColor(colors.surfaceContainer, colors.primaryContainer, highlightProgress)
    val contentColor = lerpColor(colors.onSurface, colors.onPrimaryContainer, highlightProgress)

    Card(
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(0.dp),
        modifier = modifier.height(64.dp).clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            JumpBackInArtwork(
                item = item,
                cornerRadius = cornerRadius,
                highlightProgress = highlightProgress,
                contentColor = contentColor
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.subtitle ?: stringResource(item.type.typeLabelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * The 64dp artwork slot on the card's left edge. Its left corners follow the
 * card's animated radius, so the artwork stays flush when the card rounds out
 * while playing.
 */
@Composable
private fun JumpBackInArtwork(
    item: JumpBackInItem,
    cornerRadius: androidx.compose.ui.unit.Dp,
    highlightProgress: Float,
    contentColor: Color
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(topStart = cornerRadius, bottomStart = cornerRadius))
    ) {
        when (item.type) {
            PlayedEntityType.ARTIST -> {
                if (item.artworkUri != null) {
                    AsyncImage(
                        model = item.artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    JumpBackInFallbackTile(
                        backgroundColor = lerpColor(
                            colors.surfaceContainerHighest,
                            colors.primaryContainer,
                            highlightProgress
                        ),
                        contentColor = contentColor
                    )
                }
            }
            PlayedEntityType.PLAYLIST, PlayedEntityType.MIX -> {
                if (item.artworkUri != null) {
                    AsyncImage(
                        model = item.artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (item.coverColorArgb != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(item.coverColorArgb)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = resolvePlaylistCoverContentColor(
                                item.coverColorArgb,
                                colors
                            ),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else if (item.collageArtworkUris.isNotEmpty()) {
                    JumpBackInArtCollage(
                        uris = item.collageArtworkUris,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    JumpBackInFallbackTile(
                        backgroundColor = lerpColor(
                            colors.surfaceContainerHighest,
                            colors.primaryContainer,
                            highlightProgress
                        ),
                        contentColor = contentColor
                    )
                }
            }
            PlayedEntityType.ALBUM -> {
                if (item.artworkUri != null) {
                    AsyncImage(
                        model = item.artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    JumpBackInFallbackTile(
                        backgroundColor = lerpColor(
                            colors.surfaceContainerHighest,
                            colors.primaryContainer,
                            highlightProgress
                        ),
                        contentColor = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun JumpBackInFallbackTile(
    backgroundColor: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

/**
 * A 2x2 grid of square album artworks, mirroring the playlist collage: the
 * layout adapts when there are fewer than 4 distinct covers instead of
 * repeating one.
 */
@Composable
private fun JumpBackInArtCollage(
    uris: ImmutableList<String>,
    modifier: Modifier = Modifier
) {
    when (uris.size) {
        1 -> {
            AsyncImage(
                model = uris[0],
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier.aspectRatio(1f)
            )
        }
        2 -> {
            Row(modifier = modifier.aspectRatio(1f)) {
                uris.take(2).forEach { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
        3 -> {
            Column(modifier = modifier.aspectRatio(1f)) {
                AsyncImage(
                    model = uris[0],
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                Row(modifier = Modifier.weight(1f)) {
                    uris.drop(1).take(2).forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
        else -> {
            Column(modifier = modifier.aspectRatio(1f)) {
                Row(modifier = Modifier.weight(1f)) {
                    uris.take(2).forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
                Row(modifier = Modifier.weight(1f)) {
                    uris.drop(2).take(2).forEach { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

private val PlayedEntityType.typeLabelRes: Int
    get() = when (this) {
        PlayedEntityType.ALBUM -> R.string.jump_back_in_type_album
        PlayedEntityType.ARTIST -> R.string.jump_back_in_type_artist
        PlayedEntityType.PLAYLIST -> R.string.jump_back_in_type_playlist
        PlayedEntityType.MIX -> R.string.jump_back_in_type_mix
    }
