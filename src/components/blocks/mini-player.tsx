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
  openPlayer,
} from "@/modules/ui/ui.store"
import { useThemeColors } from "@/modules/ui/theme"
import { playNext, togglePlayback } from "@/modules/player/player-controls.service"
import { usePlayerStore } from "@/modules/player/player.store"

import LocalMusicNoteSolidIcon from "../icons/local/music-note-solid"
import LocalQueueIcon from "../icons/local/queue"

interface MiniPlayerProps {
  bottomOffset?: number
}

interface MiniPlayerArtworkProps {
  image?: string
  mutedColor: string
}

function MiniPlayerArtwork({ image, mutedColor }: MiniPlayerArtworkProps) {
  return (
    <View className="h-11 w-11 items-center justify-center overflow-hidden rounded-md bg-surface">
      {image ? (
        <Image
          source={{ uri: image }}
          style={{ width: "100%", height: "100%" }}
          contentFit="cover"
        />
      ) : (
        <LocalMusicNoteSolidIcon
          fill="none"
          width={20}
          height={20}
          color={mutedColor}
        />
      )}
    </View>
  )
}

interface MiniPlayerMetaProps {
  title: string
  artist?: string | null
}

function MiniPlayerMeta({ title, artist }: MiniPlayerMetaProps) {
  return (
    <View className="flex-1 overflow-hidden">
      <MarqueeText
        text={title}
        className="text-[15px] font-bold text-foreground"
        speed={0.6}
      />
      <MarqueeText
        text={artist || "Unknown Artist"}
        className="text-[13px] text-muted"
        speed={0.5}
      />
    </View>
  )
}

interface MiniPlayerControlsProps {
  isPlaying: boolean
  foregroundColor: string
}

function MiniPlayerControls({
  isPlaying,
  foregroundColor,
}: MiniPlayerControlsProps) {
  return (
    <View className="flex-row items-center gap-3">
      <PressableFeedback onPress={togglePlayback} className="p-2 active:opacity-60">
        {isPlaying ? (
          <LocalPauseSolidIcon
            fill="none"
            width={28}
            height={28}
            color={foregroundColor}
          />
        ) : (
          <LocalPlaySolidIcon
            fill="none"
            width={28}
            height={28}
            color={foregroundColor}
          />
        )}
      </PressableFeedback>
      <PressableFeedback onPress={playNext} className="p-2 active:opacity-60">
        <LocalNextSolidIcon
          fill="none"
          width={24}
          height={24}
          color={foregroundColor}
        />
      </PressableFeedback>
      <PressableFeedback
        onPress={() => {
          openPlayer("queue")
        }}
        className="p-2 active:opacity-60"
      >
        <LocalQueueIcon
          fill="none"
          width={22}
          height={22}
          color={foregroundColor}
        />
      </PressableFeedback>
    </View>
  )
}

export const MiniPlayer: React.FC<MiniPlayerProps> = ({
  bottomOffset = 90,
}) => {
  const currentTrack = usePlayerStore((state) => state.currentTrack)
  const isPlaying = usePlayerStore((state) => state.isPlaying)

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

      <MiniPlayerProgress themeAccent={theme.accent} />

      <View className="flex-1 flex-row items-center gap-3 px-4">
        <PressableFeedback
          onPress={() => {
            openPlayer("artwork")
          }}
          className="flex-1 flex-row items-center gap-3 active:opacity-80"
        >
          <MiniPlayerArtwork image={currentTrack.image} mutedColor={theme.muted} />
          <MiniPlayerMeta title={currentTrack.title} artist={currentTrack.artist} />
        </PressableFeedback>

        <MiniPlayerControls
          isPlaying={isPlaying}
          foregroundColor={theme.foreground}
        />
      </View>
    </Animated.View>
  )
}

function MiniPlayerProgress({ themeAccent }: { themeAccent: string }) {
  const currentTime = usePlayerStore((state) => state.currentTime)
  const duration = usePlayerStore((state) => state.duration)
  const progressPercent = duration > 0 ? (currentTime / duration) * 100 : 0

  return (
    <View className="absolute top-0 right-0 left-0 h-0.75 bg-surface-tertiary">
      <View
        style={{
          width: `${progressPercent}%`,
          height: "100%",
          backgroundColor: themeAccent,
        }}
      />
    </View>
  )
}
