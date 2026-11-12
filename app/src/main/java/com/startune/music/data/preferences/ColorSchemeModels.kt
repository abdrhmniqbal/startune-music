package com.startune.music.data.preferences

import kotlinx.serialization.Serializable

object ColorSchemeIds {
    const val DYNAMIC = "dynamic"
}

/**
 * Keys for the per-role color overrides in [SchemeRoles.overrides]. Each key
 * is a Material3 ColorScheme property name. An override wins over the
 * auto-derived value for that role; a role with no override is derived
 * from the six base roles as before, so old themes (no overrides) render
 * exactly like they did.
 */
object ColorRoleKeys {
    const val ON_PRIMARY = "onPrimary"
    const val PRIMARY_CONTAINER = "primaryContainer"
    const val ON_PRIMARY_CONTAINER = "onPrimaryContainer"
    const val PRIMARY_FIXED = "primaryFixed"
    const val PRIMARY_FIXED_DIM = "primaryFixedDim"
    const val ON_PRIMARY_FIXED = "onPrimaryFixed"
    const val ON_PRIMARY_FIXED_VARIANT = "onPrimaryFixedVariant"
    const val ON_SECONDARY = "onSecondary"
    const val SECONDARY_CONTAINER = "secondaryContainer"
    const val ON_SECONDARY_CONTAINER = "onSecondaryContainer"
    const val SECONDARY_FIXED = "secondaryFixed"
    const val SECONDARY_FIXED_DIM = "secondaryFixedDim"
    const val ON_SECONDARY_FIXED = "onSecondaryFixed"
    const val ON_SECONDARY_FIXED_VARIANT = "onSecondaryFixedVariant"
    const val ON_TERTIARY = "onTertiary"
    const val TERTIARY_CONTAINER = "tertiaryContainer"
    const val ON_TERTIARY_CONTAINER = "onTertiaryContainer"
    const val TERTIARY_FIXED = "tertiaryFixed"
    const val TERTIARY_FIXED_DIM = "tertiaryFixedDim"
    const val ON_TERTIARY_FIXED = "onTertiaryFixed"
    const val ON_TERTIARY_FIXED_VARIANT = "onTertiaryFixedVariant"
    const val ON_ERROR = "onError"
    const val ERROR_CONTAINER = "errorContainer"
    const val ON_ERROR_CONTAINER = "onErrorContainer"
    const val ON_BACKGROUND = "onBackground"
    const val ON_SURFACE = "onSurface"
    const val SURFACE_VARIANT = "surfaceVariant"
    const val ON_SURFACE_VARIANT = "onSurfaceVariant"
    const val SURFACE_DIM = "surfaceDim"
    const val SURFACE_BRIGHT = "surfaceBright"
    const val SURFACE_CONTAINER_LOWEST = "surfaceContainerLowest"
    const val SURFACE_CONTAINER_LOW = "surfaceContainerLow"
    const val SURFACE_CONTAINER = "surfaceContainer"
    const val SURFACE_CONTAINER_HIGH = "surfaceContainerHigh"
    const val SURFACE_CONTAINER_HIGHEST = "surfaceContainerHighest"
    const val OUTLINE = "outline"
    const val OUTLINE_VARIANT = "outlineVariant"
    const val INVERSE_SURFACE = "inverseSurface"
    const val INVERSE_ON_SURFACE = "inverseOnSurface"
    const val INVERSE_PRIMARY = "inversePrimary"
    const val SCRIM = "scrim"
    const val SURFACE_TINT = "surfaceTint"
}

/**
 * The six editable roles of a color scheme, stored as "#RRGGBB" hex strings.
 * on-colors, containers and variants are derived from these at runtime, so a
 * custom theme can never produce unreadable text -- unless [overrides] sets
 * a role explicitly. An override is the theme author's real value for that
 * role (taken from the official theme palette for built-ins); it wins over
 * the derived guess, so a theme can match its official definition instead
 * of only approximating it through six colors.
 */
@Serializable
data class SchemeRoles(
    val primary: String,
    val secondary: String,
    val tertiary: String,
    val background: String,
    val surface: String,
    val error: String,
    val overrides: Map<String, String> = emptyMap()
)

@Serializable
data class AppColorScheme(
    val id: String,
    val name: String,
    val light: SchemeRoles,
    val dark: SchemeRoles
)

@Serializable
data class ColorSchemeExport(
    val version: Int = 2,
    val scheme: AppColorScheme
)
