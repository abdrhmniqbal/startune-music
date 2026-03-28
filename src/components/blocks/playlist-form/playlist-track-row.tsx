import type { PlaylistTrackRowProps } from "./types"

import { Checkbox } from "heroui-native"

import { TrackRow } from "@/components/patterns/track-row"

export function PlaylistTrackRow({
  track,
  isSelected,
  onPress,
}: PlaylistTrackRowProps) {
  return (
    <TrackRow
      track={track}
      onPress={onPress}
      className="w-full py-2"
      leftAction={
        <Checkbox
          variant="secondary"
          isSelected={isSelected}
          onSelectedChange={() => onPress()}
          accessibilityLabel={`Select ${track.title}`}
          className="mt-0.5"
        />
      }
    />
  )
}
