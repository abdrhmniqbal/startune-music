import type { Track } from "@/modules/player/player.types"
import {
  and,
  asc,
  desc,
  eq,
  gt,
  inArray,
  isNotNull,
  like,
  or,
  sql,
} from "drizzle-orm"

import { db } from "@/db/client"
import { albums, artists, playlists, playlistTracks, tracks } from "@/db/schema"
import { logError } from "@/modules/logging/logging.service"
import { transformDBTrackToTrack } from "@/utils/transformers"

import type { SearchResults } from "./library.types"

function normalizeLookup(value: string | null | undefined) {
  return (value || "").trim().toLowerCase()
}

export async function listArtists(
  orderByField: "name" | "trackCount" | "dateAdded" = "name",
  order: "asc" | "desc" = "asc"
) {
  const direction = order === "asc" ? asc : desc
  const artistSortNameOrderValue = sql`lower(coalesce(${artists.sortName}, ${artists.name}, ''))`
  const artistNameOrderValue = sql`lower(coalesce(${artists.name}, ''))`
  const orderBy =
    orderByField === "trackCount"
      ? [
          direction(artists.trackCount),
          direction(artistSortNameOrderValue),
          direction(artistNameOrderValue),
        ]
      : orderByField === "dateAdded"
        ? [
            direction(artists.createdAt),
            direction(artistSortNameOrderValue),
            direction(artistNameOrderValue),
          ]
        : [direction(artistSortNameOrderValue), direction(artistNameOrderValue)]

  const results = await db.query.artists.findMany({
    where: gt(artists.trackCount, 0),
    columns: {
      id: true,
      name: true,
      sortName: true,
      artwork: true,
      createdAt: true,
      trackCount: true,
    },
    with: {
      albums: {
        columns: {
          artwork: true,
        },
        limit: 1,
      },
      tracks: {
        where: and(eq(tracks.isDeleted, 0), isNotNull(tracks.artwork)),
        columns: {
          artwork: true,
        },
        limit: 1,
      },
    },
    orderBy,
  })

  return results.map((artist) => ({
    id: artist.id,
    name: artist.name,
    sortName: artist.sortName,
    artwork: artist.artwork,
    createdAt: artist.createdAt,
    albumArtwork: artist.albums[0]?.artwork || null,
    trackArtwork: artist.tracks[0]?.artwork || null,
    trackCount: artist.trackCount || 0,
  }))
}

export async function getArtistById(id: string) {
  return db.query.artists.findFirst({
    where: and(eq(artists.id, id), gt(artists.trackCount, 0)),
    with: {
      albums: {
        where: gt(albums.trackCount, 0),
        orderBy: [desc(albums.year)],
      },
      tracks: {
        where: eq(tracks.isDeleted, 0),
        with: {
          album: true,
          genres: {
            with: {
              genre: true,
            },
          },
        },
      },
    },
  })
}

export async function listAlbums(
  orderByField:
    | "title"
    | "artist"
    | "year"
    | "trackCount"
    | "dateAdded" = "title",
  order: "asc" | "desc" = "asc"
) {
  const direction = order === "asc" ? asc : desc
  const albumTitleOrderValue = sql`lower(coalesce(${albums.title}, ''))`
  const orderBy =
    orderByField === "year"
      ? [direction(albums.year), direction(albumTitleOrderValue)]
      : orderByField === "trackCount"
        ? [direction(albums.trackCount), direction(albumTitleOrderValue)]
        : orderByField === "dateAdded"
          ? [direction(albums.createdAt), direction(albumTitleOrderValue)]
          : [direction(albumTitleOrderValue)]

  const results = await db.query.albums.findMany({
    where: gt(albums.trackCount, 0),
    columns: {
      id: true,
      title: true,
      artistId: true,
      year: true,
      artwork: true,
      createdAt: true,
      trackCount: true,
    },
    with: {
      artist: true,
    },
    orderBy: orderByField === "artist" ? undefined : orderBy,
  })

  const mapped = results.map((album) => ({
    id: album.id,
    title: album.title,
    artistId: album.artistId,
    year: album.year,
    artwork: album.artwork,
    createdAt: album.createdAt,
    artist: album.artist,
    trackCount: album.trackCount || 0,
  }))

  if (orderByField !== "artist") {
    return mapped
  }

  const multiplier = order === "asc" ? 1 : -1
  return mapped.sort((a, b) => {
    const aVal = a.artist?.sortName || a.artist?.name || ""
    const bVal = b.artist?.sortName || b.artist?.name || ""
    const byArtist = aVal.localeCompare(bVal, undefined, {
      sensitivity: "base",
    })

    if (byArtist !== 0) {
      return byArtist * multiplier
    }

    return (
      (a.title || "").localeCompare(b.title || "", undefined, {
        sensitivity: "base",
      }) * multiplier
    )
  })
}

