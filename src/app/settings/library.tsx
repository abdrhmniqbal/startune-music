import { useRouter } from "expo-router"
import { Button, Dialog } from "heroui-native"
import * as React from "react"
import { ScrollView, View } from "react-native"

import { SettingsRow } from "@/components/patterns/settings-row"
import {
  setAutoScanEnabled,
} from "@/modules/settings/auto-scan"
import { forceReindexLibrary } from "@/modules/indexer/indexer.service"
import { useIndexerStore } from "@/modules/indexer/indexer.store"
import {
  getTrackDurationFilterLabel,
} from "@/modules/settings/track-duration-filter"
import { useSettingsStore } from "@/modules/settings/settings.store"
import { Switch } from "heroui-native"

export default function LibrarySettingsScreen() {
  const router = useRouter()
  const isIndexing = useIndexerStore((state) => state.indexerState.isIndexing)
  const autoScanEnabled = useSettingsStore((state) => state.autoScanEnabled)
  const trackDurationFilterConfig = useSettingsStore(
    (state) => state.trackDurationFilterConfig
  )
  const [showReindexDialog, setShowReindexDialog] = React.useState(false)

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
          <SettingsRow
            title="Folder Filters"
            description="Whitelist or blacklist specific folders."
            onPress={() => router.push("/settings/folder-filters")}
          />
          <SettingsRow
            title="Track Duration Filter"
            description={getTrackDurationFilterLabel(trackDurationFilterConfig)}
            onPress={() => router.push("/settings/track-duration-filter")}
          />
          <SettingsRow
            title="Auto Scan"
            description={
              autoScanEnabled
                ? "Re-scan on app launch and when files change."
                : "Scan manually when needed."
            }
            onPress={undefined}
            showChevron={false}
            rightContent={
              <Switch
                isSelected={autoScanEnabled}
                onSelectedChange={(isSelected) => {
                  void setAutoScanEnabled(isSelected)
                }}
              />
            }
          />
          <SettingsRow
            title="Reindex Library"
            description={
              isIndexing
                ? "Indexing in progress..."
                : "Re-scan all tracks, including unchanged files."
            }
            onPress={() => setShowReindexDialog(true)}
            showChevron={false}
            isDisabled={isIndexing}
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
