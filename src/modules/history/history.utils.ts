import type { Track } from "@/modules/player/player.types"

export function dedupeTracksById(tracks: Track[]): Track[] {
  const seen = new Set<string>()
  return tracks.filter((track) => {
    if (seen.has(track.id)) {
      return false
    }

    seen.add(track.id)
    return true
  })
}