export async function getAlbumById(id: string) {
  return db.query.albums.findFirst({
    where: and(eq(albums.id, id), gt(albums.trackCount, 0)),
    with: {
      artist: true,
      tracks: {
        where: eq(tracks.isDeleted, 0),
        orderBy: [
          asc(tracks.discNumber),
          asc(tracks.trackNumber),
          asc(sql`lower(coalesce(${tracks.title}, ''))`),
        ],
        with: {
          artist: true,
          genres: {
            with: {
              genre: true,
            },
          },
        },
      },
    },
  })
}

export async function getTracksByAlbumName(albumName: string): Promise<Track[]> {
  const normalizedAlbumName = normalizeLookup(albumName)
  const matchingAlbums = await db.query.albums.findMany({
    columns: {
      id: true,
      title: true,
    },
  })

  const matchingAlbumIds = matchingAlbums
    .filter((album) => normalizeLookup(album.title) === normalizedAlbumName)
    .map((album) => album.id)

  if (matchingAlbumIds.length === 0) {
    const fallbackTracks = await db.query.tracks.findMany({
      where: eq(tracks.isDeleted, 0),
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
        asc(tracks.discNumber),
        asc(tracks.trackNumber),
        asc(sql`lower(coalesce(${tracks.title}, ''))`),
      ],
    })

    return fallbackTracks
      .filter(
        (track) => normalizeLookup(track.album?.title) === normalizedAlbumName
      )
      .map(transformDBTrackToTrack)
  }

  const results = await db.query.tracks.findMany({
    where: and(eq(tracks.isDeleted, 0), inArray(tracks.albumId, matchingAlbumIds)),
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
      asc(tracks.discNumber),
      asc(tracks.trackNumber),
      asc(sql`lower(coalesce(${tracks.title}, ''))`),
    ],
  })

  return results.map(transformDBTrackToTrack)
}

export async function getTracksByArtistName(
  artistName: string
): Promise<Track[]> {
  const normalizedArtistName = normalizeLookup(artistName)
  const matchingArtists = await db.query.artists.findMany({
    columns: {
      id: true,
      name: true,
    },
  })

  const matchingArtistIds = matchingArtists
    .filter((artist) => normalizeLookup(artist.name) === normalizedArtistName)
    .map((artist) => artist.id)

  if (matchingArtistIds.length === 0) {
    const fallbackTracks = await db.query.tracks.findMany({
      where: eq(tracks.isDeleted, 0),
      with: {
        artist: true,
        album: true,
        genres: {
          with: {
            genre: true,
          },
        },
      },
      orderBy: [asc(sql`lower(coalesce(${tracks.title}, ''))`)],
    })

    return fallbackTracks
      .filter(
        (track) => normalizeLookup(track.artist?.name) === normalizedArtistName
      )
      .map(transformDBTrackToTrack)
  }

  const results = await db.query.tracks.findMany({
    where: and(eq(tracks.isDeleted, 0), inArray(tracks.artistId, matchingArtistIds)),
    with: {
      artist: true,
      album: true,
      genres: {
        with: {
          genre: true,
        },
      },
    },
  })

  return results.map(transformDBTrackToTrack)
}

