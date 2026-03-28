import { and, desc, eq, gt } from "drizzle-orm"

import { db } from "@/db/client"
import { albums, artists, playlists, tracks } from "@/db/schema"

import type { FavoriteEntry, FavoriteType } from "./favorites.types"

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

    if (entry.image) {
      nextValues.artwork = entry.image
    }

    await db
      .update(table)
      .set(nextValues as never)
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
      } as never)
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
      const favoriteTracks = await db.query.tracks.findMany({
        where: and(eq(tracks.isFavorite, 1), eq(tracks.isDeleted, 0)),
        orderBy: [desc(tracks.favoritedAt)],
      })
      favorites.push(
        ...favoriteTracks.map((track) => ({
          id: track.id,
          type: "track" as const,
          name: track.title,
          subtitle: undefined,
          image: track.artwork || undefined,
          dateAdded: track.favoritedAt || Date.now(),
        }))
      )
    }

    if (!type || type === "artist") {
      const favoriteArtists = await db.query.artists.findMany({
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
        ...favoriteArtists.map((artist) => ({
          id: artist.id,
          type: "artist" as const,
          name: artist.name,
          subtitle: `${artist.trackCount} tracks`,
          image:
            artist.artwork ||
            artist.tracks[0]?.artwork ||
            artist.tracks[0]?.album?.artwork ||
            undefined,
          dateAdded: artist.favoritedAt || Date.now(),
        }))
      )
    }

    if (!type || type === "album") {
      const favoriteAlbums = await db.query.albums.findMany({
        where: and(eq(albums.isFavorite, 1), gt(albums.trackCount, 0)),
        orderBy: [desc(albums.favoritedAt)],
      })
      favorites.push(
        ...favoriteAlbums.map((album) => ({
          id: album.id,
          type: "album" as const,
          name: album.title,
          subtitle: album.year?.toString() || undefined,
          image: album.artwork || undefined,
          dateAdded: album.favoritedAt || Date.now(),
        }))
      )
    }

    if (!type || type === "playlist") {
      const favoritePlaylists = await db.query.playlists.findMany({
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
        ...favoritePlaylists.map((playlist) => {
          const images: string[] = []

          if (playlist.artwork) {
            images.push(playlist.artwork)
          }

          playlist.tracks.forEach((playlistTrack) => {
            const artwork =
              playlistTrack.track?.artwork ||
              playlistTrack.track?.album?.artwork
            if (artwork && !images.includes(artwork) && images.length < 4) {
              images.push(artwork)
            }
          })

          return {
            id: playlist.id,
            type: "playlist" as const,
            name: playlist.name,
            subtitle: `${playlist.trackCount} tracks`,
            image: playlist.artwork || undefined,
            images,
            dateAdded: playlist.favoritedAt || Date.now(),
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

export async function setTrackFavoriteFlag(
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
      return
    }

    await db
      .update(tracks)
      .set({
        isFavorite: 0,
        favoritedAt: null,
      })
      .where(eq(tracks.id, trackId))
  } catch {}
}
