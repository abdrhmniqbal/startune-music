import { and, desc, eq, gt } from "drizzle-orm"

import { db } from "@/db/client"
import { albums, artists, playlists, tracks } from "@/db/schema"

export type FavoriteType = "track" | "artist" | "album" | "playlist"

export interface FavoriteEntry {
  id: string
  type: FavoriteType
  name: string
  subtitle?: string
  image?: string
  images?: string[]
  dateAdded: number
}

function getTableForType(type: FavoriteType) {
  switch (type) {
    case "track":
      return tracks
    case "artist":
      return artists
    case "album":
      return albums
    case "playlist":
      return playlists
    default:
      throw new Error(`Unknown favorite type: ${type}`)
  }
}

export async function addFavorite(entry: FavoriteEntry): Promise<void> {
  try {
    const table = getTableForType(entry.type)
    const now = Date.now()
    const nextValues: Record<string, unknown> = {
      isFavorite: 1,
      favoritedAt: now,
    }

    // Persist incoming image so favorites list can render cover/artwork consistently.
    if (entry.image) {
      nextValues.artwork = entry.image
    }

    await db
      .update(table)
      .set(nextValues as any)
      .where(eq(table.id, entry.id))
  } catch {}
}

export async function removeFavorite(
  id: string,
  type: FavoriteType
): Promise<void> {
  try {
    const table = getTableForType(type)

    await db
      .update(table)
      .set({
        isFavorite: 0,
        favoritedAt: null,
      } as any)
      .where(eq(table.id, id))
  } catch {}
}

export async function isFavorite(
  id: string,
  type: FavoriteType
): Promise<boolean> {
  try {
    let result: unknown = null

    switch (type) {
      case "track":
        result = await db.query.tracks.findFirst({
          where: and(
            eq(tracks.id, id),
            eq(tracks.isFavorite, 1),
            eq(tracks.isDeleted, 0)
          ),
        })
        break
      case "artist":
        result = await db.query.artists.findFirst({
          where: and(eq(artists.id, id), eq(artists.isFavorite, 1)),
        })
        break
      case "album":
        result = await db.query.albums.findFirst({
          where: and(eq(albums.id, id), eq(albums.isFavorite, 1)),
        })
        break
      case "playlist":
        result = await db.query.playlists.findFirst({
          where: and(eq(playlists.id, id), eq(playlists.isFavorite, 1)),
        })
        break
    }

    return !!result
  } catch {
    return false
  }
}

export async function getFavorites(
  type?: FavoriteType
): Promise<FavoriteEntry[]> {
  try {
    const favorites: FavoriteEntry[] = []

    if (!type || type === "track") {
      const favTracks = await db.query.tracks.findMany({
        where: and(eq(tracks.isFavorite, 1), eq(tracks.isDeleted, 0)),
        orderBy: [desc(tracks.favoritedAt)],
      })
      favorites.push(
        ...favTracks.map((t) => ({
          id: t.id,
          type: "track" as FavoriteType,
          name: t.title,
          subtitle: undefined,
          image: t.artwork || undefined,
          dateAdded: t.favoritedAt || Date.now(),
        }))
      )
    }

    if (!type || type === "artist") {
      const favArtists = await db.query.artists.findMany({
        where: and(eq(artists.isFavorite, 1), gt(artists.trackCount, 0)),
        orderBy: [desc(artists.favoritedAt)],
        with: {
          tracks: {
            limit: 1,
            with: {
              album: true,
            },
          },
        },
      })
      favorites.push(
        ...favArtists.map((a) => ({
          id: a.id,
          type: "artist" as FavoriteType,
          name: a.name,
          subtitle: `${a.trackCount} tracks`,
          image:
            a.artwork ||
            a.tracks[0]?.artwork ||
            a.tracks[0]?.album?.artwork ||
            undefined,
          dateAdded: a.favoritedAt || Date.now(),
        }))
      )
    }

    if (!type || type === "album") {
      const favAlbums = await db.query.albums.findMany({
        where: and(eq(albums.isFavorite, 1), gt(albums.trackCount, 0)),
        orderBy: [desc(albums.favoritedAt)],
      })
      favorites.push(
        ...favAlbums.map((a) => ({
          id: a.id,
          type: "album" as FavoriteType,
          name: a.title,
          subtitle: a.year?.toString() || undefined,
          image: a.artwork || undefined,
          dateAdded: a.favoritedAt || Date.now(),
        }))
      )
    }

    if (!type || type === "playlist") {
      const favPlaylists = await db.query.playlists.findMany({
        where: eq(playlists.isFavorite, 1),
        orderBy: [desc(playlists.favoritedAt)],
        with: {
          tracks: {
            with: {
              track: {
                with: {
                  album: true,
                },
              },
            },
            limit: 4,
          },
        },
      })
      favorites.push(
        ...favPlaylists.map((p) => {
          const images: string[] = []

          if (p.artwork) {
            images.push(p.artwork)
          }

          p.tracks.forEach((pt) => {
            const artwork = pt.track?.artwork || pt.track?.album?.artwork
            if (artwork && !images.includes(artwork) && images.length < 4) {
              images.push(artwork)
            }
          })

          return {
            id: p.id,
            type: "playlist" as FavoriteType,
            name: p.name,
            subtitle: `${p.trackCount} tracks`,
            image: p.artwork || undefined,
            images,
            dateAdded: p.favoritedAt || Date.now(),
          }
        })
      )
    }

    favorites.sort((a, b) => b.dateAdded - a.dateAdded)
    return favorites
  } catch {
    return []
  }
}

export async function toggleFavoriteDB(
  trackId: string,
  isFavoriteValue: boolean
): Promise<void> {
  try {
    if (isFavoriteValue) {
      await db
        .update(tracks)
        .set({
          isFavorite: 1,
          favoritedAt: Date.now(),
        })
        .where(eq(tracks.id, trackId))
    } else {
      await db
        .update(tracks)
        .set({
          isFavorite: 0,
          favoritedAt: null,
        })
        .where(eq(tracks.id, trackId))
    }
  } catch {}
}
