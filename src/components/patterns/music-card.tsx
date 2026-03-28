import type { ReactNode } from "react"
import { Image } from "expo-image"
import { PressableFeedback } from "heroui-native"
import { Text, View } from "react-native"
import { cn } from "tailwind-variants"

import LocalMusicNoteSolidIcon from "@/components/icons/local/music-note-solid"
import { Card } from "@/components/ui/card"
import { ICON_SIZES } from "@/constants/icon-sizes"
import { useThemeColors } from "@/hooks/use-theme-colors"

interface MusicCardProps {
  title: string
  subtitle: string
  image?: string
  icon?: ReactNode
  onPress?: () => void
  className?: string
}

export function MusicCard({
  title,
  subtitle,
  image,
  icon,
  onPress,
  className,
}: MusicCardProps) {
  const theme = useThemeColors()

  return (
    <PressableFeedback
      onPress={onPress}
      className={cn("w-36 active:opacity-70", className)}
    >
      <Card
        tone="default"
        padding="none"
        className="mb-2 h-36 w-36 overflow-hidden rounded-lg border-none"
      >
        {image ? (
          <Image
            source={{ uri: image }}
            style={{ width: "100%", height: "100%" }}
            contentFit="cover"
          />
        ) : (
          <View className="h-full w-full items-center justify-center bg-surface">
            {icon ?? (
              <LocalMusicNoteSolidIcon
                fill="none"
                width={ICON_SIZES.mediumCardFallback}
                height={ICON_SIZES.mediumCardFallback}
                color={theme.muted}
              />
            )}
          </View>
        )}
      </Card>
      <Text
        className="w-36 text-sm font-bold text-foreground"
        numberOfLines={1}
      >
        {title}
      </Text>
      <Text className="w-36 text-xs text-muted" numberOfLines={1}>
        {subtitle}
      </Text>
    </PressableFeedback>
  )
}
