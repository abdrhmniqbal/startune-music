import type { IndexerScanProgress } from "@/modules/indexer/indexer.types"

import {
  getDefaultIndexerState,
  getIndexerState,
  updateIndexerState,
} from "./indexer.store"

export function beginIndexerProgress(showProgress: boolean) {
  updateIndexerState({
    ...getDefaultIndexerState(),
    isIndexing: true,
    phase: "scanning",
    showProgress,
  })
}

export function updateIndexerProgress(progress: IndexerScanProgress) {
  updateIndexerState({
    phase: progress.phase === "scanning" ? "scanning" : "processing",
    currentFile: progress.currentFile,
    processedFiles: progress.current,
    totalFiles: progress.total,
    progress: progress.total > 0 ? (progress.current / progress.total) * 100 : 0,
  })
}

export function completeIndexerProgress() {
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
  updateIndexerState({
    isIndexing: false,
    phase: "idle",
    showProgress: false,
  })
}

export function hideIndexerProgress() {
  updateIndexerState({ phase: "idle", showProgress: false })
}

export function getIndexerProgressSnapshot() {
  const state = getIndexerState()
  return {
    processedFiles: state.processedFiles,
    totalFiles: state.totalFiles,
  }
}
