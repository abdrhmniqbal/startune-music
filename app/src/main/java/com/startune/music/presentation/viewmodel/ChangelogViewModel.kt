package com.startune.music.presentation.viewmodel

import com.startune.music.data.updates.GitHubRelease
import com.startune.music.data.updates.GitHubReleasesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class ChangelogViewModel @Inject constructor(
    private val releasesRepository: GitHubReleasesRepository
) : ViewModel() {
    sealed interface UiState {
        data object Loading : UiState
        data class Content(val releases: List<GitHubRelease>) : UiState
        data object Empty : UiState
        data object Error : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            runCatching { releasesRepository.getReleaseNotes() }
                .onSuccess { notes ->
                    _uiState.value =
                        if (notes.isEmpty()) UiState.Empty else UiState.Content(notes)
                }
                .onFailure { _uiState.value = UiState.Error }
        }
    }
}
