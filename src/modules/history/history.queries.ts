import type { Track } from "@/modules/player/player.types"
import { useQuery } from "@tanstack/react-query"

import { queryClient } from "@/lib/tanstack-query"

import { historyKeys } from "./history.keys"
import { getTopTracksByPeriod, getTrackHistory } from "./history.repository"
import type { HistoryTopTracksPeriod } from "./history.types"
import { dedupeTracksById } from "./history.utils"

export function useTrackHistory() {
  return useQuery<Track[]>(
    {
      queryKey: historyKeys.tracks(),
      queryFn: getTrackHistory,
      placeholderData: (previousData) => previousData,
    },
    queryClient
  )
}

export function useRecentlyPlayedTracks(limit = 8) {
  return useQuery<Track[]>(
    {
      queryKey: historyKeys.recentlyPlayed(limit),
      queryFn: async () => {
        const history = await getTrackHistory()
        return dedupeTracksById(history).slice(0, limit)
      },
      placeholderData: (previousData) => previousData,
    },
    queryClient
  )
}

export function useTopTracksByPeriod(
  period: HistoryTopTracksPeriod = "all",
  limit = 25
) {
  return useQuery<Track[]>(
    {
      queryKey: historyKeys.topTracks(period, limit),
      queryFn: async () => await getTopTracksByPeriod(period, limit),
      placeholderData: (previousData) => previousData,
    },
    queryClient
  )
}
