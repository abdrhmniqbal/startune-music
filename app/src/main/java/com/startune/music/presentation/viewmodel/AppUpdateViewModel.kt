package com.startune.music.presentation.viewmodel

import com.startune.music.data.updates.AppUpdateInfo
import com.startune.music.data.updates.AppUpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import androidx.lifecycle.ViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val appUpdateManager: AppUpdateManager
) : ViewModel() {
    val pendingUpdate: StateFlow<AppUpdateInfo?> = appUpdateManager.pendingUpdate
    val downloadState: StateFlow<AppUpdateManager.DownloadState> = appUpdateManager.downloadState

    val currentVersion: String get() = appUpdateManager.currentVersion

    fun checkOnStartup() = appUpdateManager.checkOnStartup()

    suspend fun checkManually() = appUpdateManager.checkManually()

    fun dismissPendingUpdate(dontRemind: Boolean) =
        appUpdateManager.dismissPendingUpdate(dontRemind)

    fun enqueueDownload(info: AppUpdateInfo) = appUpdateManager.enqueueDownload(info)

    fun installDownloadedApk(file: File) = appUpdateManager.installDownloadedApk(file)
}
