package com.startune.music.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startune.music.R
import com.startune.music.presentation.components.MiniPlayerHeight
import com.startune.music.presentation.utils.getSegmentedItemShape
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
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (state.isLoading) {
                item { DictionariesLoadingItem() }
            } else if (state.packs.isEmpty()) {
                item { DictionariesEmptyState() }
            } else {
                itemsIndexed(state.packs, key = { _, packState -> packState.pack.id }) { index, packState ->
                    DictionaryPackCard(
                        packState = packState,
                        shape = getSegmentedItemShape(index, state.packs.size),
                        onDownload = { viewModel.download(packState.pack) },
                        onCancel = { viewModel.cancel(packState.pack.id) },
                        onDelete = { viewModel.delete(packState.pack) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DictionaryPackCard(
    packState: DictionaryPackUiState,
    shape: Shape,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pack = packState.pack
    // Downloaded with no update needs no text: the check tile and the
    // Delete button already say it. A not-downloaded pack needs none
    // either: its Download button says it.
    val statusText: String? = when (packState.status) {
        DictionaryStatus.DOWNLOADED -> if (packState.hasUpdate) {
            stringResource(R.string.dictionaries_update_available)
        } else {
            null
        }
        DictionaryStatus.DOWNLOADING -> stringResource(
            R.string.dictionaries_downloading,
            ((packState.downloadProgress ?: 0f) * 100).toInt()
        )
        DictionaryStatus.FAILED -> stringResource(R.string.dictionaries_download_failed)
        DictionaryStatus.NOT_DOWNLOADED -> null
    }
    val statusIcon = when (packState.status) {
        DictionaryStatus.DOWNLOADED -> Icons.Rounded.CheckCircle
        DictionaryStatus.FAILED -> Icons.Rounded.Error
        else -> Icons.Rounded.Translate
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pack.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = pack.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            statusText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = when (packState.status) {
                        DictionaryStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (packState.status == DictionaryStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { packState.downloadProgress ?: 0f },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (packState.status) {
                DictionaryStatus.DOWNLOADING -> {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.dictionaries_cancel))
                    }
                }
                DictionaryStatus.DOWNLOADED -> {
                    if (packState.hasUpdate) {
                        FilledTonalButton(
                            onClick = onDownload,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.dictionaries_update))
                        }
                    }
                    FilledTonalButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
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
                    FilledTonalButton(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.dictionaries_retry))
                    }
                }
                DictionaryStatus.NOT_DOWNLOADED -> {
                    FilledTonalButton(
                        onClick = onDownload,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.dictionaries_download))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DictionariesLoadingItem() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        LoadingIndicator(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun DictionariesEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 56.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.Translate,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.dictionaries_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.dictionaries_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
