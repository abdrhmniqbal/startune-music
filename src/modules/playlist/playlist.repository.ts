import { and, asc, desc, eq, inArray } from "drizzle-orm"

import { db } from "@/db/client"
import { playlists, playlistTracks, tracks } from "@/db/schema"
import { logError } from "@/modules/logging/logging.service"

function generateId(): string {
  if (globalThis.crypto && globalThis.crypto.randomUUID) {
    return globalThis.crypto.randomUUID()
  }

  return Date.now().toString(36) + Math.random().toString(36).substring(2)
}

function normalizeDescription(description?: string | null): string | null {
  const value = description?.trim()
  if (!value) {
    return null
  }

  return value
}

function collectPlaylistImages(playlist: {
  artwork?: string | null
  tracks: Array<{
    track?: {
      artwork?: string | null
      album?: {
        artwork?: string | null
      } | null
    } | null
  }>
}) {
  const images = new Set<string>()

  if (playlist.artwork) {
    images.add(playlist.artwork)
  }

  for (const playlistTrack of playlist.tracks) {
    const artwork =
      playlistTrack.track?.artwork || playlistTrack.track?.album?.artwork

    if (!artwork) {
      continue
    }

    images.add(artwork)

    if (images.size >= 4) {
      break
    }
  }

  return Array.from(images)
}

async function getPlaylistDurationByTrackIds(trackIds: string[]): Promise<number> {
  if (trackIds.length === 0) {
    return 0
  }

  const rows = await db
    .select({ duration: tracks.duration })
    .from(tracks)
    .where(inArray(tracks.id, trackIds))

  return rows.reduce((sum, row) => sum + (row.duration || 0), 0)
}

async function updatePlaylistStats(playlistId: string) {
  const trackEntries = await db.query.playlistTracks.findMany({
    where: eq(playlistTracks.playlistId, playlistId),
    with: {
      track: true,
    },
  })

  const trackCount = trackEntries.length
  const duration = trackEntries.reduce(
    (sum, entry) => sum + (entry.track?.duration || 0),
    0
  )

  await db
    .update(playlists)
    .set({
      trackCount,
      duration,
      updatedAt: Date.now(),
    })
    .where(eq(playlists.id, playlistId))
}

async function resequencePlaylistTracks(playlistId: string) {
  const remainingTracks = await db.query.playlistTracks.findMany({
    where: eq(playlistTracks.playlistId, playlistId),
    orderBy: [asc(playlistTracks.position)],
  })

  await Promise.all(
    remainingTracks.map((playlistTrack, index) =>
      db
        .update(playlistTracks)
        .set({ position: index })
        .where(eq(playlistTracks.id, playlistTrack.id))
    )
  )
}

