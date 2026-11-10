package com.startune.music.data.preferences

import kotlinx.serialization.Serializable

object ColorSchemeIds {
    const val DYNAMIC = "dynamic"
    const val STARTUNE = "startune"
}

/**
 * The six editable roles of a color scheme, stored as "#RRGGBB" hex strings.
 * on-colors, containers and variants are derived from these at runtime, so a
 * custom theme can never produce unreadable text.
 */
@Serializable
data class SchemeRoles(
    val primary: String,
    val secondary: String,
    val tertiary: String,
    val background: String,
    val surface: String,
    val error: String
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
    val version: Int = 1,
    val scheme: AppColorScheme
)
