package com.startune.music.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.startune.music.ui.theme.colorToHsv
import com.startune.music.ui.theme.hsvToColor
import kotlin.math.roundToInt

private val RAINBOW_COLORS = listOf(
    Color.Red,
    Color.Yellow,
    Color.Green,
    Color.Cyan,
    Color.Blue,
    Color.Magenta,
    Color.Red
)

/**
 * Handles press and drag on a box, calling [onPosition] with the pointer
 * position (in pixels, relative to the box) on press and on every move.
 * Used by both the saturation/value square and the hue slider so a tap
 * and a drag behave the same way.
 */
private fun Modifier.pressAndDrag(onPosition: (x: Float, y: Float) -> Unit) =
    pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { offset -> onPosition(offset.x, offset.y) },
            onDrag = { change, _ ->
                change.consume()
                onPosition(change.position.x, change.position.y)
            }
        )
    }

/**
 * A full-spectrum (rainbow) color picker: a saturation/value square plus a
 * hue slider. Dragging either updates the color; [onColorChange] receives
 * the new color on every move so the preview and hex field stay in sync.
 */
@Composable
fun HsvColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val hsv = remember(color) { colorToHsv(color) }
    var hue by remember(color) { mutableStateOf(hsv[0]) }
    var saturation by remember(color) { mutableStateOf(hsv[1]) }
    var value by remember(color) { mutableStateOf(hsv[2]) }

    fun emit() {
        onColorChange(hsvToColor(hue, saturation, value))
    }

    Column(modifier = modifier) {
        SaturationValueBox(
            hue = hue,
            saturation = saturation,
            value = value,
            onChange = { s, v ->
                saturation = s
                value = v
                emit()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
        )
        Spacer(modifier = Modifier.height(12.dp))
        HueSlider(
            hue = hue,
            onChange = { h ->
                hue = h
                emit()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
        )
    }
}

@Composable
private fun SaturationValueBox(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val hueColor = hsvToColor(hue, 1f, 1f)
    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .background(hueColor)
            .pressAndDrag { x, y ->
                if (size.width == 0 || size.height == 0) return@pressAndDrag
                onChange(
                    (x / size.width.toFloat()).coerceIn(0f, 1f),
                    (1f - y / size.height.toFloat()).coerceIn(0f, 1f)
                )
            }
    ) {
        // White (no saturation) on the left, transparent on the right.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.White, Color.Transparent)
                    )
                )
        )
        // Black (no value) at the bottom, transparent at the top.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )
        )
        val density = LocalDensity.current
        val thumbX = with(density) { (saturation * size.width).toDp() }
        val thumbY = with(density) { ((1f - value) * size.height).toDp() }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (thumbX.toPx() - 12.dp.toPx()).roundToInt(),
                        y = (thumbY.toPx() - 12.dp.toPx()).roundToInt()
                    )
                }
                .size(24.dp)
                .clip(CircleShape)
                .background(hsvToColor(hue, saturation, value))
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

@Composable
private fun HueSlider(
    hue: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    Box(
        modifier = modifier
            .onSizeChanged { size = it }
            .background(Brush.horizontalGradient(colors = RAINBOW_COLORS))
            .pressAndDrag { x, _ ->
                if (size.width == 0) return@pressAndDrag
                onChange((x / size.width.toFloat()).coerceIn(0f, 1f) * 360f)
            }
    ) {
        val density = LocalDensity.current
        val thumbX = with(density) { (hue / 360f * size.width).toDp() }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (thumbX.toPx() - 14.dp.toPx()).roundToInt(),
                        y = 0
                    )
                }
                .size(28.dp)
                .clip(CircleShape)
                .background(hsvToColor(hue, 1f, 1f))
                .border(2.dp, Color.White, CircleShape)
        )
    }
}
