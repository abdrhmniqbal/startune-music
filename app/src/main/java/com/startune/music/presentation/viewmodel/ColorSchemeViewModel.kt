package com.startune.music.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startune.music.data.preferences.AppColorScheme
import com.startune.music.data.preferences.ColorSchemeExport
import com.startune.music.data.preferences.ColorSchemeIds
import com.startune.music.data.preferences.SchemeRoles
import com.startune.music.data.preferences.ThemePreferencesRepository
import com.startune.music.ui.theme.BuiltInColorSchemes
import com.startune.music.ui.theme.isValidHex
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

sealed interface ColorSchemeImportResult {
    data class Success(val name: String) : ColorSchemeImportResult
    data object Invalid : ColorSchemeImportResult
}

data class EditorInitial(
    val id: String?,
    val name: String,
    val light: SchemeRoles,
    val dark: SchemeRoles
)

private val importJson = Json { ignoreUnknownKeys = true }

private fun SchemeRoles.isValid(): Boolean =
    listOf(primary, secondary, tertiary, background, surface, error).all(::isValidHex)

private fun AppColorScheme.isValid(): Boolean =
    id.isNotBlank() && name.isNotBlank() && light.isValid() && dark.isValid()

@HiltViewModel
class ColorSchemeViewModel @Inject constructor(
    private val themePreferencesRepository: ThemePreferencesRepository
) : ViewModel() {
    val selectedSchemeId: StateFlow<String> = themePreferencesRepository.selectedColorSchemeIdFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ColorSchemeIds.DYNAMIC
        )

    val customSchemes: StateFlow<List<AppColorScheme>> = themePreferencesRepository.customColorSchemesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun selectScheme(id: String) {
        viewModelScope.launch { themePreferencesRepository.setSelectedColorSchemeId(id) }
    }

    fun newSchemeId(): String = UUID.randomUUID().toString()

    /**
     * Loads the editor's starting state. A new theme starts from the currently
     * selected scheme's colors; editing loads the stored custom theme.
     * Returns null when the id does not exist.
     */
    suspend fun loadEditorInitial(themeId: String): EditorInitial? {
        val customs = themePreferencesRepository.customColorSchemesFlow.first()
        if (themeId != "new") {
            val existing = customs.firstOrNull { it.id == themeId } ?: return null
            return EditorInitial(
                id = existing.id,
                name = existing.name,
                light = existing.light,
                dark = existing.dark
            )
        }
        val selectedId = themePreferencesRepository.selectedColorSchemeIdFlow.first()
        val base = BuiltInColorSchemes.find(selectedId)
            ?: customs.firstOrNull { it.id == selectedId }
            ?: BuiltInColorSchemes.startune
        return EditorInitial(id = null, name = "", light = base.light, dark = base.dark)
    }

    /** Inserts a new custom scheme or replaces the one with the same id, then selects it. */
    fun saveCustomScheme(scheme: AppColorScheme) {
        viewModelScope.launch {
            val current = themePreferencesRepository.customColorSchemesFlow.first()
            val updated = if (current.any { it.id == scheme.id }) {
                current.map { if (it.id == scheme.id) scheme else it }
            } else {
                current + scheme
            }
            themePreferencesRepository.setCustomColorSchemes(updated)
            themePreferencesRepository.setSelectedColorSchemeId(scheme.id)
        }
    }

    /** Deletes a custom scheme; if it was active, the app falls back to Dynamic Color. */
    fun deleteCustomScheme(id: String) {
        viewModelScope.launch {
            val current = themePreferencesRepository.customColorSchemesFlow.first()
            themePreferencesRepository.setCustomColorSchemes(current.filterNot { it.id == id })
            if (themePreferencesRepository.selectedColorSchemeIdFlow.first() == id) {
                themePreferencesRepository.setSelectedColorSchemeId(ColorSchemeIds.DYNAMIC)
            }
        }
    }

    fun exportJson(scheme: AppColorScheme): String =
        importJson.encodeToString(ColorSchemeExport.serializer(), ColorSchemeExport(scheme = scheme))

    suspend fun importFromJson(text: String): ColorSchemeImportResult {
        val parsed = runCatching {
            importJson.decodeFromString<ColorSchemeExport>(text).scheme
        }.getOrElse {
            runCatching { importJson.decodeFromString<AppColorScheme>(text) }.getOrNull()
        } ?: return ColorSchemeImportResult.Invalid
        if (!parsed.isValid()) return ColorSchemeImportResult.Invalid
        val current = themePreferencesRepository.customColorSchemesFlow.first()
        val takenIds = BuiltInColorSchemes.all.map { it.id } + current.map { it.id }
        val id = if (parsed.id in takenIds) UUID.randomUUID().toString() else parsed.id
        val takenNames = current.map { it.name }
        var name = parsed.name.trim()
        var suffix = 2
        while (name in takenNames) {
            name = "${parsed.name.trim()} ($suffix)"
            suffix++
        }
        val scheme = parsed.copy(id = id, name = name)
        themePreferencesRepository.setCustomColorSchemes(current + scheme)
        return ColorSchemeImportResult.Success(name)
    }
}
