import type { Album, Artist, LyricLine, Track } from "./player.types"
import { atom } from "nanostores"

import { processColor } from "react-native"
import { queryClient } from "@/lib/tanstack-query"
import { toggleFavoriteDB } from "@/modules/favorites/favorites.api"
import {
  addTrackToHistory,
  incrementTrackPlayCount,
} from "@/modules/history/history.api"
import {
  loadPlaybackSession,
  savePlaybackSession,
} from "@/modules/player/player-session"

import {
  Capability,
  Event,
  RepeatMode,
  State,
  TrackPlayer,
} from "@/modules/player/player.utils"

export type { Album, Artist, LyricLine, Track }

export const $tracks = atom<Track[]>([])
export const $currentTrack = atom<Track | null>(null)
export const $isPlaying = atom(false)
export const $currentTime = atom(0)
export const $duration = atom(0)
export const $playbackRefreshVersion = atom(0)

export type RepeatModeType = "off" | "track" | "queue"
export const $repeatMode = atom<RepeatModeType>("off")

let isPlayerReady = false
const MIN_SESSION_SAVE_INTERVAL_MS = 2000
let lastPlaybackSessionSavedAt = 0
const RECENTLY_PLAYED_SCREEN_LIMIT = 50
const HOME_RECENTLY_PLAYED_LIMIT = 8

function bumpPlaybackRefreshVersion() {
  $playbackRefreshVersion.set($playbackRefreshVersion.get() + 1)
}

async function invalidatePlaybackQueries() {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: ["recently-played-screen"] }),
    queryClient.invalidateQueries({ queryKey: ["home", "recently-played"] }),
    queryClient.invalidateQueries({ queryKey: ["home", "top-tracks"] }),
    queryClient.invalidateQueries({ queryKey: ["top-tracks-screen"] }),
    queryClient.invalidateQueries({ queryKey: ["tracks"] }),
  ])
}

function prependUniqueTrack(
  current: Track[] | undefined,
  track: Track,
  limit: number
): Track[] {
  const existing = current ?? []
  return [track, ...existing.filter((item) => item.id !== track.id)].slice(
    0,
    limit
  )
}

function optimisticallyUpdateRecentlyPlayedQueries(track: Track) {
  queryClient.setQueryData<Track[]>(["recently-played-screen"], (previous) =>
    prependUniqueTrack(previous, track, RECENTLY_PLAYED_SCREEN_LIMIT)
  )
  queryClient.setQueryData<Track[]>(
    ["home", "recently-played", HOME_RECENTLY_PLAYED_LIMIT],
    (previous) =>
      prependUniqueTrack(previous, track, HOME_RECENTLY_PLAYED_LIMIT)
  )
}

function mapTrackPlayerTrackToTrack(track: any): Track {
  return {
    ...$tracks.get().find((item) => item.id === String(track.id)),
    id: String(track.id),
    title: typeof track.title === "string" ? track.title : "Unknown Track",
    artist: track.artist,
    album: track.album,
    duration: track.duration || 0,
    uri: track.url as string,
    image: track.artwork as string | undefined,
  }
}

export async function syncCurrentTrackFromPlayer(): Promise<void> {
  try {
    const activeIndex = await TrackPlayer.getCurrentTrack()
    if (activeIndex !== null && activeIndex >= 0) {
      const { $queue } = await import("./queue.store")
      const queueTrack = $queue.get()[activeIndex]
      if (queueTrack) {
        $currentTrack.set(queueTrack)
        $duration.set(queueTrack.duration || 0)
        return
      }
    }

    const activeTrack = await TrackPlayer.getActiveTrack()
    if (!activeTrack) {
      return
    }

    const mappedTrack = mapTrackPlayerTrackToTrack(activeTrack)
    $currentTrack.set(mappedTrack)
    $duration.set(mappedTrack.duration || 0)
  } catch {}
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
        : ($currentTrack.get()?.id ?? null)

    await savePlaybackSession({
      queue,
      currentTrackId,
      positionSeconds,
      repeatMode: $repeatMode.get(),
      wasPlaying: $isPlaying.get(),
      savedAt: now,
    })
    lastPlaybackSessionSavedAt = now
  } catch {}
}

