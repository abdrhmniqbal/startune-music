package com.startune.music.presentation.components

import androidx.annotation.OptIn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.startune.music.data.model.Genre
import com.startune.music.presentation.components.MiniPlayerHeight
import com.startune.music.presentation.components.SmartImage
import com.startune.music.presentation.components.getNavigationBarHeight
import com.startune.music.presentation.components.resolveNavBarOccupiedHeight
import com.startune.music.presentation.utils.GenreIconProvider
import com.startune.music.presentation.screens.search.components.GenreTypography
import com.startune.music.presentation.viewmodel.PlayerViewModel
import com.startune.music.ui.theme.LocalStartuneDarkTheme
import androidx.compose.ui.res.stringResource
import com.startune.music.R
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(UnstableApi::class)
@Composable
fun GenreCategoriesGrid(
    genres: ImmutableList<Genre>,
    onGenreClick: (Genre) -> Unit,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    selectedGenres: ImmutableList<Genre> = persistentListOf(),
    isSelectionMode: Boolean = false,
    onLongPress: (Genre) -> Unit = {},
    onSelectionToggle: (Genre) -> Unit = {}
) {
    if (genres.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.no_genres_available), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val systemNavBarHeight = getNavigationBarHeight()
    val customGenreIcons = playerViewModel.customGenreIcons.collectAsStateWithLifecycle(
        initialValue = emptyMap(),
        context = kotlin.coroutines.EmptyCoroutineContext
    ).value

    val isGridView by playerViewModel.isGenreGridView.collectAsStateWithLifecycle()
    val navBarCompactMode by playerViewModel.navBarCompactMode.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = if (isGridView) GridCells.Fixed(2) else GridCells.Fixed(1),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
            .clip(AbsoluteSmoothCornerShape(
                cornerRadiusTR = 24.dp,
                smoothnessAsPercentTR = 70,
                cornerRadiusTL = 24.dp,
                smoothnessAsPercentTL = 70,
                cornerRadiusBR = 0.dp,
                smoothnessAsPercentBR = 70,
                cornerRadiusBL = 0.dp,
                smoothnessAsPercentBL = 70
            )),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 28.dp + resolveNavBarOccupiedHeight(systemNavBarHeight, navBarCompactMode) + MiniPlayerHeight
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(genres, key = { it.id }) { genre ->
            val genreSelectionIndex = selectedGenres.indexOfFirst { it.id == genre.id }
            GenreCard(
                genre = genre,
                customIcons = customGenreIcons,
                onClick = { onGenreClick(genre) },
                isGridView = isGridView,
                isSelected = genreSelectionIndex >= 0,
                selectionIndex = genreSelectionIndex.takeIf { it >= 0 }?.plus(1),
                isSelectionMode = isSelectionMode,
                onLongPress = { onLongPress(genre) },
                onSelectionToggle = { onSelectionToggle(genre) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GenreCard(
    genre: Genre,
    customIcons: Map<String, Int>,
    onClick: () -> Unit,
    isGridView: Boolean,
    isSelected: Boolean = false,
    selectionIndex: Int? = null,
    isSelectionMode: Boolean = false,
    onLongPress: () -> Unit = {},
    onSelectionToggle: () -> Unit = {}
) {
    val isDark = LocalStartuneDarkTheme.current
    val themeColor = remember(genre, isDark) {
        com.startune.music.ui.theme.GenreThemeUtils.getGenreThemeColor(
            genre = genre,
            isDark = isDark,
            fallbackGenreId = genre.id
        )
    }
    val backgroundColor = themeColor.container
    val onBackgroundColor = themeColor.onContainer

    val shape = RoundedCornerShape(20.dp)

    val cardModifier = if (isGridView) {
        Modifier.aspectRatio(1.2f)
    } else {
        Modifier.fillMaxWidth().height(100.dp)
    }

    Card(
        modifier = cardModifier
            .clip(shape)
            .combinedClickable(
                onClick = { if (isSelectionMode) onSelectionToggle() else onClick() },
                onLongClick = onLongPress
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(backgroundColor)
        ) {
            val titleStartPadding = 14.dp
            val titleEndPadding = if (isGridView) 14.dp else 96.dp
            val titlePresentation = remember(
                genre.id,
                genre.name,
                isGridView,
            ) {
                GenreTypography.resolveTitlePresentationFast(
                    genreId = genre.id,
                    genreName = genre.name,
                    isGridView = isGridView,
                )
            }

            Box(
                modifier = Modifier
                    .size(90.dp) 
                    .align(Alignment.BottomEnd)
                    .offset(x = 16.dp, y = 16.dp) 
            ) {
                SmartImage(
                    model = GenreIconProvider.getGenreImageResource(genre.name, customIcons),
                    contentDescription = stringResource(R.string.cd_genre_illustration),
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.55f),
                    colorFilter = ColorFilter.tint(onBackgroundColor),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(start = titleStartPadding, top = 14.dp, end = titleEndPadding),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = titlePresentation.firstLine,
                    style = titlePresentation.style,
                    color = onBackgroundColor,
                    softWrap = false,
                    minLines = 1,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                titlePresentation.secondLine?.let { secondLine ->
                    Text(
                        text = secondLine,
                        style = titlePresentation.style,
                        color = onBackgroundColor,
                        softWrap = false,
                        minLines = 1,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(titlePresentation.secondLineWidthFraction)
                    )
                }
            }

            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(28.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected && selectionIndex != null) {
                        Text(
                            text = selectionIndex.toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}
