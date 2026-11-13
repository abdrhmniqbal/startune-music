package com.startune.music.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import kotlin.math.pow
import kotlin.math.roundToInt
import com.startune.music.data.preferences.AppColorScheme
import com.startune.music.data.preferences.ColorRoleKeys
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

/** Converts an opaque [Color] to a "#RRGGBB" hex string. */
fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return "#%06X".format(argb and 0xFFFFFF)
}

/**
 * HSV (hue 0..360, saturation 0..1, value 0..1) to an opaque [Color].
 * Pure Kotlin, so the color picker behaves the same in unit tests.
 */
fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val hue = ((h % 360f) + 360f) % 360f
    val sat = s.coerceIn(0f, 1f)
    val value = v.coerceIn(0f, 1f)
    val c = value * sat
    val x = c * (1f - kotlin.math.abs((hue / 60f % 2f) - 1f))
    val m = value - c
    val (r, g, b) = when ((hue / 60f).toInt() % 6) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

/** Converts a [Color] to HSV: hue 0..360, saturation 0..1, value 0..1. */
fun colorToHsv(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    var h = when {
        delta == 0f -> 0f
        max == r -> ((g - b) / delta % 6f) * 60f
        max == g -> ((b - r) / delta + 2f) * 60f
        else -> ((r - g) / delta + 4f) * 60f
    }
    if (h < 0f) h += 360f
    val s = if (max == 0f) 0f else delta / max
    return floatArrayOf(h, s, max)
}

/** Reads one role of a [ColorScheme] by its [ColorRoleKeys] key. */
fun ColorScheme.roleColor(key: String): Color = when (key) {
    ColorRoleKeys.ON_PRIMARY -> onPrimary
    ColorRoleKeys.PRIMARY_CONTAINER -> primaryContainer
    ColorRoleKeys.ON_PRIMARY_CONTAINER -> onPrimaryContainer
    ColorRoleKeys.PRIMARY_FIXED -> primaryFixed
    ColorRoleKeys.PRIMARY_FIXED_DIM -> primaryFixedDim
    ColorRoleKeys.ON_PRIMARY_FIXED -> onPrimaryFixed
    ColorRoleKeys.ON_PRIMARY_FIXED_VARIANT -> onPrimaryFixedVariant
    ColorRoleKeys.ON_SECONDARY -> onSecondary
    ColorRoleKeys.SECONDARY_CONTAINER -> secondaryContainer
    ColorRoleKeys.ON_SECONDARY_CONTAINER -> onSecondaryContainer
    ColorRoleKeys.SECONDARY_FIXED -> secondaryFixed
    ColorRoleKeys.SECONDARY_FIXED_DIM -> secondaryFixedDim
    ColorRoleKeys.ON_SECONDARY_FIXED -> onSecondaryFixed
    ColorRoleKeys.ON_SECONDARY_FIXED_VARIANT -> onSecondaryFixedVariant
    ColorRoleKeys.ON_TERTIARY -> onTertiary
    ColorRoleKeys.TERTIARY_CONTAINER -> tertiaryContainer
    ColorRoleKeys.ON_TERTIARY_CONTAINER -> onTertiaryContainer
    ColorRoleKeys.TERTIARY_FIXED -> tertiaryFixed
    ColorRoleKeys.TERTIARY_FIXED_DIM -> tertiaryFixedDim
    ColorRoleKeys.ON_TERTIARY_FIXED -> onTertiaryFixed
    ColorRoleKeys.ON_TERTIARY_FIXED_VARIANT -> onTertiaryFixedVariant
    ColorRoleKeys.ON_ERROR -> onError
    ColorRoleKeys.ERROR_CONTAINER -> errorContainer
    ColorRoleKeys.ON_ERROR_CONTAINER -> onErrorContainer
    ColorRoleKeys.ON_BACKGROUND -> onBackground
    ColorRoleKeys.ON_SURFACE -> onSurface
    ColorRoleKeys.SURFACE_VARIANT -> surfaceVariant
    ColorRoleKeys.ON_SURFACE_VARIANT -> onSurfaceVariant
    ColorRoleKeys.SURFACE_DIM -> surfaceDim
    ColorRoleKeys.SURFACE_BRIGHT -> surfaceBright
    ColorRoleKeys.SURFACE_CONTAINER_LOWEST -> surfaceContainerLowest
    ColorRoleKeys.SURFACE_CONTAINER_LOW -> surfaceContainerLow
    ColorRoleKeys.SURFACE_CONTAINER -> surfaceContainer
    ColorRoleKeys.SURFACE_CONTAINER_HIGH -> surfaceContainerHigh
    ColorRoleKeys.SURFACE_CONTAINER_HIGHEST -> surfaceContainerHighest
    ColorRoleKeys.OUTLINE -> outline
    ColorRoleKeys.OUTLINE_VARIANT -> outlineVariant
    ColorRoleKeys.INVERSE_SURFACE -> inverseSurface
    ColorRoleKeys.INVERSE_ON_SURFACE -> inverseOnSurface
    ColorRoleKeys.INVERSE_PRIMARY -> inversePrimary
    ColorRoleKeys.SCRIM -> scrim
    ColorRoleKeys.SURFACE_TINT -> surfaceTint
    else -> onSurface
}

