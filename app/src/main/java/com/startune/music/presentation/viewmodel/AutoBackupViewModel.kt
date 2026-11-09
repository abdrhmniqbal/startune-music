package com.startune.music.presentation.viewmodel

import com.startune.music.data.backup.AutoBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

@HiltViewModel
class AutoBackupViewModel @Inject constructor(
    private val autoBackupManager: AutoBackupManager
) : ViewModel() {
    fun runCheck() {
        viewModelScope.launch {
            autoBackupManager.runCheck()
        }
    }
}
