import { queryClient } from "@/lib/tanstack-query"
import { invalidateIndexerQueries } from "@/modules/indexer/indexer.keys"
import { scanMediaLibrary } from "@/modules/indexer/indexer.repository"
import { logError, logInfo, logWarn } from "@/modules/logging/logging.service"
import { loadTracks } from "@/modules/player/player.service"

import { getDefaultIndexerState, getIndexerState, updateIndexerState } from "./indexer.store"

let abortController: AbortController | null = null
let runToken = 0
let completePhaseTimeout: ReturnType<typeof setTimeout> | null = null
let queuedScanRequested = false
let queuedForceFullScan = false
let queuedShowProgress = false

export async function startIndexing(
  forceFullScan = false,
  showProgress = true
) {
  const currentState = getIndexerState()
  if (currentState.isIndexing) {
    queuedScanRequested = true
    queuedForceFullScan = queuedForceFullScan || forceFullScan
    queuedShowProgress = queuedShowProgress || showProgress
    logInfo("Indexer run queued while another run is active", {
      forceFullScan,
      showProgress,
    })
    return
  }

  if (completePhaseTimeout) {
    clearTimeout(completePhaseTimeout)
    completePhaseTimeout = null
  }

  const controller = new AbortController()
  abortController = controller
  runToken += 1
  const currentRunToken = runToken

  updateIndexerState({
    ...getDefaultIndexerState(),
    isIndexing: true,
    phase: "scanning",
    showProgress,
  })
  logInfo("Indexer started", { forceFullScan, showProgress })

  try {
    await scanMediaLibrary(
      (progress) => {
        if (controller.signal.aborted || currentRunToken !== runToken) {
          return
        }

        updateIndexerState({
          phase: progress.phase === "scanning" ? "scanning" : "processing",
          currentFile: progress.currentFile,
          processedFiles: progress.current,
          totalFiles: progress.total,
          progress:
            progress.total > 0 ? (progress.current / progress.total) * 100 : 0,
        })
      },
      forceFullScan,
      controller.signal
    )

    if (controller.signal.aborted || currentRunToken !== runToken) {
      return
    }

    await loadTracks()

    if (controller.signal.aborted || currentRunToken !== runToken) {
      return
    }

    await invalidateIndexerQueries(queryClient)

    updateIndexerState({
      phase: "complete",
      progress: 100,
      isIndexing: false,
    })
    logInfo("Indexer completed successfully", {
      forceFullScan,
      processedFiles: getIndexerState().processedFiles,
      totalFiles: getIndexerState().totalFiles,
    })

    completePhaseTimeout = setTimeout(() => {
      if (currentRunToken !== runToken) {
        return
      }

      updateIndexerState({ phase: "idle", showProgress: false })
      completePhaseTimeout = null
    }, 3000)
  } catch (error) {
    if (controller.signal.aborted || currentRunToken !== runToken) {
      return
    }

    logError("Indexer run failed", error, { forceFullScan, showProgress })
    updateIndexerState({
      isIndexing: false,
      phase: "idle",
      showProgress: false,
    })
  } finally {
    if (abortController === controller) {
      abortController = null
    }

    const shouldRunQueuedScan =
      queuedScanRequested &&
      currentRunToken === runToken &&
      !controller.signal.aborted

    if (!shouldRunQueuedScan) {
      return
    }

    const nextForceFullScan = queuedForceFullScan
    const nextShowProgress = queuedShowProgress
    queuedScanRequested = false
    queuedForceFullScan = false
    queuedShowProgress = false
    logInfo("Starting queued indexer run", {
      forceFullScan: nextForceFullScan,
      showProgress: nextShowProgress,
    })
    void startIndexing(nextForceFullScan, nextShowProgress)
  }
}

export async function forceReindexLibrary(showProgress = true) {
  await startIndexing(true, showProgress)
}

export function stopIndexing() {
  logWarn("Indexer stopped")
  runToken += 1
  queuedScanRequested = false
  queuedForceFullScan = false
  queuedShowProgress = false

  if (completePhaseTimeout) {
    clearTimeout(completePhaseTimeout)
    completePhaseTimeout = null
  }

  if (abortController) {
    abortController.abort()
    abortController = null
  }

  updateIndexerState({
    ...getDefaultIndexerState(),
  })
}

export function pauseIndexing() {
  logWarn("Indexer pause requested; stopping instead because pause is unsupported")
  stopIndexing()
}

export function resumeIndexing() {
  const state = getIndexerState()
  if (state.phase === "paused" || !state.isIndexing) {
    logInfo("Indexer resume requested")
    void startIndexing(false)
  }
}