/**
 * Picks the text color (black or white) with the higher WCAG contrast ratio
 * against [color], so text stays readable on any role. A fixed luminance
 * threshold gets this wrong for light but blue-heavy colors: Nord dark
 * #88C0D0 has luminance 0.475, so the old threshold gave it white text at
 * 2:1, while black is 10.5:1. With this rule every built-in primary is at
 * least 5.1:1 (worst case Nord light #4C709C, white at 5.12:1).
 *
 * Pure Kotlin on purpose: ColorUtils.calculateLuminance delegates to
 * android.graphics.Color, which is a stub returning 0 in JVM unit tests,
 * so the mapper could not be unit-tested at all. This is the same
 * formula (sRGB linearization, 0.2126/0.7152/0.0722 weights).
 */
internal fun onColorFor(color: Color): Color {
    val luminance = relativeLuminance(color.toArgb())
    val whiteContrast = 1.05 / (luminance + 0.05)
    val blackContrast = (luminance + 0.05) / 0.05
    return if (whiteContrast > blackContrast) Color.White else Color.Black
}

/**
 * On-color for a rainbow container. Same max-contrast rule as [onColorFor],
 * except white is only chosen when it is clearly better: when the two
 * contrasts are within 0.2 of each other the choice is a coin flip on
 * near-identical colors, so similar mid-tone accents end up with different
 * icon colors and the rainbow looks inconsistent. Solarized orange #CB4B16
 * is white at 4.61 vs black at 4.56 and red #DC322F white at 4.63 vs black
 * at 4.54, while magenta #D33682 is black at 4.62 vs white at 4.55, so the
 * Settings icons flip between white and black on colors that look the
 * same brightness. Preferring black in that near-tie zone keeps the set
 * uniform at a negligible contrast cost (at most 0.2). Only the rainbow
 * uses this; [onColorFor] still decides every other role.
 */
internal fun rainbowOnColorFor(color: Color): Color {
    val luminance = relativeLuminance(color.toArgb())
    val whiteContrast = 1.05 / (luminance + 0.05)
    val blackContrast = (luminance + 0.05) / 0.05
    return if (whiteContrast > blackContrast + 0.2) Color.White else Color.Black
}

