package com.startune.music.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import com.startune.music.presentation.components.rememberModalSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.startune.music.R
import com.startune.music.data.network.deezer.DeezerArtistCandidate
import com.startune.music.presentation.components.SmartImage
import com.startune.music.presentation.components.SmartImageListTargetSize
import com.startune.music.presentation.viewmodel.ArtistDetailViewModel
import com.startune.music.ui.theme.RoundedSans
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Artwork picker for an artist, like old Startune's ArtistArtworkSheet:
 * search Deezer by name and pick a candidate photo, or choose a custom
 * photo from the device. A picked photo is stored as the custom image, so
 * Reset to default still reverts to the automatic Deezer photo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistArtworkSheet(
    artistName: String,
    currentArtworkUrl: String?,
    hasCustomImage: Boolean,
    viewModel: ArtistDetailViewModel,
    onDismiss: () -> Unit,
    onPickCustomPhoto: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalSheetState(skipPartiallyExpanded = true)
    val query by viewModel.artworkSearchQuery.collectAsStateWithLifecycle()
    val candidates by viewModel.artworkCandidates.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearchingArtwork.collectAsStateWithLifecycle()
    var isApplying by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.openArtworkSearch(artistName)
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.closeArtworkSearch() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setArtworkSearchQuery(it) },
                placeholder = {
                    Text(
                        stringResource(R.string.mix_artists_search_placeholder),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.cd_search_icon),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                ),
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setArtworkSearchQuery("") }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.cd_clear_search)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                ),
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.presentation_batch_d_select_artwork),
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = RoundedSans,
                    fontWeight = FontWeight.Bold
                )
                if (hasCustomImage) {
                    TextButton(
                        onClick = {
                            viewModel.clearCustomImage()
                            Toast.makeText(
                                context,
                                R.string.presentation_batch_d_artwork_removed,
                                Toast.LENGTH_SHORT
                            ).show()
                            onDismiss()
                        },
                        enabled = !isApplying
                    ) {
                        Text(stringResource(R.string.presentation_batch_d_reset_to_default))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable {
                        onPickCustomPhoto()
                        onDismiss()
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddAPhoto,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = stringResource(R.string.presentation_batch_d_choose_from_device),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isSearching && candidates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (candidates.isEmpty()) {
                Text(
                    text = stringResource(R.string.presentation_batch_d_no_artwork_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = candidates,
                        key = { it.id }
                    ) { candidate ->
                        ArtworkCandidateRow(
                            candidate = candidate,
                            isCurrent = currentArtworkUrl != null &&
                                currentArtworkUrl == candidate.pictureUrl,
                            enabled = !isApplying,
                            onClick = {
                                if (isApplying) return@ArtworkCandidateRow
                                isApplying = true
                                coroutineScope.launch {
                                    val applied = viewModel.applyDeezerArtwork(candidate.pictureUrl)
                                    isApplying = false
                                    if (applied) {
                                        Toast.makeText(
                                            context,
                                            R.string.presentation_batch_d_artwork_updated,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            R.string.presentation_batch_d_artwork_update_failed,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Compact count like 10.35M or 613.42K (two decimals, US formatting),
 * for the Deezer fan count. Plain number below 1,000.
 */
private fun formatCompactCount(count: Int): String {
    val value = count.toDouble()
    return when {
        count >= 1_000_000_000 -> String.format(Locale.US, "%.2fB", value / 1_000_000_000)
        count >= 1_000_000 -> String.format(Locale.US, "%.2fM", value / 1_000_000)
        count >= 1_000 -> String.format(Locale.US, "%.2fK", value / 1_000)
        else -> count.toString()
    }
}

@Composable
private fun ArtworkCandidateRow(
    candidate: DeezerArtistCandidate,
    isCurrent: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer
    val contentColor = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurface
    val subtitleColor = if (isCurrent) contentColor.copy(alpha = 0.75f)
        else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmartImage(
                model = candidate.pictureUrl,
                contentDescription = candidate.name,
                shape = CircleShape,
                targetSize = SmartImageListTargetSize,
                modifier = Modifier.size(72.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(
                    text = candidate.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1
                )
                Text(
                    text = when {
                        candidate.fanCount > 0 -> stringResource(
                            R.string.presentation_batch_d_fans,
                            formatCompactCount(candidate.fanCount)
                        )
                        candidate.albumCount > 0 -> stringResource(
                            R.string.presentation_batch_d_albums,
                            candidate.albumCount
                        )
                        else -> "Deezer ID: ${candidate.id}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = subtitleColor,
                    maxLines = 1
                )
            }

            if (isCurrent) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
