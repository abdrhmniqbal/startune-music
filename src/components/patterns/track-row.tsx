import type { ReactNode } from "react"
import type { Track } from "@/modules/player/player.store"

import { View } from "react-native"
import LocalMusicNoteSolidIcon from "@/components/icons/local/music-note-solid"
import { MediaItem } from "@/components/ui/media-item"
import { ICON_SIZES } from "@/constants/icon-sizes"
import { useThemeColors } from "@/hooks/use-theme-colors"

interface TrackRowProps {
  track: Track
  onPress?: () => void
  onLongPress?: () => void
  variant?: "list" | "grid"
  leftAction?: ReactNode
  rank?: ReactNode
  showCover?: boolean
  showArtist?: boolean
  rightAction?: ReactNode
  className?: string
  imageClassName?: string
  imageOverlay?: ReactNode
  titleClassName?: string
  descriptionClassName?: string
}

export function TrackRow({
  track,
  onPress,
  onLongPress,
  variant = "list",
  leftAction,
  rank,
  showCover = true,
  showArtist = true,
  rightAction,
  className,
  imageClassName,
  imageOverlay,
  titleClassName,
  descriptionClassName,
}: TrackRowProps) {
  const theme = useThemeColors()
  const isCompactNoCoverRow = !showCover && rank !== undefined && rank !== null
  const fallbackIconSize =
    variant === "grid" ? ICON_SIZES.gridFallback : ICON_SIZES.listFallback

  return (
    <MediaItem
      variant={variant}
      onPress={onPress}
      onLongPress={onLongPress}
      className={`${isCompactNoCoverRow ? "gap-1 py-0" : ""} ${className || ""}`}
    >
      {leftAction ? <View className="py-2 pr-1">{leftAction}</View> : null}
      {showCover ? (
        <MediaItem.Image
          icon={
            <LocalMusicNoteSolidIcon
              fill="none"
              width={fallbackIconSize}
              height={fallbackIconSize}
              color={theme.muted}
            />
          }
          image={track.image}
          className={imageClassName}
          overlay={imageOverlay}
        />
      ) : null}
      {rank !== undefined && rank !== null ? (
        <MediaItem.Rank
          className={isCompactNoCoverRow ? "w-6 text-left text-base" : ""}
        >
          {rank}
        </MediaItem.Rank>
      ) : null}
      <MediaItem.Content>
        <MediaItem.Title className={titleClassName}>
          {track.title}
        </MediaItem.Title>
        {showArtist ? (
          <MediaItem.Description className={descriptionClassName}>
            {track.artist || "Unknown Artist"}
          </MediaItem.Description>
        ) : null}
      </MediaItem.Content>
      {rightAction ? (
        <View className={isCompactNoCoverRow ? "p-0.5" : "p-2"}>
          {rightAction}
        </View>
      ) : null}
    </MediaItem>
  )
}