private fun relativeLuminance(argb: Int): Double {
    fun linear(channel: Int): Double {
        val s = channel / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return 0.2126 * linear(r) + 0.7152 * linear(g) + 0.0722 * linear(b)
}

/**
 * Returns [color] with its HSL lightness replaced by [targetLightness]
 * (0..1), keeping hue and saturation, so the result tracks the scheme's own
 * color family instead of drifting to neutral grey.
 *
 * Pure Kotlin on purpose, same formulas as ColorUtils.colorToHSL /
 * HSLToColor (see rgbToHsl / hslToColor below): ColorUtils delegates to
 * android.graphics.Color, which is a stub returning 0 in JVM unit tests,
 * so withLightness returned transparent black there and the derivation
 * could not be unit-tested. Production output is unchanged.
 */
internal fun withLightness(color: Color, targetLightness: Float): Color {
    val hsl = FloatArray(3)
    rgbToHsl(color.toArgb(), hsl)
    hsl[2] = targetLightness.coerceIn(0.02f, 0.98f)
    return hslToColor(hsl)
}

internal fun lightnessOf(color: Color): Float {
    val hsl = FloatArray(3)
    rgbToHsl(color.toArgb(), hsl)
    return hsl[2]
}

/**
 * RGB (as an ARGB int) to HSL, hsl[0] = hue degrees 0..360, hsl[1] =
 * saturation 0..1, hsl[2] = lightness 0..1. Same formulas as
 * ColorUtils.colorToHSL.
 */
internal fun rgbToHsl(argb: Int, hsl: FloatArray) {
    val rf = ((argb shr 16) and 0xFF) / 255f
    val gf = ((argb shr 8) and 0xFF) / 255f
    val bf = (argb and 0xFF) / 255f
    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val delta = max - min
    val l = (max + min) / 2f
    val (h, s) = if (delta == 0f) {
        0f to 0f
    } else {
        val h = when (max) {
            rf -> ((gf - bf) / delta) % 6f
            gf -> ((bf - rf) / delta) + 2f
            else -> ((rf - gf) / delta) + 4f
        } * 60f
        val s = delta / (1f - kotlin.math.abs(2f * l - 1f))
        h to s
    }
    var hue = h % 360f
    if (hue < 0f) hue += 360f
    hsl[0] = hue.coerceIn(0f, 360f)
    hsl[1] = s.coerceIn(0f, 1f)
    hsl[2] = l.coerceIn(0f, 1f)
}

/**
 * HSL (same layout as [rgbToHsl]) to an opaque [Color]. Same formulas as
 * ColorUtils.HSLToColor.
 */
internal fun hslToColor(hsl: FloatArray): Color {
    val h = hsl[0]
    val s = hsl[1]
    val l = hsl[2]
    val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
    val m = l - 0.5f * c
    val x = c * (1f - kotlin.math.abs((h / 60f % 2f) - 1f))
    val (r, g, b) = when (h.toInt() / 60) {
        0 -> Triple(c + m, x + m, m)
        1 -> Triple(x + m, c + m, m)
        2 -> Triple(m, c + m, x + m)
        3 -> Triple(m, x + m, c + m)
        4 -> Triple(x + m, m, c + m)
        else -> Triple(c + m, m, x + m)
    }
    return Color(
        (255f * r).roundToInt().coerceIn(0, 255),
        (255f * g).roundToInt().coerceIn(0, 255),
        (255f * b).roundToInt().coerceIn(0, 255)
    )
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
        // The surface anchor must meet the same 7-point separation the
        // background anchor promises: with only +2, a custom scheme whose
        // surface sits near its background (and whose light primary lifts
        // the gradient background) derives a surfaceContainerLow that
        // lands on -- or below -- the page behind the card.
        val low = maxOf(bgLightness + 7f, surfaceLightness + 7f)
        val container = maxOf(bgLightness + 11f, low + 3f)
        val high = maxOf(bgLightness + 16f, container + 3f)
        val highest = maxOf(bgLightness + 22f, high + 3f)
        listOf(lowest, low, container, high, highest)
    } else {
        val lowest = bgLightness + 5f
        val low = minOf(bgLightness - 5f, surfaceLightness - 5f)
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
 * replaced by the fixed neutral ramp from the upstream AMOLED
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
 * Derives the accent containers (primary/tertiary) from the surface
 * container ramp instead of blending each accent 80% toward the
 * background. That blend moved opposite to the ramp, so in dark mode
 * the selected/highlight surface landed DARKER than the neutral
 * surfaceContainer (Nord dark: primaryContainer L 30.8 vs
 * surfaceContainer L 41.6) and in light mode LIGHTER (Nord light 84.3
 * vs 80.0, Catppuccin light 87.8 vs 74.7): the selected row receded
 * instead of standing out, on every scheme. Anchoring 7 lightness
 * points beyond surfaceContainer -- lighter in dark mode, darker in
 * light mode -- restores the hierarchy sheet < unselected < selected
 * (dark) and selected < unselected < sheet (light), while
 * withLightness keeps each accent's hue. The on-colors are picked by
 * WCAG contrast, so text stays readable on the new lightness. This
 * replaces the tertiaryContainer derivation in withDerivedRoles for
 * the six-role schemes; the hardcoded Startune pair in Theme.kt keeps
 * its own values and is untouched.
 */
private fun ColorScheme.withAccentContainers(
    primary: Color,
    tertiary: Color,
    dark: Boolean
): ColorScheme {
    val target = (lightnessOf(surfaceContainer) + if (dark) 0.07f else -0.07f)
        .coerceIn(0.02f, 0.98f)
    val primaryContainer = withLightness(primary, target)
    val tertiaryContainer = withLightness(tertiary, target)
    return copy(
        primaryContainer = primaryContainer,
        onPrimaryContainer = onColorFor(primaryContainer),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onColorFor(tertiaryContainer)
    )
}

/**
 * Applies explicit per-role overrides on top of a fully derived scheme.
 * An override wins over the derived guess for that role; a role with no
 * override (or an invalid hex value, which is skipped) keeps its derived
 * color. This is what lets a theme match its official palette instead of
 * only approximating it through the six base roles.
 */
private fun ColorScheme.withOverrides(overrides: Map<String, String>): ColorScheme {
    if (overrides.isEmpty()) return this
    var scheme = this
    for ((key, hex) in overrides) {
        if (!isValidHex(hex)) continue
        val color = hexToColor(normalizeHex(hex))
        scheme = when (key) {
            ColorRoleKeys.ON_PRIMARY -> scheme.copy(onPrimary = color)
            ColorRoleKeys.PRIMARY_CONTAINER -> scheme.copy(primaryContainer = color)
            ColorRoleKeys.ON_PRIMARY_CONTAINER -> scheme.copy(onPrimaryContainer = color)
            ColorRoleKeys.PRIMARY_FIXED -> scheme.copy(primaryFixed = color)
            ColorRoleKeys.PRIMARY_FIXED_DIM -> scheme.copy(primaryFixedDim = color)
            ColorRoleKeys.ON_PRIMARY_FIXED -> scheme.copy(onPrimaryFixed = color)
            ColorRoleKeys.ON_PRIMARY_FIXED_VARIANT -> scheme.copy(onPrimaryFixedVariant = color)
            ColorRoleKeys.ON_SECONDARY -> scheme.copy(onSecondary = color)
            ColorRoleKeys.SECONDARY_CONTAINER -> scheme.copy(secondaryContainer = color)
            ColorRoleKeys.ON_SECONDARY_CONTAINER -> scheme.copy(onSecondaryContainer = color)
            ColorRoleKeys.SECONDARY_FIXED -> scheme.copy(secondaryFixed = color)
            ColorRoleKeys.SECONDARY_FIXED_DIM -> scheme.copy(secondaryFixedDim = color)
            ColorRoleKeys.ON_SECONDARY_FIXED -> scheme.copy(onSecondaryFixed = color)
            ColorRoleKeys.ON_SECONDARY_FIXED_VARIANT -> scheme.copy(onSecondaryFixedVariant = color)
            ColorRoleKeys.ON_TERTIARY -> scheme.copy(onTertiary = color)
            ColorRoleKeys.TERTIARY_CONTAINER -> scheme.copy(tertiaryContainer = color)
            ColorRoleKeys.ON_TERTIARY_CONTAINER -> scheme.copy(onTertiaryContainer = color)
            ColorRoleKeys.TERTIARY_FIXED -> scheme.copy(tertiaryFixed = color)
            ColorRoleKeys.TERTIARY_FIXED_DIM -> scheme.copy(tertiaryFixedDim = color)
            ColorRoleKeys.ON_TERTIARY_FIXED -> scheme.copy(onTertiaryFixed = color)
            ColorRoleKeys.ON_TERTIARY_FIXED_VARIANT -> scheme.copy(onTertiaryFixedVariant = color)
            ColorRoleKeys.ON_ERROR -> scheme.copy(onError = color)
            ColorRoleKeys.ERROR_CONTAINER -> scheme.copy(errorContainer = color)
            ColorRoleKeys.ON_ERROR_CONTAINER -> scheme.copy(onErrorContainer = color)
            ColorRoleKeys.ON_BACKGROUND -> scheme.copy(onBackground = color)
            ColorRoleKeys.ON_SURFACE -> scheme.copy(onSurface = color)
            ColorRoleKeys.SURFACE_VARIANT -> scheme.copy(surfaceVariant = color)
            ColorRoleKeys.ON_SURFACE_VARIANT -> scheme.copy(onSurfaceVariant = color)
            ColorRoleKeys.SURFACE_DIM -> scheme.copy(surfaceDim = color)
            ColorRoleKeys.SURFACE_BRIGHT -> scheme.copy(surfaceBright = color)
            ColorRoleKeys.SURFACE_CONTAINER_LOWEST -> scheme.copy(surfaceContainerLowest = color)
            ColorRoleKeys.SURFACE_CONTAINER_LOW -> scheme.copy(surfaceContainerLow = color)
            ColorRoleKeys.SURFACE_CONTAINER -> scheme.copy(surfaceContainer = color)
            ColorRoleKeys.SURFACE_CONTAINER_HIGH -> scheme.copy(surfaceContainerHigh = color)
            ColorRoleKeys.SURFACE_CONTAINER_HIGHEST -> scheme.copy(surfaceContainerHighest = color)
            ColorRoleKeys.OUTLINE -> scheme.copy(outline = color)
            ColorRoleKeys.OUTLINE_VARIANT -> scheme.copy(outlineVariant = color)
            ColorRoleKeys.INVERSE_SURFACE -> scheme.copy(inverseSurface = color)
            ColorRoleKeys.INVERSE_ON_SURFACE -> scheme.copy(inverseOnSurface = color)
            ColorRoleKeys.INVERSE_PRIMARY -> scheme.copy(inversePrimary = color)
            ColorRoleKeys.SCRIM -> scheme.copy(scrim = color)
            ColorRoleKeys.SURFACE_TINT -> scheme.copy(surfaceTint = color)
            else -> scheme
        }
    }
    return scheme
}

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
        secondaryContainer = secondary.copy(alpha = 0.15f),
        onSecondaryContainer = secondary.copy(alpha = 0.85f),
        // The *Fixed roles were never assigned here, so they fell through
        // from the M3 baseline scheme and every player button using them
        // (bottom toggle row, play button) stayed baseline purple/pink
        // whatever scheme was picked. Derive them from the scheme's own
        // hues: light tints for the pill (tone 90 / 80), dark tints for
        // the text on it (tone 10 / 30). Identical in light and dark,
        // as M3 defines Fixed roles.
        primaryFixed = withLightness(primary, 0.90f),
        primaryFixedDim = withLightness(primary, 0.80f),
        onPrimaryFixed = withLightness(primary, 0.10f),
        onPrimaryFixedVariant = withLightness(primary, 0.30f),
        secondaryFixed = withLightness(secondary, 0.90f),
        secondaryFixedDim = withLightness(secondary, 0.80f),
        onSecondaryFixed = withLightness(secondary, 0.10f),
        onSecondaryFixedVariant = withLightness(secondary, 0.30f),
        tertiaryFixed = withLightness(tertiary, 0.90f),
        tertiaryFixedDim = withLightness(tertiary, 0.80f),
        onTertiaryFixed = withLightness(tertiary, 0.10f),
        onTertiaryFixedVariant = withLightness(tertiary, 0.30f),
        error = error,
        onError = onColorFor(error)
    ).withDerivedRoles(dark)
        .withAccentContainers(primary, tertiary, dark)
        .withOverrides(overrides)
}

