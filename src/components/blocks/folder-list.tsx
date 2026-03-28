import type { Track } from "@/modules/player/player.store"
import {
  LegendList,
  type LegendListRef,
  type LegendListRenderItemProps,
} from "@legendapp/list"
import { Button, PressableFeedback } from "heroui-native"
import * as React from "react"
import { useEffect, useRef } from "react"

import {
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  type RefreshControlProps,
  ScrollView,
  type StyleProp,
  Text,
  View,
  type ViewStyle,
} from "react-native"
import LocalChevronLeftIcon from "@/components/icons/local/chevron-left"
import LocalChevronRightIcon from "@/components/icons/local/chevron-right"
import LocalFolderSolidIcon from "@/components/icons/local/folder-solid"
import {
  MediaItem as Item,
  MediaItemAction as ItemAction,
  MediaItemContent as ItemContent,
  MediaItemDescription as ItemDescription,
  MediaItemImage as ItemImage,
  MediaItemTitle as ItemTitle,
} from "@/components/ui/media-item"
import { EmptyState } from "@/components/ui/empty-state"
import { ICON_SIZES } from "@/constants/icon-sizes"
import { useThemeColors } from "@/hooks/use-theme-colors"
import { formatDuration } from "@/utils/format"
import { mergeText } from "@/utils/merge-text"

import LocalMusicNoteSolidIcon from "../icons/local/music-note-solid"

export interface Folder {
  id: string
  name: string
  fileCount: number
  path?: string
}

export interface FolderBreadcrumb {
  name: string
  path: string
}

interface FolderListProps {
  data: Folder[]
  tracks?: Track[]
  breadcrumbs?: FolderBreadcrumb[]
  onFolderPress?: (folder: Folder) => void
  onTrackPress?: (track: Track) => void
  onBackPress?: () => void
  onBreadcrumbPress?: (path: string) => void
  contentContainerStyle?: StyleProp<ViewStyle>
  resetScrollKey?: string
  refreshControl?: React.ReactElement<RefreshControlProps> | null
  onScroll?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollBeginDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollEndDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onMomentumScrollEnd?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
}

type FolderListItem =
  | { id: string; type: "folder"; folder: Folder }
  | { id: string; type: "track"; track: Track }

