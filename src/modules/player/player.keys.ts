import type { QueryClient } from "@tanstack/react-query"

import type { Track } from "@/modules/player/player.types"

const RECENTLY_PLAYED_SCREEN_KEY = ["recently-played-screen"] as const
const HOME_TOP_TRACKS_KEY = ["home", "top-tracks"] as const
const TOP_TRACKS_SCREEN_KEY = ["top-tracks-screen"] as const
const TRACKS_KEY = ["tracks"] as const

const RECENTLY_PLAYED_SCREEN_LIMIT = 50
const HOME_RECENTLY_PLAYED_LIMIT = 8

function prependUniqueTrack(
  current: Track[] | undefined,
  track: Track,
  limit: number
): Track[] {
  const existing = current ?? []
  return [track, ...existing.filter((item) => item.id !== track.id)].slice(
    0,
    limit
  )
}

export function optimisticallyUpdateRecentlyPlayedQueries(
  queryClient: QueryClient,
  track: Track
) {
  queryClient.setQueryData<Track[]>(RECENTLY_PLAYED_SCREEN_KEY, (previous) =>
    prependUniqueTrack(previous, track, RECENTLY_PLAYED_SCREEN_LIMIT)
  )
  queryClient.setQueryData<Track[]>(
    ["home", "recently-played", HOME_RECENTLY_PLAYED_LIMIT],
    (previous) =>
      prependUniqueTrack(previous, track, HOME_RECENTLY_PLAYED_LIMIT)
  )
}

export async function invalidatePlayerQueries(queryClient: QueryClient) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: RECENTLY_PLAYED_SCREEN_KEY }),
    queryClient.invalidateQueries({
      queryKey: ["home", "recently-played"],
    }),
    queryClient.invalidateQueries({ queryKey: HOME_TOP_TRACKS_KEY }),
    queryClient.invalidateQueries({ queryKey: TOP_TRACKS_SCREEN_KEY }),
    queryClient.invalidateQueries({ queryKey: TRACKS_KEY }),
  ])
}