fun AppColorScheme.toColorSchemePair(): ColorSchemePair =
    ColorSchemePair(
        light = light.toColorScheme(dark = false),
        dark = dark.toColorScheme(dark = true)
    )

/**
 * The rainbow for one variant: the stored official accent palette
 * (SchemeRoles.rainbow, e.g. Nord Aurora) when present, else the derived
 * palette from the variant's own ColorScheme. Each stored hex is a
 * container; its on-color comes from WCAG contrast, same as the derived
 * palette. A stored list shorter than 8 is padded from the derived
 * palette so indices always resolve.
 */
fun SchemeRoles.rainbowColors(dark: Boolean): List<GenreThemeColor> {
    val derived = toColorScheme(dark).rainbowPalette()
    if (rainbow.isEmpty()) return derived
    val stored = rainbow.map { hex ->
        val container = hexToColor(hex)
        GenreThemeColor(container, rainbowOnColorFor(container))
    }
    return if (stored.size >= derived.size) stored
    else stored + derived.drop(stored.size)
}

/** Light and dark rainbow for a scheme, in the same order as the pair. */
fun AppColorScheme.rainbowPair(): Pair<List<GenreThemeColor>, List<GenreThemeColor>> =
    light.rainbowColors(dark = false) to dark.rainbowColors(dark = true)

