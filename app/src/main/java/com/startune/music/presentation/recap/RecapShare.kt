package com.startune.music.presentation.recap

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import com.startune.music.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val RECAP_SHARE_DIRECTORY = "recap"

/**
 * Saves what [layer] last recorded as a PNG in the cache and opens the share sheet for it.
 * Each period overwrites its previous image, so the cache never collects old copies.
 */
internal suspend fun shareRecapImage(
    context: Context,
    layer: GraphicsLayer,
    fileStem: String,
    shareText: String,
    background: Color
) {
    val captured = layer.toImageBitmap().asAndroidBitmap()
    val uri = withContext(Dispatchers.IO) {
        // Recorded layers come back as hardware bitmaps, which cannot be encoded directly.
        val bitmap = if (captured.config == Bitmap.Config.HARDWARE) {
            captured.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            captured
        }
        // The story canvas is 9:16, so the image fills an Instagram story with no crop.
        val story = letterboxToStory(bitmap, background)
        val directory = File(context.cacheDir, RECAP_SHARE_DIRECTORY).apply { mkdirs() }
        val file = File(directory, "$fileStem.png")
        file.outputStream().use { stream ->
            check(story.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "PNG encoding failed" }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }

    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, shareText)
        // ClipData lets the share sheet preview the image and carries the read grant.
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, context.getString(R.string.recap_share_chooser_title))
    if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
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
