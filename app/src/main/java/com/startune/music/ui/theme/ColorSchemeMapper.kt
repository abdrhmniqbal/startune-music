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
 * Returns [color] with its HSL lightness replaced by [targetLightness]
 * (0..1), keeping hue and saturation, so the result tracks the scheme's own
 * color family instead of drifting to neutral grey.
 */
private fun withLightness(color: Color, targetLightness: Float): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[2] = targetLightness.coerceIn(0.02f, 0.98f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun lightnessOf(color: Color): Float {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    return hsl[2]
}

/**
 * Fills the roles a six-role scheme leaves at the neutral Material defaults:
 * the surface container ramp, tertiary container, surface dim/bright and the
 * inverse roles. Everything is derived from the scheme's own surface,
 * background and accent colors, so cards, list items, top bars and the
 * navbar track the selected scheme instead of looking like a second theme.
 *
 * The container ramp is anchored on the background's lightness, so every
 * step ends up clearly lighter or darker than the page behind it, whatever
 * the scheme: dark steps sit below/above the background by at least 7
 * lightness points, light steps by at least 5. That holds for schemes whose
 * surface is darker than their background too (Catppuccin), and for custom
 * schemes, because the ramp never copies the surface value itself.
 */
fun ColorScheme.withDerivedRoles(dark: Boolean): ColorScheme {
    val bgLightness = lightnessOf(background) * 100f
    val surfaceLightness = lightnessOf(surface) * 100f
    val (lowestTarget, lowTarget, containerTarget, highTarget, highestTarget) = if (dark) {
        val lowest = bgLightness - 12f
        val low = maxOf(bgLightness + 7f, surfaceLightness + 2f)
        val container = maxOf(bgLightness + 11f, low + 3f)
        val high = maxOf(bgLightness + 16f, container + 3f)
        val highest = maxOf(bgLightness + 22f, high + 3f)
        listOf(lowest, low, container, high, highest)
    } else {
        val lowest = bgLightness + 5f
        val low = minOf(bgLightness - 5f, surfaceLightness - 2f)
        val container = minOf(bgLightness - 9f, low - 3f)
        val high = minOf(bgLightness - 14f, container - 3f)
        val highest = minOf(bgLightness - 20f, high - 3f)
        listOf(lowest, low, container, high, highest)
    }
    val containerLowest = withLightness(surface, lowestTarget / 100f)
    val containerLow = withLightness(surface, lowTarget / 100f)
    val container = withLightness(surface, containerTarget / 100f)
    val containerHigh = withLightness(surface, highTarget / 100f)
    val containerHighest = withLightness(surface, highestTarget / 100f)
    return copy(
        surfaceContainerLowest = containerLowest,
        surfaceContainerLow = containerLow,
        surfaceContainer = container,
        surfaceContainerHigh = containerHigh,
        surfaceContainerHighest = containerHighest,
        tertiaryContainer = Color(ColorUtils.blendARGB(tertiary.toArgb(), background.toArgb(), 0.80f)),
        onTertiaryContainer = Color(ColorUtils.blendARGB(tertiary.toArgb(), onBackground.toArgb(), 0.55f)),
        surfaceDim = withLightness(
            surface,
            ((if (dark) bgLightness - 6f else bgLightness - 12f) / 100f)
        ),
        surfaceBright = withLightness(
            surface,
            ((if (dark) bgLightness + 28f else bgLightness + 2f) / 100f)
        ),
        inverseSurface = Color(ColorUtils.blendARGB(onBackground.toArgb(), background.toArgb(), 0.08f)),
        inverseOnSurface = Color(ColorUtils.blendARGB(background.toArgb(), onBackground.toArgb(), 0.12f)),
        inversePrimary = if (dark) {
            Color(ColorUtils.blendARGB(primary.toArgb(), Color.White.toArgb(), 0.35f))
        } else {
            Color(ColorUtils.blendARGB(primary.toArgb(), Color.Black.toArgb(), 0.30f))
        }
    )
}

/**
 * Pitch-black (AMOLED) variant of a dark scheme. The surface hierarchy is
 * replaced by the fixed neutral ramp from the upstream PixelPlayerOSS AMOLED
 * preset (background/surface/dim/lowest pure black; the elevated surfaces
 * step through 0x080808, 0x101010, 0x181818, 0x242424), so cards and sheets
 * stay distinguishable on a pure-black background. Accent and text roles
 * are untouched, so the scheme's tone is carried by its accents.
 */
fun ColorScheme.toPitchBlack(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color(0xFF242424),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF242424),
    surfaceVariant = Color(0xFF242424)
)

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
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
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
    ).withDerivedRoles(dark)
}

fun AppColorScheme.toColorSchemePair(): ColorSchemePair =
    ColorSchemePair(
        light = light.toColorScheme(dark = false),
        dark = dark.toColorScheme(dark = true)
    )

/**
 * Resolves the selected scheme id to the pair StartuneTheme should use as its
 * base. Dynamic Color resolves to null (StartuneTheme keeps its current
 * behavior: wallpaper dynamic color on Android 12+, hardcoded pair below it).
 * An unknown id (e.g. a deleted custom theme) also resolves to null.
 */
fun resolveSelectedSchemePair(
    selectedId: String,
    customs: List<AppColorScheme>
): ColorSchemePair? {
    if (selectedId == ColorSchemeIds.DYNAMIC) return null
    val scheme = BuiltInColorSchemes.find(selectedId)
        ?: customs.firstOrNull { it.id == selectedId }
        ?: return null
    return scheme.toColorSchemePair()
}
