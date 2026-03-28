import type { QueryClient } from "@tanstack/react-query"

export const PLAYLISTS_KEY = "playlists"

export const playlistKeys = {
  all: [PLAYLISTS_KEY] as const,
  detail: (playlistId: string) => [PLAYLISTS_KEY, playlistId] as const,
  membership: (trackId: string) =>
    [PLAYLISTS_KEY, "track-membership", trackId] as const,
}

export async function invalidatePlaylistQueries(
  queryClient: QueryClient,
  options?: {
    playlistId?: string | null
    trackId?: string | null
  }
) {
  const invalidations = [
    queryClient.invalidateQueries({ queryKey: playlistKeys.all }),
  ]

  if (options?.playlistId) {
    invalidations.push(
      queryClient.invalidateQueries({
        queryKey: playlistKeys.detail(options.playlistId),
      })
    )
  }

  if (options?.trackId) {
    invalidations.push(
      queryClient.invalidateQueries({
        queryKey: playlistKeys.membership(options.trackId),
      })
    )
  }

  await Promise.all(invalidations)
}
