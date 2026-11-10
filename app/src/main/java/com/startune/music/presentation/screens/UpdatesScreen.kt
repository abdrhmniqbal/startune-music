package com.startune.music.presentation.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startune.music.R
import com.startune.music.data.updates.AppUpdateManager
import com.startune.music.presentation.components.MiniPlayerHeight
import com.startune.music.presentation.viewmodel.AppUpdateViewModel
import kotlinx.coroutines.launch

/**
 * Settings → Updates. A centralized view of the app's own update flow
 * (GitHub releases): check now, download, install, and the installed
 * version. There is no release-channel selector here: Startune only
 * ships GitHub releases, so the upstream STABLE (F-Droid) channel does
 * not apply. The prerelease choice lives in the existing
 * include-prereleases preference the manager already reads.
 */
@Composable
fun UpdatesScreen(
    onBack: () -> Unit,
    viewModel: AppUpdateViewModel = hiltViewModel()
) {
    val pendingUpdate by viewModel.pendingUpdate.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var lastCheckUpToDate by remember { mutableStateOf(false) }
    var lastCheckFailed by remember { mutableStateOf(false) }

    fun checkNow() {
        if (isChecking) return
        isChecking = true
        lastCheckUpToDate = false
        lastCheckFailed = false
        coroutineScope.launch {
            when (viewModel.checkManually()) {
                is AppUpdateManager.ManualCheckResult.UpdateAvailable -> Unit
                AppUpdateManager.ManualCheckResult.UpToDate -> lastCheckUpToDate = true
                AppUpdateManager.ManualCheckResult.Error -> lastCheckFailed = true
            }
            isChecking = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.updates_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.auth_cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            )
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = scaffoldPadding.calculateTopPadding() + 12.dp,
                end = 16.dp,
                bottom = MiniPlayerHeight + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "status") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Icon(
                                Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.updates_installed_version, viewModel.currentVersion),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.updates_source_github),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val info = pendingUpdate
                        Text(
                            text = when {
                                isChecking -> stringResource(R.string.updates_status_checking)
                                downloadState is AppUpdateManager.DownloadState.Downloading -> {
                                    val progress = (downloadState as AppUpdateManager.DownloadState.Downloading).progress
                                    stringResource(
                                        R.string.updates_status_downloading_progress,
                                        info?.newVersion ?: "",
                                        (progress * 100).toInt()
                                    )
                                }
                                downloadState is AppUpdateManager.DownloadState.Downloaded -> stringResource(
                                    R.string.updates_status_downloaded,
                                    info?.newVersion ?: ""
                                )
                                downloadState is AppUpdateManager.DownloadState.Failed -> stringResource(R.string.updates_status_download_failed)
                                info != null -> stringResource(R.string.updates_status_available, info.newVersion)
                                lastCheckFailed -> stringResource(R.string.update_check_failed)
                                lastCheckUpToDate -> stringResource(R.string.update_up_to_date)
                                else -> stringResource(R.string.updates_status_idle)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        when (val state = downloadState) {
                            is AppUpdateManager.DownloadState.Downloading -> {
                                LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                            }
                            is AppUpdateManager.DownloadState.Downloaded -> {
                                Button(
                                    onClick = { viewModel.installDownloadedApk(state.file) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(stringResource(R.string.updates_action_install)) }
                                OutlinedButton(
                                    onClick = { viewModel.dismissPendingUpdate(false) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(stringResource(R.string.updates_action_dismiss)) }
                            }
                            is AppUpdateManager.DownloadState.Failed -> {
                                Button(onClick = ::checkNow, modifier = Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.updates_action_check_again))
                                }
                                OutlinedButton(
                                    onClick = { viewModel.dismissPendingUpdate(false) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(stringResource(R.string.updates_action_dismiss)) }
                            }
                            AppUpdateManager.DownloadState.Idle -> {
                                if (isChecking) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                } else if (info != null) {
                                    Button(
                                        onClick = { viewModel.enqueueDownload(info) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(stringResource(R.string.updates_action_install)) }
                                    OutlinedButton(
                                        onClick = { openUrl(context, info.htmlUrl) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(stringResource(R.string.updates_action_release_notes)) }
                                } else {
                                    Button(onClick = ::checkNow, modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            if (lastCheckUpToDate) stringResource(R.string.updates_action_check_again)
                                            else stringResource(R.string.updates_action_check)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    val uri = try {
        url.toUri()
    } catch (_: Throwable) {
        return
    }
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
