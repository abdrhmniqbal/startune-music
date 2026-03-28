import type { QueryClient } from "@tanstack/react-query"

import { PLAYLISTS_KEY } from "@/modules/playlist/playlist.keys"

export const FAVORITES_KEY = "favorites"

export async function invalidateFavoriteQueries(queryClient: QueryClient) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: [FAVORITES_KEY] }),
    queryClient.invalidateQueries({ queryKey: ["library", "favorites"] }),
    queryClient.invalidateQueries({ queryKey: ["tracks"] }),
    queryClient.invalidateQueries({ queryKey: ["library", "tracks"] }),
    queryClient.invalidateQueries({ queryKey: ["artists"] }),
    queryClient.invalidateQueries({ queryKey: ["albums"] }),
    queryClient.invalidateQueries({ queryKey: [PLAYLISTS_KEY] }),
  ])
}
