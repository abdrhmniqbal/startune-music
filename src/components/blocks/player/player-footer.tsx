import { PressableFeedback } from "heroui-native"
import * as React from "react"
import { View } from "react-native"
import { cn } from "tailwind-variants"

import LocalMicIcon from "@/components/icons/local/mic"
import LocalQueueIcon from "@/components/icons/local/queue"
import {
  togglePlayerExpandedView,
  useUIStore,
} from "@/modules/ui/ui.store"
import { useThemeColors } from "@/hooks/use-theme-colors"

export const PlayerFooter: React.FC = () => {
  const playerExpandedView = useUIStore((state) => state.playerExpandedView)
  const theme = useThemeColors()

  return (
    <View className="flex-row items-center justify-between">
      <PressableFeedback
        onPress={() => togglePlayerExpandedView("lyrics")}
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
        onPress={() => togglePlayerExpandedView("queue")}
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
