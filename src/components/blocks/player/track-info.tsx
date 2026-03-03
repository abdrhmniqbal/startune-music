import * as React from "react"
import { PressableFeedback } from "heroui-native"
import { View } from "react-native"
import Animated, { Layout } from "react-native-reanimated"
import { cn } from "tailwind-variants"

import {
  useIsFavorite,
  useToggleFavorite,
} from "@/modules/favorites/favorites.queries"
import type { Track } from "@/modules/player/player.store"
import LocalFavouriteIcon from "@/components/icons/local/favourite"
import LocalFavouriteSolidIcon from "@/components/icons/local/favourite-solid"
import { MarqueeText } from "@/components/ui"

interface TrackInfoProps {
  track: Track
  compact?: boolean
  onPressArtist?: () => void
}

export const TrackInfo: React.FC<TrackInfoProps> = ({
  track,
  compact = false,
  onPressArtist,
}) => {
  const { data: isFavoriteQuery = track.isFavorite ?? false } = useIsFavorite(
    "track",
    track.id
  )
  const toggleFavoriteMutation = useToggleFavorite()
  const isFavorite = Boolean(isFavoriteQuery)
  const titleClassName = cn(
    "mb-1 font-bold text-white",
    compact ? "text-xl" : "text-2xl"
  )
  const artistClassName = cn("text-white/60", compact ? "text-base" : "text-lg")
  const artistName = track.artist || "Unknown Artist"
  const isArtistPressable = Boolean(onPressArtist && track.artist?.trim())

  return (
    <Animated.View
      layout={Layout.duration(300)}
      className={`flex-row items-center justify-between ${compact ? "mb-3" : "mb-6"}`}
    >
      <View className="mr-4 flex-1">
        <MarqueeText text={track.title} className={titleClassName} />
        {isArtistPressable ? (
          <PressableFeedback onPress={onPressArtist} hitSlop={8}>
            <MarqueeText text={artistName} className={artistClassName} />
          </PressableFeedback>
        ) : (
          <MarqueeText text={artistName} className={artistClassName} />
        )}
      </View>
      <PressableFeedback
        onPress={() => {
          void toggleFavoriteMutation.mutateAsync({
            type: "track",
            itemId: track.id,
            isCurrentlyFavorite: isFavorite,
            name: track.title,
            subtitle: track.artist,
            image: track.image,
          })
        }}
      >
        {isFavorite ? (
          <LocalFavouriteSolidIcon
            fill="none"
            width={24}
            height={24}
            color="#ef4444"
          />
        ) : (
          <LocalFavouriteIcon
            fill="none"
            width={24}
            height={24}
            color="white"
          />
        )}
      </PressableFeedback>
    </Animated.View>
  )
}