/**
 * All overridable roles for one variant, resolved to explicit values:
 * the stored overrides where present, the mapper's derivation elsewhere.
 * This is what a full theme export writes, so the JSON defines every
 * role and the theme does not depend on the derivation at import time.
 *
 * Roles the derivation defines with an alpha (secondaryContainer,
 * onSecondaryContainer, outlineVariant) are composited over the surface
 * they are drawn on, to their effective opaque hex. Exporting the raw
 * RGB would drop the alpha and the imported theme would render them
 * opaque instead of translucent.
 */
fun SchemeRoles.fullOverrides(dark: Boolean): Map<String, String> {
    val scheme = toColorScheme(dark)
    val surface = scheme.surface
    val container = compositeOver(scheme.secondaryContainer, surface)
    return ALL_ROLE_KEYS.associateWith { key ->
        val color = scheme.roleColor(key)
        val effective = when (key) {
            ColorRoleKeys.SECONDARY_CONTAINER -> container
            ColorRoleKeys.ON_SECONDARY_CONTAINER ->
                compositeOver(color, container)
            ColorRoleKeys.OUTLINE_VARIANT ->
                compositeOver(color, surface)
            else -> color
        }
        colorToHex(effective)
    }
}

/** Composites [foreground] (possibly translucent) over [background]. */
private fun compositeOver(foreground: Color, background: Color): Color {
    val a = foreground.alpha
    if (a >= 1f) return foreground
    val r = (foreground.red * a + background.red * (1f - a))
    val g = (foreground.green * a + background.green * (1f - a))
    val b = (foreground.blue * a + background.blue * (1f - a))
    return Color(r, g, b)
}

