package com.startune.music.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.MicExternalOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Piano
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.startune.music.R
import com.startune.music.utils.resolvePlaylistCoverContentColor

/**
 * Shared cover pickers for the playlist form and the mix form: the same
 * 10 background colors and the same 8 icons, so a mix cover is picked
 * exactly like a playlist cover.
 */

fun getIconByName(name: String?): ImageVector? {
    return when (name) {
        "MusicNote" -> Icons.Rounded.MusicNote
        "Headphones" -> Icons.Rounded.Headphones
        "Album" -> Icons.Rounded.Album
        "Mic" -> Icons.Rounded.MicExternalOn
        "Speaker" -> Icons.Rounded.Speaker
        "Favorite" -> Icons.Rounded.Favorite
        "Piano" -> Icons.Rounded.Piano
        "Queue" -> Icons.AutoMirrored.Rounded.QueueMusic
        else -> null
    }
}


fun getThemeContentColor(colorArgb: Int, scheme: androidx.compose.material3.ColorScheme): Color {
    return resolvePlaylistCoverContentColor(colorArgb, scheme)
}


@Composable
fun CoverColorPicker(
    selectedColor: Int?,
    onColorChange: (Int) -> Unit
) {
     Text(
         modifier = Modifier.padding(start = 22.dp),
         text = stringResource(R.string.presentation_batch_f_background_color),
         style = MaterialTheme.typography.titleSmall,
         color = MaterialTheme.colorScheme.onSurfaceVariant
     )

     val colors = listOf( 
         MaterialTheme.colorScheme.primary.toArgb(),
         MaterialTheme.colorScheme.primaryContainer.toArgb(),
         MaterialTheme.colorScheme.secondary.toArgb(),
         MaterialTheme.colorScheme.secondaryContainer.toArgb(),
         MaterialTheme.colorScheme.tertiary.toArgb(),
         MaterialTheme.colorScheme.tertiaryContainer.toArgb(),
         MaterialTheme.colorScheme.error.toArgb(),
         MaterialTheme.colorScheme.errorContainer.toArgb(),
         MaterialTheme.colorScheme.surfaceContainerHigh.toArgb(),
         MaterialTheme.colorScheme.inverseSurface.toArgb()
     )

     FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
     ) {
         colors.forEach { color ->
             val isSelected = selectedColor == color
             val cornerRadius by animateDpAsState(targetValue = if (isSelected) 12.dp else 24.dp, label = "CornerRadius")

             Box(
                 modifier = Modifier
                     .size(52.dp)
                    .clip(RoundedCornerShape(cornerRadius))
                     .background(if (isSelected) Color(color) else Color.Transparent)
                    .border(
                        width = if (isSelected) 3.dp else 0.dp,
                        color = if (isSelected) Color(color) else Color.Transparent,
                        shape = RoundedCornerShape(cornerRadius)
                    ),
                 contentAlignment = Alignment.Center
             ) {
                  Box(
                     modifier = Modifier
                         .size(if (isSelected) 42.dp else 48.dp)
                        .clip(RoundedCornerShape(if (isSelected) 8.dp else cornerRadius))
                        .background(Color(color))
                         .clickable { onColorChange(color) }
                  )
             }
         }
     }
}


@Composable
fun CoverIconPicker(
    selectedIconName: String?,
    onIconChange: (String) -> Unit
) {
     Text(
         modifier = Modifier.padding(start = 22.dp),
         text = stringResource(R.string.presentation_batch_f_icon_symbol),
         style = MaterialTheme.typography.titleSmall,
         color = MaterialTheme.colorScheme.onSurfaceVariant
     )

     val icons = listOf(
        "MusicNote", "Headphones", "Album", "Mic", "Speaker", "Favorite", "Piano", "Queue"
     )

     FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
     ) {
         icons.forEach { iconName ->
             val isSelected = selectedIconName == iconName
             val cornerRadius by animateDpAsState(targetValue = if (isSelected) 12.dp else 24.dp, label = "CornerRadius")

             Box(
                 modifier = Modifier
                     .size(52.dp)
                     .clip(RoundedCornerShape(cornerRadius))
                     .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                     .clickable { onIconChange(iconName) },
                 contentAlignment = Alignment.Center
             ) {
                 Icon(
                     imageVector = getIconByName(iconName) ?: Icons.Rounded.MusicNote,
                     contentDescription = null,
                     tint = if(isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                 )
             }
         }
     }
}
