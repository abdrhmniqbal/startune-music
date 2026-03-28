import { Directory } from "expo-file-system"
import { BottomSheet, Button, PressableFeedback } from "heroui-native"
import * as React from "react"
import { ScrollView, Text, View } from "react-native"
import { useSafeAreaInsets } from "react-native-safe-area-context"

import LocalAddIcon from "@/components/icons/local/add"
import LocalCancelIcon from "@/components/icons/local/cancel"
import LocalFolderSolidIcon from "@/components/icons/local/folder-solid"
import LocalTickIcon from "@/components/icons/local/tick"
import { EmptyState } from "@/components/ui/empty-state"
import {
  commitFolderFilterConfig,
  type FolderFilterConfig,
  type FolderFilterMode,
  getFolderNameFromPath,
  getFolderPathFromUri,
  normalizeFolderPath,
} from "@/modules/indexer/folder-filters"
import { startIndexing } from "@/modules/indexer/indexer.service"
import { useIndexerStore } from "@/modules/indexer/indexer.store"
import { usePlayerStore } from "@/modules/player/player.store"
import { useSettingsStore } from "@/modules/settings/settings.store"
import { useThemeColors } from "@/modules/ui/theme"

interface FolderEntry {
  path: string
  name: string
  trackCount: number
}

function buildFolderEntries(
  trackUris: Array<{ uri?: string | null }>
): FolderEntry[] {
  const folderMap = new Map<string, FolderEntry>()

  for (const track of trackUris) {
    const uri = track.uri || ""
    const folderPath = getFolderPathFromUri(uri)
    if (!folderPath) {
      continue
    }

    const existing = folderMap.get(folderPath)
    if (existing) {
      existing.trackCount += 1
      continue
    }

    folderMap.set(folderPath, {
      path: folderPath,
      name: getFolderNameFromPath(folderPath),
      trackCount: 1,
    })
  }

  return Array.from(folderMap.values()).sort((a, b) =>
    a.name.localeCompare(b.name, undefined, { sensitivity: "base" })
  )
}

interface FolderRowProps {
  path: string
  allFolders: FolderEntry[]
  removeIconColor: string
  isLast?: boolean
  isDisabled?: boolean
  onRemove: (path: string) => void
}

function FolderRow({
  path,
  allFolders,
  removeIconColor,
  isLast = false,
  isDisabled = false,
  onRemove,
}: FolderRowProps) {
  const folder = allFolders.find((entry) => entry.path === path)
  const displayName = folder?.name || getFolderNameFromPath(path)
  const trackCount = folder?.trackCount ?? 0

  return (
    <View className={`py-3 ${isLast ? "" : "border-b border-border/60"}`}>
      <View className="flex-row items-start justify-between">
        <View className="flex-1 gap-1 pr-2">
          <Text className="text-base font-semibold text-foreground">
            {displayName}
          </Text>
          <Text className="text-xs leading-4 text-muted" numberOfLines={2}>
            {path}
          </Text>
          {trackCount > 0 ? (
            <Text className="text-xs text-muted">
              {trackCount} {trackCount === 1 ? "track" : "tracks"}
            </Text>
          ) : null}
        </View>
        <Button
          variant="ghost"
          onPress={() => onRemove(path)}
          isDisabled={isDisabled}
          hitSlop={8}
          isIconOnly
        >
          <LocalCancelIcon
            fill="none"
            width={18}
            height={18}
            color={removeIconColor}
          />
        </Button>
      </View>
    </View>
  )
}

const EMPTY_PENDING: FolderFilterConfig = { whitelist: [], blacklist: [] }

function getModeFromConfig(config: FolderFilterConfig): FolderFilterMode {
  if (config.whitelist.length > 0) {
    return "whitelist"
  }

  if (config.blacklist.length > 0) {
    return "blacklist"
  }

  return "whitelist"
}

