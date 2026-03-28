import type {
  Album,
  Artist,
  LyricLine,
  RepeatModeType,
  Track,
} from "./player.types"
import { create } from "zustand"

export type { Album, Artist, LyricLine, RepeatModeType, Track }

interface PlayerState {
  tracks: Track[]
  currentTrack: Track | null
  isPlaying: boolean
  currentTime: number
  duration: number
  playbackRefreshVersion: number
  repeatMode: RepeatModeType
  queue: Track[]
  originalQueue: Track[]
  isShuffled: boolean
}

export const usePlayerStore = create<PlayerState>(() => ({
  tracks: [],
  currentTrack: null,
  isPlaying: false,
  currentTime: 0,
  duration: 0,
  playbackRefreshVersion: 0,
  repeatMode: "off",
  queue: [],
  originalQueue: [],
  isShuffled: false,
}))

export const $tracks = {
  get: () => usePlayerStore.getState().tracks,
  set: (value: Track[]) => usePlayerStore.setState({ tracks: value }),
}

export const $currentTrack = {
  get: () => usePlayerStore.getState().currentTrack,
  set: (value: Track | null) => usePlayerStore.setState({ currentTrack: value }),
}

export const $isPlaying = {
  get: () => usePlayerStore.getState().isPlaying,
  set: (value: boolean) => usePlayerStore.setState({ isPlaying: value }),
}

export const $currentTime = {
  get: () => usePlayerStore.getState().currentTime,
  set: (value: number) => {
    if (usePlayerStore.getState().currentTime === value) {
      return
    }
    usePlayerStore.setState({ currentTime: value })
  },
}

export const $duration = {
  get: () => usePlayerStore.getState().duration,
  set: (value: number) => {
    if (usePlayerStore.getState().duration === value) {
      return
    }
    usePlayerStore.setState({ duration: value })
  },
}

export const $playbackRefreshVersion = {
  get: () => usePlayerStore.getState().playbackRefreshVersion,
  set: (value: number) =>
    usePlayerStore.setState({ playbackRefreshVersion: value }),
}

export const $repeatMode = {
  get: () => usePlayerStore.getState().repeatMode,
  set: (value: RepeatModeType) => usePlayerStore.setState({ repeatMode: value }),
}

export const $queue = {
  get: () => usePlayerStore.getState().queue,
  set: (value: Track[]) => usePlayerStore.setState({ queue: value }),
}

export const $originalQueue = {
  get: () => usePlayerStore.getState().originalQueue,
  set: (value: Track[]) => usePlayerStore.setState({ originalQueue: value }),
}

export const $isShuffled = {
  get: () => usePlayerStore.getState().isShuffled,
  set: (value: boolean) => usePlayerStore.setState({ isShuffled: value }),
}

export {
  loadTracks,
  pauseTrack,
  PlaybackService,
  persistPlaybackSession,
  playNext,
  playPrevious,
  playTrack,
  restorePlaybackSession,
  resumeTrack,
  seekTo,
  setRepeatMode,
  setupPlayer,
  syncCurrentTrackFromPlayer,
  toggleFavorite,
  togglePlayback,
  toggleRepeatMode,
} from "./player.service"
