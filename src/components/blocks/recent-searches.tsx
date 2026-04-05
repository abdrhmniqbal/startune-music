import { PressableFeedback } from "heroui-native"
import * as React from "react"
import { useCallback } from "react"
import { Text, View } from "react-native"

import LocalCancelIcon from "@/components/icons/local/cancel"
import LocalClockSolidIcon from "@/components/icons/local/clock-solid"
import LocalPlaylistSolidIcon from "@/components/icons/local/playlist-solid"
import LocalUserIcon from "@/components/icons/local/user"
import LocalVynilSolidIcon from "@/components/icons/local/vynil-solid"
import {
  MediaItem as Item,
  MediaItemAction as ItemAction,
  MediaItemContent as ItemContent,
  MediaItemDescription as ItemDescription,
  MediaItemImage as ItemImage,
  MediaItemTitle as ItemTitle,
} from "@/components/ui/media-item"
import { useThemeColors } from "@/modules/ui/theme"

export interface RecentSearchItem {
  id: string
  query: string
  title: string
  subtitle: string
  type?: "track" | "album" | "artist" | "playlist"
  targetId?: string
  image?: string
}

interface RecentSearchesProps {
  searches: RecentSearchItem[]
  onClear: () => void
  onItemPress: (item: RecentSearchItem) => void
  onRemoveItem: (id: string) => void
}

interface RecentSearchRowProps {
  item: RecentSearchItem
  icon: React.ReactNode
  mutedColor: string
  onPress: (item: RecentSearchItem) => void
  onRemove: (id: string) => void
}

function RecentSearchRow({
  item,
  icon,
  mutedColor,
  onPress,
  onRemove,
}: RecentSearchRowProps) {
  const imageClassName = item.type === "artist" ? "rounded-full" : "rounded-md"

  return (
    <Item onPress={() => onPress(item)}>
      <ItemImage icon={icon} image={item.image} className={imageClassName} />
      <ItemContent>
        <ItemTitle>{item.title}</ItemTitle>
        <ItemDescription>{item.subtitle}</ItemDescription>
      </ItemContent>
      <ItemAction className="p-2" onPress={() => onRemove(item.id)}>
        <LocalCancelIcon
          fill="none"
          width={20}
          height={20}
          color={mutedColor}
        />
      </ItemAction>
    </Item>
  )
}

export const RecentSearches: React.FC<RecentSearchesProps> = ({
  searches,
  onClear,
  onItemPress,
  onRemoveItem,
}) => {
  const theme = useThemeColors()

  const getIconForType = useCallback((type?: string) => {
    switch (type) {
      case "artist":
        return (
          <LocalUserIcon
            fill="none"
            width={24}
            height={24}
            color={theme.muted}
          />
        )
      case "album":
        return (
          <LocalVynilSolidIcon
            fill="none"
            width={24}
            height={24}
            color={theme.muted}
          />
        )
      case "playlist":
        return (
          <LocalPlaylistSolidIcon
            fill="none"
            width={24}
            height={24}
            color={theme.muted}
          />
        )
      default:
        return (
          <LocalClockSolidIcon
            fill="none"
            width={24}
            height={24}
            color={theme.muted}
          />
        )
    }
  }, [theme.muted])

  if (searches.length === 0) {
    return null
  }

  return (
    <View className="px-4 py-4">
      <View className="mb-6 flex-row items-center justify-between">
        <Text className="text-lg font-bold text-foreground">
          Recent Searches
        </Text>
        <PressableFeedback className="active:opacity-50" onPress={onClear}>
          <Text className="text-muted">Clear</Text>
        </PressableFeedback>
      </View>
      <View className="gap-2">
        {searches.map((item) => (
          <RecentSearchRow
            key={item.id}
            item={item}
            icon={getIconForType(item.type)}
            mutedColor={theme.muted}
            onPress={onItemPress}
            onRemove={onRemoveItem}
          />
        ))}
      </View>
    </View>
  )
}
