import {
  getDefaultTrackDurationFilterConfig,
  getSettingsState,
  updateSettingsState,
} from "@/modules/settings/settings.store"
import {
  createSettingsConfigFile,
  loadSettingsConfig,
  saveSettingsConfig,
} from "@/modules/settings/settings.repository"
import type {
  TrackDurationFilterConfig,
  TrackDurationFilterMode,
} from "@/modules/settings/settings.types"

export type { TrackDurationFilterConfig, TrackDurationFilterMode }

const TRACK_DURATION_FILTER_FILE = createSettingsConfigFile(
  "track-duration-filter.json"
)

let loadPromise: Promise<TrackDurationFilterConfig> | null = null
let hasLoadedConfig = false

function clampCustomSeconds(value: number): number {
  if (!Number.isFinite(value)) {
    return getDefaultTrackDurationFilterConfig().customMinimumSeconds
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
        getDefaultTrackDurationFilterConfig().customMinimumSeconds
    ),
  }
}

async function persistConfig(config: TrackDurationFilterConfig): Promise<void> {
  await saveSettingsConfig(TRACK_DURATION_FILTER_FILE, config)
}

export async function ensureTrackDurationFilterConfigLoaded(): Promise<TrackDurationFilterConfig> {
  if (hasLoadedConfig) {
    return getSettingsState().trackDurationFilterConfig
  }

  if (loadPromise) {
    return loadPromise
  }

  loadPromise = (async () => {
    const next = await loadSettingsConfig(
      TRACK_DURATION_FILTER_FILE,
      getDefaultTrackDurationFilterConfig(),
      sanitizeConfig
    )
    updateSettingsState({ trackDurationFilterConfig: next })
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
  const current = getSettingsState().trackDurationFilterConfig
  const next = sanitizeConfig({ ...current, ...updates })
  updateSettingsState({ trackDurationFilterConfig: next })
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
