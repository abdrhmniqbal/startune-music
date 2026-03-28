import { useQuery } from "@tanstack/react-query"

import { queryClient } from "@/lib/tanstack-query"

import { FAVORITES_KEY } from "./favorites.keys"
import {
  getFavorites,
  isFavorite,
} from "./favorites.repository"
import type { FavoriteType } from "./favorites.types"

interface QueryOptions {
  enabled?: boolean
}

export function useFavorites(type?: FavoriteType, options: QueryOptions = {}) {
  return useQuery(
    {
      queryKey: [FAVORITES_KEY, type],
      enabled: options.enabled ?? true,
      placeholderData: (previousData) => previousData,
      queryFn: () => getFavorites(type),
    },
    queryClient
  )
}

export function useIsFavorite(type: FavoriteType, itemId: string) {
  const normalizedItemId = itemId.trim()

  return useQuery(
    {
      queryKey: [FAVORITES_KEY, type, normalizedItemId],
      enabled: normalizedItemId.length > 0,
      placeholderData: (previousData) => previousData,
      queryFn: () => isFavorite(normalizedItemId, type),
    },
    queryClient
  )
}
