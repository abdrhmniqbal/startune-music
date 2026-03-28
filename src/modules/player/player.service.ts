import { processColor } from "react-native"

import { queryClient } from "@/lib/tanstack-query"
import { invalidateFavoriteQueries } from "@/modules/favorites/favorites.keys"
import { setTrackFavoriteFlag } from "@/modules/favorites/favorites.repository"
import { logError, logInfo, logWarn } from "@/modules/logging/logging.service"
import { persistPlaybackSession } from "@/modules/player/player-session.service"
import type { Track } from "@/modules/player/player.types"
import { mapTrackToTrackPlayerInput } from "@/modules/player/player-adapter"
import { setActiveTrack, setPlaybackProgress } from "@/modules/player/player-runtime-state"
import { handleTrackActivated } from "@/modules/player/player-activity.service"

import { Capability, TrackPlayer } from "@/modules/player/player.utils"

import {
  getCurrentTrackState,
  getTracksState,
  setIsPlayingState,
  setTracksState,
} from "./player.store"

let isPlayerReady = false

async function setQueueStore(tracks: Track[]): Promise<void> {
  const { setQueue } = await import("./queue.store")
  setQueue(tracks)
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
