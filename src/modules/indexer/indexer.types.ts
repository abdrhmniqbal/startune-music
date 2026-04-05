export interface IndexerScanProgress {
  phase: "scanning" | "processing" | "complete"
  current: number
  total: number
  currentFile: string
}

export interface IndexerRunSnapshot {
  startedAt: number
  finishedAt: number
  durationMs: number
  forceFullScan: boolean
  discoveredAssets: number
  scopedAssets: number
  skippedByUri: number
  skippedByExtension: number
  skippedByFolderFilters: number
  skippedByDurationFilters: number
  deletedTracks: number
  changedAssets: number
  unchangedAssets: number
  preparedAssets: number
  committedAssets: number
  failedAssets: number
}
