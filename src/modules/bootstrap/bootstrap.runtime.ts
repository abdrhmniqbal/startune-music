import * as MediaLibrary from "expo-media-library"
import { AppState, type AppStateStatus } from "react-native"

import { requestMediaLibraryPermission } from "@/core/storage/media-library.service"
import { bootstrapApp } from "@/modules/bootstrap/bootstrap.utils"
import { ensureAutoScanConfigLoaded } from "@/modules/indexer/auto-scan"
import { startIndexing } from "@/modules/indexer/indexer.service"
import {
  initializeLogging,
  logError,
  logInfo,
  logWarn,
} from "@/modules/logging/logging.service"

type DatabaseStatus = "pending" | "ready" | "error"

const MIN_AUTO_SCAN_INTERVAL_MS = 5000

let loggingInitializationPromise: Promise<void> | null = null
let bootstrapPromise: Promise<void> | null = null
let databaseStatus: DatabaseStatus = "pending"
let isBootstrapped = false
let lastAutoScanAt = 0

export function ensureLoggingInitialized() {
  if (loggingInitializationPromise) {
    return loggingInitializationPromise
  }

  loggingInitializationPromise = initializeLogging().catch((error) => {
    loggingInitializationPromise = null
    throw error
  })

  return loggingInitializationPromise
}

export async function completeBootstrap() {
  if (databaseStatus !== "ready") {
    return
  }

  if (isBootstrapped) {
    return
  }

  if (bootstrapPromise) {
    await bootstrapPromise
    return
  }

  bootstrapPromise = (async () => {
    try {
      await ensureLoggingInitialized()
      logInfo("App bootstrap started")
      await bootstrapApp()
      logInfo("App bootstrap completed")
    } catch (error) {
      logError("App bootstrap failed", error)
    } finally {
      isBootstrapped = true
      bootstrapPromise = null
    }
  })()

  await bootstrapPromise
}

export async function handleBootstrapDatabaseReady() {
  if (databaseStatus !== "pending") {
    return
  }

  databaseStatus = "ready"
  logInfo("Database marked ready for bootstrap")
  await completeBootstrap()
}

export function handleBootstrapDatabaseError() {
  if (databaseStatus === "error") {
    return
  }

  databaseStatus = "error"
  logWarn("Database failed before bootstrap completed")
}

export async function runAutoScan(options?: { bypassThrottle?: boolean }) {
  if (databaseStatus !== "ready" || !isBootstrapped) {
    return
  }

  const bypassThrottle = options?.bypassThrottle === true
  const now = Date.now()
  if (!bypassThrottle && now - lastAutoScanAt < MIN_AUTO_SCAN_INTERVAL_MS) {
    return
  }

  try {
    const isAutoScanEnabled = await ensureAutoScanConfigLoaded()
    if (!isAutoScanEnabled) {
      return
    }

    const { status } = await requestMediaLibraryPermission()
    if (status !== "granted") {
      return
    }

    lastAutoScanAt = now
    logInfo("Auto scan triggered", { bypassThrottle })
    await startIndexing(false, false)
  } catch (error) {
    logError("Auto scan failed", error, { bypassThrottle })
  }
}

export function shouldTriggerAutoScanOnMediaLibraryEvent(
  event: MediaLibrary.MediaLibraryAssetsChangeEvent
) {
  return (
    event.hasIncrementalChanges === false ||
    (event.deletedAssets?.length ?? 0) > 0
  )
}

export function registerBootstrapListeners() {
  let previousState: AppStateStatus = AppState.currentState

  const appStateSubscription = AppState.addEventListener("change", (nextState) => {
    const isReturningToForeground =
      (previousState === "background" || previousState === "inactive") &&
      nextState === "active"
    previousState = nextState

    if (!isReturningToForeground) {
      return
    }

    void runAutoScan()
  })

  const mediaLibrarySubscription = MediaLibrary.addListener((event) => {
    void runAutoScan({
      bypassThrottle: shouldTriggerAutoScanOnMediaLibraryEvent(event),
    })
  })

  return () => {
    appStateSubscription.remove()
    mediaLibrarySubscription.remove()
  }
}
