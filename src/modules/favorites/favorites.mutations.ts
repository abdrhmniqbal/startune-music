import { useMutation } from "@tanstack/react-query"

import { queryClient } from "@/lib/tanstack-query"

import { FAVORITES_KEY, invalidateFavoriteQueries } from "./favorites.keys"
import {
  addFavorite,
  removeFavorite,
} from "./favorites.repository"
import type { FavoriteType } from "./favorites.types"

export function useAddFavorite() {
  return useMutation(
    {
      mutationFn: async ({
        type,
        itemId,
        name,
        subtitle,
        image,
      }: {
        type: FavoriteType
        itemId: string
        name: string
        subtitle?: string
        image?: string
      }) => {
        const now = Date.now()
        await addFavorite({
          id: itemId,
          type,
          name,
          subtitle,
          image,
          dateAdded: now,
        })

        return { type, itemId, favoritedAt: now }
      },
      onSuccess: async () => {
        await invalidateFavoriteQueries(queryClient)
      },
    },
    queryClient
  )
}

export function useRemoveFavorite() {
  return useMutation(
    {
      mutationFn: async ({
        type,
        itemId,
      }: {
        type: FavoriteType
        itemId: string
      }) => {
        await removeFavorite(itemId, type)

        return { type, itemId }
      },
      onSuccess: async () => {
        await invalidateFavoriteQueries(queryClient)
      },
    },
    queryClient
  )
}

export function useToggleFavorite() {
  return useMutation(
    {
      mutationFn: async ({
        type,
        itemId,
        isCurrentlyFavorite,
        name,
        subtitle,
        image,
      }: {
        type: FavoriteType
        itemId: string
        isCurrentlyFavorite: boolean
        name: string
        subtitle?: string
        image?: string
      }) => {
        if (isCurrentlyFavorite) {
          await removeFavorite(itemId, type)
        } else {
          await addFavorite({
            id: itemId,
            type,
            name,
            subtitle,
            image,
            dateAdded: Date.now(),
          })
        }

        return !isCurrentlyFavorite
      },
      onMutate: async (variables) => {
        await queryClient.cancelQueries({
          queryKey: [FAVORITES_KEY, variables.type, variables.itemId],
        })
        const previousValue = queryClient.getQueryData<boolean>([
          FAVORITES_KEY,
          variables.type,
          variables.itemId,
        ])

        queryClient.setQueryData(
          [FAVORITES_KEY, variables.type, variables.itemId],
          !variables.isCurrentlyFavorite
        )

        return { previousValue }
      },
      onError: (_error, variables, context) => {
        queryClient.setQueryData(
          [FAVORITES_KEY, variables.type, variables.itemId],
          context?.previousValue
        )
      },
      onSettled: async () => {
        await invalidateFavoriteQueries(queryClient)
      },
    },
    queryClient
  )
}
