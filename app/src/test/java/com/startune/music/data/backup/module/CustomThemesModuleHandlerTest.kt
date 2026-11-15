package com.startune.music.data.backup.module

import com.google.gson.GsonBuilder
import com.startune.music.data.preferences.PreferenceBackupEntry
import com.startune.music.data.preferences.UserPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomThemesModuleHandlerTest {

    private val userPreferencesRepository: UserPreferencesRepository = mockk(relaxed = true)
    private val handler = CustomThemesModuleHandler(
        userPreferencesRepository = userPreferencesRepository,
        gson = GsonBuilder().serializeNulls().create()
    )

    private val allEntries = listOf(
        PreferenceBackupEntry(key = "custom_color_schemes_json", type = "string", stringValue = "[]"),
        PreferenceBackupEntry(key = "selected_color_scheme_id", type = "string", stringValue = "nord"),
        PreferenceBackupEntry(key = "app_theme_mode", type = "string", stringValue = "dark")
    )

    @Test
    fun `export only includes the theme keys`() = runTest {
        coEvery { userPreferencesRepository.exportPreferencesForBackup() } returns allEntries

        val payload = handler.export()

        assertTrue(payload.contains("custom_color_schemes_json"))
        assertTrue(payload.contains("selected_color_scheme_id"))
        assertFalse(payload.contains("app_theme_mode"))
    }

    @Test
    fun `countEntries only counts the theme keys`() = runTest {
        coEvery { userPreferencesRepository.exportPreferencesForBackup() } returns allEntries

        assertEquals(2, handler.countEntries())
    }

    @Test
    fun `restore clears only the theme keys then imports them`() = runTest {
        val payload = """
            [
              {"key": "custom_color_schemes_json", "type": "string", "stringValue": "[]"},
              {"key": "selected_color_scheme_id", "type": "string", "stringValue": "nord"}
            ]
        """.trimIndent()

        handler.restore(payload)

        coVerify(exactly = 1) {
            userPreferencesRepository.clearPreferencesByKeys(
                CustomThemesModuleHandler.CUSTOM_THEMES_KEYS
            )
        }
        coVerify(exactly = 1) {
            userPreferencesRepository.importPreferencesFromBackup(any(), clearExisting = false)
        }
    }
}
