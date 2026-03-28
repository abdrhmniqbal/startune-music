import { create } from "zustand"

import {
  createIndexerConfigFile,
  loadIndexerConfig,
  saveIndexerConfig,
} from "@/modules/indexer/indexer-config.repository"

export type TrackDurationFilterMode =
  | "off"
  | "min30s"
  | "min60s"
  | "min120s"
  | "custom"

export interface TrackDurationFilterConfig {
  mode: TrackDurationFilterMode
  customMinimumSeconds: number
}

const TRACK_DURATION_FILTER_FILE = createIndexerConfigFile(
  "track-duration-filter.json"
)
const DEFAULT_TRACK_DURATION_FILTER: TrackDurationFilterConfig = {
  mode: "off",
  customMinimumSeconds: 180,
}

interface TrackDurationFilterStoreState {
  trackDurationFilterConfig: TrackDurationFilterConfig
}

export const useTrackDurationFilterStore =
  create<TrackDurationFilterStoreState>(() => ({
    trackDurationFilterConfig: DEFAULT_TRACK_DURATION_FILTER,
  }))

export const $trackDurationFilterConfig = {
  get: () => useTrackDurationFilterStore.getState().trackDurationFilterConfig,
  set: (value: TrackDurationFilterConfig) =>
    useTrackDurationFilterStore.setState({ trackDurationFilterConfig: value }),
}

let loadPromise: Promise<TrackDurationFilterConfig> | null = null
let hasLoadedConfig = false

function clampCustomSeconds(value: number): number {
  if (!Number.isFinite(value)) {
    return DEFAULT_TRACK_DURATION_FILTER.customMinimumSeconds
  }

  return Math.max(0, Math.min(1200, Math.round(value)))
}

function sanitizeConfig(
  config: Partial<TrackDurationFilterConfig>
): TrackDurationFilterConfig {
  const mode: TrackDurationFilterMode =
    config.mode === "min30s" ||
    config.mode === "min60s" ||
    config.mode === "min120s" ||
    config.mode === "custom"
      ? config.mode
      : "off"

  return {
    mode,
    customMinimumSeconds: clampCustomSeconds(
      config.customMinimumSeconds ??
        DEFAULT_TRACK_DURATION_FILTER.customMinimumSeconds
    ),
  }
}

async function persistConfig(config: TrackDurationFilterConfig): Promise<void> {
  await saveIndexerConfig(TRACK_DURATION_FILTER_FILE, config)
}

export async function ensureTrackDurationFilterConfigLoaded(): Promise<TrackDurationFilterConfig> {
  if (hasLoadedConfig) {
    return $trackDurationFilterConfig.get()
  }

  if (loadPromise) {
    return loadPromise
  }

  loadPromise = (async () => {
    const next = await loadIndexerConfig(
      TRACK_DURATION_FILTER_FILE,
      DEFAULT_TRACK_DURATION_FILTER,
      sanitizeConfig
    )
    $trackDurationFilterConfig.set(next)
    hasLoadedConfig = true
    return next
  })()

  const result = await loadPromise
  loadPromise = null
  return result
}

export async function setTrackDurationFilterConfig(
  updates: Partial<TrackDurationFilterConfig>
): Promise<TrackDurationFilterConfig> {
  await ensureTrackDurationFilterConfigLoaded()
  const current = $trackDurationFilterConfig.get()
  const next = sanitizeConfig({ ...current, ...updates })
  $trackDurationFilterConfig.set(next)
  hasLoadedConfig = true
  await persistConfig(next)
  return next
}

export function getTrackDurationMinimumSeconds(
  config: TrackDurationFilterConfig
): number {
  if (config.mode === "min30s") {
    return 30
  }

  if (config.mode === "min60s") {
    return 60
  }

  if (config.mode === "min120s") {
    return 120
  }

  if (config.mode === "custom") {
    return clampCustomSeconds(config.customMinimumSeconds)
  }

  return 0
}

export function isAssetAllowedByTrackDuration(
  durationSeconds: number,
  config: TrackDurationFilterConfig
): boolean {
  const minDuration = getTrackDurationMinimumSeconds(config)
  if (minDuration <= 0) {
    return true
  }

  if (!Number.isFinite(durationSeconds) || durationSeconds <= 0) {
    return false
  }

  return durationSeconds >= minDuration
}

export function getTrackDurationFilterLabel(
  config: TrackDurationFilterConfig
): string {
  if (config.mode === "min30s") {
    return "At least 30s"
  }

  if (config.mode === "min60s") {
    return "At least 1m"
  }

  if (config.mode === "min120s") {
    return "At least 2m"
  }

  if (config.mode === "custom") {
    const seconds = getTrackDurationMinimumSeconds(config)
    if (seconds < 60) {
      return `Custom ${seconds}s`
    }
    const minutes = Math.floor(seconds / 60)
    const rem = seconds % 60
    return rem > 0 ? `Custom ${minutes}m ${rem}s` : `Custom ${minutes}m`
  }

  return "No filter"
}