export async function listPlaylists() {
  const results = await db.query.playlists.findMany({
    orderBy: [desc(playlists.createdAt)],
    with: {
      tracks: {
        limit: 10,
        orderBy: [asc(playlistTracks.position)],
        with: {
          track: {
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
      },
    },
  })

  return results.map((playlist) => ({
    id: playlist.id,
    name: playlist.name,
    title: playlist.name,
    dateAdded: playlist.createdAt,
    trackCount: playlist.trackCount || 0,
    image: playlist.artwork || undefined,
    images: collectPlaylistImages(playlist),
  }))
}

export async function listPlaylistsForTrack(trackId: string | null) {
  const [results, membershipRows] = await Promise.all([
    listPlaylists(),
    trackId
      ? db
          .select({ playlistId: playlistTracks.playlistId })
          .from(playlistTracks)
          .where(eq(playlistTracks.trackId, trackId))
      : Promise.resolve([]),
  ])

  const playlistIdsWithTrack = new Set(
    membershipRows.map((row) => row.playlistId)
  )

  return results.map((playlist) => ({
    ...playlist,
    hasTrack: playlistIdsWithTrack.has(playlist.id),
  }))
}

export async function getPlaylistById(id: string) {
  const result = await db.query.playlists.findFirst({
    where: eq(playlists.id, id),
    with: {
      tracks: {
        orderBy: [asc(playlistTracks.position)],
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
      },
    },
  })

  return result ?? null
}

export async function createPlaylist(
  name: string,
  description?: string | null,
  trackIds: string[] = []
): Promise<void> {
  try {
    const id = generateId()
    const now = Date.now()
    const duration = await getPlaylistDurationByTrackIds(trackIds)
    const normalizedDescription = normalizeDescription(description)

    await db.insert(playlists).values({
      id,
      name,
      description: normalizedDescription,
      trackCount: trackIds.length,
      duration,
      createdAt: now,
      updatedAt: now,
    })

    if (trackIds.length > 0) {
      await db.insert(playlistTracks).values(
        trackIds.map((trackId, index) => ({
          id: generateId(),
          playlistId: id,
          trackId,
          position: index,
          addedAt: now,
        }))
      )
    }
  } catch (error) {
    logError("Failed to create playlist", error, {
      name,
      trackCount: trackIds.length,
    })
    throw error
  }
}

export async function updatePlaylist(
  id: string,
  name: string,
  description?: string | null,
  trackIds: string[] = []
): Promise<void> {
  try {
    const now = Date.now()
    const duration = await getPlaylistDurationByTrackIds(trackIds)
    const normalizedDescription = normalizeDescription(description)

    await db
      .update(playlists)
      .set({
        name,
        description: normalizedDescription,
        trackCount: trackIds.length,
        duration,
        updatedAt: now,
      })
      .where(eq(playlists.id, id))

    await db.delete(playlistTracks).where(eq(playlistTracks.playlistId, id))

    if (trackIds.length > 0) {
      await db.insert(playlistTracks).values(
        trackIds.map((trackId, index) => ({
          id: generateId(),
          playlistId: id,
          trackId,
          position: index,
          addedAt: now,
        }))
      )
    }
  } catch (error) {
    logError("Failed to update playlist", error, {
      id,
      name,
      trackCount: trackIds.length,
    })
    throw error
  }
}

export async function updatePlaylistMetadata({
  id,
  name,
  description,
}: {
  id: string
  name?: string
  description?: string
}) {
  await db
    .update(playlists)
    .set({
      ...(name !== undefined ? { name } : {}),
      ...(description !== undefined
        ? { description: normalizeDescription(description) }
        : {}),
      updatedAt: Date.now(),
    })
    .where(eq(playlists.id, id))
}

export async function deletePlaylist(id: string) {
  await db.delete(playlists).where(eq(playlists.id, id))
}

export async function addTrackToPlaylist({
  playlistId,
  trackId,
}: {
  playlistId: string
  trackId: string
}) {
  const existingEntry = await db.query.playlistTracks.findFirst({
    where: and(
      eq(playlistTracks.playlistId, playlistId),
      eq(playlistTracks.trackId, trackId)
    ),
  })

  if (existingEntry) {
    return { playlistId, trackId, skipped: true as const }
  }

  const existingTracks = await db.query.playlistTracks.findMany({
    where: eq(playlistTracks.playlistId, playlistId),
    orderBy: [desc(playlistTracks.position)],
    limit: 1,
  })

  const nextPosition =
    existingTracks.length > 0 ? (existingTracks[0].position || 0) + 1 : 0

  await db.insert(playlistTracks).values({
    id: generateId(),
    playlistId,
    trackId,
    position: nextPosition,
    addedAt: Date.now(),
  })

  await updatePlaylistStats(playlistId)

  return { playlistId, trackId, skipped: false as const }
}

export async function removeTrackFromPlaylist({
  playlistId,
  trackId,
}: {
  playlistId: string
  trackId: string
}) {
  await db
    .delete(playlistTracks)
    .where(
      and(
        eq(playlistTracks.playlistId, playlistId),
        eq(playlistTracks.trackId, trackId)
      )
    )

  await resequencePlaylistTracks(playlistId)
  await updatePlaylistStats(playlistId)

  return { playlistId, trackId }
}

export async function reorderPlaylistTracks({
  playlistId,
  trackIds,
}: {
  playlistId: string
  trackIds: string[]
}) {
  await Promise.all(
    trackIds.map((trackId, index) =>
      db
        .update(playlistTracks)
        .set({ position: index })
        .where(
          and(
            eq(playlistTracks.playlistId, playlistId),
            eq(playlistTracks.trackId, trackId)
          )
        )
    )
  )
}
