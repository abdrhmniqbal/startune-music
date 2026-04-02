import {
  createSettingsConfigFile,
  loadSettingsConfig,
  saveSettingsConfig,
} from "@/modules/settings/settings.repository"
import {
  getDefaultFolderFilterConfig,
  getSettingsState,
  updateSettingsState,
} from "@/modules/settings/settings.store"
import type {
  FolderFilterConfig,
  FolderFilterMode,
} from "@/modules/settings/settings.types"

export type { FolderFilterConfig, FolderFilterMode }

const FOLDER_FILTERS_FILE = createSettingsConfigFile("folder-filters.json")
const EMPTY_FILTER_CONFIG = getDefaultFolderFilterConfig()

let loadPromise: Promise<FolderFilterConfig> | null = null

function convertContentUriToFilePath(uri: string): string | null {
  if (!uri.startsWith("content://")) {
    return null
  }

  const treeMatch = uri.match(/\/tree\/([^/?#]+)/)
  const documentMatch = uri.match(/\/document\/([^/?#]+)/)
  const encodedDocumentId = treeMatch?.[1] ?? documentMatch?.[1]

  if (!encodedDocumentId) {
    return null
  }

  let documentId = ""
  try {
    documentId = decodeURIComponent(encodedDocumentId)
  } catch {
    return null
  }

  const separatorIndex = documentId.indexOf(":")
  if (separatorIndex < 0) {
    return null
  }

  const volume = documentId.slice(0, separatorIndex)
  const relativePath = documentId.slice(separatorIndex + 1).replace(/^\/+/, "")
  const basePath =
    volume.toLowerCase() === "primary"
      ? "/storage/emulated/0"
      : `/storage/${volume}`

  return relativePath ? `${basePath}/${relativePath}` : basePath
}

function normalizePath(path: string): string {
  if (path.startsWith("content://")) {
    const converted = convertContentUriToFilePath(path)
    if (!converted) {
      return ""
    }
    path = converted
  }

  const withoutScheme = path.replace(/^file:\/\//, "")
  const normalizedSlashes = withoutScheme.replace(/\\/g, "/")
  const withoutQuery = normalizedSlashes.split("?")[0].split("#")[0] || ""
  const trimmed = withoutQuery.trim()

  if (!trimmed) {
    return ""
  }

  if (trimmed.length > 1 && trimmed.endsWith("/")) {
    return trimmed.slice(0, -1)
  }

  return trimmed
}

export function normalizeFolderPath(path: string): string {
  return normalizePath(path)
}

function sanitizeConfig(config: FolderFilterConfig): FolderFilterConfig {
  const whitelist = Array.from(
    new Set(config.whitelist.map(normalizePath).filter(Boolean))
  )
  const blacklist = Array.from(
    new Set(
      config.blacklist
        .map(normalizePath)
        .filter((path) => path.length > 0 && !whitelist.includes(path))
    )
  )

  return { whitelist, blacklist }
}

async function persistConfig(config: FolderFilterConfig): Promise<void> {
  await saveSettingsConfig(FOLDER_FILTERS_FILE, config)
}

export async function ensureFolderFilterConfigLoaded(): Promise<FolderFilterConfig> {
  if (loadPromise) {
    return loadPromise
  }

  loadPromise = (async () => {
    const next = await loadSettingsConfig(
      FOLDER_FILTERS_FILE,
      EMPTY_FILTER_CONFIG,
      (parsed) =>
        sanitizeConfig({
          whitelist: parsed.whitelist ?? [],
          blacklist: parsed.blacklist ?? [],
        })
    )
    updateSettingsState({ folderFilterConfig: next })
    return next
  })()

  const result = await loadPromise
  loadPromise = null
  return result
}

export async function setFolderFilterMode(
  path: string,
  mode: FolderFilterMode | null
): Promise<FolderFilterConfig> {
  const normalizedPath = normalizePath(path)
  if (!normalizedPath) {
    return getSettingsState().folderFilterConfig
  }

  await ensureFolderFilterConfigLoaded()
  const current = getSettingsState().folderFilterConfig

  const whitelist = current.whitelist.filter((item) => item !== normalizedPath)
  const blacklist = current.blacklist.filter((item) => item !== normalizedPath)

  if (mode === "whitelist") {
    whitelist.push(normalizedPath)
  } else if (mode === "blacklist") {
    blacklist.push(normalizedPath)
  }

  const next = sanitizeConfig({ whitelist, blacklist })
  updateSettingsState({ folderFilterConfig: next })
  await persistConfig(next)
  return next
}

export async function clearFolderFilters(): Promise<void> {
  updateSettingsState({ folderFilterConfig: EMPTY_FILTER_CONFIG })
  await persistConfig(EMPTY_FILTER_CONFIG)
}

export async function commitFolderFilterConfig(
  config: FolderFilterConfig
): Promise<void> {
  const sanitized = sanitizeConfig(config)
  updateSettingsState({ folderFilterConfig: sanitized })
  await persistConfig(sanitized)
}

export async function setAllFolderFiltersMode(
  mode: FolderFilterMode
): Promise<FolderFilterConfig> {
  await ensureFolderFilterConfigLoaded()
  const current = getSettingsState().folderFilterConfig
  const folders = Array.from(
    new Set([...current.whitelist, ...current.blacklist])
  )

  const next =
    mode === "whitelist"
      ? sanitizeConfig({ whitelist: folders, blacklist: [] })
      : sanitizeConfig({ whitelist: [], blacklist: folders })

  updateSettingsState({ folderFilterConfig: next })
  await persistConfig(next)
  return next
}

function isSameOrChildPath(path: string, parentPath: string): boolean {
  if (path === parentPath) {
    return true
  }

  return path.startsWith(`${parentPath}/`)
}

export function getFolderPathFromUri(uri: string): string {
  const normalized = normalizePath(uri)
  const lastSlash = normalized.lastIndexOf("/")
  if (lastSlash <= 0) {
    return ""
  }

  return normalized.slice(0, lastSlash)
}

export function isAssetAllowedByFolderFilters(
  assetUri: string,
  config: FolderFilterConfig
): boolean {
  const folderPath = getFolderPathFromUri(assetUri)
  if (!folderPath) {
    return true
  }

  const hasWhitelist = config.whitelist.length > 0
  const inWhitelist = config.whitelist.some((allowedPath) =>
    isSameOrChildPath(folderPath, allowedPath)
  )
  const inBlacklist = config.blacklist.some((blockedPath) =>
    isSameOrChildPath(folderPath, blockedPath)
  )

  if (hasWhitelist && !inWhitelist) {
    return false
  }

  if (inBlacklist) {
    return false
  }

  return true
}

export function getFolderNameFromPath(path: string): string {
  const normalized = normalizePath(path)
  if (!normalized) {
    return ""
  }

  const parts = normalized.split("/").filter(Boolean)
  const segment = parts[parts.length - 1] || normalized
  try {
    return decodeURIComponent(segment)
  } catch {
    return segment
  }
}
