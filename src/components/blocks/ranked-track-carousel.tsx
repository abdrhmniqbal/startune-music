import type { ReactNode } from "react"
import * as React from "react"
import { View } from "react-native"

import { TrackRow } from "@/components/patterns/track-row"
import { ScaleLoader } from "@/components/ui/scale-loader"
import { playTrack } from "@/modules/player/player.service"
import { usePlayerStore, type Track } from "@/modules/player/player.store"
import { chunkArray } from "@/utils/array"

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
  const currentTrackId = usePlayerStore((state) => state.currentTrack?.id)
  const chunks = React.useMemo(
    () => chunkArray(data, chunkSize),
    [data, chunkSize]
  )

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
                currentTrackId === track.id ? "text-accent" : undefined
              }
              imageOverlay={
                currentTrackId === track.id ? (
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
