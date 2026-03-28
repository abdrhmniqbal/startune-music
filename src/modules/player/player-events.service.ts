import { logError } from "@/modules/logging/logging.service"
import {
  pauseTrack,
  playNext,
  playPrevious,
  resumeTrack,
  seekTo,
} from "@/modules/player/player-controls.service"
import { setPlaybackProgress } from "@/modules/player/player-runtime-state"
import {
  persistPlaybackSession,
  syncCurrentTrackFromPlayer,
} from "@/modules/player/player-session.service"
import { Event, State, TrackPlayer } from "@/modules/player/player.utils"

import { handleTrackActivated } from "./player-activity.service"
import {
  getCurrentTrackState,
  getRepeatModeState,
  setIsPlayingState,
} from "./player.store"

export async function PlaybackService() {
  TrackPlayer.addEventListener(Event.RemotePlay, () => {
    void resumeTrack()
  })

  TrackPlayer.addEventListener(Event.RemotePause, () => {
    void pauseTrack()
  })

  TrackPlayer.addEventListener(Event.RemoteNext, () => {
    void playNext()
  })

  TrackPlayer.addEventListener(Event.RemotePrevious, () => {
    void playPrevious()
  })

  TrackPlayer.addEventListener(Event.RemoteSeek, (event) => {
    if (event.position !== undefined) {
      void seekTo(event.position)
    }
  })

  TrackPlayer.addEventListener(Event.PlaybackState, (event) => {
    setIsPlayingState(event.state === State.Playing)
    void persistPlaybackSession()
  })

  TrackPlayer.addEventListener(Event.PlaybackTrackChanged, async () => {
    try {
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
    } catch (error) {
      logError("Failed to handle playback track change", error)
    }
  })

  TrackPlayer.addEventListener(Event.PlaybackProgressUpdated, (event) => {
    setPlaybackProgress(event.position, event.duration)
    void persistPlaybackSession()
  })
}
