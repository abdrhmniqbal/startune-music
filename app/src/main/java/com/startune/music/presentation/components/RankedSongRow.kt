package com.startune.music.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.startune.music.data.model.Song
import com.startune.music.presentation.components.subcomps.EnhancedSongListItem

/**
 * Shared ranked row for Home Top Songs and Genre Top Songs.
 *
 * Same design as the Top Songs detail screen (TopTracksScreen): the rank
 * is plain text outside the row, the song itself is EnhancedSongListItem
 * (art, title, artist, more button). The only difference from the detail
 * list is the pager: rows slide sideways in pages of 5, with the entrance
 * stagger while a page arrives.
 */
@Composable
fun RankedSongRow(
    rank: Int,
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onMoreOptionsClick: (Song) -> Unit,
    isSelected: Boolean = false,
    selectionIndex: Int? = null,
    isSelectionMode: Boolean = false,
    entranceProgress: Float = 1f,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = entranceProgress
                translationY = (1f - entranceProgress) * 24f
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = rank.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp)
        )
        EnhancedSongListItem(
            modifier = Modifier.weight(1f),
            song = song,
            isPlaying = isPlaying,
            isCurrentSong = isCurrentSong,
            isSelected = isSelected,
            selectionIndex = selectionIndex,
            isSelectionMode = isSelectionMode,
            onLongPress = { onLongClick?.invoke() },
            onMoreOptionsClick = onMoreOptionsClick,
            onClick = onClick
        )
    }
}
