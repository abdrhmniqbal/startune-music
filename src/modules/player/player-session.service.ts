import { logError, logInfo } from "@/modules/logging/logging.service"
import {
  loadPlaybackSession,
  savePlaybackSession,
} from "@/modules/player/player-session.repository"
import { getQueueState } from "@/modules/player/player.store"
import { RepeatMode, State, TrackPlayer } from "@/modules/player/player.utils"

import {
  mapRepeatMode,
  mapTrackPlayerRepeatMode,
  mapTrackPlayerTrackToTrack,
  mapTrackToTrackPlayerInput,
} from "./player-adapter"
import { setActiveTrack, setPlaybackProgress } from "./player-runtime-state"
import {
  getCurrentTrackState,
  getImmediateQueueTrackIdsState,
  getIsShuffledState,
  getIsPlayingState,
  getOriginalQueueTrackIdsState,
  getQueueTrackIdsState,
  getRepeatModeState,
  getTracksState,
  setImmediateQueueTrackIdsState,
  setIsPlayingState,
  setIsShuffledState,
  setOriginalQueueState,
  setOriginalQueueTrackIdsState,
  setQueueState,
  setQueueTrackIdsState,
  setRepeatModeState,
} from "./player.store"

const MIN_SESSION_SAVE_INTERVAL_MS = 2000
const MAX_TRACKMAP_SIZE = 300
const TRACKMAP_ACTIVE_WINDOW = 120

let lastPlaybackSessionSavedAt = 0

function mapNativeQueueToTracks(nativeQueue: Awaited<ReturnType<typeof TrackPlayer.getQueue>>) {
  return nativeQueue
    .map((track) => mapTrackPlayerTrackToTrack(track, getTracksState()))
    .filter((track) => track.id && track.uri)
}

function createPersistedTrackMap(queueTracks: ReturnType<typeof getQueueState>) {
  if (queueTracks.length === 0) {
    return {}
  }

  const currentTrackId = getCurrentTrackState()?.id ?? null
  const currentIndex = currentTrackId
    ? queueTracks.findIndex((track) => track.id === currentTrackId)
    : 0

  const startIndex =
    currentIndex >= 0 ? Math.max(0, currentIndex - TRACKMAP_ACTIVE_WINDOW) : 0
  const endIndex =
    currentIndex >= 0
      ? Math.min(queueTracks.length, currentIndex + TRACKMAP_ACTIVE_WINDOW + 1)
      : Math.min(queueTracks.length, MAX_TRACKMAP_SIZE)

  const selectedIds = new Set(
    queueTracks.slice(startIndex, endIndex).map((track) => track.id)
  )

  if (currentTrackId) {
    selectedIds.add(currentTrackId)
  }

  if (selectedIds.size < MAX_TRACKMAP_SIZE) {
    for (const trackId of getImmediateQueueTrackIdsState()) {
      selectedIds.add(trackId)
      if (selectedIds.size >= MAX_TRACKMAP_SIZE) {
        break
      }
    }
  }

  if (selectedIds.size < MAX_TRACKMAP_SIZE) {
    for (const track of queueTracks) {
      if (selectedIds.size >= MAX_TRACKMAP_SIZE) {
        break
      }
      selectedIds.add(track.id)
    }
  }

  return Object.fromEntries(
    queueTracks
      .filter((track) => selectedIds.has(track.id))
      .map((track) => [track.id, track])
  )
}

