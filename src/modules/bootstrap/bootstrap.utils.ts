import { count } from "drizzle-orm"

import {
  initializeTrackPlayer,
  registerPlaybackService,
} from "@/core/audio/track-player.service"
import {
  getMediaLibraryPermission,
  requestMediaLibraryPermission,
} from "@/core/storage/media-library.service"
import { db } from "@/db/client"
import { tracks } from "@/db/schema"
import { ensureAutoScanConfigLoaded } from "@/modules/settings/auto-scan"
import { ensureFolderFilterConfigLoaded } from "@/modules/settings/folder-filters"
import { ensureTrackDurationFilterConfigLoaded } from "@/modules/settings/track-duration-filter"
import { startIndexing } from "@/modules/indexer/indexer.service"
import { ensureLoggingConfigLoaded } from "@/modules/logging/logging.store"
import { logInfo } from "@/modules/logging/logging.service"
import { restorePlaybackSession } from "@/modules/player/player-session.service"

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

  const permission = await getMediaLibraryPermission()
  const status =
    permission.status === "undetermined" && permission.canAskAgain
      ? (await requestMediaLibraryPermission()).status
      : permission.status
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

    // Only show startup index progress on the very first full scan.
    // Incremental bootstrap scans should stay silent.
    void startIndexing(isFreshDatabase, isFreshDatabase)
  }
}
