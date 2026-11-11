package com.startune.music.presentation.utils

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Morphs the corners of a segmented [base] shape into a fully rounded container as
 * [progress] goes from 0 to 1. Corner sizes are resolved per-measure, so percent-based
 * corners and any item height morph correctly.
 *
 * Moved here from EnhancedSongListItem so RankedSongRow can share the playing morph.
 */
class HighlightMorphShape(
    private val base: CornerBasedShape,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val full = size.minDimension / 2f
        fun corner(cornerSize: CornerSize): CornerSize =
            CornerSize(lerpFloat(cornerSize.toPx(size, density), full, progress))
        return base.copy(
            topStart = corner(base.topStart),
            topEnd = corner(base.topEnd),
            bottomEnd = corner(base.bottomEnd),
            bottomStart = corner(base.bottomStart),
        ).createOutline(size, layoutDirection, density)
    }
}

private fun lerpFloat(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
