import type { RepeatModeType, Track } from "@/modules/player/player.types"
import { RepeatMode } from "@/modules/player/player.utils"

export function mapTrackPlayerTrackToTrack(
  track: any,
  allTracks: Track[]
): Track {
  return {
    ...allTracks.find((item) => item.id === String(track.id)),
    id: String(track.id),
    title: typeof track.title === "string" ? track.title : "Unknown Track",
    artist: track.artist,
    album: track.album,
    duration: track.duration || 0,
    uri: track.url as string,
    image: track.artwork as string | undefined,
  }
}

export function mapTrackToTrackPlayerInput(track: Track) {
  return {
    id: track.id,
    url: track.uri,
    title: track.title,
    artist: track.artist,
    album: track.album,
    artwork: track.image,
    duration: track.duration,
  }
}

export function mapTrackPlayerRepeatMode(mode: RepeatMode): RepeatModeType {
  switch (mode) {
    case RepeatMode.Track:
      return "track"
    case RepeatMode.Queue:
      return "queue"
    case RepeatMode.Off:
    default:
      return "off"
  }
}

export function mapRepeatMode(mode: RepeatModeType): RepeatMode {
  switch (mode) {
    case "track":
      return RepeatMode.Track
    case "queue":
      return RepeatMode.Queue
    case "off":
    default:
      return RepeatMode.Off
  }
}
