import { refreshIndexedMediaState } from "@/modules/indexer/indexer-refresh.service"
import { scanMediaLibrary } from "@/modules/indexer/indexer.repository"
import {
  consumeQueuedIndexerRun,
  finishIndexerRunRuntime,
  isIndexerRunStale,
  queueIndexerRun,
  scheduleIndexerCompletePhaseReset,
  startIndexerRunRuntime,
  stopIndexerRunRuntime,
} from "@/modules/indexer/indexer-runtime"
import { logError, logInfo, logWarn } from "@/modules/logging/logging.service"

import { getDefaultIndexerState, getIndexerState, updateIndexerState } from "./indexer.store"

export async function startIndexing(
  forceFullScan = false,
  showProgress = true
) {
  const currentState = getIndexerState()
  if (currentState.isIndexing) {
    queueIndexerRun(forceFullScan, showProgress)
    logInfo("Indexer run queued while another run is active", {
      forceFullScan,
      showProgress,
    })
    return
  }

  const { controller, runToken: currentRunToken } = startIndexerRunRuntime()

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
        if (isIndexerRunStale(controller, currentRunToken)) {
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

    if (isIndexerRunStale(controller, currentRunToken)) {
      return
    }

    await refreshIndexedMediaState()

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

    scheduleIndexerCompletePhaseReset(currentRunToken, () => {
      updateIndexerState({ phase: "idle", showProgress: false })
    })
  } catch (error) {
    if (isIndexerRunStale(controller, currentRunToken)) {
      return
    }

    logError("Indexer run failed", error, { forceFullScan, showProgress })
    updateIndexerState({
      isIndexing: false,
      phase: "idle",
      showProgress: false,
    })
  } finally {
    finishIndexerRunRuntime(controller)

    const nextQueuedRun = consumeQueuedIndexerRun(controller, currentRunToken)
    if (!nextQueuedRun) {
      return
    }

    logInfo("Starting queued indexer run", {
      forceFullScan: nextQueuedRun.forceFullScan,
      showProgress: nextQueuedRun.showProgress,
    })
    void startIndexing(nextQueuedRun.forceFullScan, nextQueuedRun.showProgress)
  }
}

export async function forceReindexLibrary(showProgress = true) {
  await startIndexing(true, showProgress)
}

export function stopIndexing() {
  logWarn("Indexer stopped")
  stopIndexerRunRuntime()

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
