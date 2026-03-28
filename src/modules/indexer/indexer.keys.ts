import type { QueryClient } from "@tanstack/react-query"

import { FAVORITES_KEY } from "@/modules/favorites/favorites.keys"
import { GENRES_KEY } from "@/modules/genres/genres.keys"
import {
  ALBUMS_KEY,
  ARTISTS_KEY,
  SEARCH_KEY,
} from "@/modules/library/library.keys"
import {
  HISTORY_RECENTLY_PLAYED_KEY,
  HISTORY_TOP_TRACKS_KEY,
} from "@/modules/history/history.keys"
import { PLAYLISTS_KEY } from "@/modules/playlist/playlist.keys"
import {
  GENRE_ALBUMS_KEY,
  GENRE_DETAILS_KEY,
  GENRE_TOP_TRACKS_KEY,
  SEARCH_GENRES_KEY,
} from "@/modules/search/search.keys"
import { TRACKS_KEY } from "@/modules/tracks/tracks.keys"

export async function invalidateIndexerQueries(queryClient: QueryClient) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: [TRACKS_KEY] }),
    queryClient.invalidateQueries({ queryKey: ["library", TRACKS_KEY] }),
    queryClient.invalidateQueries({ queryKey: [ALBUMS_KEY] }),
    queryClient.invalidateQueries({ queryKey: [ARTISTS_KEY] }),
    queryClient.invalidateQueries({ queryKey: [GENRES_KEY] }),
    queryClient.invalidateQueries({ queryKey: [PLAYLISTS_KEY] }),
    queryClient.invalidateQueries({ queryKey: [FAVORITES_KEY] }),
    queryClient.invalidateQueries({ queryKey: ["library", FAVORITES_KEY] }),
    queryClient.invalidateQueries({ queryKey: [SEARCH_KEY] }),
    queryClient.invalidateQueries({ queryKey: [SEARCH_GENRES_KEY] }),
    queryClient.invalidateQueries({ queryKey: [GENRE_DETAILS_KEY] }),
    queryClient.invalidateQueries({ queryKey: [GENRE_TOP_TRACKS_KEY] }),
    queryClient.invalidateQueries({ queryKey: [GENRE_ALBUMS_KEY] }),
    queryClient.invalidateQueries({ queryKey: [HISTORY_RECENTLY_PLAYED_KEY] }),
    queryClient.invalidateQueries({ queryKey: [HISTORY_TOP_TRACKS_KEY] }),
  ])
}
