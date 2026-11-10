package com.startune.music.presentation.screens

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startune.music.R
import com.startune.music.presentation.components.MiniPlayerHeight
import com.startune.music.presentation.viewmodel.DictionariesViewModel
import com.startune.music.presentation.viewmodel.DictionaryPackUiState
import com.startune.music.presentation.viewmodel.DictionaryStatus

@Composable
fun DictionariesScreen(
    onBack: () -> Unit,
    viewModel: DictionariesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dictionaries_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = onBack,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.auth_cd_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
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
            item {
                Text(
                    text = stringResource(R.string.dictionaries_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            items(state.packs, key = { it.pack.id }) { packState ->
                DictionaryPackCard(
                    packState = packState,
                    onDownload = { viewModel.download(packState.pack) },
                    onCancel = { viewModel.cancel(packState.pack.id) },
                    onDelete = { viewModel.delete(packState.pack) }
                )
            }
        }
    }
}

@Composable
private fun DictionaryPackCard(
    packState: DictionaryPackUiState,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pack = packState.pack
    val downloadSize = Formatter.formatShortFileSize(context, pack.downloadSizeBytes)
    val installedSize = Formatter.formatShortFileSize(context, pack.installedSizeBytes)
    val statusText = when (packState.status) {
        DictionaryStatus.DOWNLOADED -> if (packState.hasUpdate) {
            stringResource(R.string.dictionaries_update_available)
        } else {
            stringResource(R.string.dictionaries_downloaded)
        }
        DictionaryStatus.DOWNLOADING -> stringResource(
            R.string.dictionaries_downloading,
            ((packState.downloadProgress ?: 0f) * 100).toInt()
        )
        DictionaryStatus.FAILED -> stringResource(R.string.dictionaries_download_failed)
        DictionaryStatus.NOT_DOWNLOADED -> stringResource(R.string.dictionaries_not_downloaded)
    }
    val statusIcon = when (packState.status) {
        DictionaryStatus.DOWNLOADED -> Icons.Rounded.CheckCircle
        DictionaryStatus.FAILED -> Icons.Rounded.Error
        else -> Icons.Rounded.Translate
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pack.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = pack.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = stringResource(R.string.dictionaries_download_size, downloadSize),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (packState.status == DictionaryStatus.DOWNLOADED) {
                Text(
                    text = stringResource(R.string.dictionaries_installed_size, installedSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = when (packState.status) {
                    DictionaryStatus.FAILED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (packState.status == DictionaryStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { packState.downloadProgress ?: 0f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (packState.status) {
                    DictionaryStatus.DOWNLOADING -> {
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.dictionaries_cancel))
                        }
                    }
                    DictionaryStatus.DOWNLOADED -> {
                        if (packState.hasUpdate) {
                            Button(
                                onClick = onDownload,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.dictionaries_update))
                            }
                        }
                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.dictionaries_delete))
                        }
                    }
                    DictionaryStatus.FAILED -> {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.dictionaries_retry))
                        }
                    }
                    DictionaryStatus.NOT_DOWNLOADED -> {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.dictionaries_download))
                        }
                    }
                }
            }
        }
    }
}
