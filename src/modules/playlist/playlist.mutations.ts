import { useMutation } from "@tanstack/react-query"

import { queryClient } from "@/lib/tanstack-query"
import { invalidateFavoriteQueries } from "@/modules/favorites/favorites.keys"

import { invalidatePlaylistQueries } from "./playlist.keys"
import {
  addTrackToPlaylist,
  createPlaylist,
  deletePlaylist,
  removeTrackFromPlaylist,
  reorderPlaylistTracks,
  updatePlaylistMetadata,
} from "./playlist.repository"

export function useCreatePlaylist() {
  return useMutation(
    {
      mutationFn: async ({
        name,
        description,
        trackIds,
      }: {
        name: string
        description?: string
        trackIds: string[]
      }) => {
        await createPlaylist(name, description, trackIds)
      },
      onSuccess: async () => {
        await invalidatePlaylistQueries(queryClient)
      },
    },
    queryClient
  )
}

export function useUpdatePlaylist() {
  return useMutation(
    {
      mutationFn: async ({
        id,
        name,
        description,
      }: {
        id: string
        name?: string
        description?: string
      }) => {
        await updatePlaylistMetadata({ id, name, description })
      },
      onSuccess: async (_result, variables) => {
        await invalidatePlaylistQueries(queryClient, {
          playlistId: variables.id,
        })
      },
    },
    queryClient
  )
}

export function useDeletePlaylist() {
  return useMutation(
    {
      mutationFn: deletePlaylist,
      onSuccess: async (_result, deletedPlaylistId) => {
        await Promise.all([
          invalidatePlaylistQueries(queryClient, {
            playlistId: deletedPlaylistId,
          }),
          invalidateFavoriteQueries(queryClient),
        ])
      },
    },
    queryClient
  )
}

export function useAddTrackToPlaylist() {
  return useMutation(
    {
      mutationFn: addTrackToPlaylist,
      onSuccess: async (_result, variables) => {
        await invalidatePlaylistQueries(queryClient, {
          playlistId: variables.playlistId,
          trackId: variables.trackId,
        })
      },
    },
    queryClient
  )
}

export function useRemoveTrackFromPlaylist() {
  return useMutation(
    {
      mutationFn: removeTrackFromPlaylist,
      onSuccess: async (_result, variables) => {
        await invalidatePlaylistQueries(queryClient, {
          playlistId: variables.playlistId,
          trackId: variables.trackId,
        })
      },
    },
    queryClient
  )
}

export function useReorderPlaylistTracks() {
  return useMutation(
    {
      mutationFn: reorderPlaylistTracks,
      onSuccess: async (_result, variables) => {
        await invalidatePlaylistQueries(queryClient, {
          playlistId: variables.playlistId,
        })
      },
    },
    queryClient
  )
}
