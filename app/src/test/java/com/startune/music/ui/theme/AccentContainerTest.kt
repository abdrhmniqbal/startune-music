package com.startune.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.pow

/**
 * The selected/highlight surface (primaryContainer, and tertiaryContainer
 * which shares the derivation) must always be more prominent than the
 * neutral surfaceContainer: lighter in dark mode, darker in light mode,
 * by at least 6 lightness points, on every built-in scheme. The old rule
 * blended each accent 80% toward the background, which inverted the
 * hierarchy in both modes (Nord dark primaryContainer L 30.8 vs
 * surfaceContainer L 41.6). The on-colors must keep WCAG AA contrast
 * against their container.
 *
 * Lightness and contrast are computed here in pure Kotlin (no
 * ColorUtils): in JVM unit tests android.graphics.Color is a stub
 * returning 0, so ColorUtils HSL/luminance calls return transparent
 * black and cannot be used to measure the result.
 */
class AccentContainerTest {

    @Test
    fun accentContainers_standOutFromSurfaceContainer_onEveryBuiltInScheme() {
        val violations = mutableListOf<String>()
        for (scheme in BuiltInColorSchemes.all) {
            val pair = scheme.toColorSchemePair()
            for ((dark, colorScheme) in listOf(false to pair.light, true to pair.dark)) {
                val containerLightness = lightnessOf(colorScheme.surfaceContainer)
                for ((name, container) in listOf(
                    "primaryContainer" to colorScheme.primaryContainer,
                    "tertiaryContainer" to colorScheme.tertiaryContainer
                )) {
                    val delta = lightnessOf(container) - containerLightness
                    val ok = if (dark) delta >= 0.06f else delta <= -0.06f
                    if (!ok) {
                        violations += "${scheme.id} ${if (dark) "dark" else "light"} $name " +
                            "delta=$delta containerL=${lightnessOf(container)} surfaceL=$containerLightness"
                    }
                }
                val primaryContrast = contrast(
                    colorScheme.onPrimaryContainer, colorScheme.primaryContainer
                )
                val tertiaryContrast = contrast(
                    colorScheme.onTertiaryContainer, colorScheme.tertiaryContainer
                )
                if (primaryContrast < 4.5) {
                    violations += "${scheme.id} ${if (dark) "dark" else "light"} " +
                        "onPrimaryContainer contrast=$primaryContrast"
                }
                if (tertiaryContrast < 4.5) {
                    violations += "${scheme.id} ${if (dark) "dark" else "light"} " +
                        "onTertiaryContainer contrast=$tertiaryContrast"
                }
            }
        }
        assertThat(violations).isEmpty()
    }

    private fun lightnessOf(color: Color): Float {
        val argb = color.toArgb()
        val rf = ((argb shr 16) and 0xFF) / 255f
        val gf = ((argb shr 8) and 0xFF) / 255f
        val bf = (argb and 0xFF) / 255f
        val max = maxOf(rf, gf, bf)
        val min = minOf(rf, gf, bf)
        return (max + min) / 2f
    }

    private fun contrast(foreground: Color, background: Color): Double {
        val l1 = relativeLuminance(foreground.toArgb())
        val l2 = relativeLuminance(background.toArgb())
        val (lighter, darker) = if (l1 > l2) l1 to l2 else l2 to l1
        return (lighter + 0.05) / (darker + 0.05)
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
}