export async function persistPlaybackSession(options?: {
  force?: boolean
  skipQueueSync?: boolean
}): Promise<void> {
  const now = Date.now()
  if (
    !options?.force &&
    now - lastPlaybackSessionSavedAt < MIN_SESSION_SAVE_INTERVAL_MS
  ) {
    return
  }

  try {
    const shouldSyncQueueWithNativePlayer =
      options?.skipQueueSync !== true &&
      (options?.force === true || getQueueTrackIdsState().length === 0)

    const queueTracks = shouldSyncQueueWithNativePlayer
      ? mapNativeQueueToTracks(await TrackPlayer.getQueue())
      : getQueueState()
    const queueTrackIds = queueTracks.map((track) => track.id)
    const trackMap = createPersistedTrackMap(queueTracks)

    let currentTrackId = getCurrentTrackState()?.id ?? null

    if (shouldSyncQueueWithNativePlayer) {
      const currentIndex = await TrackPlayer.getCurrentTrack()
      currentTrackId =
        currentIndex !== null &&
        currentIndex >= 0 &&
        currentIndex < queueTracks.length
          ? (queueTracks[currentIndex]?.id ?? null)
          : (getCurrentTrackState()?.id ?? null)
    }

    const positionSeconds = await TrackPlayer.getPosition()

    await savePlaybackSession({
      queueTrackIds,
      originalQueueTrackIds:
        getOriginalQueueTrackIdsState().length > 0
          ? getOriginalQueueTrackIdsState()
          : queueTrackIds,
      immediateQueueTrackIds: getImmediateQueueTrackIdsState(),
      trackMap,
      currentTrackId,
      positionSeconds,
      repeatMode: getRepeatModeState(),
      wasPlaying: getIsPlayingState(),
      isShuffled: getIsShuffledState(),
      savedAt: now,
    })
    lastPlaybackSessionSavedAt = now
  } catch (error) {
    logError("Failed to persist playback session", error, options)
  }
}

export async function restorePlaybackSession(): Promise<void> {
  try {
    const nativeQueue = await TrackPlayer.getQueue()

    if (nativeQueue.length > 0) {
      logInfo("Restoring playback session from native queue", {
        queueLength: nativeQueue.length,
      })
      const mappedQueue = mapNativeQueueToTracks(nativeQueue)
      if (mappedQueue.length > 0) {
        setQueueState(mappedQueue)
        setOriginalQueueState(mappedQueue)
        setQueueTrackIdsState(mappedQueue.map((track) => track.id))
        setOriginalQueueTrackIdsState(mappedQueue.map((track) => track.id))
        setImmediateQueueTrackIdsState([])
      }

      const currentIndex = await TrackPlayer.getCurrentTrack()
      if (
        currentIndex !== null &&
        currentIndex >= 0 &&
        currentIndex < mappedQueue.length
      ) {
        setActiveTrack(mappedQueue[currentIndex] || null)
      } else {
        setActiveTrack(mappedQueue[0] || null)
      }

      const [position, playbackState, repeatMode] = await Promise.all([
        TrackPlayer.getPosition(),
        TrackPlayer.getState(),
        TrackPlayer.getRepeatMode(),
      ])

      setPlaybackProgress(position, getCurrentTrackState()?.duration || 0)
      setIsPlayingState(playbackState === State.Playing)
      setRepeatModeState(mapTrackPlayerRepeatMode(repeatMode as RepeatMode))
      await persistPlaybackSession({ force: true, skipQueueSync: true })
      return
    }

    const snapshot = await loadPlaybackSession()
    if (!snapshot || snapshot.queueTrackIds.length === 0) {
      logInfo("No playback session snapshot available to restore")
      return
    }

    const libraryTrackMap = new Map(getTracksState().map((track) => [track.id, track]))
    const resolveTrack = (trackId: string) =>
      libraryTrackMap.get(trackId) || snapshot.trackMap[trackId]

    const resolvedQueue = snapshot.queueTrackIds
      .map((trackId) => resolveTrack(trackId))
      .filter((track): track is NonNullable<typeof track> => Boolean(track))

    if (resolvedQueue.length === 0) {
      logInfo("Playback session queue could not be resolved from IDs")
      return
    }

    const resolvedOriginalQueue = snapshot.originalQueueTrackIds
      .map((trackId) => resolveTrack(trackId))
      .filter((track): track is NonNullable<typeof track> => Boolean(track))

    const resolvedImmediateQueueIds = snapshot.immediateQueueTrackIds.filter((trackId) =>
      resolvedQueue.some((track) => track.id === trackId)
    )

    logInfo("Restoring playback session from saved snapshot", {
      queueLength: resolvedQueue.length,
      currentTrackId: snapshot.currentTrackId,
      wasPlaying: snapshot.wasPlaying,
    })

    await TrackPlayer.reset()
    await TrackPlayer.add(resolvedQueue.map(mapTrackToTrackPlayerInput))

    const currentIndex =
      snapshot.currentTrackId !== null
        ? resolvedQueue.findIndex(
            (track) => track.id === snapshot.currentTrackId
          )
        : 0
    const targetIndex = currentIndex >= 0 ? currentIndex : 0
    const targetPosition = Math.max(0, snapshot.positionSeconds || 0)

    await TrackPlayer.skip(targetIndex, targetPosition)
    await TrackPlayer.setRepeatMode(mapRepeatMode(snapshot.repeatMode))

    const currentTrack = resolvedQueue[targetIndex] || null
    setQueueState(resolvedQueue)
    setQueueTrackIdsState(resolvedQueue.map((track) => track.id))
    setOriginalQueueState(
      resolvedOriginalQueue.length > 0 ? resolvedOriginalQueue : resolvedQueue
    )
    setOriginalQueueTrackIdsState(
      (resolvedOriginalQueue.length > 0 ? resolvedOriginalQueue : resolvedQueue).map(
        (track) => track.id
      )
    )
    setImmediateQueueTrackIdsState(resolvedImmediateQueueIds)
    setIsShuffledState(snapshot.isShuffled)
    setActiveTrack(currentTrack)
    setPlaybackProgress(targetPosition, currentTrack?.duration || 0)
    setRepeatModeState(snapshot.repeatMode)

    await TrackPlayer.pause()
    setIsPlayingState(false)

    await persistPlaybackSession({ force: true, skipQueueSync: true })
  } catch (error) {
    logError("Failed to restore playback session", error)
  }
}

