import {
  createIndexerConfigFile,
  loadIndexerConfig,
  saveIndexerConfig,
} from "@/modules/indexer/indexer-config.repository"
import {
  getAutoScanEnabledState,
  getDefaultAutoScanEnabled,
  setAutoScanEnabledState,
} from "@/modules/settings/settings.store"

interface AutoScanConfig {
  enabled: boolean
}

const AUTO_SCAN_FILE = createIndexerConfigFile("indexer-auto-scan.json")

let loadPromise: Promise<boolean> | null = null
let hasLoadedConfig = false

export async function ensureAutoScanConfigLoaded(): Promise<boolean> {
  if (hasLoadedConfig) {
    return getAutoScanEnabledState()
  }

  if (loadPromise) {
    return loadPromise
  }

  loadPromise = (async () => {
    const config = await loadIndexerConfig(
      AUTO_SCAN_FILE,
      { enabled: getDefaultAutoScanEnabled() },
      (parsed) => ({
        enabled:
          typeof parsed.enabled === "boolean"
            ? parsed.enabled
            : getDefaultAutoScanEnabled(),
      })
    )

    setAutoScanEnabledState(config.enabled)
    hasLoadedConfig = true
    return config.enabled
  })()

  const result = await loadPromise
  loadPromise = null
  return result
}

export async function setAutoScanEnabled(enabled: boolean): Promise<boolean> {
  await ensureAutoScanConfigLoaded()
  setAutoScanEnabledState(enabled)
  hasLoadedConfig = true
  await saveIndexerConfig(AUTO_SCAN_FILE, { enabled })
  return enabled
}
