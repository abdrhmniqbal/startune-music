package com.startune.music.presentation.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs

// Edge scale for horizontal carousels: an item sits at full size at
// the viewport center and slightly smaller at the edge (visibly, not
// tiny), growing as it scrolls toward the center. Scroll-driven, so
// no separate animation is needed.
private const val CarouselEdgeScale = 0.9f

@Composable
fun Modifier.carouselItemScale(listState: LazyListState, index: Int): Modifier {
    val scale by remember(listState, index) {
        derivedStateOf {
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                ?: return@derivedStateOf 1f
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            val itemCenter = item.offset + item.size / 2f
            val distance = abs(viewportCenter - itemCenter)
            val maxDistance = (info.viewportEndOffset - info.viewportStartOffset) / 2f
            if (maxDistance <= 0f) 1f
            else (1f - (distance / maxDistance) * (1f - CarouselEdgeScale))
                .coerceIn(CarouselEdgeScale, 1f)
        }
    }
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
