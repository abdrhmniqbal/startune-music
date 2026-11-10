package com.startune.music.data.dictionary

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.startune.music.data.preferences.UserPreferencesRepository
import com.startune.music.utils.MultiLangRomanizer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber

/**
 * On-demand dictionaries (Kuromoji IPADIC for Japanese lyric romanization).
 *
 * Nothing downloads on its own: the user taps Download on the Dictionaries
 * screen, the zip comes from the startune-dictionaries repo, is verified
 * against the manifest sha256, extracted to internal storage, and only then
 * does the romanizer pick it up. Deleting a pack removes its files and
 * unloads it from the romanizer.
 */
@Singleton
class DictionaryManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    val installedVersionsFlow: Flow<Map<String, Int>> =
        userPreferencesRepository.installedDictionariesFlow

    fun dictionariesDir(): File = File(context.filesDir, DICTIONARIES_DIR_NAME)

    fun packDir(packId: String): File = File(dictionariesDir(), packId)

    fun isPackFilesPresent(packId: String): Boolean {
        val dir = packDir(packId)
        if (!dir.isDirectory) return false
        return DICTIONARY_FILE_NAMES.all { File(dir, it).isFile }
    }

    /** The installed directory for [packId], or null when it is not installed. */
    suspend fun installedPackDir(packId: String): File? {
        val installed = installedVersionsFlow.first()
        if (!installed.containsKey(packId)) return null
        return packDir(packId).takeIf { isPackFilesPresent(packId) }
    }

    suspend fun fetchManifest(): DictionaryManifest? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(MANIFEST_URL).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                manifestJson.decodeFromString<DictionaryManifest>(body)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch dictionary manifest")
            null
        }
    }

    fun enqueueDownload(pack: DictionaryPack) {
        val request = OneTimeWorkRequestBuilder<DictionaryDownloadWorker>()
            .setInputData(
                workDataOf(
                    DictionaryDownloadWorker.KEY_PACK_ID to pack.id,
                    DictionaryDownloadWorker.KEY_URL to pack.url,
                    DictionaryDownloadWorker.KEY_SHA256 to pack.sha256,
                    DictionaryDownloadWorker.KEY_VERSION to pack.version
                )
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            downloadWorkName(pack.id),
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun cancelDownload(packId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(downloadWorkName(packId))
    }

    fun downloadWorkInfo(packId: String): Flow<WorkInfo?> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(downloadWorkName(packId))
            .map { it.firstOrNull() }

    suspend fun deletePack(packId: String) {
        cancelDownload(packId)
        withContext(Dispatchers.IO) {
            packDir(packId).deleteRecursively()
        }
        userPreferencesRepository.removeInstalledDictionaryVersion(packId)
        if (packId == KUROMOJI_PACK_ID) {
            MultiLangRomanizer.setDictionaryDirectory(null)
        }
    }

    companion object {
        const val MANIFEST_URL =
            "https://raw.githubusercontent.com/abdrhmniqbal/startune-dictionaries/main/manifest.json"
        const val DICTIONARIES_CHANNEL_ID = "dictionary_downloads"
        const val DICTIONARIES_DIR_NAME = "dictionaries"
        const val KUROMOJI_PACK_ID = "kuromoji-ipadic"

        /** The 8 IPADIC files kuromoji loads; the zip must contain all of them. */
        val DICTIONARY_FILE_NAMES = listOf(
            "characterDefinitions.bin",
            "connectionCosts.bin",
            "doubleArrayTrie.bin",
            "tokenInfoDictionary.bin",
            "tokenInfoFeaturesMap.bin",
            "tokenInfoPartOfSpeechMap.bin",
            "tokenInfoTargetMap.bin",
            "unknownDictionary.bin"
        )

        /** The one pack that exists today, used when the manifest cannot be fetched. */
        val BAKED_IN_PACK = DictionaryPack(
            id = KUROMOJI_PACK_ID,
            name = "Japanese (IPADIC)",
            description = "Japanese tokenizer dictionary for lyric romanization",
            version = 1,
            downloadSizeBytes = 13212909,
            installedSizeBytes = 33426790,
            sha256 = "df4ef64c1a2b604614f9033fecc93b7dffd71ab5fa1e0851c41be68773519818",
            url = "https://raw.githubusercontent.com/abdrhmniqbal/startune-dictionaries/main/kuromoji-ipadic.zip"
        )
        val BAKED_IN_MANIFEST = DictionaryManifest(version = 1, packs = listOf(BAKED_IN_PACK))

        private val manifestJson = Json { ignoreUnknownKeys = true }

        fun downloadWorkName(packId: String): String = "dictionary_download_$packId"
    }
}
