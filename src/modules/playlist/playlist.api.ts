import { eq, inArray } from "drizzle-orm"

import { db } from "@/db/client"
import { playlists, playlistTracks, tracks } from "@/db/schema"
import { logError } from "@/modules/logging"

function generateId(): string {
  if (globalThis.crypto && globalThis.crypto.randomUUID) {
    return globalThis.crypto.randomUUID()
  }

  return Date.now().toString(36) + Math.random().toString(36).substring(2)
}

export async function createPlaylist(
  name: string,
  description?: string | null,
  trackIds: string[] = []
): Promise<void> {
  try {
    const id = generateId()
    const now = Date.now()
    const duration = await getPlaylistDuration(trackIds)
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
  } catch (e) {
    logError("Failed to create playlist", e, {
      name,
      trackCount: trackIds.length,
    })
    throw e
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
    const duration = await getPlaylistDuration(trackIds)
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
  } catch (e) {
    logError("Failed to update playlist", e, {
      id,
      name,
      trackCount: trackIds.length,
    })
    throw e
  }
}

async function getPlaylistDuration(trackIds: string[]): Promise<number> {
  if (trackIds.length === 0) {
    return 0
  }

  const rows = await db
    .select({ duration: tracks.duration })
    .from(tracks)
    .where(inArray(tracks.id, trackIds))

  return rows.reduce((sum, row) => sum + (row.duration || 0), 0)
}

function normalizeDescription(description?: string | null): string | null {
  const value = description?.trim()
  if (!value) {
    return null
  }

  return description ?? null
}
