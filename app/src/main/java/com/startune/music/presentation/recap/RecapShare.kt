package com.startune.music.presentation.recap

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Captures what [layer] last drew, centred on a 9:16 story canvas, and
 * returns the finished bitmap. The caller decides what to do with it: the
 * recap share sheet previews it and offers Instagram, Save, or Share More.
 */
internal suspend fun captureRecapBitmap(
    layer: GraphicsLayer,
    background: Color
): Bitmap {
    val captured = layer.toImageBitmap().asAndroidBitmap()
    return withContext(Dispatchers.IO) {
        // Recorded layers come back as hardware bitmaps, which cannot be encoded directly.
        val bitmap = if (captured.config == Bitmap.Config.HARDWARE) {
            captured.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            captured
        }
        // The story canvas is 9:16, so the image fills an Instagram story with no crop.
        letterboxToStory(bitmap, background)
    }
}

/**
 * The 9:16 canvas a captured bitmap is centred on, so the shared image fits an Instagram
 * story. The canvas always contains the bitmap at its own size: it is never upscaled or
 * stretched, and its width is a multiple of 9 so width*16 == height*9 exactly.
 */
internal fun storyCanvasSize(width: Int, height: Int): Pair<Int, Int> {
    val canvasWidth = ((maxOf(width, (height * 9 + 15) / 16) + 8) / 9) * 9
    return canvasWidth to canvasWidth / 9 * 16
}

/** Centres [bitmap] on a 9:16 canvas filled with [background]; returns [bitmap] unchanged when it already is 9:16. */
internal fun letterboxToStory(bitmap: Bitmap, background: Color): Bitmap {
    val (canvasWidth, canvasHeight) = storyCanvasSize(bitmap.width, bitmap.height)
    if (canvasWidth == bitmap.width && canvasHeight == bitmap.height) return bitmap
    return Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888).also { story ->
        Canvas(story).apply {
            drawColor(background.toArgb())
            drawBitmap(
                bitmap,
                ((canvasWidth - bitmap.width) / 2).toFloat(),
                ((canvasHeight - bitmap.height) / 2).toFloat(),
                null
            )
        }
    }
}
