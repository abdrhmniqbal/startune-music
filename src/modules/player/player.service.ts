import { processColor } from "react-native"

import { queryClient } from "@/lib/tanstack-query"
import { invalidateFavoriteQueries } from "@/modules/favorites/favorites.keys"
import { setTrackFavoriteFlag } from "@/modules/favorites/favorites.repository"
import {
  addTrackToHistory,
  incrementTrackPlayCount,
} from "@/modules/history/history.repository"
import { logError, logInfo, logWarn } from "@/modules/logging/logger"
import {
  loadPlaybackSession,
  savePlaybackSession,
} from "@/modules/player/player-session.repository"
import { updateColorsForImage } from "@/modules/player/player-colors.service"
import {
  invalidatePlayerQueries,
  optimisticallyUpdateRecentlyPlayedQueries,
} from "@/modules/player/player.keys"
import type { RepeatModeType, Track } from "@/modules/player/player.types"

import {
  Capability,
  Event,
  RepeatMode,
  State,
  TrackPlayer,
} from "@/modules/player/player.utils"

import {
  getCurrentTrackState,
  getDurationState,
  getIsPlayingState,
  getPlaybackRefreshVersionState,
  getQueueState,
  getRepeatModeState,
  getTracksState,
  setCurrentTrackState,
  setDurationState,
  setIsPlayingState,
  setPlaybackRefreshVersionState,
  setRepeatModeState,
  setTracksState,
  usePlayerStore,
} from "./player.store"

let isPlayerReady = false
const MIN_SESSION_SAVE_INTERVAL_MS = 2000
let lastPlaybackSessionSavedAt = 0
let lastProgressPosition = 0
let lastProgressDuration = 0

function setActiveTrack(track: Track | null) {
  setCurrentTrackState(track)
  setDurationState(track?.duration || 0)
  void updateColorsForImage(track?.image)
}

function bumpPlaybackRefreshVersion() {
  setPlaybackRefreshVersionState(getPlaybackRefreshVersionState() + 1)
}

function setPlaybackProgress(position: number, duration: number) {
  const nextPosition = Number.isFinite(position) ? Math.max(0, position) : 0
  const nextDuration = Number.isFinite(duration) ? Math.max(0, duration) : 0

  if (
    Math.abs(lastProgressPosition - nextPosition) < 0.01 &&
    Math.abs(lastProgressDuration - nextDuration) < 0.01
  ) {
    return
  }

  lastProgressPosition = nextPosition
  lastProgressDuration = nextDuration
  usePlayerStore.setState({
    currentTime: nextPosition,
    duration: nextDuration,
  })
}

async function handleTrackActivated(track: Track) {
  optimisticallyUpdateRecentlyPlayedQueries(queryClient, track)
  await Promise.allSettled([
    addTrackToHistory(track.id),
    incrementTrackPlayCount(track.id),
  ])
  bumpPlaybackRefreshVersion()
  void invalidatePlayerQueries(queryClient)
}

function mapTrackPlayerTrackToTrack(track: any): Track {
  return {
    ...getTracksState().find((item) => item.id === String(track.id)),
    id: String(track.id),
    title: typeof track.title === "string" ? track.title : "Unknown Track",
    artist: track.artist,
    album: track.album,
    duration: track.duration || 0,
    uri: track.url as string,
    image: track.artwork as string | undefined,
  }
}

