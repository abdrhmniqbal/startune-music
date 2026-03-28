import { useRouter } from "expo-router"
import { Button, Dialog, PressableFeedback, Switch } from "heroui-native"
import * as React from "react"
import { ScrollView, Text, View } from "react-native"

import LocalChevronRightIcon from "@/components/icons/local/chevron-right"
import { useThemeColors } from "@/hooks/use-theme-colors"
import {
  ensureAutoScanConfigLoaded,
  setAutoScanEnabled,
  useAutoScanStore,
} from "@/modules/indexer/auto-scan"
import { forceReindexLibrary, useIndexerStore } from "@/modules/indexer/indexer.store"
import {
  ensureTrackDurationFilterConfigLoaded,
  getTrackDurationFilterLabel,
  useTrackDurationFilterStore,
} from "@/modules/indexer/track-duration-filter"

interface LibrarySettingItemProps {
  title: string
  description?: string
  onPress?: () => void
  rightIcon?: React.ReactNode
  isDisabled?: boolean
  showChevron?: boolean
}

function LibrarySettingItem({
  title,
  description,
  onPress,
  rightIcon,
  isDisabled = false,
  showChevron = true,
}: LibrarySettingItemProps) {
  const theme = useThemeColors()

  return (
    <PressableFeedback
      onPress={isDisabled ? undefined : onPress}
      className={`flex-row items-center bg-background px-6 py-4 ${
        isDisabled ? "opacity-60" : "active:opacity-70"
      }`}
    >
      <View className="flex-1 gap-1">
        <Text className="text-[17px] font-normal text-foreground">{title}</Text>
        {description ? (
          <Text className="text-[13px] leading-5 text-muted">
            {description}
          </Text>
        ) : null}
      </View>
      <View className="flex-row items-center gap-2">
        {rightIcon}
        {showChevron ? (
          <LocalChevronRightIcon
            fill="none"
            width={20}
            height={20}
            color={theme.muted}
          />
        ) : null}
      </View>
    </PressableFeedback>
  )
}

export default function LibrarySettingsScreen() {
  const router = useRouter()
  const indexerState = useIndexerStore((state) => state.indexerState)
  const autoScanEnabled = useAutoScanStore((state) => state.autoScanEnabled)
  const trackDurationFilterConfig = useTrackDurationFilterStore(
    (state) => state.trackDurationFilterConfig
  )
  const [showReindexDialog, setShowReindexDialog] = React.useState(false)

  React.useEffect(() => {
    void ensureAutoScanConfigLoaded()
    void ensureTrackDurationFilterConfigLoaded()
  }, [])

  function handleConfirmForceReindex() {
    setShowReindexDialog(false)
    void forceReindexLibrary(true)
  }

  return (
    <>
      <ScrollView
        className="flex-1 bg-background"
        contentContainerStyle={{ paddingBottom: 40 }}
      >
        <View className="py-2">
          <LibrarySettingItem
            title="Folder Filters"
            description="Whitelist or blacklist specific folders."
            onPress={() => router.push("/settings/folder-filters")}
          />
          <LibrarySettingItem
            title="Track Duration Filter"
            description={getTrackDurationFilterLabel(trackDurationFilterConfig)}
            onPress={() => router.push("/settings/track-duration-filter")}
          />
          <LibrarySettingItem
            title="Auto Scan"
            description={
              autoScanEnabled
                ? "Re-scan on app launch and when files change."
                : "Scan manually when needed."
            }
            onPress={undefined}
            showChevron={false}
            rightIcon={
              <Switch
                isSelected={autoScanEnabled}
                onSelectedChange={(isSelected) => {
                  void setAutoScanEnabled(isSelected)
                }}
              />
            }
          />
          <LibrarySettingItem
            title="Reindex Library"
            description={
              indexerState.isIndexing
                ? "Indexing in progress..."
                : "Re-scan all tracks, including unchanged files."
            }
            onPress={() => setShowReindexDialog(true)}
            showChevron={false}
            isDisabled={indexerState.isIndexing}
          />
        </View>
      </ScrollView>

      <Dialog isOpen={showReindexDialog} onOpenChange={setShowReindexDialog}>
        <Dialog.Portal>
          <Dialog.Overlay />
          <Dialog.Content className="gap-4">
            <View className="gap-1.5">
              <Dialog.Title>Force reindex library?</Dialog.Title>
              <Dialog.Description>
                This will re-scan all music files, including already indexed and
                unchanged files. It may take longer than normal indexing.
              </Dialog.Description>
            </View>
            <View className="flex-row justify-end gap-3">
              <Button
                variant="ghost"
                onPress={() => setShowReindexDialog(false)}
              >
                Cancel
              </Button>
              <Button onPress={handleConfirmForceReindex}>Reindex</Button>
            </View>
          </Dialog.Content>
        </Dialog.Portal>
      </Dialog>
    </>
  )
}
