import {
  PlaybackService,
  setupPlayer,
} from "@/modules/player/player.service"
import { TrackPlayer } from "@/modules/player/player.utils"

let isPlaybackServiceRegistered = false

export function registerPlaybackService(): void {
  if (isPlaybackServiceRegistered) {
    return
  }

  try {
    TrackPlayer.registerPlaybackService(() => PlaybackService)
    isPlaybackServiceRegistered = true
  } catch {
    // TrackPlayer throws when service is already registered.
    isPlaybackServiceRegistered = true
  }
}

export async function initializeTrackPlayer(): Promise<void> {
  await setupPlayer()
}