export async function searchLibrary(query: string): Promise<SearchResults> {
  const normalizedQuery = query.trim()

  if (!normalizedQuery) {
    return {
      tracks: [],
      artists: [],
      albums: [],
      playlists: [],
    }
  }

  const searchTerm = `%${normalizedQuery}%`
  const emptyResults: SearchResults = {
    tracks: [],
    artists: [],
    albums: [],
    playlists: [],
  }

  try {
    const [artistResults, albumResults, playlistResults, titleTrackResults] =
      await Promise.all([
        db.query.artists.findMany({
          where: and(like(artists.name, searchTerm), gt(artists.trackCount, 0)),
          with: {
            albums: {
              columns: {
                artwork: true,
              },
              limit: 1,
            },
            tracks: {
              where: and(eq(tracks.isDeleted, 0), isNotNull(tracks.artwork)),
              columns: {
                artwork: true,
              },
              limit: 1,
            },
          },
          orderBy: [asc(sql`lower(coalesce(${artists.name}, ''))`)],
          limit: 10,
        }),
        db.query.albums.findMany({
          where: and(like(albums.title, searchTerm), gt(albums.trackCount, 0)),
          with: { artist: true },
          orderBy: [asc(sql`lower(coalesce(${albums.title}, ''))`)],
          limit: 10,
        }),
        db.query.playlists.findMany({
          where: like(playlists.name, searchTerm),
          orderBy: [desc(playlists.updatedAt)],
          limit: 10,
          with: {
            tracks: {
              limit: 4,
              orderBy: [asc(playlistTracks.position)],
              with: {
                track: {
                  with: {
                    album: true,
                  },
                },
              },
            },
          },
        }),
        db.query.tracks.findMany({
          where: and(eq(tracks.isDeleted, 0), like(tracks.title, searchTerm)),
          with: {
            artist: true,
            album: true,
            genres: {
              with: {
                genre: true,
              },
            },
          },
          orderBy: [desc(tracks.playCount), desc(tracks.lastPlayedAt)],
          limit: 20,
        }),
      ])

    const matchedArtistIds = artistResults.map((artist) => artist.id)
    const matchedAlbumIds = albumResults.map((album) => album.id)

    const relationTrackFilter =
      matchedArtistIds.length > 0 && matchedAlbumIds.length > 0
        ? or(
            inArray(tracks.artistId, matchedArtistIds),
            inArray(tracks.albumId, matchedAlbumIds)
          )
        : matchedArtistIds.length > 0
          ? inArray(tracks.artistId, matchedArtistIds)
          : matchedAlbumIds.length > 0
            ? inArray(tracks.albumId, matchedAlbumIds)
            : null

    const relationTrackResults = relationTrackFilter
      ? await db.query.tracks.findMany({
          where: and(eq(tracks.isDeleted, 0), relationTrackFilter),
          with: {
            artist: true,
            album: true,
            genres: {
              with: {
                genre: true,
              },
            },
          },
          orderBy: [desc(tracks.playCount), desc(tracks.lastPlayedAt)],
          limit: 40,
        })
      : []

    const mergedTrackResults = [...titleTrackResults]
    const trackIds = new Set(titleTrackResults.map((track) => track.id))

    for (const track of relationTrackResults) {
      if (trackIds.has(track.id)) {
        continue
      }

      trackIds.add(track.id)
      mergedTrackResults.push(track)

      if (mergedTrackResults.length >= 20) {
        break
      }
    }

    return {
      tracks: mergedTrackResults.map(transformDBTrackToTrack),
      artists: artistResults.map((artist) => ({
        id: artist.id,
        name: artist.name,
        type: "Artist",
        followerCount: 0,
        isVerified: false,
        image:
          artist.artwork ||
          artist.tracks[0]?.artwork ||
          artist.albums[0]?.artwork ||
          undefined,
      })),
      albums: albumResults.map((album) => ({
        id: album.id,
        title: album.title,
        artist: album.artist?.name || "Unknown Artist",
        isVerified: false,
        image: album.artwork || undefined,
      })),
      playlists: playlistResults.map((playlist) => {
        const images = new Set<string>()

        if (playlist.artwork) {
          images.add(playlist.artwork)
        }

        for (const playlistTrack of playlist.tracks) {
          const image =
            playlistTrack.track?.artwork ||
            playlistTrack.track?.album?.artwork ||
            undefined

          if (image) {
            images.add(image)
          }

          if (images.size >= 4) {
            break
          }
        }

        return {
          id: playlist.id,
          title: playlist.name,
          trackCount: playlist.trackCount || 0,
          image: playlist.artwork || undefined,
          images: Array.from(images),
        }
      }),
    }
  } catch (error) {
    logError("Search query failed", error, { query: normalizedQuery })
    return emptyResults
  }
}

export async function getRecentSearches() {
  return [] as string[]
}