function mapTrackToTrackPlayerInput(track: Track) {
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

function mapTrackPlayerRepeatMode(mode: RepeatMode): RepeatModeType {
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

function mapRepeatMode(mode: RepeatModeType): RepeatMode {
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

async function setQueueStore(tracks: Track[]): Promise<void> {
  const { setQueue } = await import("./queue.store")
  setQueue(tracks)
}

export async function syncCurrentTrackFromPlayer(): Promise<void> {
  try {
    const activeIndex = await TrackPlayer.getCurrentTrack()
    if (activeIndex !== null && activeIndex >= 0) {
      const queueTrack = getQueueState()[activeIndex]
      if (queueTrack) {
        setActiveTrack(queueTrack)
        return
      }
    }

    const activeTrack = await TrackPlayer.getActiveTrack()
    if (!activeTrack) {
      return
    }

    const mappedTrack = mapTrackPlayerTrackToTrack(activeTrack)
    setActiveTrack(mappedTrack)
  } catch (error) {
    logError("Failed to sync current track from player", error)
  }
}

export async function persistPlaybackSession(options?: {
  force?: boolean
}): Promise<void> {
  if (!isPlayerReady) {
    return
  }

  const now = Date.now()
  if (
    !options?.force &&
    now - lastPlaybackSessionSavedAt < MIN_SESSION_SAVE_INTERVAL_MS
  ) {
    return
  }

  try {
    const queue = (await TrackPlayer.getQueue())
      .map(mapTrackPlayerTrackToTrack)
      .filter((track) => track.id && track.uri)
    const currentIndex = await TrackPlayer.getCurrentTrack()
    const positionSeconds = await TrackPlayer.getPosition()

    const currentTrackId =
      currentIndex !== null && currentIndex >= 0 && currentIndex < queue.length
        ? (queue[currentIndex]?.id ?? null)
        : (getCurrentTrackState()?.id ?? null)

    await savePlaybackSession({
      queue,
      currentTrackId,
      positionSeconds,
      repeatMode: getRepeatModeState(),
      wasPlaying: getIsPlayingState(),
      savedAt: now,
    })
    lastPlaybackSessionSavedAt = now
  } catch (error) {
    logError("Failed to persist playback session", error, options)
  }
}

export async function restorePlaybackSession(): Promise<void> {
  if (!isPlayerReady) {
    return
  }

  try {
    const nativeQueue = await TrackPlayer.getQueue()

    if (nativeQueue.length > 0) {
      logInfo("Restoring playback session from native queue", {
        queueLength: nativeQueue.length,
      })
      const mappedQueue = nativeQueue
        .map(mapTrackPlayerTrackToTrack)
        .filter((track) => track.id && track.uri)
      if (mappedQueue.length > 0) {
        await setQueueStore(mappedQueue)
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
      setRepeatModeState(mapTrackPlayerRepeatMode(repeatMode))
      await persistPlaybackSession({ force: true })
      return
    }

    const snapshot = await loadPlaybackSession()
    if (!snapshot || snapshot.queue.length === 0) {
      logInfo("No playback session snapshot available to restore")
      return
    }

    logInfo("Restoring playback session from saved snapshot", {
      queueLength: snapshot.queue.length,
      currentTrackId: snapshot.currentTrackId,
      wasPlaying: snapshot.wasPlaying,
    })

    await TrackPlayer.reset()
    await TrackPlayer.add(snapshot.queue.map(mapTrackToTrackPlayerInput))

    const currentIndex =
      snapshot.currentTrackId !== null
        ? snapshot.queue.findIndex(
            (track) => track.id === snapshot.currentTrackId
          )
        : 0
    const targetIndex = currentIndex >= 0 ? currentIndex : 0
    const targetPosition = Math.max(0, snapshot.positionSeconds || 0)

    await TrackPlayer.skip(targetIndex, targetPosition)
    await TrackPlayer.setRepeatMode(mapRepeatMode(snapshot.repeatMode))

    const currentTrack = snapshot.queue[targetIndex] || null
    await setQueueStore(snapshot.queue)
    setActiveTrack(currentTrack)
    setPlaybackProgress(targetPosition, currentTrack?.duration || 0)
    setRepeatModeState(snapshot.repeatMode)

    await TrackPlayer.pause()
    setIsPlayingState(false)

    await persistPlaybackSession({ force: true })
  } catch (error) {
    logError("Failed to restore playback session", error)
  }
}

export async function setupPlayer() {
  try {
    if (isPlayerReady) {
      return
    }

    logInfo("Setting up track player")
    await TrackPlayer.setupPlayer({
      autoHandleInterruptions: true,
    })

    await TrackPlayer.updateOptions({
      capabilities: [
        Capability.Play,
        Capability.Pause,
        Capability.SkipToNext,
        Capability.SkipToPrevious,
        Capability.SeekTo,
      ],
      compactCapabilities: [
        Capability.Play,
        Capability.Pause,
        Capability.SkipToNext,
        Capability.SkipToPrevious,
      ],
      progressUpdateEventInterval: 0.1,
      color: processColor("#0088F6") as number,
    })

    isPlayerReady = true
    logInfo("Track player setup completed")
  } catch (error: any) {
    if (error?.message?.includes("already been initialized")) {
      isPlayerReady = true
      logInfo("Track player already initialized")
      return
    }

    logError("Track player setup failed", error)
  }
}

export async function PlaybackService() {
  TrackPlayer.addEventListener(Event.RemotePlay, () => {
    TrackPlayer.play()
  })

  TrackPlayer.addEventListener(Event.RemotePause, () => {
    TrackPlayer.pause()
  })

  TrackPlayer.addEventListener(Event.RemoteNext, () => {
    playNext()
  })

  TrackPlayer.addEventListener(Event.RemotePrevious, () => {
    playPrevious()
  })

  TrackPlayer.addEventListener(Event.RemoteSeek, (event) => {
    if (event.position !== undefined) {
      TrackPlayer.seekTo(event.position)
    }
  })

  TrackPlayer.addEventListener(Event.PlaybackState, (event) => {
    setIsPlayingState(event.state === State.Playing)
    void persistPlaybackSession()
  })

  TrackPlayer.addEventListener(Event.PlaybackTrackChanged, async () => {
    const previousTrackId = getCurrentTrackState()?.id ?? null
    await syncCurrentTrackFromPlayer()
    const currentTrack = getCurrentTrackState()
    const isTrackRepeat = getRepeatModeState() === "track"
    if (
      !currentTrack ||
      (currentTrack.id === previousTrackId && !isTrackRepeat)
    ) {
      return
    }

    await handleTrackActivated(currentTrack)
    void persistPlaybackSession({ force: true })
  })

  TrackPlayer.addEventListener(Event.PlaybackProgressUpdated, (event) => {
    setPlaybackProgress(event.position, event.duration)
    void persistPlaybackSession()
  })
}

export async function playTrack(track: Track, playlistTracks?: Track[]) {
  if (!isPlayerReady) {
    logWarn("Ignored playTrack call because player is not ready", {
      trackId: track.id,
    })
    return
  }

  try {
    logInfo("Playing track", {
      trackId: track.id,
      queueLength: playlistTracks?.length ?? getTracksState().length,
    })
    await TrackPlayer.reset()

    const tracks = playlistTracks || getTracksState()
    const selectedTrackIndex = tracks.findIndex((t) => t.id === track.id)
    const currentTrackIndex = selectedTrackIndex >= 0 ? selectedTrackIndex : 0

    const queue = tracks
      .slice(currentTrackIndex)
      .concat(tracks.slice(0, currentTrackIndex))

    await setQueueStore(queue)

    await TrackPlayer.add(queue.map(mapTrackToTrackPlayerInput))

    setActiveTrack(track)

    await TrackPlayer.play()
    setIsPlayingState(true)
    setPlaybackProgress(0, track.duration || 0)
    await handleTrackActivated(track)
    await persistPlaybackSession({ force: true })
  } catch (error) {
    logError("Failed to play track", error, { trackId: track.id })
  }
}

export async function pauseTrack() {
  try {
    logInfo("Pausing playback")
    await TrackPlayer.pause()
    setIsPlayingState(false)
    await persistPlaybackSession({ force: true })
  } catch (error) {
    logError("Failed to pause playback", error)
  }
}

export async function resumeTrack() {
  try {
    logInfo("Resuming playback")
    await TrackPlayer.play()
    setIsPlayingState(true)
    await persistPlaybackSession({ force: true })
  } catch (error) {
    logError("Failed to resume playback", error)
  }
}

export async function togglePlayback() {
  const state = await TrackPlayer.getState()
  if (state === State.Playing) {
    await pauseTrack()
  } else {
    await resumeTrack()
  }
}

export async function playNext() {
  try {
    logInfo("Skipping to next track")
    await TrackPlayer.skipToNext()
    await persistPlaybackSession({ force: true })
  } catch (error) {
    logWarn("Failed to skip to next track, falling back to queue restart", {
      error: error instanceof Error ? error.message : String(error),
    })
    const queue = getQueueState()
    if (queue.length > 0) {
      await playTrack(queue[0], queue)
    }
  }
}

export async function playPrevious() {
  try {
    const position = await TrackPlayer.getPosition()
    if (position > 3) {
      logInfo("Restarting current track from beginning")
      await TrackPlayer.seekTo(0)
    } else {
      logInfo("Skipping to previous track")
      await TrackPlayer.skipToPrevious()
      await persistPlaybackSession({ force: true })
    }
  } catch (error) {
    logError("Failed to play previous track", error)
  }
}

export async function seekTo(seconds: number) {
  try {
    logInfo("Seeking playback", { seconds })
    await TrackPlayer.seekTo(seconds)
    setPlaybackProgress(seconds, usePlayerStore.getState().duration)
    await persistPlaybackSession({ force: true })
  } catch (error) {
    logError("Failed to seek playback", error, { seconds })
  }
}

export async function setRepeatMode(mode: RepeatModeType) {
  try {
    logInfo("Updating repeat mode", { mode })
    await TrackPlayer.setRepeatMode(mapRepeatMode(mode))
    setRepeatModeState(mode)
    await persistPlaybackSession({ force: true })
  } catch (error) {
    logError("Failed to update repeat mode", error, { mode })
  }
}

export async function toggleRepeatMode() {
  const currentMode = getRepeatModeState()
  const nextMode: RepeatModeType =
    currentMode === "off" ? "track" : currentMode === "track" ? "queue" : "off"
  await setRepeatMode(nextMode)
}

export function toggleFavorite(trackId: string) {
  const tracks = getTracksState()
  const index = tracks.findIndex((track) => track.id === trackId)
  if (index === -1) {
    return
  }

  const track = tracks[index]
  if (!track) {
    return
  }

  const newStatus = !track.isFavorite
  const newTracks = [...tracks]
  newTracks[index] = { ...track, isFavorite: newStatus }
  setTracksState(newTracks)

  const current = getCurrentTrackState()
  if (current?.id === trackId) {
    setActiveTrack({ ...current, isFavorite: newStatus })
  }

  void setTrackFavoriteFlag(trackId, newStatus).then(async () => {
    await invalidateFavoriteQueries(queryClient)
  })
}

export async function loadTracks() {
  try {
    const { getAllTracks } = await import("./player.repository")
    const trackList = await getAllTracks()
    setTracksState(trackList)
    logInfo("Loaded tracks into player store", { trackCount: trackList.length })
  } catch (error) {
    logError("Failed to load tracks into player store", error)
  }
}
