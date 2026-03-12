import {
  LegendList,
  type LegendListRef,
  type LegendListRenderItemProps,
} from "@legendapp/list"
import * as React from "react"
import { useEffect, useRef } from "react"
import {
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  type RefreshControlProps,
  type StyleProp,
  View,
  type ViewStyle,
} from "react-native"

import LocalAddIcon from "@/components/icons/local/add"
import LocalChevronRightIcon from "@/components/icons/local/chevron-right"
import LocalPlaylistSolidIcon from "@/components/icons/local/playlist-solid"
import { PlaylistArtwork } from "@/components/patterns"
import {
  EmptyState,
  Item,
  ItemAction,
  ItemContent,
  ItemDescription,
  ItemImage,
  ItemTitle,
} from "@/components/ui"
import { useThemeColors } from "@/hooks/use-theme-colors"

export interface Playlist {
  id: string
  title: string
  trackCount: number
  image?: string
  images?: string[]
}

type PlaylistListRow =
  | { id: string; rowType: "create" }
  | (Playlist & { rowType: "playlist" })

interface PlaylistListProps {
  data: Playlist[]
  onPlaylistPress?: (playlist: Playlist) => void
  onCreatePlaylist?: () => void
  scrollEnabled?: boolean
  contentContainerStyle?: StyleProp<ViewStyle>
  resetScrollKey?: string
  refreshControl?: React.ReactElement<RefreshControlProps> | null
  onScroll?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollBeginDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollEndDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onMomentumScrollEnd?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
}

export const PlaylistList: React.FC<PlaylistListProps> = ({
  data,
  onPlaylistPress,
  onCreatePlaylist,
  scrollEnabled = true,
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

  const handlePress = (playlist: Playlist) => {
    onPlaylistPress?.(playlist)
  }

  const handleCreate = () => {
    onCreatePlaylist?.()
  }

  const formatTrackCount = (count: number) =>
    `${count} ${count === 1 ? "track" : "tracks"}`

  const renderCreateButton = () => (
    <Item key="create" onPress={handleCreate}>
      <ItemImage className="items-center justify-center bg-surface">
        <LocalAddIcon
          fill="none"
          width={24}
          height={24}
          color={theme.foreground}
        />
      </ItemImage>
      <ItemContent>
        <ItemTitle>New Playlist</ItemTitle>
      </ItemContent>
    </Item>
  )

  const renderPlaylistItem = (item: Playlist) => (
    <Item key={item.id} onPress={() => handlePress(item)}>
      <ItemImage className="items-center justify-center overflow-hidden bg-default">
        <PlaylistArtwork
          images={
            item.images && item.images.length > 0
              ? item.images
              : item.image
                ? [item.image]
                : undefined
          }
        />
      </ItemImage>
      <ItemContent>
        <ItemTitle>{item.title}</ItemTitle>
        <ItemDescription>{formatTrackCount(item.trackCount)}</ItemDescription>
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

  if (data.length === 0) {
    return (
      <View style={{ flex: 1 }}>
        <LegendList
          ref={listRef}
          maintainVisibleContentPosition={false}
          dataVersion={resetScrollKey}
          data={[{ id: "create", rowType: "create" }]}
          renderItem={() => renderCreateButton()}
          keyExtractor={(item) => item.id}
          getItemType={(item) => item.rowType}
          scrollEnabled={scrollEnabled}
          contentContainerStyle={[{ gap: 8 }, contentContainerStyle]}
          onScroll={onScroll}
          onScrollBeginDrag={onScrollBeginDrag}
          onScrollEndDrag={onScrollEndDrag}
          onMomentumScrollEnd={onMomentumScrollEnd}
          scrollEventThrottle={16}
          refreshControl={refreshControl || undefined}
          recycleItems={true}
          initialContainerPoolRatio={3}
          ListEmptyComponent={
            <EmptyState
              icon={
                <LocalPlaylistSolidIcon
                  fill="none"
                  width={48}
                  height={48}
                  color={theme.muted}
                />
              }
              title="No Playlists"
              message="Create your first playlist to organize your music."
            />
          }
          estimatedItemSize={68}
          drawDistance={180}
          style={{ flex: 1, minHeight: 1 }}
        />
      </View>
    )
  }

  const listData: PlaylistListRow[] = [
    { id: "create", rowType: "create" },
    ...data.map((playlist) => ({ ...playlist, rowType: "playlist" as const })),
  ]

  return (
    <View style={{ flex: 1 }}>
      <LegendList
        ref={listRef}
        maintainVisibleContentPosition={false}
        dataVersion={resetScrollKey}
        data={listData}
        renderItem={({ item }: LegendListRenderItemProps<PlaylistListRow>) => {
          if (item.rowType === "create") {
            return renderCreateButton()
          }
          return renderPlaylistItem(item)
        }}
        keyExtractor={(item) => item.id}
        getItemType={(item) => item.rowType}
        scrollEnabled={scrollEnabled}
        contentContainerStyle={[{ gap: 8 }, contentContainerStyle]}
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
      />
    </View>
  )
}