export const FolderList: React.FC<FolderListProps> = ({
  data,
  tracks = [],
  breadcrumbs = [],
  onFolderPress,
  onTrackPress,
  onBackPress,
  onBreadcrumbPress,
  contentContainerStyle,
  resetScrollKey,
  refreshControl,
  onScroll,
  onScrollBeginDrag,
  onScrollEndDrag,
  onMomentumScrollEnd,
}) => {
  const theme = useThemeColors()
  const listRef = useRef<LegendListRef | null>(null)

  useEffect(() => {
    if (!resetScrollKey) {
      return
    }

    let frameB: number | null = null
    let timeoutId: ReturnType<typeof setTimeout> | null = null
    const frameA = requestAnimationFrame(() => {
      frameB = requestAnimationFrame(() => {
        listRef.current?.scrollToOffset({ offset: 0, animated: false })
      })
    })
    timeoutId = setTimeout(() => {
      listRef.current?.scrollToOffset({ offset: 0, animated: false })
    }, 80)

    return () => {
      cancelAnimationFrame(frameA)
      if (frameB !== null) {
        cancelAnimationFrame(frameB)
      }
      if (timeoutId !== null) {
        clearTimeout(timeoutId)
      }
    }
  }, [resetScrollKey])

  const handlePress = (folder: Folder) => {
    onFolderPress?.(folder)
  }

  const handleTrackPress = (track: Track) => {
    onTrackPress?.(track)
  }

  const formatItemCount = (count: number) =>
    `${count} ${count === 1 ? "item" : "items"}`

  const renderFolderItem = (item: Folder) => (
    <Item key={item.id} onPress={() => handlePress(item)}>
      <ItemImage
        icon={
          <LocalFolderSolidIcon
            fill="none"
            width={ICON_SIZES.listFallback}
            height={ICON_SIZES.listFallback}
            color={theme.muted}
          />
        }
      />
      <ItemContent>
        <ItemTitle>{item.name}</ItemTitle>
        <ItemDescription>{formatItemCount(item.fileCount)}</ItemDescription>
      </ItemContent>
      <ItemAction>
        <LocalChevronRightIcon
          fill="none"
          width={24}
          height={24}
          color={theme.muted}
        />
      </ItemAction>
    </Item>
  )

  const renderTrackItem = (track: Track) => (
    <Item key={track.id} onPress={() => handleTrackPress(track)}>
      <ItemImage
        icon={
          <LocalMusicNoteSolidIcon
            fill="none"
            width={ICON_SIZES.listFallback}
            height={ICON_SIZES.listFallback}
            color={theme.muted}
          />
        }
        image={track.image}
      />
      <ItemContent>
        <ItemTitle>
          {track.title || track.filename || "Unknown Track"}
        </ItemTitle>
        <ItemDescription>
          {mergeText([
            track.artist || "Unknown Artist",
            formatDuration(track.duration || 0),
          ])}
        </ItemDescription>
      </ItemContent>
    </Item>
  )

  const hasEntries = data.length > 0 || tracks.length > 0
  const hasNestedPath = breadcrumbs.length > 0
  const listData: FolderListItem[] = [
    ...data.map((folder) => ({
      id: `folder-${folder.id}`,
      type: "folder" as const,
      folder,
    })),
    ...tracks.map((track) => ({
      id: `track-${track.id}`,
      type: "track" as const,
      track,
    })),
  ]

  if (!hasEntries) {
    return (
      <EmptyState
        icon={
          <LocalFolderSolidIcon
            fill="none"
            width={48}
            height={48}
            color={theme.muted}
          />
        }
        title="No Folders"
        message="Music folders you add will appear here."
      />
    )
  }

  return (
    <View style={{ flex: 1 }}>
      <LegendList
        ref={listRef}
        maintainVisibleContentPosition={false}
        dataVersion={resetScrollKey}
        data={listData}
        keyExtractor={(item) => item.id}
        getItemType={(item) => item.type}
        renderItem={({ item }: LegendListRenderItemProps<FolderListItem>) =>
          item.type === "folder"
            ? renderFolderItem(item.folder)
            : renderTrackItem(item.track)
        }
        contentContainerStyle={[
          { gap: 8, paddingBottom: 16 },
          contentContainerStyle,
        ]}
        onScroll={onScroll}
        onScrollBeginDrag={onScrollBeginDrag}
        onScrollEndDrag={onScrollEndDrag}
        onMomentumScrollEnd={onMomentumScrollEnd}
        scrollEventThrottle={16}
        refreshControl={refreshControl || undefined}
        recycleItems={true}
        initialContainerPoolRatio={3}
        estimatedItemSize={68}
        drawDistance={180}
        style={{ flex: 1, minHeight: 1 }}
        ListHeaderComponent={
          hasNestedPath ? (
            <View className="mb-2">
              <View className="mb-2 flex-row items-center gap-2">
                <Button
                  onPress={onBackPress}
                  variant="secondary"
                  className="h-8 w-8"
                  isIconOnly
                >
                  <LocalChevronLeftIcon
                    fill="none"
                    width={16}
                    height={16}
                    color={theme.foreground}
                  />
                </Button>
                <ScrollView
                  horizontal
                  showsHorizontalScrollIndicator={false}
                  contentContainerStyle={{ alignItems: "center", gap: 8 }}
                >
                  <PressableFeedback
                    onPress={() => onBreadcrumbPress?.("")}
                    className="max-w-24"
                  >
                    <Text
                      className="text-sm text-muted"
                      numberOfLines={1}
                      ellipsizeMode="tail"
                    >
                      Folders
                    </Text>
                  </PressableFeedback>
                  {breadcrumbs.map((breadcrumb) => (
                    <View
                      key={breadcrumb.path}
                      className="flex-row items-center gap-2"
                    >
                      <LocalChevronRightIcon
                        fill="none"
                        width={12}
                        height={12}
                        color={theme.foreground}
                      />
                      <PressableFeedback
                        onPress={() => onBreadcrumbPress?.(breadcrumb.path)}
                        className="max-w-28"
                      >
                        <Text
                          className="text-sm text-foreground"
                          numberOfLines={1}
                          ellipsizeMode="tail"
                        >
                          {breadcrumb.name}
                        </Text>
                      </PressableFeedback>
                    </View>
                  ))}
                </ScrollView>
              </View>
            </View>
          ) : null
        }
      />
    </View>
  )
}
