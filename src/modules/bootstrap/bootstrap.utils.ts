import { count } from "drizzle-orm"

import {
  initializeTrackPlayer,
  registerPlaybackService,
} from "@/core/audio/track-player.service"
import { requestMediaLibraryPermission } from "@/core/storage/media-library.service"
import { db } from "@/db/client"
import { tracks } from "@/db/schema"
import { ensureAutoScanConfigLoaded } from "@/modules/indexer/auto-scan"
import { ensureFolderFilterConfigLoaded } from "@/modules/indexer/folder-filters"
import { ensureTrackDurationFilterConfigLoaded } from "@/modules/indexer/track-duration-filter"
import { startIndexing } from "@/modules/indexer/indexer.service"
import { ensureLoggingConfigLoaded } from "@/modules/logging/logging.store"
import { logInfo } from "@/modules/logging/logging.service"
import { restorePlaybackSession } from "@/modules/player/player.service"

async function preloadLocalSettings() {
  logInfo("Preloading local settings")
  await Promise.all([
    ensureAutoScanConfigLoaded(),
    ensureFolderFilterConfigLoaded(),
    ensureTrackDurationFilterConfigLoaded(),
    ensureLoggingConfigLoaded(),
  ])
}

export async function bootstrapApp(): Promise<void> {
  logInfo("Registering playback service")
  registerPlaybackService()
  logInfo("Initializing track player")
  await initializeTrackPlayer()
  logInfo("Restoring playback session")
  await restorePlaybackSession()
  await preloadLocalSettings()

  const { status } = await requestMediaLibraryPermission()
  logInfo("Media library permission resolved during bootstrap", { status })
  if (status === "granted") {
    const isAutoScanEnabled = await ensureAutoScanConfigLoaded()
    if (!isAutoScanEnabled) {
      logInfo("Auto scan disabled during bootstrap")
      return
    }

    const result = await db.select({ value: count() }).from(tracks)

    const trackCount = result[0]?.value ?? 0
    const isFreshDatabase = trackCount === 0
    logInfo("Scheduling bootstrap index run", {
      trackCount,
      isFreshDatabase,
    })

    // Auto-index on app bootstrap when enabled; force a full scan only for fresh databases.
    // Uses indexer store so progress UI is shown.
    void startIndexing(isFreshDatabase, true)
  }
}
