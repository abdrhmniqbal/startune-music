package com.startune.music.data.backup.module

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.startune.music.data.backup.model.BackupSection
import com.startune.music.data.preferences.PreferenceBackupEntry
import com.startune.music.data.preferences.UserPreferencesRepository
import com.startune.music.di.BackupGson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomThemesModuleHandler @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @BackupGson private val gson: Gson
) : BackupModuleHandler {

    override val section = BackupSection.CUSTOM_THEMES

    override suspend fun export(): String = withContext(Dispatchers.IO) {
        val entries = userPreferencesRepository.exportPreferencesForBackup()
            .filter { it.key in CUSTOM_THEMES_KEYS }
        gson.toJson(entries)
    }

    override suspend fun countEntries(): Int = withContext(Dispatchers.IO) {
        userPreferencesRepository.exportPreferencesForBackup()
            .count { it.key in CUSTOM_THEMES_KEYS }
    }

    override suspend fun snapshot(): String = export()

    override suspend fun restore(payload: String) = withContext(Dispatchers.IO) {
        val type = TypeToken.getParameterized(List::class.java, PreferenceBackupEntry::class.java).type
        val entries: List<PreferenceBackupEntry> = gson.fromJson(payload, type)
        userPreferencesRepository.clearPreferencesByKeys(CUSTOM_THEMES_KEYS)
        if (entries.isNotEmpty()) {
            userPreferencesRepository.importPreferencesFromBackup(entries, clearExisting = false)
        }
    }

    override suspend fun rollback(snapshot: String) = restore(snapshot)

    companion object {
        val CUSTOM_THEMES_KEYS = setOf(
            "custom_color_schemes_json",
            "selected_color_scheme_id"
        )
    }
}
