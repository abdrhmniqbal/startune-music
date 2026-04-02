import type { IndexerScanProgress } from "@/modules/indexer/indexer.types"
import { logInfo, logWarn } from "@/modules/logging/logging.service"
import {
  beginIndexerProgressNotification,
  completeIndexerProgressNotification,
  dismissIndexerProgressNotification,
  failIndexerProgressNotification,
  pauseIndexerProgressNotification,
  resumeIndexerProgressNotification,
  updateIndexerProgressNotification,
} from "@/modules/indexer/indexer-notification.service"

import {
  getDefaultIndexerState,
  getIndexerState,
  updateIndexerState,
} from "./indexer.store"

const VISIBLE_PROGRESS_UPDATE_INTERVAL_MS = 120
const NOTIFICATION_PROGRESS_UPDATE_INTERVAL_MS = 750
const NOTIFICATION_PROGRESS_DELTA_PERCENT = 1
const NOTIFICATION_PROGRESS_DELTA_FILES = 5

let lastVisibleProgressUpdateAt = 0
let lastNotificationProgressUpdateAt = 0
let lastNotificationProgressPercent = -1
let lastNotificationProcessedFiles = -1
let lastNotificationPhase: IndexerScanProgress["phase"] | null = null

export function beginIndexerProgress(showProgress: boolean) {
  lastVisibleProgressUpdateAt = 0
  lastNotificationProgressUpdateAt = 0
  lastNotificationProgressPercent = -1
  lastNotificationProcessedFiles = -1
  lastNotificationPhase = null
  logInfo("Indexer progress started", { showProgress })

  if (showProgress) {
    void beginIndexerProgressNotification()
  }

  if (!showProgress) {
    updateIndexerState({
      ...getDefaultIndexerState(),
      showProgress: false,
    })
    logInfo("Indexer progress hidden for this run")
    return
  }

  updateIndexerState({
    ...getDefaultIndexerState(),
    isIndexing: true,
    phase: "scanning",
    showProgress,
  })
}

export function updateIndexerProgress(progress: IndexerScanProgress) {
  const state = getIndexerState()
  if (!state.showProgress) {
    return
  }

  if (state.phase === "paused") {
    return
  }

  const now = Date.now()
  if (
    progress.phase !== "complete" &&
    progress.current < progress.total &&
    now - lastVisibleProgressUpdateAt < VISIBLE_PROGRESS_UPDATE_INTERVAL_MS
  ) {
    return
  }

  lastVisibleProgressUpdateAt = now

  const nextProgressPercent =
    progress.total > 0 ? (progress.current / progress.total) * 100 : 0
  const hasMeaningfulPercentDelta =
    lastNotificationProgressPercent < 0 ||
    nextProgressPercent - lastNotificationProgressPercent >=
      NOTIFICATION_PROGRESS_DELTA_PERCENT
  const hasMeaningfulFileDelta =
    lastNotificationProcessedFiles < 0 ||
    progress.current - lastNotificationProcessedFiles >=
      NOTIFICATION_PROGRESS_DELTA_FILES
  const hasPhaseChange =
    lastNotificationPhase !== null && progress.phase !== lastNotificationPhase
  const hasIntervalElapsed =
    now - lastNotificationProgressUpdateAt >=
    NOTIFICATION_PROGRESS_UPDATE_INTERVAL_MS

  if (
    progress.phase === "complete" ||
    (hasIntervalElapsed && (hasMeaningfulPercentDelta || hasMeaningfulFileDelta || hasPhaseChange))
  ) {
    lastNotificationProgressUpdateAt = now
    lastNotificationProgressPercent = nextProgressPercent
    lastNotificationProcessedFiles = progress.current
    lastNotificationPhase = progress.phase
    void updateIndexerProgressNotification(progress)
  }

  updateIndexerState({
    phase: progress.phase === "scanning" ? "scanning" : "processing",
    currentFile: progress.currentFile,
    processedFiles: progress.current,
    totalFiles: progress.total,
    progress: nextProgressPercent,
  })
}

export function completeIndexerProgress() {
  if (!getIndexerState().showProgress) {
    logInfo("Indexer progress completed while progress UI hidden")
    void dismissIndexerProgressNotification()
    resetIndexerProgress()
    return
  }

  updateIndexerState({
    phase: "complete",
    progress: 100,
    isIndexing: false,
  })
  const totalFiles = getIndexerState().totalFiles
  void completeIndexerProgressNotification(totalFiles)
  logInfo("Indexer progress completed")
}

export function resetIndexerProgress() {
  void dismissIndexerProgressNotification()
  lastNotificationProgressPercent = -1
  lastNotificationProcessedFiles = -1
  lastNotificationPhase = null
  updateIndexerState({
    ...getDefaultIndexerState(),
  })
}

export function failIndexerProgress() {
  if (!getIndexerState().showProgress) {
    logWarn("Indexer progress failed while progress UI hidden")
    void dismissIndexerProgressNotification()
    resetIndexerProgress()
    return
  }

  updateIndexerState({
    isIndexing: false,
    phase: "idle",
    showProgress: false,
  })
  void failIndexerProgressNotification()
  logWarn("Indexer progress failed")
}

export function hideIndexerProgress() {
  if (!getIndexerState().showProgress) {
    logInfo("Indexer progress hide requested while already hidden")
    void dismissIndexerProgressNotification()
    resetIndexerProgress()
    return
  }

  void dismissIndexerProgressNotification()
  updateIndexerState({ phase: "idle", showProgress: false })
  logInfo("Indexer progress hidden")
}

export function pauseIndexerProgress() {
  const state = getIndexerState()
  if (!state.showProgress || !state.isIndexing) {
    return
  }

  updateIndexerState({ phase: "paused" })
  void pauseIndexerProgressNotification({
    current: state.processedFiles,
    total: state.totalFiles,
    currentFile: state.currentFile,
  })
}

export function resumeIndexerProgress() {
  const state = getIndexerState()
  if (!state.showProgress || !state.isIndexing || state.phase !== "paused") {
    return
  }

  const resumedPhase = state.totalFiles > 0 ? "processing" : "scanning"
  updateIndexerState({ phase: resumedPhase })
  void resumeIndexerProgressNotification({
    phase: resumedPhase,
    current: state.processedFiles,
    total: state.totalFiles,
    currentFile: state.currentFile,
  })
}

export function getIndexerProgressSnapshot() {
  const state = getIndexerState()
  return {
    processedFiles: state.processedFiles,
    totalFiles: state.totalFiles,
  }
}