export async function syncCurrentTrackFromPlayer(): Promise<void> {
  try {
    const [activeIndex, nativeQueue, activeTrack] = await Promise.all([
      TrackPlayer.getCurrentTrack(),
      TrackPlayer.getQueue(),
      TrackPlayer.getActiveTrack(),
    ])

    const mappedQueue = mapNativeQueueToTracks(nativeQueue)
    if (mappedQueue.length > 0) {
      setQueueState(mappedQueue)
      setQueueTrackIdsState(mappedQueue.map((track) => track.id))

      const mappedQueueIdSet = new Set(mappedQueue.map((track) => track.id))
      const currentImmediateIds = getImmediateQueueTrackIdsState()
      const nextImmediateIds = currentImmediateIds.filter((trackId) =>
        mappedQueueIdSet.has(trackId)
      )

      if (nextImmediateIds.length !== currentImmediateIds.length) {
        setImmediateQueueTrackIdsState(nextImmediateIds)
      }
    }

    if (
      activeIndex !== null &&
      activeIndex >= 0 &&
      activeIndex < mappedQueue.length
    ) {
      const consumedTrackIds = new Set(
        mappedQueue.slice(0, activeIndex + 1).map((track) => track.id)
      )
      const currentImmediateIds = getImmediateQueueTrackIdsState()
      const nextImmediateIds = currentImmediateIds.filter(
        (trackId) => !consumedTrackIds.has(trackId)
      )

      if (nextImmediateIds.length !== currentImmediateIds.length) {
        setImmediateQueueTrackIdsState(nextImmediateIds)
      }

      setActiveTrack(mappedQueue[activeIndex] || null)
      return
    }

    if (!activeTrack) {
      setActiveTrack(mappedQueue[0] || null)
      return
    }

    const mappedTrack = mapTrackPlayerTrackToTrack(activeTrack, getTracksState())
    setActiveTrack(mappedTrack)
  } catch (error) {
    logError("Failed to sync current track from player", error)
  }
}
