package com.startune.music.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.startune.music.ui.theme.RoundedSans

/**
 * One provider row: tinted icon tile, title + subtitle, circular chevron.
 * Shared by the Home streaming provider sheet and the Accounts page.
 */
@Composable
fun ProviderRow(
    iconPainter: Painter,
    iconTint: Color,
    title: String,
    subtitle: String,
    shape: RoundedCornerShape,
    isConnected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val containerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceContainerLowest
        isConnected -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val titleColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
    }
    val subtitleColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
        isConnected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val arrowContainerColor = when {
        !enabled -> MaterialTheme.colorScheme.surfaceContainerHighest
        isConnected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceBright
    }
    val arrowTint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        isConnected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
    }
    val iconTileShape = RoundedCornerShape(14.dp)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.62f)
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick),
        shape = shape,
        color = containerColor
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = RoundedSans,
                    fontWeight = FontWeight.Medium,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = RoundedSans,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(iconTileShape)
                        .background(iconTint.copy(alpha = if (enabled) 0.14f else 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = iconTint
                    )
                }
            },
            trailingContent = {
                Surface(
                    shape = CircleShape,
                    color = arrowContainerColor
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .size(26.dp),
                        tint = arrowTint
                    )
                }
            }
        )
    }
}
