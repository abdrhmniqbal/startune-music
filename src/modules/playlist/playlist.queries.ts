import { useQuery } from "@tanstack/react-query"

import { queryClient } from "@/lib/tanstack-query"

import { playlistKeys } from "./playlist.keys"
import {
  getPlaylistById,
  listPlaylists,
  listPlaylistsForTrack,
} from "./playlist.repository"

export function usePlaylists() {
  return usePlaylistsWithOptions(true)
}

export function usePlaylistsWithOptions(enabled: boolean) {
  return useQuery(
    {
      queryKey: playlistKeys.all,
      enabled,
      placeholderData: (previousData) => previousData,
      queryFn: listPlaylists,
    },
    queryClient
  )
}

export function usePlaylistsForTrack(trackId: string | null, enabled: boolean) {
  return useQuery(
    {
      queryKey: playlistKeys.membership(trackId ?? ""),
      enabled,
      placeholderData: (previousData) => previousData,
      queryFn: async () => await listPlaylistsForTrack(trackId),
    },
    queryClient
  )
}

export function usePlaylist(id: string, enabled: boolean = true) {
  return useQuery(
    {
      queryKey: playlistKeys.detail(id),
      enabled: enabled && id.length > 0,
      queryFn: async () => await getPlaylistById(id),
    },
    queryClient
  )
}
