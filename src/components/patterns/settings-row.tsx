import { PressableFeedback } from "heroui-native"
import type { ReactNode } from "react"
import { Text, View } from "react-native"

import LocalChevronRightIcon from "@/components/icons/local/chevron-right"
import { useThemeColors } from "@/modules/ui/theme"

interface SettingsRowProps {
  title: string
  description?: string
  onPress?: () => void
  rightContent?: ReactNode
  showChevron?: boolean
  isDisabled?: boolean
  className?: string
}

export function SettingsRow({
  title,
  description,
  onPress,
  rightContent,
  showChevron = true,
  isDisabled = false,
  className,
}: SettingsRowProps) {
  const theme = useThemeColors()

  return (
    <PressableFeedback
      onPress={isDisabled ? undefined : onPress}
      className={`flex-row items-center bg-background px-6 py-4 ${
        isDisabled ? "opacity-60" : "active:opacity-70"
      } ${className || ""}`}
    >
      <View className="flex-1 gap-1">
        <Text className="text-[17px] font-normal text-foreground">{title}</Text>
        {description ? (
          <Text className="text-[13px] leading-5 text-muted">
            {description}
          </Text>
        ) : null}
      </View>
      <View className="flex-row items-center gap-2">
        {rightContent}
        {showChevron ? (
          <LocalChevronRightIcon
            fill="none"
            width={20}
            height={20}
            color={theme.muted}
          />
        ) : null}
      </View>
    </PressableFeedback>
  )
}
