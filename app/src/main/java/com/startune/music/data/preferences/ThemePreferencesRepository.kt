package com.startune.music.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val PLAYER_THEME_PREFERENCE = stringPreferencesKey("player_theme_preference_v2")
        val ALBUM_ART_PALETTE_STYLE = stringPreferencesKey("album_art_palette_style_v1")
        val ALBUM_ART_COLOR_ACCURACY = intPreferencesKey("album_art_color_accuracy_v1")
        val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val GLOBAL_NOW_PLAYING_THEME_ENABLED = booleanPreferencesKey("global_now_playing_theme_enabled_v1")
        val SELECTED_COLOR_SCHEME_ID = stringPreferencesKey("selected_color_scheme_id")
        val CUSTOM_COLOR_SCHEMES = stringPreferencesKey("custom_color_schemes_json")
        val PITCH_BLACK_ENABLED = booleanPreferencesKey("pitch_black_enabled")
    }

    private val schemeJson = Json { ignoreUnknownKeys = true }

    val appThemeModeFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.APP_THEME_MODE] ?: AppThemeMode.FOLLOW_SYSTEM
    }

    val playerThemePreferenceFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.PLAYER_THEME_PREFERENCE] ?: ThemePreference.ALBUM_ART
    }

    val globalNowPlayingThemeEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.GLOBAL_NOW_PLAYING_THEME_ENABLED] ?: false
    }

    val albumArtPaletteStyleFlow: Flow<AlbumArtPaletteStyle> = dataStore.data.map { preferences ->
        AlbumArtPaletteStyle.fromStorageKey(preferences[Keys.ALBUM_ART_PALETTE_STYLE])
    }

    val albumArtColorAccuracyFlow: Flow<Int> = dataStore.data.map { preferences ->
        AlbumArtColorAccuracy.clamp(preferences[Keys.ALBUM_ART_COLOR_ACCURACY] ?: AlbumArtColorAccuracy.DEFAULT)
    }

    val selectedColorSchemeIdFlow: Flow<String> = dataStore.data.map { preferences ->
        preferences[Keys.SELECTED_COLOR_SCHEME_ID] ?: ColorSchemeIds.DYNAMIC
    }

    val customColorSchemesFlow: Flow<List<AppColorScheme>> = dataStore.data.map { preferences ->
        val raw = preferences[Keys.CUSTOM_COLOR_SCHEMES] ?: return@map emptyList()
        runCatching { schemeJson.decodeFromString<List<AppColorScheme>>(raw) }.getOrDefault(emptyList())
    }

    val pitchBlackEnabledFlow: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[Keys.PITCH_BLACK_ENABLED] ?: false
    }

    suspend fun setPlayerThemePreference(themeMode: String) =
        dataStore.edit { preferences ->
            preferences[Keys.PLAYER_THEME_PREFERENCE] = themeMode
        }

    suspend fun setAppThemeMode(themeMode: String) =
        dataStore.edit { preferences ->
            preferences[Keys.APP_THEME_MODE] = themeMode
        }

    suspend fun setGlobalNowPlayingThemeEnabled(enabled: Boolean) =
        dataStore.edit { preferences ->
            preferences[Keys.GLOBAL_NOW_PLAYING_THEME_ENABLED] = enabled
        }

    suspend fun setSelectedColorSchemeId(id: String) =
        dataStore.edit { preferences ->
            preferences[Keys.SELECTED_COLOR_SCHEME_ID] = id
        }

    suspend fun setCustomColorSchemes(schemes: List<AppColorScheme>) =
        dataStore.edit { preferences ->
            preferences[Keys.CUSTOM_COLOR_SCHEMES] = schemeJson.encodeToString(schemes)
        }

    suspend fun setPitchBlackEnabled(enabled: Boolean) =
        dataStore.edit { preferences ->
            preferences[Keys.PITCH_BLACK_ENABLED] = enabled
        }

    suspend fun initializeAppThemeMode(themeMode: String) =
        dataStore.edit { preferences ->
            if (preferences[Keys.APP_THEME_MODE] == null) {
                preferences[Keys.APP_THEME_MODE] = themeMode
            }
        }

    suspend fun setAlbumArtPaletteStyle(style: AlbumArtPaletteStyle) =
        dataStore.edit { preferences ->
            preferences[Keys.ALBUM_ART_PALETTE_STYLE] = style.storageKey
        }

    suspend fun setAlbumArtColorAccuracy(level: Int) =
        dataStore.edit { preferences ->
            preferences[Keys.ALBUM_ART_COLOR_ACCURACY] = AlbumArtColorAccuracy.clamp(level)
        }

    suspend fun setAlbumArtPaletteSettings(
        style: AlbumArtPaletteStyle,
        accuracyLevel: Int
    ) = dataStore.edit { preferences ->
        preferences[Keys.ALBUM_ART_PALETTE_STYLE] = style.storageKey
        preferences[Keys.ALBUM_ART_COLOR_ACCURACY] = AlbumArtColorAccuracy.clamp(accuracyLevel)
    }
}
