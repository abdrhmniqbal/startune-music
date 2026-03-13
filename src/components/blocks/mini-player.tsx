import { useStore } from "@nanostores/react"
import { Image } from "expo-image"
import { PressableFeedback } from "heroui-native"
import * as React from "react"
import { View } from "react-native"
import Animated, { SlideInDown, SlideOutDown } from "react-native-reanimated"

import LocalNextSolidIcon from "@/components/icons/local/next-solid"
import LocalPauseSolidIcon from "@/components/icons/local/pause-solid"
import LocalPlaySolidIcon from "@/components/icons/local/play-solid"
import { MarqueeText } from "@/components/ui/marquee-text"
import {
  $isPlayerExpanded,
  $playerExpandedView,
} from "@/hooks/scroll-bars.store"
import { useThemeColors } from "@/hooks/use-theme-colors"
import {
  $currentTime,
  $currentTrack,
  $duration,
  $isPlaying,
  playNext,
  togglePlayback,
} from "@/modules/player/player.store"

import LocalMusicNoteSolidIcon from "../icons/local/music-note-solid"
import LocalQueueIcon from "../icons/local/queue"

interface MiniPlayerProps {
  bottomOffset?: number
}

export const MiniPlayer: React.FC<MiniPlayerProps> = ({
  bottomOffset = 90,
}) => {
  const currentTrack = useStore($currentTrack)
  const isPlaying = useStore($isPlaying)
  const currentTime = useStore($currentTime)
  const duration = useStore($duration)

  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0

  const theme = useThemeColors()

  if (!currentTrack) return null

  return (
    <Animated.View
      entering={SlideInDown.duration(300)}
      exiting={SlideOutDown.duration(300)}
      className="absolute right-0 left-0 h-16 overflow-hidden"
      style={{
        bottom: bottomOffset,
      }}
    >
      <View
        className="absolute inset-0 border-t border-border bg-surface-secondary"
        style={{ borderTopColor: theme.border }}
      />

      <View className="absolute top-0 right-0 left-0 h-0.75 bg-surface-tertiary">
        <View
          style={{
            width: `${progressPercent}%`,
            height: "100%",
            backgroundColor: theme.accent,
          }}
        />
      </View>

      <View className="flex-1 flex-row items-center gap-3 px-4">
        <PressableFeedback
          onPress={() => {
            $playerExpandedView.set("artwork")
            $isPlayerExpanded.set(true)
          }}
          className="flex-1 flex-row items-center gap-3 active:opacity-80"
        >
          <View className="h-11 w-11 items-center justify-center overflow-hidden rounded-md bg-surface">
            {currentTrack.image ? (
              <Image
                source={{ uri: currentTrack.image }}
                style={{ width: "100%", height: "100%" }}
                contentFit="cover"
              />
            ) : (
              <LocalMusicNoteSolidIcon
                fill="none"
                width={20}
                height={20}
                color={theme.muted}
              />
            )}
          </View>

          <View className="flex-1 overflow-hidden">
            <MarqueeText
              text={currentTrack.title}
              className="text-[15px] font-bold text-foreground"
              speed={0.6}
            />
            <MarqueeText
              text={currentTrack.artist || "Unknown Artist"}
              className="text-[13px] text-muted"
              speed={0.5}
            />
          </View>
        </PressableFeedback>

        <View className="flex-row items-center gap-3">
          <PressableFeedback
            onPress={togglePlayback}
            className="p-2 active:opacity-60"
          >
            {isPlaying ? (
              <LocalPauseSolidIcon
                fill="none"
                width={28}
                height={28}
                color={theme.foreground}
              />
            ) : (
              <LocalPlaySolidIcon
                fill="none"
                width={28}
                height={28}
                color={theme.foreground}
              />
            )}
          </PressableFeedback>
          <PressableFeedback
            onPress={playNext}
            className="p-2 active:opacity-60"
          >
            <LocalNextSolidIcon
              fill="none"
              width={24}
              height={24}
              color={theme.foreground}
            />
          </PressableFeedback>
          <PressableFeedback
            onPress={() => {
              $playerExpandedView.set("queue")
              $isPlayerExpanded.set(true)
            }}
            className="p-2 active:opacity-60"
          >
            <LocalQueueIcon
              fill="none"
              width={22}
              height={22}
              color={theme.foreground}
            />
          </PressableFeedback>
        </View>
      </View>
    </Animated.View>
  )
}
