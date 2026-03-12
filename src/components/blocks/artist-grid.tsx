import {
  LegendList,
  type LegendListRef,
  type LegendListRenderItemProps,
} from "@legendapp/list"
import * as React from "react"
import { useEffect, useRef } from "react"
import {
  Dimensions,
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  type RefreshControlProps,
  type StyleProp,
  View,
  type ViewStyle,
} from "react-native"

import LocalUserSolidIcon from "@/components/icons/local/user-solid"
import {
  EmptyState,
  Item,
  ItemContent,
  ItemDescription,
  ItemImage,
  ItemTitle,
} from "@/components/ui"
import { ICON_SIZES } from "@/constants/icon-sizes"
import { useThemeColors } from "@/hooks/use-theme-colors"

export interface Artist {
  id: string
  name: string
  trackCount: number
  image?: string
  dateAdded: number
}

interface ArtistGridProps {
  data: Artist[]
  onArtistPress?: (artist: Artist) => void
  scrollEnabled?: boolean
  contentContainerStyle?: StyleProp<ViewStyle>
  resetScrollKey?: string
  onScroll?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollBeginDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollEndDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onMomentumScrollEnd?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  refreshControl?: React.ReactElement<RefreshControlProps> | null
}

const GAP = 12
const NUM_COLUMNS = 3
const SCREEN_WIDTH = Dimensions.get("window").width
const HORIZONTAL_PADDING = 32
const ITEM_WIDTH =
  (SCREEN_WIDTH - HORIZONTAL_PADDING - GAP * (NUM_COLUMNS - 1)) / NUM_COLUMNS

export const ArtistGrid: React.FC<ArtistGridProps> = ({
  data,
  onArtistPress,
  scrollEnabled = true,
  contentContainerStyle,
  resetScrollKey,
  onScroll,
  onScrollBeginDrag,
  onScrollEndDrag,
  onMomentumScrollEnd,
  refreshControl,
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

  const handlePress = (artist: Artist) => {
    onArtistPress?.(artist)
  }

  const formatTrackCount = (count: number) =>
    `${count} ${count === 1 ? "track" : "tracks"}`

  if (data.length === 0) {
    return (
      <EmptyState
        icon={
          <LocalUserSolidIcon
            fill="none"
            width={ICON_SIZES.emptyState}
            height={ICON_SIZES.emptyState}
            color={theme.muted}
          />
        }
        title="No Artists"
        message="Artists from your music library will appear here."
      />
    )
  }

  return (
    <View style={{ flex: 1 }}>
      <LegendList
        ref={listRef}
        maintainVisibleContentPosition={false}
        dataVersion={resetScrollKey}
        data={data}
        renderItem={({ item, index }: LegendListRenderItemProps<Artist>) => {
          const column = index % NUM_COLUMNS
          return (
            <Item
              key={item.id}
              variant="grid"
              style={{
                width: ITEM_WIDTH,
                marginRight: column < NUM_COLUMNS - 1 ? GAP : 0,
                marginBottom: GAP,
              }}
              onPress={() => handlePress(item)}
            >
              <ItemImage
                icon={
                  <LocalUserSolidIcon
                    fill="none"
                    width={ICON_SIZES.gridFallback}
                    height={ICON_SIZES.gridFallback}
                    color={theme.muted}
                  />
                }
                image={item.image}
                className="aspect-square w-full rounded-full bg-default"
              />
              <ItemContent className="mt-1 items-center">
                <ItemTitle
                  className="text-center text-sm normal-case"
                  numberOfLines={1}
                >
                  {item.name}
                </ItemTitle>
                <ItemDescription className="text-center">
                  {formatTrackCount(item.trackCount)}
                </ItemDescription>
              </ItemContent>
            </Item>
          )
        }}
        keyExtractor={(item) => item.id}
        scrollEnabled={scrollEnabled}
        numColumns={NUM_COLUMNS}
        contentContainerStyle={[{ paddingBottom: 8 }, contentContainerStyle]}
        onScroll={onScroll}
        onScrollBeginDrag={onScrollBeginDrag}
        onScrollEndDrag={onScrollEndDrag}
        onMomentumScrollEnd={onMomentumScrollEnd}
        scrollEventThrottle={16}
        refreshControl={refreshControl || undefined}
        style={{ flex: 1, minHeight: 1 }}
        recycleItems={true}
        initialContainerPoolRatio={2.5}
        estimatedItemSize={132}
        drawDistance={160}
      />
    </View>
  )
}
