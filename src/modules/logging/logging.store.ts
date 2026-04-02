import {
  getDefaultLoggingConfig,
  getSettingsState,
  updateSettingsState,
  useSettingsStore,
} from "@/modules/settings/settings.store"
import type { AppLogLevel, LoggingConfig } from "@/modules/settings/settings.types"
import {
  createSettingsConfigFile,
  loadSettingsConfig,
  saveSettingsConfig,
} from "@/modules/settings/settings.repository"

export type { AppLogLevel, LoggingConfig }

const LOG_CONFIG_FILE = createSettingsConfigFile("logging-config.json")

let configLoadPromise: Promise<LoggingConfig> | null = null
let hasLoadedConfig = false

export function useLoggingStore<T>(
  selector: (state: { loggingConfig: LoggingConfig }) => T
) {
  return useSettingsStore((state) =>
    selector({ loggingConfig: state.loggingConfig })
  )
}

export function getLoggingConfigState() {
  return getSettingsState().loggingConfig
}

export function setLoggingConfigState(value: LoggingConfig) {
  updateSettingsState({ loggingConfig: value })
}

function isValidLogLevel(value: unknown): value is AppLogLevel {
  return value === "minimal" || value === "extra"
}

function sanitizeConfig(value: Partial<LoggingConfig>): LoggingConfig {
  return {
    level: isValidLogLevel(value.level)
      ? value.level
      : getDefaultLoggingConfig().level,
  }
}

async function persistConfig(config: LoggingConfig): Promise<void> {
  await saveSettingsConfig(LOG_CONFIG_FILE, config)
}

export async function ensureLoggingConfigLoaded(): Promise<LoggingConfig> {
  if (hasLoadedConfig) {
    return getLoggingConfigState()
  }

  if (configLoadPromise) {
    return configLoadPromise
  }

  configLoadPromise = (async () => {
    try {
      const parsed = await loadSettingsConfig(
        LOG_CONFIG_FILE,
        getDefaultLoggingConfig(),
        sanitizeConfig
      )
      setLoggingConfigState(parsed)
      hasLoadedConfig = true
      return parsed
    } catch {
      setLoggingConfigState(getDefaultLoggingConfig())
      hasLoadedConfig = true
      return getDefaultLoggingConfig()
    }
  })()

  const result = await configLoadPromise
  configLoadPromise = null
  return result
}

export async function setAppLogLevel(
  level: AppLogLevel
): Promise<LoggingConfig> {
  await ensureLoggingConfigLoaded()
  const next = sanitizeConfig({ ...getLoggingConfigState(), level })
  setLoggingConfigState(next)
  hasLoadedConfig = true
  await persistConfig(next)
  return next
}
