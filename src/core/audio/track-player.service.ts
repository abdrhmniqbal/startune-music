import {
  setupPlayer,
} from "@/modules/player/player.service"
import { logError, logInfo, logWarn } from "@/modules/logging/logging.service"
import { PlaybackService } from "@/modules/player/player-events.service"
import { TrackPlayer } from "@/modules/player/player.utils"

let isPlaybackServiceRegistered = false

export function registerPlaybackService(): void {
  if (isPlaybackServiceRegistered) {
    logInfo("Playback service registration skipped because it is already registered")
    return
  }

  logInfo("Registering TrackPlayer playback service")
  try {
    TrackPlayer.registerPlaybackService(() => PlaybackService)
    isPlaybackServiceRegistered = true
    logInfo("TrackPlayer playback service registered")
  } catch {
    // TrackPlayer throws when service is already registered.
    isPlaybackServiceRegistered = true
    logWarn("TrackPlayer playback service registration reported existing registration")
  }
}

export async function initializeTrackPlayer(): Promise<void> {
  try {
    logInfo("Starting TrackPlayer setup")
    await setupPlayer()
    logInfo("TrackPlayer setup completed")
  } catch (error) {
    logError("TrackPlayer setup failed", error)
    throw error
  }
}
