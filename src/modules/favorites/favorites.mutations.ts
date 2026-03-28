import { useMutation } from "@tanstack/react-query"

import { queryClient } from "@/lib/tanstack-query"
import { logError, logInfo } from "@/modules/logging/logger"

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
        logInfo("Adding favorite", { type, itemId })
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
      onSuccess: async (_result, variables) => {
        logInfo("Added favorite", {
          type: variables.type,
          itemId: variables.itemId,
        })
        await invalidateFavoriteQueries(queryClient)
      },
      onError: (error, variables) => {
        logError("Failed to add favorite", error, {
          type: variables.type,
          itemId: variables.itemId,
        })
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
        logInfo("Removing favorite", { type, itemId })
        await removeFavorite(itemId, type)

        return { type, itemId }
      },
      onSuccess: async (_result, variables) => {
        logInfo("Removed favorite", {
          type: variables.type,
          itemId: variables.itemId,
        })
        await invalidateFavoriteQueries(queryClient)
      },
      onError: (error, variables) => {
        logError("Failed to remove favorite", error, {
          type: variables.type,
          itemId: variables.itemId,
        })
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
        logInfo("Toggling favorite", {
          type,
          itemId,
          isCurrentlyFavorite,
        })
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
      onError: (error, variables, context) => {
        logError("Failed to toggle favorite", error, {
          type: variables.type,
          itemId: variables.itemId,
          isCurrentlyFavorite: variables.isCurrentlyFavorite,
        })
        queryClient.setQueryData(
          [FAVORITES_KEY, variables.type, variables.itemId],
          context?.previousValue
        )
      },
      onSuccess: (isFavorite, variables) => {
        logInfo("Toggled favorite", {
          type: variables.type,
          itemId: variables.itemId,
          isFavorite,
        })
      },
      onSettled: async (_result, _error, variables) => {
        logInfo("Refreshing favorite queries after toggle", {
          type: variables.type,
          itemId: variables.itemId,
        })
        await invalidateFavoriteQueries(queryClient)
      },
    },
    queryClient
  )
}
