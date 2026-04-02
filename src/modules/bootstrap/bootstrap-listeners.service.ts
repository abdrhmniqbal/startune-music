import * as MediaLibrary from "expo-media-library"
import {
  AppState,
  InteractionManager,
  type AppStateStatus,
} from "react-native"

import { runAutoScan } from "@/modules/bootstrap/bootstrap.runtime"
import { logInfo } from "@/modules/logging/logging.service"

const FOREGROUND_AUTO_SCAN_DELAY_MS = 1500
const LONG_BACKGROUND_THRESHOLD_MS = 2 * 60 * 1000
const LONG_BACKGROUND_AUTO_SCAN_DELAY_MS = 12 * 1000

export function shouldTriggerAutoScanOnMediaLibraryEvent(
  event: MediaLibrary.MediaLibraryAssetsChangeEvent
) {
  return (
    event.hasIncrementalChanges === false ||
    (event.deletedAssets?.length ?? 0) > 0
  )
}

export function registerBootstrapListeners() {
  logInfo("Registering bootstrap listeners")
  let previousState: AppStateStatus = AppState.currentState
  let backgroundedAt: number | null = null
  let pendingForegroundAutoScanTimeout: ReturnType<typeof setTimeout> | null = null
  let pendingInteractionHandle: ReturnType<
    typeof InteractionManager.runAfterInteractions
  > | null = null

  const clearPendingForegroundAutoScan = () => {
    if (pendingForegroundAutoScanTimeout) {
      clearTimeout(pendingForegroundAutoScanTimeout)
      pendingForegroundAutoScanTimeout = null
    }

    pendingInteractionHandle?.cancel()
    pendingInteractionHandle = null
  }

  const scheduleForegroundAutoScan = (delayMs: number) => {
    clearPendingForegroundAutoScan()

    pendingForegroundAutoScanTimeout = setTimeout(() => {
      pendingForegroundAutoScanTimeout = null
      pendingInteractionHandle = InteractionManager.runAfterInteractions(() => {
        pendingInteractionHandle = null
        logInfo("Foreground auto scan scheduled run started", { delayMs })
        void runAutoScan()
      })
    }, delayMs)
  }

  const appStateSubscription = AppState.addEventListener("change", (nextState) => {
    if (nextState === "background" || nextState === "inactive") {
      backgroundedAt = Date.now()
      clearPendingForegroundAutoScan()
    }

    const isReturningToForeground =
      (previousState === "background" || previousState === "inactive") &&
      nextState === "active"
    previousState = nextState

    if (!isReturningToForeground) {
      return
    }

    const timeInBackgroundMs =
      backgroundedAt === null ? 0 : Date.now() - backgroundedAt
    const isLongBackgroundSession =
      timeInBackgroundMs >= LONG_BACKGROUND_THRESHOLD_MS
    const delayMs = isLongBackgroundSession
      ? LONG_BACKGROUND_AUTO_SCAN_DELAY_MS
      : FOREGROUND_AUTO_SCAN_DELAY_MS

    logInfo("App returned to foreground, scheduling auto scan", {
      timeInBackgroundMs,
      isLongBackgroundSession,
      delayMs,
    })
    scheduleForegroundAutoScan(delayMs)
  })

  const mediaLibrarySubscription = MediaLibrary.addListener((event) => {
    const bypassThrottle = shouldTriggerAutoScanOnMediaLibraryEvent(event)
    logInfo("Media library changed, running auto scan", {
      bypassThrottle,
      hasIncrementalChanges: event.hasIncrementalChanges,
      deletedAssetsCount: event.deletedAssets?.length ?? 0,
      insertedAssetsCount: event.insertedAssets?.length ?? 0,
    })
    void runAutoScan({
      bypassThrottle,
    })
  })

  return () => {
    logInfo("Unregistering bootstrap listeners")
    clearPendingForegroundAutoScan()
    appStateSubscription.remove()
    mediaLibrarySubscription.remove()
  }
}
