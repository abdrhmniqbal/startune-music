package com.startune.music.presentation.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.startune.music.ui.theme.RoundedSans
import com.startune.music.utils.shapes.RoundedStarShape

/**
 * The only empty-state component in the app: a star-shaped primaryContainer
 * icon badge, title, subtitle. Pass [contained] for empty states that sit
 * inside a card section (Stats, Recently Played, Top Tracks, Recently Added,
 * folder filter); everywhere else the bare version is used. Callers only
 * choose the icon, title, and subtitle; the visuals are the same on every
 * page.
 */
@Composable
fun ExpressiveEmptyState(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes subtitleRes: Int,
    modifier: Modifier = Modifier,
    contained: Boolean = false
) {
    ExpressiveEmptyStateLayout(
        icon = {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = stringResource(titleRes),
        subtitle = stringResource(subtitleRes),
        modifier = modifier,
        contained = contained
    )
}

/**
 * Same component as [ExpressiveEmptyState], for callers with a vector icon and
 * already-resolved strings. Pass null for [subtitle] to show the title only.
 */
@Composable
fun ExpressiveEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    contained: Boolean = false
) {
    ExpressiveEmptyStateLayout(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        contained = contained
    )
}

@Composable
private fun ExpressiveEmptyStateLayout(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    contained: Boolean = false
) {
    if (contained) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            ExpressiveEmptyStateContent(
                icon = icon,
                title = title,
                subtitle = subtitle,
                modifier = Modifier.padding(32.dp)
            )
        }
    } else {
        ExpressiveEmptyStateContent(
            icon = icon,
            title = title,
            subtitle = subtitle,
            modifier = modifier.padding(horizontal = 28.dp)
        )
    }
}

@Composable
private fun ExpressiveEmptyStateContent(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedStarShape(sides = 8, curve = 0.1, rotation = 0f))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = RoundedSans,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
