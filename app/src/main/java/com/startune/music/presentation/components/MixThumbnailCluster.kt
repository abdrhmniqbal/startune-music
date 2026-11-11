package com.startune.music.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.startune.music.R
import com.startune.music.ui.theme.ShapeCache

/**
 * The mix's signature thumbnail: three overlapping song arts in the mix's
 * three shapes (6-sided star, circle, smooth corners) with -15/0/15
 * rotations, taken from the mix detail header. Shared by the detail header
 * (large form) and the library mix card (small form) so both stay identical.
 */
@Composable
fun MixThumbnailCluster(
    artUrls: List<String?>,
    centerSize: Dp,
    overlap: Dp,
    thirdShapeCornerRadius: Dp = 16.dp,
    borderWidth: Dp = 0.dp,
    centerThird: Boolean = false,
    modifier: Modifier = Modifier
) {
    val arts = artUrls.take(3)
    if (arts.isEmpty()) {
        Box(
            modifier = modifier
                .size(centerSize)
                .clip(ShapeCache.expressiveAvatar)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        return
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(-overlap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        arts.forEachIndexed { index, artUrl ->
            val size = when (index) {
                0 -> centerSize * 0.82f
                1 -> centerSize
                else -> centerSize * 0.82f
            }
            val rotation = when (index) {
                0 -> -15f
                1 -> 0f
                else -> 15f
            }
            val shape = threeShapeSwitch(index, thirdShapeCornerRadius = thirdShapeCornerRadius)
            if (centerThird && index == 2) {
                // The detail header centers the third tile in the full header
                // width (its cascade overflows to the left); kept verbatim.
                Box(
                    modifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            Constraints.fixed(width = size.roundToPx(), height = size.roundToPx())
                        )
                        layout(constraints.maxWidth, placeable.height) {
                            val xOffset = (constraints.maxWidth - placeable.width) / 2
                            placeable.placeRelative(xOffset, 0)
                        }
                    }
                ) {
                    ClusterTile(size, rotation, shape, artUrl, borderWidth)
                }
            } else {
                ClusterTile(size, rotation, shape, artUrl, borderWidth)
            }
        }
    }
}

@Composable
private fun ClusterTile(
    size: Dp,
    rotation: Float,
    shape: Shape,
    artUrl: String?,
    borderWidth: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { rotationZ = rotation }
            .clip(shape)
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(borderWidth, MaterialTheme.colorScheme.surface, shape)
                } else {
                    Modifier
                }
            )
    ) {
        SmartImage(
            model = artUrl ?: R.drawable.rounded_album_24,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
