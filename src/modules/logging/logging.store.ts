import { File, Paths } from "expo-file-system"
import {
  getDefaultLoggingConfig,
  getLoggingConfigState,
  setLoggingConfigState,
} from "@/modules/settings/settings.store"
import type { AppLogLevel, LoggingConfig } from "@/modules/settings/settings.types"

export type { AppLogLevel, LoggingConfig }

const LOG_CONFIG_FILE = new File(Paths.document, "logging-config.json")

let configLoadPromise: Promise<LoggingConfig> | null = null
let hasLoadedConfig = false

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
  if (!LOG_CONFIG_FILE.exists) {
    LOG_CONFIG_FILE.create({
      intermediates: true,
      overwrite: true,
    })
  }

  LOG_CONFIG_FILE.write(JSON.stringify(config), { encoding: "utf8" })
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
      if (!LOG_CONFIG_FILE.exists) {
        setLoggingConfigState(getDefaultLoggingConfig())
        hasLoadedConfig = true
        return getDefaultLoggingConfig()
      }

      const raw = await LOG_CONFIG_FILE.text()
      const parsed = sanitizeConfig(JSON.parse(raw) as Partial<LoggingConfig>)
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
