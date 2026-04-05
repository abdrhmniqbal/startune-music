import * as MediaLibrary from "expo-media-library"
import {
  AppState,
  InteractionManager,
  type AppStateStatus,
} from "react-native"

import { runAutoScan } from "@/modules/bootstrap/bootstrap.runtime"
import {
  isExtraLoggingEnabled,
  logInfo,
} from "@/modules/logging/logging.service"

const FOREGROUND_AUTO_SCAN_DELAY_MS = 1500
const MEDIA_EVENT_AUTO_SCAN_DELAY_MS = 1500
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
  let pendingDeferredMediaAutoScan = false
  let pendingDeferredMediaAutoScanBypassThrottle = false

  const clearPendingForegroundAutoScan = () => {
    if (pendingForegroundAutoScanTimeout) {
      clearTimeout(pendingForegroundAutoScanTimeout)
      pendingForegroundAutoScanTimeout = null
    }

    pendingInteractionHandle?.cancel()
    pendingInteractionHandle = null
  }

  const scheduleForegroundAutoScan = (options: {
    delayMs: number
    bypassThrottle?: boolean
    source: "foreground" | "media-library"
  }) => {
    const delayMs = options.delayMs
    const bypassThrottle = options.bypassThrottle ?? false
    clearPendingForegroundAutoScan()

    pendingForegroundAutoScanTimeout = setTimeout(() => {
      pendingForegroundAutoScanTimeout = null
      pendingInteractionHandle = InteractionManager.runAfterInteractions(() => {
        pendingInteractionHandle = null
        logInfo("Deferred auto scan scheduled run started", {
          delayMs,
          source: options.source,
          bypassThrottle,
        })
        void runAutoScan({ bypassThrottle })
      })
    }, delayMs)
  }

  const appStateSubscription = AppState.addEventListener("change", (nextState) => {
    if (nextState === "background") {
      backgroundedAt = Date.now()
      pendingDeferredMediaAutoScan = false
      pendingDeferredMediaAutoScanBypassThrottle = false
      clearPendingForegroundAutoScan()
    }

    const isReturningToForeground =
      previousState === "background" && nextState === "active"
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
      hasDeferredMediaAutoScan: pendingDeferredMediaAutoScan,
    })

    if (pendingDeferredMediaAutoScan) {
      scheduleForegroundAutoScan({
        delayMs,
        source: "media-library",
        bypassThrottle: pendingDeferredMediaAutoScanBypassThrottle,
      })
      pendingDeferredMediaAutoScan = false
      pendingDeferredMediaAutoScanBypassThrottle = false
      return
    }

    scheduleForegroundAutoScan({ delayMs, source: "foreground" })
  })

  const mediaLibrarySubscription = MediaLibrary.addListener((event) => {
    const bypassThrottle = shouldTriggerAutoScanOnMediaLibraryEvent(event)
    const appState = AppState.currentState

    if (appState !== "active") {
      pendingDeferredMediaAutoScan = true
      pendingDeferredMediaAutoScanBypassThrottle =
        pendingDeferredMediaAutoScanBypassThrottle || bypassThrottle

      if (isExtraLoggingEnabled()) {
        logInfo("Media library changed while app not active, deferring auto scan", {
          appState,
          bypassThrottle,
        })
      }
      return
    }

    if (isExtraLoggingEnabled()) {
      logInfo("Media library changed, running auto scan", {
        bypassThrottle,
        hasIncrementalChanges: event.hasIncrementalChanges,
        deletedAssetsCount: event.deletedAssets?.length ?? 0,
        insertedAssetsCount: event.insertedAssets?.length ?? 0,
      })
    }
    scheduleForegroundAutoScan({
      delayMs: MEDIA_EVENT_AUTO_SCAN_DELAY_MS,
      source: "media-library",
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