export async function restorePlaybackSession(): Promise<void> {
  if (!isPlayerReady) {
    return
  }

  try {
    const nativeQueue = await TrackPlayer.getQueue()

    if (nativeQueue.length > 0) {
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
        $currentTrack.set(mappedQueue[currentIndex] || null)
      } else {
        $currentTrack.set(mappedQueue[0] || null)
      }

      const [position, playbackState, repeatMode] = await Promise.all([
        TrackPlayer.getPosition(),
        TrackPlayer.getState(),
        TrackPlayer.getRepeatMode(),
      ])

      $currentTime.set(position)
      $duration.set($currentTrack.get()?.duration || 0)
      $isPlaying.set(playbackState === State.Playing)
      $repeatMode.set(mapTrackPlayerRepeatMode(repeatMode))
      await persistPlaybackSession({ force: true })
      return
    }

    const snapshot = await loadPlaybackSession()
    if (!snapshot || snapshot.queue.length === 0) {
      return
    }

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
    $currentTrack.set(currentTrack)
    $currentTime.set(targetPosition)
    $duration.set(currentTrack?.duration || 0)
    $repeatMode.set(snapshot.repeatMode)

    // Do not auto-play from persisted snapshot state.
    // Auto-resume should only happen when playback is already active in the
    // native player process (handled by the nativeQueue branch above).
    await TrackPlayer.pause()
    $isPlaying.set(false)

    await persistPlaybackSession({ force: true })
  } catch {}
}

export async function setupPlayer() {
  try {
    // Check if player is already initialized
    if (isPlayerReady) {
      return
    }

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
  } catch (e: any) {
    // If already initialized, mark as ready
    if (e?.message?.includes("already been initialized")) {
      isPlayerReady = true
    }
  }
}

// Playback service for background controls
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
    $isPlaying.set(event.state === State.Playing)
    void persistPlaybackSession()
  })

  // v4 API: keep UI metadata synced from active track source of truth.
  TrackPlayer.addEventListener(Event.PlaybackTrackChanged, async () => {
    const previousTrackId = $currentTrack.get()?.id ?? null
    await syncCurrentTrackFromPlayer()
    const currentTrack = $currentTrack.get()
    const isTrackRepeat = $repeatMode.get() === "track"
    if (
      !currentTrack ||
      (currentTrack.id === previousTrackId && !isTrackRepeat)
    ) {
      return
    }

    optimisticallyUpdateRecentlyPlayedQueries(currentTrack)
    await Promise.allSettled([
      addTrackToHistory(currentTrack.id),
      incrementTrackPlayCount(currentTrack.id),
    ])
    bumpPlaybackRefreshVersion()
    void invalidatePlaybackQueries()
    void persistPlaybackSession({ force: true })
  })

  TrackPlayer.addEventListener(Event.PlaybackProgressUpdated, (event) => {
    $currentTime.set(event.position)
    $duration.set(event.duration)
    void persistPlaybackSession()
  })
}

export async function playTrack(track: Track, playlistTracks?: Track[]) {
  if (!isPlayerReady) {
    return
  }

  try {
    await TrackPlayer.reset()

    const tracks = playlistTracks || $tracks.get()
    const selectedTrackIndex = tracks.findIndex((t) => t.id === track.id)
    const currentTrackIndex = selectedTrackIndex >= 0 ? selectedTrackIndex : 0

    const queue = tracks
      .slice(currentTrackIndex)
      .concat(tracks.slice(0, currentTrackIndex))

    await setQueueStore(queue)

    await TrackPlayer.add(queue.map(mapTrackToTrackPlayerInput))

    $currentTrack.set(track)
    optimisticallyUpdateRecentlyPlayedQueries(track)

    await TrackPlayer.play()
    $isPlaying.set(true)
    $currentTime.set(0)
    $duration.set(track.duration || 0)
    await Promise.allSettled([
      addTrackToHistory(track.id),
      incrementTrackPlayCount(track.id),
    ])
    bumpPlaybackRefreshVersion()
    void invalidatePlaybackQueries()
    await persistPlaybackSession({ force: true })
  } catch {}
}

