import { eq, inArray } from "drizzle-orm"

import { db } from "@/db/client"
import { playlists, playlistTracks, tracks } from "@/db/schema"

function uniqueTrackIds(trackIds: string[]): string[] {
  return [...new Set(trackIds.filter((id) => id.trim().length > 0))]
}

async function updatePlaylistStats(playlistId: string) {
  const entries = await db.query.playlistTracks.findMany({
    where: eq(playlistTracks.playlistId, playlistId),
    with: {
      track: true,
    },
  })

  const trackCount = entries.length
  const duration = entries.reduce(
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

export async function removeTracksFromFavoritesAndPlaylists(
  rawTrackIds: string[]
): Promise<void> {
  const trackIds = uniqueTrackIds(rawTrackIds)
  if (trackIds.length === 0) {
    return
  }

  const now = Date.now()

  // Ensure deleted tracks cannot remain in favorites.
  await db
    .update(tracks)
    .set({
      isFavorite: 0,
      favoritedAt: null,
      updatedAt: now,
    })
    .where(inArray(tracks.id, trackIds))

  const affectedPlaylistRows = await db
    .select({ playlistId: playlistTracks.playlistId })
    .from(playlistTracks)
    .where(inArray(playlistTracks.trackId, trackIds))

  if (affectedPlaylistRows.length === 0) {
    return
  }

  await db
    .delete(playlistTracks)
    .where(inArray(playlistTracks.trackId, trackIds))

  const affectedPlaylistIds = [
    ...new Set(affectedPlaylistRows.map((row) => row.playlistId)),
  ]

  await Promise.all(
    affectedPlaylistIds.map((playlistId) => updatePlaylistStats(playlistId))
  )
}

export async function hardDeleteTrack(trackId: string): Promise<void> {
  const normalizedTrackId = trackId.trim()
  if (normalizedTrackId.length === 0) {
    return
  }

  await removeTracksFromFavoritesAndPlaylists([normalizedTrackId])

  await db
    .update(tracks)
    .set({
      isDeleted: 1,
      updatedAt: Date.now(),
    })
    .where(eq(tracks.id, normalizedTrackId))

  await db.delete(tracks).where(eq(tracks.id, normalizedTrackId))
}
