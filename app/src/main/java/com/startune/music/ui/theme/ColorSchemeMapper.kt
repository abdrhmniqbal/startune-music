package com.startune.music.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import com.startune.music.data.preferences.AppColorScheme
import com.startune.music.data.preferences.ColorSchemeIds
import com.startune.music.data.preferences.SchemeRoles
import com.startune.music.presentation.viewmodel.ColorSchemePair

private val HEX_REGEX = Regex("^#[0-9a-fA-F]{6}$")

fun isValidHex(hex: String): Boolean = HEX_REGEX.matches(hex.trim())

fun normalizeHex(hex: String): String {
    val trimmed = hex.trim().uppercase()
    return if (trimmed.startsWith("#")) trimmed else "#$trimmed"
}

fun hexToColor(hex: String): Color {
    val clean = hex.removePrefix("#")
    return Color(
        clean.substring(0, 2).toInt(16),
        clean.substring(2, 4).toInt(16),
        clean.substring(4, 6).toInt(16)
    )
}

private fun onColorFor(color: Color): Color =
    if (ColorUtils.calculateLuminance(color.toArgb()) > 0.55) Color.Black else Color.White

/**
 * Builds a full Material3 scheme from the six editable roles. on-colors are
 * picked by luminance and containers/variants are blended from the roles, so
 * every combination stays readable. Mirrors the role set the hardcoded
 * Startune schemes use (primary/secondary/tertiary/background/surface/error
 * plus containers, variants and outlines).
 */
fun SchemeRoles.toColorScheme(dark: Boolean): ColorScheme {
    val primary = hexToColor(primary)
    val secondary = hexToColor(secondary)
    val tertiary = hexToColor(tertiary)
    val background = hexToColor(background)
    val surface = hexToColor(surface)
    val error = hexToColor(error)
    val onBackground = onColorFor(background)
    val onSurface = onColorFor(surface)
    val surfaceVariant = Color(ColorUtils.blendARGB(surface.toArgb(), primary.toArgb(), 0.10f))
    val onSurfaceVariant = Color(ColorUtils.blendARGB(onSurface.toArgb(), background.toArgb(), 0.35f))
    val outline = Color(ColorUtils.blendARGB(onSurface.toArgb(), background.toArgb(), 0.45f))
    val primaryContainer = Color(ColorUtils.blendARGB(primary.toArgb(), background.toArgb(), 0.80f))
    val onPrimaryContainer = Color(ColorUtils.blendARGB(primary.toArgb(), onBackground.toArgb(), 0.55f))
    val builder = if (dark) ::darkColorScheme else ::lightColorScheme
    return builder(
        primary = primary,
        onPrimary = onColorFor(primary),
        secondary = secondary,
        onSecondary = onColorFor(secondary),
        tertiary = tertiary,
        onTertiary = onColorFor(tertiary),
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outline.copy(alpha = 0.6f),
        surfaceTint = primary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondaryContainer = secondary.copy(alpha = 0.15f),
        onSecondaryContainer = secondary.copy(alpha = 0.85f),
        error = error,
        onError = onColorFor(error)
    )
}

fun AppColorScheme.toColorSchemePair(): ColorSchemePair =
    ColorSchemePair(
        light = light.toColorScheme(dark = false),
        dark = dark.toColorScheme(dark = true)
    )

/**
 * Resolves the selected scheme id to the pair StartuneTheme should use as its
 * base. Dynamic Color resolves to null (StartuneTheme keeps its current
 * behavior: wallpaper dynamic color on Android 12+, Startune pair below it).
 * Startune resolves to the existing hardcoded pair, byte-identical to today.
 * An unknown id (e.g. a deleted custom theme) also resolves to null.
 */
fun resolveSelectedSchemePair(
    selectedId: String,
    customs: List<AppColorScheme>
): ColorSchemePair? {
    if (selectedId == ColorSchemeIds.DYNAMIC) return null
    if (selectedId == ColorSchemeIds.STARTUNE) {
        return ColorSchemePair(light = LightColorScheme, dark = DarkColorScheme)
    }
    val scheme = BuiltInColorSchemes.find(selectedId)
        ?: customs.firstOrNull { it.id == selectedId }
        ?: return null
    return scheme.toColorSchemePair()
}
