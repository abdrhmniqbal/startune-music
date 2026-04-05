import type { RepeatModeType, Track } from "./player.types"
import { File, Paths } from "expo-file-system"

interface PersistedPlaybackSession {
  queueTrackIds: string[]
  originalQueueTrackIds: string[]
  immediateQueueTrackIds: string[]
  trackMap: Record<string, Track>
  currentTrackId: string | null
  positionSeconds: number
  repeatMode: RepeatModeType
  wasPlaying: boolean
  isShuffled: boolean
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
  const hasIdQueue = Array.isArray(payload.queueTrackIds)
  const hasLegacyQueue = Array.isArray((payload as { queue?: unknown }).queue)

  if (!hasIdQueue && !hasLegacyQueue) {
    return null
  }

  const trackMap: Record<string, Track> = {}
  let queueTrackIds: string[] = []

  if (hasIdQueue) {
    const payloadTrackMap =
      payload.trackMap && typeof payload.trackMap === "object"
        ? payload.trackMap
        : {}

    for (const [trackId, value] of Object.entries(payloadTrackMap)) {
      const sanitized = sanitizeTrack(value as Track)
      if (!sanitized.id || !sanitized.uri || !sanitized.title) {
        continue
      }

      trackMap[sanitized.id] = sanitized
      if (sanitized.id !== trackId) {
        trackMap[trackId] = sanitized
      }
    }

    queueTrackIds = payload.queueTrackIds
      ?.filter((trackId): trackId is string => typeof trackId === "string")
      .filter((trackId) => Boolean(trackMap[trackId]))
      .filter((trackId, index, array) => array.indexOf(trackId) === index) || []
  } else {
    const legacyQueue = ((payload as { queue?: Track[] }).queue || [])
      .map((item) => sanitizeTrack(item))
      .filter((item) => item.id && item.uri && item.title)

    for (const item of legacyQueue) {
      trackMap[item.id] = item
    }

    queueTrackIds = legacyQueue.map((item) => item.id)
  }

  const originalQueueTrackIds = Array.isArray(payload.originalQueueTrackIds)
    ? payload.originalQueueTrackIds
        .filter((trackId): trackId is string => typeof trackId === "string")
        .filter((trackId) => Boolean(trackMap[trackId]))
        .filter((trackId, index, array) => array.indexOf(trackId) === index)
    : [...queueTrackIds]

  const immediateQueueTrackIds = Array.isArray(payload.immediateQueueTrackIds)
    ? payload.immediateQueueTrackIds
        .filter((trackId): trackId is string => typeof trackId === "string")
        .filter((trackId) => Boolean(trackMap[trackId]))
        .filter((trackId, index, array) => array.indexOf(trackId) === index)
    : []

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
      ? (trackMap[payload.currentTrackId] ? payload.currentTrackId : null)
      : null

  return {
    queueTrackIds,
    originalQueueTrackIds,
    immediateQueueTrackIds,
    trackMap,
    currentTrackId,
    positionSeconds,
    repeatMode,
    wasPlaying: Boolean(payload.wasPlaying),
    isShuffled: Boolean(payload.isShuffled),
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
