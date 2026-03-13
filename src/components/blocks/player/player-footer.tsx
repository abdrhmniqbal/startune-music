import { useStore } from "@nanostores/react"
import { PressableFeedback } from "heroui-native"
import * as React from "react"
import { View } from "react-native"
import { cn } from "tailwind-variants"

import LocalMicIcon from "@/components/icons/local/mic"
import LocalQueueIcon from "@/components/icons/local/queue"
import { $playerExpandedView } from "@/hooks/scroll-bars.store"
import { useThemeColors } from "@/hooks/use-theme-colors"

export const PlayerFooter: React.FC = () => {
  const playerExpandedView = useStore($playerExpandedView)
  const theme = useThemeColors()

  return (
    <View className="flex-row items-center justify-between">
      <PressableFeedback
        onPress={() => {
          $playerExpandedView.set(
            playerExpandedView === "lyrics" ? "artwork" : "lyrics"
          )
        }}
        className={cn(playerExpandedView !== "lyrics" && "opacity-60")}
      >
        <LocalMicIcon
          fill="none"
          width={24}
          height={24}
          color={playerExpandedView === "lyrics" ? theme.accent : "white"}
        />
      </PressableFeedback>
      <PressableFeedback
        onPress={() => {
          $playerExpandedView.set(
            playerExpandedView === "queue" ? "artwork" : "queue"
          )
        }}
        className={cn(playerExpandedView !== "queue" && "opacity-60")}
      >
        <LocalQueueIcon
          fill="none"
          width={24}
          height={24}
          color={playerExpandedView === "queue" ? theme.accent : "white"}
        />
      </PressableFeedback>
    </View>
  )
}
