package com.startune.music.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.startune.music.data.dictionary.DictionaryManager
import com.startune.music.data.dictionary.DictionaryManifest
import com.startune.music.data.dictionary.DictionaryPack
import com.startune.music.data.dictionary.DictionaryDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DictionaryStatus {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

data class DictionaryPackUiState(
    val pack: DictionaryPack,
    val status: DictionaryStatus,
    val downloadProgress: Float?,
    val installedVersion: Int?,
    val hasUpdate: Boolean
)

data class DictionariesUiState(
    val packs: List<DictionaryPackUiState> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DictionariesViewModel @Inject constructor(
    private val dictionaryManager: DictionaryManager
) : ViewModel() {

    private val manifestFlow = MutableStateFlow<DictionaryManifest?>(null)
    private val workInfosFlow = MutableStateFlow<Map<String, WorkInfo?>>(emptyMap())

    val uiState: StateFlow<DictionariesUiState> = combine(
        manifestFlow,
        dictionaryManager.installedVersionsFlow,
        workInfosFlow
    ) { manifest, installedVersions, workInfos ->
        val packs = manifest?.packs.orEmpty().map { pack ->
            val installedVersion = installedVersions[pack.id]
            val filesPresent = dictionaryManager.isPackFilesPresent(pack.id)
            val workInfo = workInfos[pack.id]
            val bytes = workInfo?.progress?.getLong(DictionaryDownloadWorker.PROGRESS_BYTES, 0L) ?: 0L
            val total = workInfo?.progress?.getLong(DictionaryDownloadWorker.PROGRESS_TOTAL, 0L) ?: 0L
            val progress = if (total > 0) (bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f) else null
            val status = when {
                workInfo?.state == WorkInfo.State.RUNNING ||
                    workInfo?.state == WorkInfo.State.ENQUEUED -> DictionaryStatus.DOWNLOADING
                workInfo?.state == WorkInfo.State.FAILED -> DictionaryStatus.FAILED
                installedVersion != null && filesPresent -> DictionaryStatus.DOWNLOADED
                else -> DictionaryStatus.NOT_DOWNLOADED
            }
            DictionaryPackUiState(
                pack = pack,
                status = status,
                downloadProgress = progress,
                installedVersion = installedVersion,
                hasUpdate = installedVersion != null && installedVersion < pack.version && filesPresent
            )
        }
        DictionariesUiState(packs = packs, isLoading = manifest == null)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DictionariesUiState())

    init {
        viewModelScope.launch {
            val manifest = dictionaryManager.fetchManifest()
                ?: DictionaryManager.BAKED_IN_MANIFEST
            manifestFlow.value = manifest
            manifest.packs.forEach { pack ->
                launch {
                    dictionaryManager.downloadWorkInfo(pack.id).collect { info ->
                        workInfosFlow.update { it + (pack.id to info) }
                    }
                }
            }
        }
    }

    fun download(pack: DictionaryPack) {
        dictionaryManager.enqueueDownload(pack)
    }

    fun cancel(packId: String) {
        dictionaryManager.cancelDownload(packId)
    }

    fun delete(pack: DictionaryPack) {
        viewModelScope.launch {
            dictionaryManager.deletePack(pack.id)
        }
    }
}
