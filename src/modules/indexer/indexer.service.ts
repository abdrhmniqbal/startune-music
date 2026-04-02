import { refreshIndexedMediaState } from "@/modules/indexer/indexer-refresh.service"
import { scanMediaLibrary } from "@/modules/indexer/indexer.repository"
import {
  consumeQueuedIndexerRun,
  finishIndexerRunRuntime,
  isIndexerRunActive,
  isIndexerRunPaused,
  isIndexerRunStale,
  pauseIndexerRunRuntime,
  queueIndexerRun,
  resumeIndexerRunRuntime,
  scheduleIndexerCompletePhaseReset,
  startIndexerRunRuntime,
  stopIndexerRunRuntime,
} from "@/modules/indexer/indexer-runtime"
import {
  beginIndexerProgress,
  completeIndexerProgress,
  failIndexerProgress,
  getIndexerProgressSnapshot,
  hideIndexerProgress,
  pauseIndexerProgress,
  resetIndexerProgress,
  resumeIndexerProgress,
  updateIndexerProgress,
} from "@/modules/indexer/indexer-progress.service"
import { logError, logInfo, logWarn } from "@/modules/logging/logging.service"

export async function startIndexing(
  forceFullScan = false,
  showProgress = true
) {
  if (isIndexerRunActive()) {
    queueIndexerRun(forceFullScan, showProgress)
    logInfo("Indexer run queued while another run is active", {
      forceFullScan,
      showProgress,
    })
    return
  }

  const { controller, runToken: currentRunToken } = startIndexerRunRuntime()

  beginIndexerProgress(showProgress)
  logInfo("Indexer started", { forceFullScan, showProgress })

  try {
    await scanMediaLibrary(
      (progress) => {
        if (isIndexerRunStale(controller, currentRunToken)) {
          return
        }

        updateIndexerProgress(progress)
      },
      forceFullScan,
      controller.signal
    )

    if (isIndexerRunStale(controller, currentRunToken)) {
      return
    }

    await refreshIndexedMediaState()

    completeIndexerProgress()
    const progressSnapshot = getIndexerProgressSnapshot()
    logInfo("Indexer completed successfully", {
      forceFullScan,
      processedFiles: progressSnapshot.processedFiles,
      totalFiles: progressSnapshot.totalFiles,
    })

    scheduleIndexerCompletePhaseReset(currentRunToken, () => {
      hideIndexerProgress()
    })
  } catch (error) {
    if (isIndexerRunStale(controller, currentRunToken)) {
      return
    }

    logError("Indexer run failed", error, { forceFullScan, showProgress })
    failIndexerProgress()
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
  resetIndexerProgress()
  logInfo("Indexer stop handling completed")
}

export function pauseIndexing() {
  const didPause = pauseIndexerRunRuntime()
  if (!didPause) {
    return false
  }

  pauseIndexerProgress()
  logInfo("Indexer paused")
  return true
}

export function resumeIndexing() {
  const didResume = resumeIndexerRunRuntime()
  if (!didResume) {
    return false
  }

  resumeIndexerProgress()
  logInfo("Indexer resumed")
  return true
}

export function cancelIndexing() {
  if (!isIndexerRunActive() && !isIndexerRunPaused()) {
    return false
  }

  stopIndexing()
  logInfo("Indexer canceled")
  return true
}
