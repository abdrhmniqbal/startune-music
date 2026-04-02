import type { IndexerScanProgress } from "@/modules/indexer/indexer.types"

import {
  getDefaultIndexerState,
  getIndexerState,
  updateIndexerState,
} from "./indexer.store"

const VISIBLE_PROGRESS_UPDATE_INTERVAL_MS = 120

let lastVisibleProgressUpdateAt = 0

export function beginIndexerProgress(showProgress: boolean) {
  lastVisibleProgressUpdateAt = 0

  if (!showProgress) {
    updateIndexerState({
      ...getDefaultIndexerState(),
      showProgress: false,
    })
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

  const now = Date.now()
  if (
    progress.phase !== "complete" &&
    progress.current < progress.total &&
    now - lastVisibleProgressUpdateAt < VISIBLE_PROGRESS_UPDATE_INTERVAL_MS
  ) {
    return
  }

  lastVisibleProgressUpdateAt = now

  updateIndexerState({
    phase: progress.phase === "scanning" ? "scanning" : "processing",
    currentFile: progress.currentFile,
    processedFiles: progress.current,
    totalFiles: progress.total,
    progress: progress.total > 0 ? (progress.current / progress.total) * 100 : 0,
  })
}

export function completeIndexerProgress() {
  if (!getIndexerState().showProgress) {
    resetIndexerProgress()
    return
  }

  updateIndexerState({
    phase: "complete",
    progress: 100,
    isIndexing: false,
  })
}

export function resetIndexerProgress() {
  updateIndexerState({
    ...getDefaultIndexerState(),
  })
}

export function failIndexerProgress() {
  if (!getIndexerState().showProgress) {
    resetIndexerProgress()
    return
  }

  updateIndexerState({
    isIndexing: false,
    phase: "idle",
    showProgress: false,
  })
}

export function hideIndexerProgress() {
  if (!getIndexerState().showProgress) {
    resetIndexerProgress()
    return
  }

  updateIndexerState({ phase: "idle", showProgress: false })
}

export function getIndexerProgressSnapshot() {
  const state = getIndexerState()
  return {
    processedFiles: state.processedFiles,
    totalFiles: state.totalFiles,
  }
}
