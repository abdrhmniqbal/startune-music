import {
  LegendList,
  type LegendListRef,
  type LegendListRenderItemProps,
} from "@legendapp/list"
import { PressableFeedback } from "heroui-native"
import * as React from "react"
import { useRef, useState } from "react"
import {
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  type RefreshControlProps,
  type StyleProp,
  View,
  type ViewStyle,
} from "react-native"

import { TrackActionSheet } from "@/components/blocks/track-action-sheet"
import { LEGEND_LIST_ROW_CONFIG } from "@/components/blocks/legend-list-config"
import LocalMoreHorizontalCircleSolidIcon from "@/components/icons/local/more-horizontal-circle-solid"
import LocalMusicNoteSolidIcon from "@/components/icons/local/music-note-solid"
import { TrackRow } from "@/components/patterns/track-row"
import { EmptyState } from "@/components/ui/empty-state"
import { ScaleLoader } from "@/components/ui/scale-loader"
import { useThemeColors } from "@/modules/ui/theme"
import { playTrack } from "@/modules/player/player.service"
import { usePlayerStore, type Track } from "@/modules/player/player.store"
import { useResetScrollOnKey } from "@/components/blocks/use-reset-scroll-on-key"

interface TrackListProps {
  data: Track[]
  onTrackPress?: (track: Track) => void
  showNumbers?: boolean
  hideCover?: boolean
  hideArtist?: boolean
  getNumber?: (track: Track, index: number) => number | string
  scrollEnabled?: boolean
  listHeader?: React.ReactElement | null
  listFooter?: React.ReactElement | null
  contentContainerStyle?: StyleProp<ViewStyle>
  showsVerticalScrollIndicator?: boolean
  scrollEventThrottle?: number
  onScroll?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollBeginDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollEndDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onMomentumScrollEnd?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  refreshControl?: React.ReactElement<RefreshControlProps> | null
  resetScrollKey?: string
  renderItemPrefix?: (
    track: Track,
    index: number,
    data: Track[]
  ) => React.ReactNode
}

export const TrackList: React.FC<TrackListProps> = ({
  data,
  onTrackPress,
  showNumbers = false,
  hideCover = false,
  hideArtist = false,
  getNumber,
  scrollEnabled = true,
  listHeader = null,
  listFooter = null,
  contentContainerStyle,
  showsVerticalScrollIndicator = false,
  scrollEventThrottle = 16,
  onScroll,
  onScrollBeginDrag,
  onScrollEndDrag,
  onMomentumScrollEnd,
  refreshControl,
  resetScrollKey,
  renderItemPrefix,
}) => {
  const theme = useThemeColors()
  const [selectedTrack, setSelectedTrack] = useState<Track | null>(null)
  const [isSheetOpen, setIsSheetOpen] = useState(false)
  const listRef = useRef<LegendListRef | null>(null)
  const isCompactNumberedList = hideCover && showNumbers
  const currentTrackId = usePlayerStore((state) => state.currentTrack?.id)

  useResetScrollOnKey(listRef, resetScrollKey)

  const handlePress = (track: Track) => {
    if (onTrackPress) {
      onTrackPress(track)
    } else {
      playTrack(track, data)
    }
  }

  const showActionMenu = (track: Track) => {
    setSelectedTrack(track)
    setIsSheetOpen(true)
  }

  const renderTrackItem = (item: Track, index: number) => (
    <>
      {renderItemPrefix?.(item, index, data) || null}
      <TrackRow
        key={`${item.id}-${index}`}
        track={item}
        onPress={() => handlePress(item)}
        onLongPress={() => showActionMenu(item)}
        rank={
          showNumbers
            ? getNumber
              ? getNumber(item, index)
              : index + 1
            : undefined
        }
        showCover={!hideCover}
        showArtist={!hideArtist}
        titleClassName={currentTrackId === item.id ? "text-accent" : undefined}
        imageOverlay={
          currentTrackId === item.id ? <ScaleLoader size={16} /> : undefined
        }
        rightAction={
          <PressableFeedback
            onPress={(event) => {
              event.stopPropagation()
              showActionMenu(item)
            }}
            className="p-2"
          >
            <LocalMoreHorizontalCircleSolidIcon
              fill="none"
              width={24}
              height={24}
              color={theme.muted}
            />
          </PressableFeedback>
        }
      />
    </>
  )

  return (
    <View style={{ flex: 1 }}>
      <LegendList
        ref={listRef}
        maintainVisibleContentPosition={false}
        dataVersion={resetScrollKey}
        data={data}
        renderItem={({ item, index }: LegendListRenderItemProps<Track>) =>
          renderTrackItem(item, index)
        }
        keyExtractor={(item) => item.id}
        style={{ flex: 1, minHeight: 1 }}
        scrollEnabled={scrollEnabled}
        showsVerticalScrollIndicator={showsVerticalScrollIndicator}
        ListHeaderComponent={listHeader}
        ListFooterComponent={listFooter}
        ListEmptyComponent={
          <EmptyState
            icon={
              <LocalMusicNoteSolidIcon
                fill="none"
                width={48}
                height={48}
                color={theme.muted}
              />
            }
            title="No Tracks"
            message="Tracks you add to your library will appear here."
          />
        }
        contentContainerStyle={[
          { gap: isCompactNumberedList ? 0 : 8 },
          contentContainerStyle,
        ]}
        onScroll={onScroll}
        onScrollBeginDrag={onScrollBeginDrag}
        onScrollEndDrag={onScrollEndDrag}
        onMomentumScrollEnd={onMomentumScrollEnd}
        scrollEventThrottle={scrollEventThrottle}
        refreshControl={refreshControl || undefined}
        {...LEGEND_LIST_ROW_CONFIG}
      />
      <TrackActionSheet
        track={selectedTrack}
        isOpen={isSheetOpen}
        onClose={() => setIsSheetOpen(false)}
        tracks={data}
      />
    </View>
  )
}
