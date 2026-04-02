import { create } from "zustand"

import type {
  FolderFilterConfig,
  LoggingConfig,
  TrackDurationFilterConfig,
} from "./settings.types"

const DEFAULT_AUTO_SCAN_ENABLED = true
const DEFAULT_FOLDER_FILTER_CONFIG: FolderFilterConfig = {
  whitelist: [],
  blacklist: [],
}
const DEFAULT_LOGGING_CONFIG: LoggingConfig = {
  level: "minimal",
}
const DEFAULT_TRACK_DURATION_FILTER: TrackDurationFilterConfig = {
  mode: "off",
  customMinimumSeconds: 180,
}

interface SettingsState {
  autoScanEnabled: boolean
  folderFilterConfig: FolderFilterConfig
  loggingConfig: LoggingConfig
  trackDurationFilterConfig: TrackDurationFilterConfig
}

export const useSettingsStore = create<SettingsState>(() => ({
  autoScanEnabled: DEFAULT_AUTO_SCAN_ENABLED,
  folderFilterConfig: DEFAULT_FOLDER_FILTER_CONFIG,
  loggingConfig: DEFAULT_LOGGING_CONFIG,
  trackDurationFilterConfig: DEFAULT_TRACK_DURATION_FILTER,
}))

export function getDefaultAutoScanEnabled() {
  return DEFAULT_AUTO_SCAN_ENABLED
}

export function getDefaultLoggingConfig() {
  return DEFAULT_LOGGING_CONFIG
}

export function getDefaultFolderFilterConfig() {
  return DEFAULT_FOLDER_FILTER_CONFIG
}

export function getDefaultTrackDurationFilterConfig() {
  return DEFAULT_TRACK_DURATION_FILTER
}

export function getSettingsState() {
  return useSettingsStore.getState()
}

export function updateSettingsState(updates: Partial<SettingsState>) {
  useSettingsStore.setState(updates)
}
