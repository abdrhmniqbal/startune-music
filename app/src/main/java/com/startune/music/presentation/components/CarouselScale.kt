package com.startune.music.presentation.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

// Edge scale for horizontal carousels: a fully visible item sits at full
// size, and an item peeking in from the edge shrinks toward 0.9 in
// proportion to how much of it is hidden. The first items are therefore
// full size at rest and only the trailing peek reads smaller.
// Scroll-driven, so no separate animation is needed.
private const val CarouselEdgeScale = 0.9f

@Composable
fun Modifier.carouselItemScale(listState: LazyListState, index: Int): Modifier {
    val scale by remember(listState, index) {
        derivedStateOf {
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                ?: return@derivedStateOf 1f
            val viewportStart = info.viewportStartOffset.toFloat()
            val viewportEnd = info.viewportEndOffset.toFloat()
            val itemStart = item.offset.toFloat()
            val itemEnd = itemStart + item.size
            val visibleWidth = (minOf(itemEnd, viewportEnd) - maxOf(itemStart, viewportStart))
                .coerceAtLeast(0f)
            val visibleFraction = if (item.size <= 0) 1f
            else (visibleWidth / item.size).coerceIn(0f, 1f)
            CarouselEdgeScale + (1f - CarouselEdgeScale) * visibleFraction
        }
    }
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
