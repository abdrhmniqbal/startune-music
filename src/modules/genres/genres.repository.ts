import type { Track } from "@/modules/player/player.types"

import { asc, desc, eq, sql } from "drizzle-orm"

import { db } from "@/db/client"
import { genres, tracks } from "@/db/schema"
import { transformDBTrackToTrack } from "@/utils/transformers"

import { GENRE_COLORS, GENRE_SHAPES } from "./genres.constants"
import type { GenreAlbumInfo, GenreVisual } from "./genres.types"

function hashString(value: string): number {
  let hash = 0
  for (let i = 0; i < value.length; i++) {
    hash = (hash * 31 + value.charCodeAt(i)) >>> 0
  }
  return hash
}

export async function getAllGenres(): Promise<string[]> {
  try {
    const result = await db.query.genres.findMany({
      orderBy: (genres, { asc }) => [
        asc(sql`lower(coalesce(${genres.name}, ''))`),
      ],
      columns: {
        name: true,
      },
    })

    return result.map((genre) => genre.name)
  } catch {
    return []
  }
}

export async function getAllGenreVisuals(): Promise<GenreVisual[]> {
  try {
    const result = await db.query.genres.findMany({
      orderBy: (genres, { asc }) => [
        asc(sql`lower(coalesce(${genres.name}, ''))`),
      ],
      columns: {
        name: true,
        color: true,
        shape: true,
      },
    })

    return result.map((genre) => ({
      name: genre.name,
      color: genre.color,
      shape: genre.shape as GenreVisual["shape"],
    }))
  } catch {
    const names = await getAllGenres()

    return names.map((name) => {
      const hash = hashString(name)

      return {
        name,
        color: GENRE_COLORS[hash % GENRE_COLORS.length],
        shape:
          GENRE_SHAPES[
            Math.floor(hash / GENRE_COLORS.length) % GENRE_SHAPES.length
          ],
      }
    })
  }
}

export async function getTopTracksByGenre(
  genre: string,
  limit = 25
): Promise<Track[]> {
  try {
    const trimmedGenre = genre.trim()

    const matchingGenres = await db.query.genres.findMany({
      where: (g, { sql }) => sql`${g.name} LIKE ${trimmedGenre}`,
      columns: { id: true },
    })

    if (matchingGenres.length === 0) {
      return []
    }

    const genreIds = matchingGenres.map((matchingGenre) => matchingGenre.id)

    const loadedTracks = await db.query.tracks.findMany({
      where: (track, { and, eq }) =>
        and(
          eq(track.isDeleted, 0),
          sql`${track.id} IN (SELECT track_id FROM track_genres WHERE genre_id IN (${sql.join(
            genreIds.map((id) => sql`${id}`),
            sql`, `
          )}))`
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
      orderBy: [
        desc(tracks.playCount),
        desc(tracks.lastPlayedAt),
        asc(sql`lower(coalesce(${tracks.title}, ''))`),
      ],
      limit,
    })

    return loadedTracks.map(transformDBTrackToTrack)
  } catch {
    return []
  }
}

export async function getAllTracksByGenre(genre: string): Promise<Track[]> {
  try {
    const trimmedGenre = genre.trim()

    const matchingGenres = await db.query.genres.findMany({
      where: (g, { sql }) => sql`${g.name} LIKE ${trimmedGenre}`,
      columns: { id: true },
    })

    if (matchingGenres.length === 0) {
      return []
    }

    const genreIds = matchingGenres.map((matchingGenre) => matchingGenre.id)

    const loadedTracks = await db.query.tracks.findMany({
      where: (track, { and, eq }) =>
        and(
          eq(track.isDeleted, 0),
          sql`${track.id} IN (SELECT track_id FROM track_genres WHERE genre_id IN (${sql.join(
            genreIds.map((id) => sql`${id}`),
            sql`, `
          )}))`
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
      orderBy: [
        desc(tracks.playCount),
        desc(tracks.lastPlayedAt),
        asc(sql`lower(coalesce(${tracks.title}, ''))`),
      ],
    })

    return loadedTracks.map(transformDBTrackToTrack)
  } catch {
    return []
  }
}

export async function getAlbumsByGenre(
  genre: string
): Promise<GenreAlbumInfo[]> {
  try {
    const trimmedGenre = genre.trim()

    const matchingGenres = await db.query.genres.findMany({
      where: (g, { sql }) => sql`LOWER(${g.name}) LIKE LOWER(${trimmedGenre})`,
      columns: { id: true },
    })

    if (matchingGenres.length === 0) {
      return []
    }

    const genreIds = matchingGenres.map((matchingGenre) => matchingGenre.id)

    const tracksInGenre = await db.query.tracks.findMany({
      where: (track, { and, eq }) =>
        and(
          eq(track.isDeleted, 0),
          sql`${track.id} IN (SELECT track_id FROM track_genres WHERE genre_id IN (${sql.join(
            genreIds.map((id) => sql`${id}`),
            sql`, `
          )}))`,
          sql`${track.albumId} IS NOT NULL`
        ),
      with: {
        album: true,
        artist: true,
      },
    })

    const albumMap = new Map<string, GenreAlbumInfo>()

    for (const track of tracksInGenre) {
      if (!track.albumId || !track.album) {
        continue
      }

      const albumName = track.album.title || "Unknown Album"
      const key = `${albumName}-${track.album.artistId || ""}`

      if (!albumMap.has(key)) {
        albumMap.set(key, {
          name: albumName,
          artist: track.artist?.name || undefined,
          image: track.album.artwork || track.artwork || undefined,
          trackCount: 0,
          year: track.album.year || track.year || undefined,
        })
      }

      albumMap.get(key)!.trackCount++
    }

    return Array.from(albumMap.values()).sort(
      (a, b) => b.trackCount - a.trackCount
    )
  } catch {
    return []
  }
}

export async function listGenres() {
  return db.query.genres.findMany({
    orderBy: (genres, { asc }) => [asc(sql`lower(coalesce(${genres.name}, ''))`)],
  })
}

export async function getGenreById(id: string) {
  return db.query.genres.findFirst({
    where: eq(genres.id, id),
    with: {
      tracks: {
        with: {
          track: {
            with: {
              artist: true,
              album: true,
            },
          },
        },
      },
    },
  })
}
