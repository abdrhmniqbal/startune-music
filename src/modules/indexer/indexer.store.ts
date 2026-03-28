import { create } from "zustand"

import { queryClient } from "@/lib/tanstack-query"
import { invalidateIndexerQueries } from "@/modules/indexer/indexer.keys"
import { scanMediaLibrary } from "@/modules/indexer/indexer.repository"
import { logError, logInfo, logWarn } from "@/modules/logging/logger"
import { loadTracks } from "@/modules/player/player.service"

export interface IndexerState {
  isIndexing: boolean
  progress: number
  currentFile: string
  totalFiles: number
  processedFiles: number
  phase: "idle" | "scanning" | "processing" | "cleanup" | "complete" | "paused"
  showProgress: boolean
}

const DEFAULT_INDEXER_STATE: IndexerState = {
  isIndexing: false,
  progress: 0,
  currentFile: "",
  totalFiles: 0,
  processedFiles: 0,
  phase: "idle",
  showProgress: false,
}

interface IndexerStoreState {
  indexerState: IndexerState
}

export const useIndexerStore = create<IndexerStoreState>(() => ({
  indexerState: DEFAULT_INDEXER_STATE,
}))

export const $indexerState = {
  get: () => useIndexerStore.getState().indexerState,
  set: (value: IndexerState) => useIndexerStore.setState({ indexerState: value }),
}

let abortController: AbortController | null = null
let runToken = 0
let completePhaseTimeout: ReturnType<typeof setTimeout> | null = null
let queuedScanRequested = false
let queuedForceFullScan = false
let queuedShowProgress = false

function updateState(updates: Partial<IndexerState>) {
  $indexerState.set({ ...$indexerState.get(), ...updates })
}

export async function startIndexing(
  forceFullScan = false,
  showProgress = true
) {
  const current = $indexerState.get()
  if (current.isIndexing) {
    // Queue the next run so library changes that happen during indexing
    // (including deletions) are not dropped.
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

  updateState({
    isIndexing: true,
    progress: 0,
    processedFiles: 0,
    phase: "scanning",
    showProgress,
    currentFile: "",
    totalFiles: 0,
  })
  logInfo("Indexer started", { forceFullScan, showProgress })

  try {
    await scanMediaLibrary(
      (progress) => {
        if (controller.signal.aborted || currentRunToken !== runToken) return

        updateState({
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

    // Reload tracks from database after indexing completes
    await loadTracks()

    if (controller.signal.aborted || currentRunToken !== runToken) {
      return
    }

    await invalidateIndexerQueries(queryClient)

    updateState({
      phase: "complete",
      progress: 100,
      isIndexing: false,
    })
    logInfo("Indexer completed successfully", {
      forceFullScan,
      processedFiles: $indexerState.get().processedFiles,
      totalFiles: $indexerState.get().totalFiles,
    })

    // Reset to idle after 3 seconds
    completePhaseTimeout = setTimeout(() => {
      if (currentRunToken !== runToken) return
      updateState({ phase: "idle", showProgress: false })
      completePhaseTimeout = null
    }, 3000)
  } catch (error) {
    if (controller.signal.aborted || currentRunToken !== runToken) {
      return
    }

    logError("Indexer run failed", error, { forceFullScan, showProgress })
    updateState({
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

    if (shouldRunQueuedScan) {
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

  updateState({
    isIndexing: false,
    phase: "idle",
    showProgress: false,
    currentFile: "",
    progress: 0,
  })
}

export function pauseIndexing() {
  // Note: The current scanner doesn't support pausing, so we just stop
  logWarn("Indexer pause requested; stopping instead because pause is unsupported")
  stopIndexing()
}

export function resumeIndexing() {
  const state = $indexerState.get()
  if (state.phase === "paused" || !state.isIndexing) {
    logInfo("Indexer resume requested")
    startIndexing(false)
  }
}
