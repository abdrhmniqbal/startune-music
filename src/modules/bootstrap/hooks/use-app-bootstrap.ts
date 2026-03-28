import * as MediaLibrary from "expo-media-library"
import { useCallback, useEffect, useRef, useState } from "react"
import { AppState, type AppStateStatus } from "react-native"

import { requestMediaLibraryPermission } from "@/core/storage/media-library.service"
import { bootstrapApp } from "@/modules/bootstrap/bootstrap.utils"
import { ensureAutoScanConfigLoaded } from "@/modules/indexer/auto-scan"
import { startIndexing } from "@/modules/indexer/indexer.store"
import { initializeLogging, logError } from "@/modules/logging/logger"

type DatabaseStatus = "pending" | "ready" | "error"

let loggingInitializationPromise: Promise<void> | null = null

function ensureLoggingInitialized() {
  if (loggingInitializationPromise) {
    return loggingInitializationPromise
  }

  loggingInitializationPromise = initializeLogging().catch((error) => {
    loggingInitializationPromise = null
    throw error
  })

  return loggingInitializationPromise
}

export function useAppBootstrap(options?: { onReady?: () => void }) {
  const [isReady, setIsReady] = useState(false)
  const lastAutoScanAtRef = useRef(0)
  const bootstrapPromiseRef = useRef<Promise<void> | null>(null)
  const databaseStatusRef = useRef<DatabaseStatus>("pending")
  const isBootstrappedRef = useRef(false)
  const hasSignaledReadyRef = useRef(false)

  const signalReady = useCallback(() => {
    if (hasSignaledReadyRef.current) {
      return
    }

    hasSignaledReadyRef.current = true
    setIsReady(true)
    options?.onReady?.()
  }, [options])

  const runAutoScan = useCallback(
    async (options?: { bypassThrottle?: boolean }) => {
      if (
        databaseStatusRef.current !== "ready" ||
        !isBootstrappedRef.current
      ) {
        return
      }

      const bypassThrottle = options?.bypassThrottle === true
      const now = Date.now()
      const minAutoScanIntervalMs = 5000
      if (
        !bypassThrottle &&
        now - lastAutoScanAtRef.current < minAutoScanIntervalMs
      ) {
        return
      }

      const isAutoScanEnabled = await ensureAutoScanConfigLoaded()
      if (!isAutoScanEnabled) {
        return
      }

      const { status } = await requestMediaLibraryPermission()
      if (status !== "granted") {
        return
      }

      lastAutoScanAtRef.current = now
      await startIndexing(false, false)
    },
    []
  )

  const completeBootstrap = useCallback(async () => {
    if (databaseStatusRef.current !== "ready") {
      return
    }

    if (isBootstrappedRef.current) {
      signalReady()
      return
    }

    if (bootstrapPromiseRef.current) {
      await bootstrapPromiseRef.current
      return
    }

    bootstrapPromiseRef.current = (async () => {
      try {
        await ensureLoggingInitialized()
        await bootstrapApp()
      } catch (error) {
        logError("App bootstrap failed", error)
      } finally {
        isBootstrappedRef.current = true
        signalReady()
        bootstrapPromiseRef.current = null
      }
    })()

    await bootstrapPromiseRef.current
  }, [signalReady])

  useEffect(() => {
    let previousState: AppStateStatus = AppState.currentState

    const subscription = AppState.addEventListener("change", (nextState) => {
      const isReturningToForeground =
        (previousState === "background" || previousState === "inactive") &&
        nextState === "active"
      previousState = nextState

      if (!isReturningToForeground) {
        return
      }

      void runAutoScan()
    })

    return () => {
      subscription.remove()
    }
  }, [runAutoScan])

  useEffect(() => {
    const subscription = MediaLibrary.addListener((event) => {
      const hasDeletedAssets =
        event.hasIncrementalChanges === false ||
        (event.deletedAssets?.length ?? 0) > 0
      void runAutoScan({ bypassThrottle: hasDeletedAssets })
    })

    return () => {
      subscription.remove()
    }
  }, [runAutoScan])

  return {
    isReady,
    handleDatabaseReady: useCallback(() => {
      if (databaseStatusRef.current !== "pending") {
        return
      }

      databaseStatusRef.current = "ready"
      void completeBootstrap()
    }, [completeBootstrap]),
    handleDatabaseError: useCallback(() => {
      if (databaseStatusRef.current === "error") {
        return
      }

      databaseStatusRef.current = "error"
      signalReady()
    }, [signalReady]),
  }
}
