import { useMutation } from "@tanstack/react-query"

import { queryClient } from "@/lib/tanstack-query"

import { trackKeys } from "./tracks.keys"
import {
  incrementTrackPlayCount,
  setTrackFavoriteStatus,
} from "./tracks.repository"

export function useToggleFavoriteTrack() {
  return useMutation(
    {
      mutationFn: setTrackFavoriteStatus,
      onMutate: async ({ trackId, isFavorite }) => {
        await queryClient.cancelQueries({
          queryKey: trackKeys.detail(trackId),
        })
        const previousTrack = queryClient.getQueryData(
          trackKeys.detail(trackId)
        )

        queryClient.setQueryData(trackKeys.detail(trackId), (old: any) => ({
          ...old,
          isFavorite,
        }))

        return { previousTrack }
      },
      onError: (_error, variables, context) => {
        queryClient.setQueryData(
          trackKeys.detail(variables.trackId),
          context?.previousTrack
        )
      },
      onSettled: async (_data, _error, variables) => {
        await Promise.all([
          queryClient.invalidateQueries({
            queryKey: trackKeys.detail(variables.trackId),
          }),
          queryClient.invalidateQueries({ queryKey: [trackKeys.all()[0]] }),
        ])
      },
    },
    queryClient
  )
}

export function useIncrementTrackPlayCount() {
  return useMutation(
    {
      mutationFn: incrementTrackPlayCount,
      onSuccess: async (trackId) => {
        await queryClient.invalidateQueries({
          queryKey: trackKeys.detail(trackId),
        })
      },
    },
    queryClient
  )
}
