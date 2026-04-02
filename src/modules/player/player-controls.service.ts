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
import { syncCurrentTrackFromPlayer } from "./player-session.service"

export async function pauseTrack() {
  try {
    logInfo("Pausing playback")
    await TrackPlayer.pause()
    setIsPlayingState(false)
    await persistPlaybackSession({ force: true })
    logInfo("Playback paused")
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
    logInfo("Playback resumed")
  } catch (error) {
    logError("Failed to resume playback", error)
  }
}

export async function togglePlayback() {
  try {
    const state = await TrackPlayer.getState()
    logInfo("Toggling playback", {
      currentState: state,
    })

    if (state === State.Playing) {
      await pauseTrack()
      return
    }

    await resumeTrack()
  } catch (error) {
    logError("Failed to toggle playback", error)
  }
}

export async function playNext() {
  try {
    const [activeIndex, nativeQueue] = await Promise.all([
      TrackPlayer.getCurrentTrack(),
      TrackPlayer.getQueue(),
    ])

    if (
      activeIndex === null ||
      activeIndex < 0 ||
      nativeQueue.length === 0
    ) {
      logWarn("Skipped next track because no active queue item is available")
      return
    }

    if (activeIndex >= nativeQueue.length - 1) {
      if (getRepeatModeState() === "queue" && nativeQueue.length > 0) {
        logInfo("Wrapping to first track in queue")
        await TrackPlayer.skip(0)
        await syncCurrentTrackFromPlayer()
        await persistPlaybackSession({ force: true })
        logInfo("Wrapped to first track in queue")
      } else {
        logInfo("Ignored next track because queue is already at the end", {
          activeIndex,
          queueLength: nativeQueue.length,
        })
      }
      return
    }

    logInfo("Skipping to next track")
    await TrackPlayer.skipToNext()
    await syncCurrentTrackFromPlayer()
    await persistPlaybackSession({ force: true })
    logInfo("Skipped to next track")
  } catch (error) {
    logWarn("Failed to skip to next track, falling back to queue restart", {
      error: error instanceof Error ? error.message : String(error),
    })
    const queue = getQueueState()
    if (queue.length > 0) {
      await playTrack(queue[0], queue)
      logInfo("Recovered playback by restarting queue from first track")
    }
  }
}

export async function playPrevious() {
  try {
    logInfo("Playing previous track")
    const position = await TrackPlayer.getPosition()
    if (position > 3) {
      logInfo("Restarting current track from beginning")
      await TrackPlayer.seekTo(0)
      logInfo("Restarted current track from beginning")
    } else {
      logInfo("Skipping to previous track")
      await TrackPlayer.skipToPrevious()
      await syncCurrentTrackFromPlayer()
      await persistPlaybackSession({ force: true })
      logInfo("Skipped to previous track")
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
    logInfo("Playback seek completed", { seconds })
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
    logInfo("Repeat mode updated", { mode })
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
