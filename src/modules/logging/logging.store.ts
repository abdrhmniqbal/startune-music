import { File, Paths } from "expo-file-system"
import { create } from "zustand"

export type AppLogLevel = "minimal" | "extra"

export interface LoggingConfig {
  level: AppLogLevel
}

const DEFAULT_LOGGING_CONFIG: LoggingConfig = {
  level: "minimal",
}

const LOG_CONFIG_FILE = new File(Paths.document, "logging-config.json")

interface LoggingStoreState {
  loggingConfig: LoggingConfig
}

export const useLoggingStore = create<LoggingStoreState>(() => ({
  loggingConfig: DEFAULT_LOGGING_CONFIG,
}))

let configLoadPromise: Promise<LoggingConfig> | null = null
let hasLoadedConfig = false

export function getDefaultLoggingConfig() {
  return DEFAULT_LOGGING_CONFIG
}

export function getLoggingConfigState() {
  return useLoggingStore.getState().loggingConfig
}

export function setLoggingConfigState(value: LoggingConfig) {
  useLoggingStore.setState({ loggingConfig: value })
}

function isValidLogLevel(value: unknown): value is AppLogLevel {
  return value === "minimal" || value === "extra"
}

function sanitizeConfig(value: Partial<LoggingConfig>): LoggingConfig {
  return {
    level: isValidLogLevel(value.level)
      ? value.level
      : DEFAULT_LOGGING_CONFIG.level,
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
        setLoggingConfigState(DEFAULT_LOGGING_CONFIG)
        hasLoadedConfig = true
        return DEFAULT_LOGGING_CONFIG
      }

      const raw = await LOG_CONFIG_FILE.text()
      const parsed = sanitizeConfig(JSON.parse(raw) as Partial<LoggingConfig>)
      setLoggingConfigState(parsed)
      hasLoadedConfig = true
      return parsed
    } catch {
      setLoggingConfigState(DEFAULT_LOGGING_CONFIG)
      hasLoadedConfig = true
      return DEFAULT_LOGGING_CONFIG
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