export default function FolderFiltersScreen() {
  const insets = useSafeAreaInsets()
  const theme = useThemeColors()
  const tracks = usePlayerStore((state) => state.tracks)
  const folderFilterConfig = useSettingsStore((state) => state.folderFilterConfig)
  const indexerState = useIndexerStore((state) => state.indexerState)
  const [pendingConfig, setPendingConfig] =
    React.useState<FolderFilterConfig>(folderFilterConfig)
  const [selectedMode, setSelectedMode] =
    React.useState<FolderFilterMode>(() => getModeFromConfig(folderFilterConfig))
  const [hasPendingChanges, setHasPendingChanges] = React.useState(false)
  const [isModeSheetOpen, setIsModeSheetOpen] = React.useState(false)

  const allFolders = buildFolderEntries(tracks)
  const folderPaths = Array.from(
    new Set([...pendingConfig.whitelist, ...pendingConfig.blacklist])
  ).sort((a, b) =>
    getFolderNameFromPath(a).localeCompare(
      getFolderNameFromPath(b),
      undefined,
      {
        sensitivity: "base",
      }
    )
  )
  const hasAnyFilters = folderPaths.length > 0

  async function pickFolder() {
    try {
      const directory = await Directory.pickDirectoryAsync()
      if (!directory?.uri) {
        return
      }

      const normalizedPath = normalizeFolderPath(directory.uri)
      if (!normalizedPath) {
        return
      }

      setPendingConfig((prev) => {
        const whitelist = prev.whitelist.filter((p) => p !== normalizedPath)
        const blacklist = prev.blacklist.filter((p) => p !== normalizedPath)
        if (selectedMode === "whitelist") {
          whitelist.push(normalizedPath)
        } else {
          blacklist.push(normalizedPath)
        }
        return { whitelist, blacklist }
      })
      setHasPendingChanges(true)
    } catch {
      // User cancelled picker.
    }
  }

  function removeFolder(path: string) {
    setPendingConfig((prev) => ({
      whitelist: prev.whitelist.filter((p) => p !== path),
      blacklist: prev.blacklist.filter((p) => p !== path),
    }))
    setHasPendingChanges(true)
  }

  function clearAllFolders() {
    setPendingConfig(EMPTY_PENDING)
    setHasPendingChanges(true)
  }

  function setUnifiedMode(mode: FolderFilterMode) {
    if (mode === selectedMode) {
      return
    }

    setSelectedMode(mode)
    if (!hasAnyFilters) {
      return
    }

    setPendingConfig((prev) => {
      const folders = Array.from(
        new Set([...prev.whitelist, ...prev.blacklist])
      )
      return mode === "whitelist"
        ? { whitelist: folders, blacklist: [] }
        : { whitelist: [], blacklist: folders }
    })
    setHasPendingChanges(true)
  }

  async function applyFilter() {
    if (!hasPendingChanges) {
      return
    }

    await commitFolderFilterConfig(pendingConfig)
    await startIndexing(false, true)
    setHasPendingChanges(false)
  }

  function getModeLabel() {
    if (selectedMode === "whitelist") {
      return "Whitelist"
    }
    if (selectedMode === "blacklist") {
      return "Blacklist"
    }
    return "Whitelist"
  }

  return (
    <View className="flex-1 bg-background">
      <ScrollView
        className="flex-1"
        contentContainerStyle={{
          paddingBottom: 112 + insets.bottom,
          paddingHorizontal: 24,
          paddingTop: 16,
        }}
      >
        <View className="mb-6">
          <View className="flex-row items-center justify-between py-3">
            <Text className="text-[17px] font-normal text-foreground">
              Filter Mode
            </Text>
            <Button
              variant="secondary"
              onPress={() => setIsModeSheetOpen(true)}
              isDisabled={indexerState.isIndexing}
            >
              {getModeLabel()}
            </Button>
          </View>

          <Button
            onPress={() => {
              void pickFolder()
            }}
            variant="secondary"
            isDisabled={indexerState.isIndexing}
            className="my-3"
          >
            <LocalAddIcon
              fill="none"
              width={24}
              height={24}
              color={theme.accent}
            />
            <Button.Label>Add New Folder</Button.Label>
          </Button>
        </View>

        <View className="mb-2 flex-row items-center justify-between">
          <Text className="text-lg font-bold text-foreground">Folders</Text>
          <Button
            variant="ghost"
            onPress={clearAllFolders}
            isDisabled={!hasAnyFilters || indexerState.isIndexing}
          >
            Clear All
          </Button>
        </View>

        {folderPaths.length === 0 ? (
          <EmptyState
            icon={
              <LocalFolderSolidIcon
                fill="none"
                width={40}
                height={40}
                color="#94a3b8"
              />
            }
            title="No folders added"
            message="Add one or more folders, then apply filter."
            className="mt-4"
          />
        ) : (
          <View>
            {folderPaths.map((path, index) => (
              <FolderRow
                key={path}
                path={path}
                allFolders={allFolders}
                removeIconColor={theme.muted}
                isLast={index === folderPaths.length - 1}
                onRemove={removeFolder}
                isDisabled={indexerState.isIndexing}
              />
            ))}
          </View>
        )}
      </ScrollView>

      <View
        className="absolute right-0 bottom-0 left-0 border-t border-border bg-background px-4 pt-3"
        style={{ paddingBottom: Math.max(insets.bottom, 12) }}
      >
        <Button
          onPress={() => {
            void applyFilter()
          }}
          isDisabled={!hasPendingChanges || indexerState.isIndexing}
        >
          {indexerState.isIndexing ? "Indexing..." : "Apply Filter"}
        </Button>
      </View>

      <BottomSheet isOpen={isModeSheetOpen} onOpenChange={setIsModeSheetOpen}>
        <BottomSheet.Portal>
          <BottomSheet.Overlay />
          <BottomSheet.Content
            className="gap-1"
            backgroundClassName="bg-surface"
          >
            <BottomSheet.Title className="mb-1 text-xl">
              Select Filter Mode
            </BottomSheet.Title>
            <PressableFeedback
              className="h-14 flex-row items-center justify-between active:opacity-60"
              onPress={() => {
                setUnifiedMode("whitelist")
                setIsModeSheetOpen(false)
              }}
            >
              <Text
                className={`text-base ${
                  selectedMode === "whitelist"
                    ? "text-accent"
                    : "text-foreground"
                }`}
              >
                Whitelist
              </Text>
              {selectedMode === "whitelist" ? (
                <LocalTickIcon
                  fill="none"
                  width={20}
                  height={20}
                  color={theme.accent}
                />
              ) : null}
            </PressableFeedback>
            <PressableFeedback
              className="h-14 flex-row items-center justify-between active:opacity-60"
              onPress={() => {
                setUnifiedMode("blacklist")
                setIsModeSheetOpen(false)
              }}
            >
              <Text
                className={`text-base ${
                  selectedMode === "blacklist"
                    ? "text-accent"
                    : "text-foreground"
                }`}
              >
                Blacklist
              </Text>
              {selectedMode === "blacklist" ? (
                <LocalTickIcon
                  fill="none"
                  width={20}
                  height={20}
                  color={theme.accent}
                />
              ) : null}
            </PressableFeedback>
          </BottomSheet.Content>
        </BottomSheet.Portal>
      </BottomSheet>
    </View>
  )
}
