package com.startune.music.presentation.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.startune.music.data.updates.AppUpdateInfo
import com.startune.music.data.updates.AppUpdateManager
import com.startune.music.presentation.viewmodel.AppUpdateViewModel

/**
 * Content of the update sheet shown at startup when a newer release exists.
 * Download runs in the background (WorkManager); the sheet shows live
 * progress and turns into an Install button when the APK is ready.
 */
@Composable
fun AppUpdateSheet(
    update: AppUpdateInfo,
    onDismiss: () -> Unit,
    onDontRemind: () -> Unit,
    viewModel: AppUpdateViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.update_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.size(8.dp))
            VersionBadge(versionNumber = update.newVersion)
            if (update.prerelease) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.presentation_batch_g_changelog_prerelease),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 6.dp
        ) {
            Text(
                text = update.body.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.presentation_batch_g_changelog_no_release_notes),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        when (val state = downloadState) {
            is AppUpdateManager.DownloadState.Downloading -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(
                            R.string.update_downloading,
                            (state.progress * 100).toInt()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is AppUpdateManager.DownloadState.Downloaded -> {
                Button(
                    onClick = {
                        if (viewModel.installDownloadedApk(state.file)) {
                            onDismiss()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.update_enable_install),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.update_install))
                }
            }
            AppUpdateManager.DownloadState.Failed -> {
                Text(
                    text = stringResource(R.string.update_download_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Button(
                    onClick = { viewModel.enqueueDownload(update) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.update_download_and_install))
                }
            }
            AppUpdateManager.DownloadState.Idle -> {
                if (update.downloadUrl.isBlank()) {
                    Button(
                        onClick = { openUpdateUrl(context, update.htmlUrl) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.update_view_on_github))
                    }
                } else {
                    Button(
                        onClick = { viewModel.enqueueDownload(update) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.update_download_and_install))
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDontRemind) {
                Text(stringResource(R.string.update_dont_remind))
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.update_later))
            }
        }
    }
}

private fun openUpdateUrl(context: Context, url: String) {
    if (url.isBlank()) return
    val uri = try { url.toUri() } catch (_: Throwable) { url.toUri() }
    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}
