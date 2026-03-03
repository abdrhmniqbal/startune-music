import type { ReactNode } from "react"
import { useStore } from "@nanostores/react"
import { View } from "react-native"

import {
  $currentTrack,
  playTrack,
  type Track,
} from "@/modules/player/player.store"
import { chunkArray } from "@/utils/array"
import { TrackRow } from "@/components/patterns"
import { ScaleLoader } from "@/components/ui"

import { MediaCarousel } from "./media-carousel"

interface EmptyStateConfig {
  icon: ReactNode
  title: string
  message: string
}

interface RankedTrackCarouselProps {
  data: Track[]
  chunkSize?: number
  emptyState?: EmptyStateConfig
  onItemPress?: (track: Track) => void
  className?: string
}

export function RankedTrackCarousel({
  data,
  chunkSize = 5,
  emptyState,
  onItemPress,
  className,
}: RankedTrackCarouselProps) {
  const currentTrack = useStore($currentTrack)
  const chunks = chunkArray(data, chunkSize)

  const handlePress = (track: Track) => {
    if (onItemPress) {
      onItemPress(track)
      return
    }

    playTrack(track, data)
  }

  return (
    <MediaCarousel
      data={chunks}
      keyExtractor={(_, index) => `chunk-${index}`}
      emptyState={emptyState}
      gap={24}
      className={className}
      renderItem={(chunk, chunkIndex) => (
        <View className="w-75">
          {chunk.map((track, index) => (
            <TrackRow
              key={track.id}
              track={track}
              rank={chunkIndex * chunkSize + index + 1}
              onPress={() => handlePress(track)}
              titleClassName={
                currentTrack?.id === track.id ? "text-accent" : undefined
              }
              imageOverlay={
                currentTrack?.id === track.id ? (
                  <ScaleLoader size={16} />
                ) : undefined
              }
            />
          ))}
        </View>
      )}
    />
  )
}
