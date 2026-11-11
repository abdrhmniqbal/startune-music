package com.startune.music.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import com.startune.music.R
import com.startune.music.presentation.components.subcomps.PlayingEqIcon
import com.startune.music.ui.theme.ShapeCache

/**
 * Shared ranked row for Home Top Songs and Genre Top Songs (direction A:
 * pager of 5 rows per page, rows made expressive).
 *
 * Podium, global by rank (not per page): #1 gets the biggest art and a
 * solid badge, #2-3 medium, #4 and below the quiet standard row.
 * The badge is the expressive cookie shape.
 *
 * Playing state morphs like the track list: the container color lerps
 * to primaryContainer and the corners lerp to fully rounded (tween 400).
 * Press scales the row to 0.97 with a spring.
 *
 * entranceProgress (0..1) is driven by the pager offset by the caller:
 * rows fade and slide up with a per-row stagger while their page
 * arrives, and sit at full size once the page settles.
 */
@Composable
fun RankedSongRow(
    rank: Int,
    title: String,
    artist: String,
    artUri: String?,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    selectionIndex: Int? = null,
    entranceProgress: Float = 1f,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val highlightProgress by animateFloatAsState(
        targetValue = if (isCurrentSong) 1f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "rankedRowHighlight"
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rankedRowPressScale"
    )
    val containerColor = lerpColor(Color.Transparent, colors.primaryContainer, highlightProgress)
    val cornerRadius = lerpDp(12.dp, 50.dp, highlightProgress)
    val albumCornerRadius = lerpDp(14.dp, 50.dp, highlightProgress)
    val artSize = when {
        rank == 1 -> 64.dp
        rank <= 3 -> 56.dp
        else -> 52.dp
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                alpha = entranceProgress
                translationY = (1f - entranceProgress) * 24f
            },
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    interactionSource = interactionSource
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RankedSongRankBadge(rank = rank)
            Box(
                modifier = Modifier.size(artSize),
                contentAlignment = Alignment.Center
            ) {
                SmartImage(
                    model = artUri,
                    contentDescription = title,
                    modifier = Modifier.size(artSize),
                    shape = RoundedCornerShape(albumCornerRadius)
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                color = colors.primary.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(albumCornerRadius)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectionIndex != null && selectionIndex >= 0) {
                            Text(
                                text = selectionIndex.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = stringResource(R.string.presentation_batch_g_list_cd_selected),
                                tint = colors.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isCurrentSong) colors.primary else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isCurrentSong) {
                PlayingEqIcon(
                    modifier = Modifier.size(width = 18.dp, height = 16.dp),
                    color = colors.primary,
                    isPlaying = isPlaying
                )
            }
        }
    }
}

@Composable
private fun RankedSongRankBadge(rank: Int) {
    val colors = MaterialTheme.colorScheme
    val containerColor = when {
        rank == 1 -> colors.primary
        rank <= 3 -> colors.primary.copy(alpha = 0.24f)
        else -> colors.surfaceContainerHighest
    }
    val contentColor = when {
        rank == 1 -> colors.onPrimary
        rank <= 3 -> colors.primary
        else -> colors.onSurfaceVariant
    }
    val badgeSize = if (rank == 1) 40.dp else 34.dp
    val textStyle = if (rank == 1) MaterialTheme.typography.titleLarge
    else MaterialTheme.typography.titleMedium
    Surface(
        shape = ShapeCache.expressiveAvatar,
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = badgeSize, minHeight = badgeSize)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                style = textStyle,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
