@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.startune.music.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.startune.music.data.stats.StatsTimeRange
import com.startune.music.presentation.screens.TabAnimation
import com.startune.music.presentation.stats.displayNameRes

/**
 * Range tabs for the Recently Played and Top Songs screens. This is the same
 * tab row as Listening Stats (RangeTabsHeader in StatsScreen): a scrollable
 * primary tab row of TabAnimation pills, no icons, no indicator, no divider.
 * It used to be a LazyRow of outlined chips with an icon on the
 * selected chip; that variant is gone.
 */
@Composable
fun RecentlyPlayedRangeSelector(
    selected: StatsTimeRange,
    onRangeSelected: (StatsTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val ranges = StatsTimeRange.entries
    val selectedIndex = remember(ranges, selected) { ranges.indexOf(selected).coerceAtLeast(0) }
    PrimaryScrollableTabRow(
        modifier = modifier.fillMaxWidth(),
        selectedTabIndex = selectedIndex,
        edgePadding = 12.dp,
        divider = {},
        containerColor = Color.Transparent,
        indicator = {}
    ) {
        ranges.forEachIndexed { index, range ->
            TabAnimation(
                index = index,
                selectedIndex = selectedIndex,
                onClick = { onRangeSelected(range) },
                selectedColor = MaterialTheme.colorScheme.primary,
                onSelectedColor = MaterialTheme.colorScheme.onPrimary,
                unselectedColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                onUnselectedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = stringResource(range.displayNameRes())
            ) {
                Text(
                    text = stringResource(range.displayNameRes()),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
