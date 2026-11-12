package com.startune.music.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.startune.music.presentation.viewmodel.ColorSchemePair
import androidx.core.graphics.ColorUtils

val LocalStartuneDarkTheme = staticCompositionLocalOf { false }
val LocalShowScrollbar = staticCompositionLocalOf { true }

/**
 * The active rainbow palette (genre cards, Settings icons): the selected
 * theme's stored official accent palette when it has one, else null and
 * callers fall back to the derived palette from the ColorScheme. A
 * now-playing override also falls back to derived, because the override
 * scheme has no stored rainbow.
 */
val LocalRainbowPalette = staticCompositionLocalOf<List<GenreThemeColor>?> { null }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Suppress("DEPRECATION")
@Composable
fun StartuneStatusBarStyle(
    color: Color,
    useDarkIcons: Boolean = ColorUtils.calculateLuminance(color.toArgb()) > 0.55,
    navigationColor: Color? = null,
    useDarkNavigationIcons: Boolean = navigationColor
        ?.let { ColorUtils.calculateLuminance(it.toArgb()) > 0.55 }
        ?: useDarkIcons
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val updateNavigationBar = navigationColor != null
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }

        WindowCompat.getInsetsController(window, view).run {
            isAppearanceLightStatusBars = useDarkIcons

            if (updateNavigationBar) {
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                isAppearanceLightNavigationBars = useDarkNavigationIcons
            }
        }
    }
}

val DarkColorScheme = darkColorScheme(
    primary = StartunePurplePrimary,
    secondary = StartunePink,
    tertiary = StartuneOrange,
    background = StartunePurpleDark,
    surface = StartuneSurface,
    onPrimary = StartuneWhite,
    onSecondary = StartuneWhite,
    onTertiary = StartuneWhite,
    onBackground = StartuneWhite,
    onSurface = StartuneLightPurple,
    error = Color(0xFFFF5252),
    onError = StartuneWhite
).withDerivedRoles(dark = true)

val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = StartuneWhite,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = StartunePink,
    onSecondary = StartuneWhite,
    secondaryContainer = StartunePink.copy(alpha = 0.15f),
    onSecondaryContainer = StartunePink.copy(alpha = 0.85f),
    tertiary = StartuneOrange,
    onTertiary = StartuneBlack,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.6f),
    surfaceTint = LightPrimary,
    error = Color(0xFFD32F2F),
    onError = StartuneWhite
).withDerivedRoles(dark = false)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StartuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorSchemePairOverride: ColorSchemePair? = null,
    baseColorSchemePair: ColorSchemePair? = null,
    rainbowPair: Pair<List<GenreThemeColor>, List<GenreThemeColor>>? = null,
    pitchBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val finalColorScheme = when {
        colorSchemePairOverride != null -> {
            if (darkTheme) colorSchemePairOverride.dark else colorSchemePairOverride.light
        }
        baseColorSchemePair != null -> {
            if (darkTheme) baseColorSchemePair.dark else baseColorSchemePair.light
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            try {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (e: Exception) {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Pitch black only applies in dark mode; the tone is kept via the
    // scheme's accents and the toned-down elevated surfaces.
    val displayColorScheme = if (pitchBlack && darkTheme) finalColorScheme.toPitchBlack() else finalColorScheme

    StartuneStatusBarStyle(
        color = displayColorScheme.background,
        navigationColor = displayColorScheme.background
    )

    // The stored rainbow belongs to the base theme; a now-playing
    // override has none, so it falls back to the derived palette.
    val activeRainbow = if (colorSchemePairOverride != null) {
        null
    } else {
        rainbowPair?.let { if (darkTheme) it.second else it.first }
    }

    CompositionLocalProvider(
        LocalStartuneDarkTheme provides darkTheme,
        LocalRainbowPalette provides activeRainbow
    ) {
        MaterialTheme(
            colorScheme = displayColorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
