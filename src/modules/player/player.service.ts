import { processColor } from "react-native"

import { logError, logInfo, logWarn } from "@/modules/logging/logging.service"
import { persistPlaybackSession } from "@/modules/player/player-session.service"
import type { Track } from "@/modules/player/player.types"
import { mapTrackToTrackPlayerInput } from "@/modules/player/player-adapter"
import { setActiveTrack, setPlaybackProgress } from "@/modules/player/player-runtime-state"
import { handleTrackActivated } from "@/modules/player/player-activity.service"

import {
  AndroidAudioContentType,
  AppKilledPlaybackBehavior,
  Capability,
  IOSCategory,
  TrackPlayer,
} from "@/modules/player/player.utils"

import {
  setImmediateQueueTrackIdsState,
  getTracksState,
  setIsShuffledState,
  setOriginalQueueState,
  setOriginalQueueTrackIdsState,
  setIsPlayingState,
  setQueueState,
  setQueueTrackIdsState,
} from "./player.store"

let isPlayerReady = false

export async function setupPlayer() {
  try {
    if (isPlayerReady) {
      return
    }

    logInfo("Setting up track player")
    await TrackPlayer.setupPlayer({
      autoHandleInterruptions: true,
      androidAudioContentType: AndroidAudioContentType.Music,
      iosCategory: IOSCategory.Playback,
    })

    await TrackPlayer.updateOptions({
      android: {
        appKilledPlaybackBehavior: AppKilledPlaybackBehavior.ContinuePlayback,
        stopForegroundGracePeriod: 30,
      },
      capabilities: [
        Capability.Play,
        Capability.Pause,
        Capability.SkipToNext,
        Capability.SkipToPrevious,
        Capability.SeekTo,
      ],
      notificationCapabilities: [
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
      progressUpdateEventInterval: 0.5,
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
    const queueTrackIds = queue.map((item) => item.id)

    setQueueState(queue)
    setOriginalQueueState(queue)
    setQueueTrackIdsState(queueTrackIds)
    setOriginalQueueTrackIdsState(queueTrackIds)
    setImmediateQueueTrackIdsState([])
    setIsShuffledState(false)

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
