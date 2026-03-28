import { useQuery } from "@tanstack/react-query"

import { queryClient } from "@/lib/tanstack-query"

import { trackKeys } from "./tracks.keys"
import { getTrackById, listTracks } from "./tracks.repository"
import type { TrackFilter } from "./tracks.types"

export type { TrackFilter } from "./tracks.types"

export function useTracks(filters?: TrackFilter) {
  return useQuery(
    {
      queryKey: trackKeys.all(filters),
      placeholderData: (previousData) => previousData,
      queryFn: async () => await listTracks(filters),
    },
    queryClient
  )
}

export function useTrack(id: string) {
  const normalizedId = id.trim()

  return useQuery(
    {
      queryKey: trackKeys.detail(normalizedId),
      enabled: normalizedId.length > 0,
      queryFn: async () => await getTrackById(normalizedId),
    },
    queryClient
  )
}
