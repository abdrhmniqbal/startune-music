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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import com.startune.music.data.network.deezer.DeezerArtistCandidate
import com.startune.music.presentation.components.SmartImage
import com.startune.music.presentation.components.SmartImageListTargetSize
import com.startune.music.presentation.viewmodel.ArtistDetailViewModel
import com.startune.music.ui.theme.RoundedSans
import kotlinx.coroutines.launch

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
            TextField(
                value = query,
                onValueChange = { viewModel.setArtworkSearchQuery(it) },
                placeholder = { Text(stringResource(R.string.presentation_batch_d_search_deezer_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                ),
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                },
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
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                shape = AbsoluteSmoothCornerShape(
                    cornerRadiusTL = 10.dp, smoothnessAsPercentBL = 60,
                    cornerRadiusTR = 10.dp, smoothnessAsPercentBR = 60,
                    cornerRadiusBL = 10.dp, smoothnessAsPercentTL = 60,
                    cornerRadiusBR = 10.dp, smoothnessAsPercentTR = 60
                ),
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
                    verticalArrangement = Arrangement.spacedBy(2.dp)
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

@Composable
private fun ArtworkCandidateRow(
    candidate: DeezerArtistCandidate,
    isCurrent: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(
                if (isCurrent) MaterialTheme.colorScheme.surfaceContainerLow
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
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
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = when {
                    candidate.fanCount > 0 -> stringResource(
                        R.string.presentation_batch_d_fans,
                        candidate.fanCount
                    )
                    candidate.albumCount > 0 -> stringResource(
                        R.string.presentation_batch_d_albums,
                        candidate.albumCount
                    )
                    else -> "Deezer ID: ${candidate.id}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        if (isCurrent) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
