import { PressableFeedback } from "heroui-native"
import * as React from "react"
import { View } from "react-native"
import Animated, { Layout } from "react-native-reanimated"

import LocalNextSolidIcon from "@/components/icons/local/next-solid"
import LocalPauseCircleSolidIcon from "@/components/icons/local/pause-circle-solid"
import LocalPlayCircleSolidIcon from "@/components/icons/local/play-circle-solid"
import LocalPreviousSolidIcon from "@/components/icons/local/previous-solid"
import LocalRepeatIcon from "@/components/icons/local/repeat"
import LocalRepeatOneIcon from "@/components/icons/local/repeat-one"
import LocalShuffleIcon from "@/components/icons/local/shuffle"
import { useThemeColors } from "@/modules/ui/theme"
import {
  playNext,
  playPrevious,
  togglePlayback,
  toggleRepeatMode,
} from "@/modules/player/player-controls.service"
import {
  type RepeatModeType,
  usePlayerStore,
} from "@/modules/player/player.store"
import { toggleShuffle } from "@/modules/player/queue.service"
import { cn } from "@/utils/common"

interface PlaybackControlsProps {
  isPlaying: boolean
  compact?: boolean
}

function getRepeatIcon(mode: RepeatModeType) {
  return mode === "track" ? "repeat-once" : "repeat"
}

export const PlaybackControls: React.FC<PlaybackControlsProps> = ({
  isPlaying,
  compact = false,
}) => {
  const theme = useThemeColors()
  const iconSize = compact ? 32 : 36
  const playButtonSize = compact ? 64 : 80
  const containerClass = compact ? "w-16 h-16" : "w-20 h-20"
  const gapClass = compact ? "gap-6" : "gap-8"
  const repeatMode = usePlayerStore((state) => state.repeatMode)
  const isShuffled = usePlayerStore((state) => state.isShuffled)

  const getRepeatColor = (mode: RepeatModeType) => {
    return mode === "off" ? "white" : theme.accent
  }

  return (
    <Animated.View
      layout={Layout.duration(300)}
      className={cn(
        "flex-row items-center justify-between",
        compact ? "mb-6" : "mb-8"
      )}
    >
      <PressableFeedback
        onPress={toggleRepeatMode}
        className={cn(repeatMode === "off" && "opacity-60")}
      >
        {getRepeatIcon(repeatMode) === "repeat-once" ? (
          <LocalRepeatOneIcon
            fill="none"
            width={24}
            height={24}
            color={getRepeatColor(repeatMode)}
          />
        ) : (
          <LocalRepeatIcon
            fill="none"
            width={24}
            height={24}
            color={getRepeatColor(repeatMode)}
          />
        )}
      </PressableFeedback>

      <View className={cn("flex-row items-center", gapClass)}>
        <PressableFeedback onPress={playPrevious}>
          <LocalPreviousSolidIcon
            fill="none"
            width={iconSize}
            height={iconSize}
            color="white"
          />
        </PressableFeedback>

        <PressableFeedback
          className={cn("items-center justify-center", containerClass)}
          onPress={togglePlayback}
        >
          {isPlaying ? (
            <LocalPauseCircleSolidIcon
              fill="none"
              width={playButtonSize}
              height={playButtonSize}
              color="white"
            />
          ) : (
            <LocalPlayCircleSolidIcon
              fill="none"
              width={playButtonSize}
              height={playButtonSize}
              color="white"
            />
          )}
        </PressableFeedback>

        <PressableFeedback onPress={playNext}>
          <LocalNextSolidIcon
            fill="none"
            width={iconSize}
            height={iconSize}
            color="white"
          />
        </PressableFeedback>
      </View>

      <PressableFeedback
        onPress={toggleShuffle}
        className={cn(!isShuffled && "opacity-60")}
      >
        <LocalShuffleIcon
          fill="none"
          width={24}
          height={24}
          color={isShuffled ? theme.accent : "white"}
        />
      </PressableFeedback>
    </Animated.View>
  )
}
