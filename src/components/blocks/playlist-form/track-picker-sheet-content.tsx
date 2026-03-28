import type { TrackPickerSheetContentProps } from "./types"
import { BottomSheetScrollView } from "@gorhom/bottom-sheet"
import { LegendList, type LegendListRenderItemProps } from "@legendapp/list"
import { BottomSheet, Button } from "heroui-native"
import { Text, View } from "react-native"

import { useSafeAreaInsets } from "react-native-safe-area-context"
import LocalMusicNoteSolidIcon from "@/components/icons/local/music-note-solid"
import { EmptyState } from "@/components/ui/empty-state"

import { useThemeColors } from "@/hooks/use-theme-colors"
import { PlaylistTrackRow } from "./playlist-track-row"
import { SheetSearchInput } from "./sheet-search-input"

const TRACK_PICKER_SNAP_POINTS = ["72%", "90%"]

export function TrackPickerSheetContent({
  inputKey,
  searchQuery,
  setSearchQuery,
  filteredTracks,
  selectedTracks,
  onToggleTrack,
  onApply,
  onClearSelection,
}: TrackPickerSheetContentProps) {
  const theme = useThemeColors()
  const insets = useSafeAreaInsets()
  const selectedCount = selectedTracks.size
  const applyLabel = selectedCount === 0 ? "Apply" : `Apply (${selectedCount})`

  return (
    <BottomSheet.Content
      index={1}
      snapPoints={TRACK_PICKER_SNAP_POINTS}
      enableOverDrag={false}
      enableDynamicSizing={false}
      contentContainerClassName="h-full pt-16 pb-2"
      keyboardBehavior="extend"
      backgroundClassName="bg-surface"
    >
      <SheetSearchInput
        inputKey={inputKey}
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
      />
      <LegendList
        data={filteredTracks}
        getItemType={() => "track"}
        renderItem={({
          item,
        }: LegendListRenderItemProps<(typeof filteredTracks)[number]>) => (
          <PlaylistTrackRow
            track={item}
            isSelected={selectedTracks.has(item.id)}
            onPress={() => onToggleTrack(item.id)}
          />
        )}
        keyExtractor={(item) => item.id}
        style={{ flex: 1, minHeight: 1, width: "100%" }}
        contentContainerStyle={{
          paddingTop: 6,
          paddingBottom: 24,
          paddingHorizontal: 4,
        }}
        renderScrollComponent={(props) => <BottomSheetScrollView {...props} />}
        nestedScrollEnabled={true}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
        extraData={selectedTracks}
        ListEmptyComponent={() => (
          <EmptyState
            icon={
              <LocalMusicNoteSolidIcon
                fill="none"
                width={48}
                height={48}
                color={theme.muted}
              />
            }
            title="No tracks found"
            message="Try a different keyword."
            className="py-10"
          />
        )}
        recycleItems={true}
        initialContainerPoolRatio={3}
        estimatedItemSize={68}
        drawDistance={180}
      />
      <View
        className="border-t border-border/60 px-4 pt-3"
        style={{ paddingBottom: Math.max(insets.bottom, 12) }}
      >
        <View className="mb-2 flex-row items-center justify-between">
          <Text className="text-sm text-muted">{selectedCount} selected</Text>
          <Button
            onPress={onClearSelection}
            variant="ghost"
            isDisabled={selectedCount === 0}
            className="h-8 px-0"
          >
            Clear selection
          </Button>
        </View>
        <Button onPress={onApply} variant="primary" className="w-full">
          {applyLabel}
        </Button>
      </View>
    </BottomSheet.Content>
  )
}
