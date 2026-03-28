import { logError, logInfo, logWarn } from "@/modules/logging/logging.service"
import { persistPlaybackSession } from "@/modules/player/player-session.service"
import type { RepeatModeType } from "@/modules/player/player.types"
import { mapRepeatMode } from "@/modules/player/player-adapter"
import { setPlaybackProgress } from "@/modules/player/player-runtime-state"
import { State, TrackPlayer } from "@/modules/player/player.utils"

import { playTrack } from "./player.service"
import {
  getQueueState,
  getRepeatModeState,
  setIsPlayingState,
  setRepeatModeState,
  usePlayerStore,
} from "./player.store"

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
