import { logError, logInfo } from "@/modules/logging/logging.service"
import { getAllTracks } from "@/modules/player/player.repository"

import { setTracksState } from "./player.store"

export async function loadTracks() {
  try {
    const trackList = await getAllTracks()
    setTracksState(trackList)
    logInfo("Loaded tracks into player store", { trackCount: trackList.length })
  } catch (error) {
    logError("Failed to load tracks into player store", error)
  }
}