export async function pauseTrack() {
  try {
    await TrackPlayer.pause()
    $isPlaying.set(false)
    await persistPlaybackSession({ force: true })
  } catch {}
}

export async function resumeTrack() {
  try {
    await TrackPlayer.play()
    $isPlaying.set(true)
    await persistPlaybackSession({ force: true })
  } catch {}
}

export async function togglePlayback() {
  // v4 API: getState() returns the state directly
  const state = await TrackPlayer.getState()
  if (state === State.Playing) {
    await pauseTrack()
  } else {
    await resumeTrack()
  }
}

export async function playNext() {
  try {
    await TrackPlayer.skipToNext()
    await syncCurrentTrackFromPlayer()
    const newTrack = $currentTrack.get()
    if (newTrack) {
      optimisticallyUpdateRecentlyPlayedQueries(newTrack)
      await Promise.allSettled([
        addTrackToHistory(newTrack.id),
        incrementTrackPlayCount(newTrack.id),
      ])
      bumpPlaybackRefreshVersion()
      void invalidatePlaybackQueries()
    }
    await persistPlaybackSession({ force: true })
  } catch {
    const { $queue } = await import("./queue.store")
    const queue = $queue.get()
    if (queue.length > 0) {
      await playTrack(queue[0], queue)
    }
  }
}

export async function playPrevious() {
  try {
    const position = await TrackPlayer.getPosition()
    if (position > 3) {
      await TrackPlayer.seekTo(0)
    } else {
      await TrackPlayer.skipToPrevious()
      await syncCurrentTrackFromPlayer()
      const newTrack = $currentTrack.get()
      if (newTrack) {
        optimisticallyUpdateRecentlyPlayedQueries(newTrack)
        await Promise.allSettled([
          addTrackToHistory(newTrack.id),
          incrementTrackPlayCount(newTrack.id),
        ])
        bumpPlaybackRefreshVersion()
        void invalidatePlaybackQueries()
      }
      await persistPlaybackSession({ force: true })
    }
  } catch {}
}

export async function seekTo(seconds: number) {
  try {
    await TrackPlayer.seekTo(seconds)
    $currentTime.set(seconds)
    await persistPlaybackSession({ force: true })
  } catch {}
}

export async function setRepeatMode(mode: RepeatModeType) {
  try {
    await TrackPlayer.setRepeatMode(mapRepeatMode(mode))
    $repeatMode.set(mode)
    await persistPlaybackSession({ force: true })
  } catch {}
}

export async function toggleRepeatMode() {
  const currentMode = $repeatMode.get()
  const nextMode: RepeatModeType =
    currentMode === "off" ? "track" : currentMode === "track" ? "queue" : "off"
  await setRepeatMode(nextMode)
}

export function toggleFavorite(trackId: string) {
  const tracks = $tracks.get()
  const index = tracks.findIndex((t) => t.id === trackId)
  if (index === -1) return

  const track = tracks[index]
  const newStatus = !track.isFavorite

  // Create new array reference for immutability
  const newTracks = [...tracks]
  newTracks[index] = { ...track, isFavorite: newStatus }
  $tracks.set(newTracks)

  const current = $currentTrack.get()
  if (current?.id === trackId) {
    $currentTrack.set({ ...current, isFavorite: newStatus })
  }

  void toggleFavoriteDB(trackId, newStatus).then(async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ["favorites"] }),
      queryClient.invalidateQueries({ queryKey: ["library", "favorites"] }),
      queryClient.invalidateQueries({ queryKey: ["tracks"] }),
      queryClient.invalidateQueries({ queryKey: ["library", "tracks"] }),
    ])
  })
}

export async function setQueue(tracks: Track[]) {
  $tracks.set(tracks)
}

// Load tracks from database
export async function loadTracks() {
  try {
    const { getAllTracks } = await import("@/modules/player/player.api")
    const trackList = await getAllTracks()
    $tracks.set(trackList)
  } catch {}
}
