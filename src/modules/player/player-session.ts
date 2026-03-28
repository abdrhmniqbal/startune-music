import type { RepeatModeType, Track } from "./player.types"
import { File, Paths } from "expo-file-system"

interface PersistedPlaybackSession {
  queue: Track[]
  currentTrackId: string | null
  positionSeconds: number
  repeatMode: RepeatModeType
  wasPlaying: boolean
  savedAt: number
}

const PLAYBACK_SESSION_FILE = new File(Paths.document, "playback-session.json")

function sanitizeTrack(track: Track): Track {
  return {
    id: track.id,
    title: track.title,
    artist: track.artist,
    artistId: track.artistId,
    albumArtist: track.albumArtist,
    album: track.album,
    albumId: track.albumId,
    duration: Number.isFinite(track.duration) ? track.duration : 0,
    uri: track.uri,
    image: track.image,
    lyrics: track.lyrics,
    fileHash: track.fileHash,
    scanTime: track.scanTime,
    isDeleted: track.isDeleted,
    playCount: track.playCount,
    lastPlayedAt: track.lastPlayedAt,
    year: track.year,
    filename: track.filename,
    dateAdded: track.dateAdded,
    isFavorite: track.isFavorite,
    discNumber: track.discNumber,
    trackNumber: track.trackNumber,
    genre: track.genre,
  }
}

function sanitizeSession(
  payload: Partial<PersistedPlaybackSession>
): PersistedPlaybackSession | null {
  if (!Array.isArray(payload.queue)) {
    return null
  }

  const queue = payload.queue
    .map((item) => sanitizeTrack(item))
    .filter((item) => item.id && item.uri && item.title)

  const positionSeconds = Number.isFinite(payload.positionSeconds)
    ? Math.max(0, payload.positionSeconds ?? 0)
    : 0

  const repeatMode: RepeatModeType =
    payload.repeatMode === "track" ||
    payload.repeatMode === "queue" ||
    payload.repeatMode === "off"
      ? payload.repeatMode
      : "off"

  const currentTrackId =
    typeof payload.currentTrackId === "string" &&
    payload.currentTrackId.length > 0
      ? payload.currentTrackId
      : null

  return {
    queue,
    currentTrackId,
    positionSeconds,
    repeatMode,
    wasPlaying: Boolean(payload.wasPlaying),
    savedAt: Number.isFinite(payload.savedAt)
      ? (payload.savedAt ?? Date.now())
      : Date.now(),
  }
}

export async function savePlaybackSession(
  session: PersistedPlaybackSession
): Promise<void> {
  const sanitized = sanitizeSession(session)
  if (!sanitized) {
    return
  }

  if (!PLAYBACK_SESSION_FILE.exists) {
    PLAYBACK_SESSION_FILE.create({
      intermediates: true,
      overwrite: true,
    })
  }

  PLAYBACK_SESSION_FILE.write(JSON.stringify(sanitized), {
    encoding: "utf8",
  })
}

export async function loadPlaybackSession(): Promise<PersistedPlaybackSession | null> {
  try {
    if (!PLAYBACK_SESSION_FILE.exists) {
      return null
    }

    const raw = await PLAYBACK_SESSION_FILE.text()
    const parsed = JSON.parse(raw) as Partial<PersistedPlaybackSession>
    return sanitizeSession(parsed)
  } catch {
    return null
  }
}
