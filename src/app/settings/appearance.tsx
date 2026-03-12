import { PressableFeedback } from "heroui-native"
import { ScrollView, Text, View } from "react-native"
import { Uniwind, useUniwind } from "uniwind"

import LocalTickIcon from "@/components/icons/local/tick"
import { useThemeColors } from "@/hooks/use-theme-colors"

type ThemeValue = "light" | "dark" | "system"

interface AppearanceOption {
  label: string
  value: ThemeValue
}

const APPEARANCE_OPTIONS: AppearanceOption[] = [
  { label: "Light", value: "light" },
  { label: "Dark", value: "dark" },
  { label: "System", value: "system" },
]

export default function AppearanceSettingsScreen() {
  const { theme: currentTheme, hasAdaptiveThemes } = useUniwind()
  const theme = useThemeColors()

  const currentMode: ThemeValue = hasAdaptiveThemes
    ? "system"
    : (currentTheme as ThemeValue)

  function handleThemeChange(value: ThemeValue) {
    Uniwind.setTheme(value)
  }

  return (
    <ScrollView className="flex-1 bg-background">
      <View className="py-2">
        <View className="px-6 pt-8 pb-3">
          <Text className="text-[17px] font-normal text-foreground">
            Theme Mode
          </Text>
          <Text className="text-[13px] leading-5 text-muted">
            Choose how Startune Music looks across the app.
          </Text>
        </View>
        {APPEARANCE_OPTIONS.map((option) => (
          <PressableFeedback
            key={option.value}
            onPress={() => handleThemeChange(option.value)}
            className="flex-row items-center bg-background px-6 py-4 active:opacity-70"
          >
            <Text className="flex-1 text-[17px] font-normal text-foreground">
              {option.label}
            </Text>
            {currentMode === option.value && (
              <LocalTickIcon
                fill="none"
                width={24}
                height={24}
                color={theme.accent}
              />
            )}
          </PressableFeedback>
        ))}
      </View>
    </ScrollView>
  )
}
