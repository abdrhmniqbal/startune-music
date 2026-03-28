import type { Track } from "@/modules/player/player.types"

import { desc, eq, sql } from "drizzle-orm"

import { db } from "@/db/client"
import { playHistory, tracks } from "@/db/schema"

import type { HistoryTopTracksPeriod } from "./history.types"

interface TrackWithRelations {
  id: string
  title: string
  artistId: string | null
  albumId: string | null
  duration: number
  uri: string
  artwork: string | null
  playCount: number | null
  lastPlayedAt: number | null
  year: number | null
  isFavorite: number | null
  trackNumber: number | null
  discNumber: number | null
  artist?: {
    name: string | null
  } | null
  album?: {
    title: string | null
  } | null
  genres?:
    | {
        genre?: {
          name: string | null
        } | null
      }[]
    | null
}

function mapTrackRecord(track: TrackWithRelations): Track {
  return {
    id: track.id,
    title: track.title,
    artist: track.artist?.name || undefined,
    artistId: track.artistId || undefined,
    albumArtist: track.artist?.name || undefined,
    album: track.album?.title || undefined,
    albumId: track.albumId || undefined,
    duration: track.duration,
    uri: track.uri,
    image: track.artwork || undefined,
    playCount: track.playCount || 0,
    lastPlayedAt: track.lastPlayedAt || undefined,
    year: track.year || undefined,
    isFavorite: Boolean(track.isFavorite),
    trackNumber: track.trackNumber || undefined,
    discNumber: track.discNumber || undefined,
    genre: track.genres?.[0]?.genre?.name || undefined,
  }
}

export async function getTrackHistory(): Promise<Track[]> {
  try {
    const history = await db.query.playHistory.findMany({
      orderBy: [desc(playHistory.playedAt)],
      with: {
        track: {
          with: {
            artist: true,
            album: true,
            genres: {
              with: {
                genre: true,
              },
            },
          },
        },
      },
      limit: 50,
    })

    return history
      .filter((item) => item.track && !item.track.isDeleted)
      .map((item) => mapTrackRecord(item.track))
  } catch {
    return []
  }
}

export async function getTopTracksByPeriod(
  period: HistoryTopTracksPeriod = "all",
  limit: number = 25
): Promise<Track[]> {
  try {
    if (period === "all") {
      const topTracks = await db.query.tracks.findMany({
        where: eq(tracks.isDeleted, 0),
        orderBy: [desc(tracks.playCount), desc(tracks.lastPlayedAt)],
        with: {
          artist: true,
          album: true,
          genres: {
            with: {
              genre: true,
            },
          },
        },
        limit,
      })

      return topTracks
        .filter((track) => track.playCount && track.playCount > 0)
        .map(mapTrackRecord)
    }

    const timeThreshold =
      period === "day"
        ? Date.now() - 24 * 60 * 60 * 1000
        : Date.now() - 7 * 24 * 60 * 60 * 1000

    const history = await db.query.playHistory.findMany({
      where: sql`${playHistory.playedAt} >= ${timeThreshold}`,
      with: {
        track: {
          with: {
            artist: true,
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

    const trackCounts = new Map<
      string,
      { track: (typeof history)[number]["track"]; count: number }
    >()

    for (const entry of history) {
      if (entry.track && !entry.track.isDeleted) {
        const existing = trackCounts.get(entry.trackId)
        if (existing) {
          existing.count++
        } else {
          trackCounts.set(entry.trackId, { track: entry.track, count: 1 })
        }
      }
    }

    return Array.from(trackCounts.values())
      .sort((a, b) => b.count - a.count)
      .slice(0, limit)
      .map((item) => mapTrackRecord(item.track))
  } catch {
    return []
  }
}

export async function addTrackToHistory(trackId: string): Promise<void> {
  try {
    await db.insert(playHistory).values({
      id: `${trackId}-${Date.now()}`,
      trackId,
      playedAt: Date.now(),
      duration: 0,
      completed: 0,
    })

    await db.run(sql`
      DELETE FROM ${playHistory}
      WHERE ${playHistory.id} IN (
        SELECT ${playHistory.id}
        FROM ${playHistory}
        ORDER BY ${playHistory.playedAt} DESC
        LIMIT -1 OFFSET 50
      )
    `)
  } catch {
    // no-op
  }
}

export async function incrementTrackPlayCount(trackId: string): Promise<void> {
  try {
    await db
      .update(tracks)
      .set({
        playCount: sql`${tracks.playCount} + 1`,
        lastPlayedAt: Date.now(),
      })
      .where(eq(tracks.id, trackId))
  } catch {
    // no-op
  }
}