/** Every key in [ColorRoleKeys] that [ColorScheme.roleColor] can read. */
private val ALL_ROLE_KEYS = listOf(
    ColorRoleKeys.ON_PRIMARY,
    ColorRoleKeys.PRIMARY_CONTAINER,
    ColorRoleKeys.ON_PRIMARY_CONTAINER,
    ColorRoleKeys.PRIMARY_FIXED,
    ColorRoleKeys.PRIMARY_FIXED_DIM,
    ColorRoleKeys.ON_PRIMARY_FIXED,
    ColorRoleKeys.ON_PRIMARY_FIXED_VARIANT,
    ColorRoleKeys.ON_SECONDARY,
    ColorRoleKeys.SECONDARY_CONTAINER,
    ColorRoleKeys.ON_SECONDARY_CONTAINER,
    ColorRoleKeys.SECONDARY_FIXED,
    ColorRoleKeys.SECONDARY_FIXED_DIM,
    ColorRoleKeys.ON_SECONDARY_FIXED,
    ColorRoleKeys.ON_SECONDARY_FIXED_VARIANT,
    ColorRoleKeys.ON_TERTIARY,
    ColorRoleKeys.TERTIARY_CONTAINER,
    ColorRoleKeys.ON_TERTIARY_CONTAINER,
    ColorRoleKeys.TERTIARY_FIXED,
    ColorRoleKeys.TERTIARY_FIXED_DIM,
    ColorRoleKeys.ON_TERTIARY_FIXED,
    ColorRoleKeys.ON_TERTIARY_FIXED_VARIANT,
    ColorRoleKeys.ON_ERROR,
    ColorRoleKeys.ERROR_CONTAINER,
    ColorRoleKeys.ON_ERROR_CONTAINER,
    ColorRoleKeys.ON_BACKGROUND,
    ColorRoleKeys.ON_SURFACE,
    ColorRoleKeys.SURFACE_VARIANT,
    ColorRoleKeys.ON_SURFACE_VARIANT,
    ColorRoleKeys.SURFACE_DIM,
    ColorRoleKeys.SURFACE_BRIGHT,
    ColorRoleKeys.SURFACE_CONTAINER_LOWEST,
    ColorRoleKeys.SURFACE_CONTAINER_LOW,
    ColorRoleKeys.SURFACE_CONTAINER,
    ColorRoleKeys.SURFACE_CONTAINER_HIGH,
    ColorRoleKeys.SURFACE_CONTAINER_HIGHEST,
    ColorRoleKeys.OUTLINE,
    ColorRoleKeys.OUTLINE_VARIANT,
    ColorRoleKeys.INVERSE_SURFACE,
    ColorRoleKeys.INVERSE_ON_SURFACE,
    ColorRoleKeys.INVERSE_PRIMARY,
    ColorRoleKeys.SCRIM,
    ColorRoleKeys.SURFACE_TINT
)

/**
 * A copy of this scheme whose overrides are complete: every overridable
 * role has an explicit value. This is the form the exporter writes, so
 * an exported theme is a full theme.
 */
fun AppColorScheme.toFullScheme(): AppColorScheme = copy(
    light = light.copy(overrides = light.fullOverrides(dark = false)),
    dark = dark.copy(overrides = dark.fullOverrides(dark = true))
)

/**
 * Resolves the selected scheme id to its rainbow pair, parallel to
 * [resolveSelectedSchemePair]. Dynamic Color and unknown ids resolve to
 * null (the derived palette is used).
 */
fun resolveSelectedRainbowPair(
    selectedId: String,
    customs: List<AppColorScheme>
): Pair<List<GenreThemeColor>, List<GenreThemeColor>>? {
    if (selectedId == ColorSchemeIds.DYNAMIC) return null
    val scheme = BuiltInColorSchemes.find(selectedId)
        ?: customs.firstOrNull { it.id == selectedId }
        ?: return null
    return scheme.rainbowPair()
}

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
