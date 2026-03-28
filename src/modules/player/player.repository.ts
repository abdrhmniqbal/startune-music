import type { Track } from "@/modules/player/player.types"

import { eq } from "drizzle-orm"
import { db } from "@/db/client"
import { tracks } from "@/db/schema"
import { transformDBTrackToTrack } from "@/utils/transformers"

export async function getAllTracks(): Promise<Track[]> {
  const dbTracks = await db.query.tracks.findMany({
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
  })

  return dbTracks.map(transformDBTrackToTrack)
}
