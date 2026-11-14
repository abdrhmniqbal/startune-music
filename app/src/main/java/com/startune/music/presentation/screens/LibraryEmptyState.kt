package com.startune.music.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.startune.music.R
import com.startune.music.data.model.LibraryTabId
import com.startune.music.data.model.StorageFilter
import com.startune.music.presentation.components.ExpressiveEmptyState
import com.startune.music.presentation.components.MiniPlayerHeight

private data class LibraryEmptySpec(
    val iconRes: Int,
    val titleRes: Int,
    val subtitleRes: Int
)

private fun libraryEmptySpec(
    tabId: LibraryTabId,
    storageFilter: StorageFilter
): LibraryEmptySpec {
    return when (tabId) {
        LibraryTabId.SONGS -> when (storageFilter) {
            StorageFilter.ALL -> LibraryEmptySpec(
                iconRes = R.drawable.rounded_music_off_24,
                titleRes = R.string.lib_empty_songs_all_title,
                subtitleRes = R.string.lib_empty_songs_all_subtitle
            )
            StorageFilter.OFFLINE -> LibraryEmptySpec(
                iconRes = R.drawable.rounded_music_off_24,
                titleRes = R.string.lib_empty_songs_offline_title,
                subtitleRes = R.string.lib_empty_songs_offline_subtitle
            )
            StorageFilter.ONLINE -> LibraryEmptySpec(
                iconRes = R.drawable.rounded_music_off_24,
                titleRes = R.string.lib_empty_songs_online_title,
                subtitleRes = R.string.lib_empty_songs_online_subtitle
            )
        }

        LibraryTabId.ALBUMS -> when (storageFilter) {
            StorageFilter.ALL -> LibraryEmptySpec(
                iconRes = R.drawable.rounded_album_24,
                titleRes = R.string.lib_empty_albums_all_title,
                subtitleRes = R.string.lib_empty_albums_all_subtitle
            )
            StorageFilter.OFFLINE -> LibraryEmptySpec(
                iconRes = R.drawable.rounded_album_24,
                titleRes = R.string.lib_empty_albums_offline_title,
                subtitleRes = R.string.lib_empty_albums_offline_subtitle
            )
            StorageFilter.ONLINE -> LibraryEmptySpec(
                iconRes = R.drawable.rounded_album_24,
                titleRes = R.string.lib_empty_albums_online_title,
                subtitleRes = R.string.lib_empty_albums_online_subtitle
            )
        }

        LibraryTabId.ARTISTS -> when (storageFilter) {
            StorageFilter.ALL -> LibraryEmptySpec(
                iconRes = R.drawable.rounded_artist_24,
                titleRes = R.string.lib_empty_artists_all_title,
                subtitleRes = R.string.lib_empty_artists_all_subtitle
            )
            StorageFilter.OFFLINE -> LibraryEmptySpec(
                iconRes = R.drawable.rounded_artist_24,
                titleRes = R.string.lib_empty_artists_offline_title,
                subtitleRes = R.string.lib_empty_artists_offline_subtitle
            )
            StorageFilter.ONLINE -> LibraryEmptySpec(
                iconRes = R.drawable.rounded_artist_24,
                titleRes = R.string.lib_empty_artists_online_title,
                subtitleRes = R.string.lib_empty_artists_online_subtitle
            )
        }

        LibraryTabId.LIKED -> when (storageFilter) {
            StorageFilter.ALL -> LibraryEmptySpec(
                iconRes = R.drawable.round_favorite_24,
                titleRes = R.string.lib_empty_liked_all_title,
                subtitleRes = R.string.lib_empty_liked_all_subtitle
            )
            StorageFilter.OFFLINE -> LibraryEmptySpec(
                iconRes = R.drawable.round_favorite_24,
                titleRes = R.string.lib_empty_liked_offline_title,
                subtitleRes = R.string.lib_empty_liked_offline_subtitle
            )
            StorageFilter.ONLINE -> LibraryEmptySpec(
                iconRes = R.drawable.round_favorite_24,
                titleRes = R.string.lib_empty_liked_online_title,
                subtitleRes = R.string.lib_empty_liked_online_subtitle
            )
        }

        LibraryTabId.FOLDERS -> LibraryEmptySpec(
            iconRes = R.drawable.ic_folder,
            titleRes = R.string.lib_empty_folders_title,
            subtitleRes = R.string.lib_empty_folders_subtitle
        )

        LibraryTabId.PLAYLISTS -> LibraryEmptySpec(
            iconRes = R.drawable.rounded_playlist_play_24,
            titleRes = R.string.lib_empty_playlists_title,
            subtitleRes = R.string.lib_empty_playlists_subtitle
        )

        LibraryTabId.MIXES -> LibraryEmptySpec(
            iconRes = R.drawable.rounded_instant_mix_24,
            titleRes = R.string.mix_empty_title,
            subtitleRes = R.string.mix_empty_hint
        )

        LibraryTabId.GENRES -> LibraryEmptySpec(
            iconRes = R.drawable.rounded_music_off_24,
            titleRes = R.string.lib_empty_genres_title,
            subtitleRes = R.string.lib_empty_genres_subtitle
        )
    }
}

/**
 * Library empty state: picks the icon/title/subtitle for the tab and clears
 * the bottom bar. The visuals are the shared [ExpressiveEmptyState]; this
 * only resolves the spec and positions it.
 */
@Composable
internal fun LibraryEmptyState(
    tabId: LibraryTabId,
    storageFilter: StorageFilter,
    bottomBarHeight: Dp,
    modifier: Modifier = Modifier
) {
    val spec = remember(tabId, storageFilter) { libraryEmptySpec(tabId, storageFilter) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = bottomBarHeight + MiniPlayerHeight + 24.dp),
        contentAlignment = Alignment.Center
    ) {
        ExpressiveEmptyState(
            iconRes = spec.iconRes,
            titleRes = spec.titleRes,
            subtitleRes = spec.subtitleRes
        )
    }
}
