import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { and, asc, desc, eq, like, sql } from "drizzle-orm"

import { db } from "@/db/client"
import { playHistory, tracks } from "@/db/schema"

const TRACKS_KEY = "tracks"

export interface TrackFilter {
  artistId?: string
  albumId?: string
  genreId?: string
  isFavorite?: boolean
  searchQuery?: string
  sortBy?: "title" | "artist" | "album" | "dateAdded" | "playCount" | "rating"
  sortOrder?: "asc" | "desc"
}

export function useTracks(filters?: TrackFilter) {
  return useQuery({
    queryKey: [TRACKS_KEY, filters],
    placeholderData: (previousData) => previousData,
    queryFn: async () => {
      const sortField = filters?.sortBy || "title"
      const sortOrder = filters?.sortOrder || "asc"
      const multiplier = sortOrder === "asc" ? 1 : -1
      const orderByDirection = sortOrder === "asc" ? asc : desc

      const dbOrderBy =
        sortField === "title"
          ? [orderByDirection(sql`lower(coalesce(${tracks.title}, ''))`)]
          : sortField === "dateAdded"
            ? [orderByDirection(tracks.dateAdded)]
            : sortField === "playCount"
              ? [orderByDirection(tracks.playCount)]
              : sortField === "rating"
                ? [orderByDirection(tracks.rating)]
                : []

      const results = await db.query.tracks.findMany({
        where: and(
          eq(tracks.isDeleted, 0),
          filters?.artistId ? eq(tracks.artistId, filters.artistId) : undefined,
          filters?.albumId ? eq(tracks.albumId, filters.albumId) : undefined,
          filters?.isFavorite ? eq(tracks.isFavorite, 1) : undefined,
          filters?.searchQuery
            ? like(tracks.title, `%${filters.searchQuery}%`)
            : undefined
        ),
        with: {
          artist: true,
          album: true,
          genres: {
            with: {
              genre: true,
            },
          },
        },
        orderBy: dbOrderBy.length > 0 ? dbOrderBy : undefined,
      })

      if (dbOrderBy.length > 0) {
        return results
      }

      return results.sort((a, b) => {
        let aVal: string | number | null = null
        let bVal: string | number | null = null

        switch (sortField) {
          case "title":
            aVal = a.title.toLowerCase()
            bVal = b.title.toLowerCase()
            break
          case "artist":
            aVal = a.artist?.name?.toLowerCase() || ""
            bVal = b.artist?.name?.toLowerCase() || ""
            break
          case "album":
            aVal = a.album?.title?.toLowerCase() || ""
            bVal = b.album?.title?.toLowerCase() || ""
            break
          case "dateAdded":
            aVal = a.dateAdded || 0
            bVal = b.dateAdded || 0
            break
          case "playCount":
            aVal = a.playCount || 0
            bVal = b.playCount || 0
            break
          case "rating":
            aVal = a.rating || 0
            bVal = b.rating || 0
            break
        }

        if (aVal === null || bVal === null) return 0
        if (aVal < bVal) return -1 * multiplier
        if (aVal > bVal) return 1 * multiplier
        return 0
      })
    },
  })
}

export function useTrack(id: string) {
  const normalizedId = id.trim()

  return useQuery({
    queryKey: [TRACKS_KEY, normalizedId],
    enabled: normalizedId.length > 0,
    queryFn: async () => {
      return db.query.tracks.findFirst({
        where: eq(tracks.id, normalizedId),
        with: {
          artist: true,
          album: {
            with: {
              artist: true,
            },
          },
          featuredArtists: {
            with: {
              artist: true,
            },
          },
          genres: {
            with: {
              genre: true,
            },
          },
        },
      })
    },
  })
}

export function useToggleFavoriteTrack() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async ({
      trackId,
      isFavorite,
    }: {
      trackId: string
      isFavorite: boolean
    }) => {
      await db
        .update(tracks)
        .set({ isFavorite: isFavorite ? 1 : 0 })
        .where(eq(tracks.id, trackId))

      return { trackId, isFavorite }
    },
    onMutate: async ({ trackId, isFavorite }) => {
      await queryClient.cancelQueries({ queryKey: [TRACKS_KEY, trackId] })
      const previousTrack = queryClient.getQueryData([TRACKS_KEY, trackId])

      queryClient.setQueryData([TRACKS_KEY, trackId], (old: any) => ({
        ...old,
        isFavorite,
      }))

      return { previousTrack }
    },
    onError: (_error, variables, context) => {
      queryClient.setQueryData(
        [TRACKS_KEY, variables.trackId],
        context?.previousTrack
      )
    },
    onSettled: (data, error, variables) => {
      queryClient.invalidateQueries({
        queryKey: [TRACKS_KEY, variables.trackId],
      })
      queryClient.invalidateQueries({ queryKey: [TRACKS_KEY] })
    },
  })
}

export function useIncrementTrackPlayCount() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: async (trackId: string) => {
      const now = Date.now()

      await db
        .update(tracks)
        .set({
          playCount: sql`${tracks.playCount} + 1`,
          lastPlayedAt: now,
        })
        .where(eq(tracks.id, trackId))

      await db.insert(playHistory).values({
        id: `${trackId}-${now}`,
        trackId,
        playedAt: now,
      })

      return trackId
    },
    onSuccess: (trackId) => {
      queryClient.invalidateQueries({ queryKey: [TRACKS_KEY, trackId] })
    },
  })
}
