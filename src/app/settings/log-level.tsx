import { PressableFeedback } from "heroui-native"
import { useEffect } from "react"
import { ScrollView, Text, View } from "react-native"

import LocalTickIcon from "@/components/icons/local/tick"
import { useThemeColors } from "@/hooks/use-theme-colors"
import {
  type AppLogLevel,
  ensureLoggingConfigLoaded,
  setAppLogLevel,
  useLoggingStore,
} from "@/modules/logging/logger"

interface LogLevelOption {
  label: string
  value: AppLogLevel
  description: string
}

const LOG_LEVEL_OPTIONS: LogLevelOption[] = [
  {
    label: "Minimal",
    value: "minimal",
    description: "Critical errors only.",
  },
  {
    label: "Extra",
    value: "extra",
    description: "Log everything.",
  },
]

export default function LogLevelSettingsScreen() {
  const theme = useThemeColors()
  const loggingLevel = useLoggingStore((state) => state.loggingConfig.level)

  useEffect(() => {
    void ensureLoggingConfigLoaded()
  }, [])

  async function handleSelect(level: AppLogLevel) {
    await setAppLogLevel(level)
  }

  return (
    <ScrollView className="flex-1 bg-background">
      <View className="py-2">
        {LOG_LEVEL_OPTIONS.map((option) => (
          <PressableFeedback
            key={option.value}
            onPress={() => {
              void handleSelect(option.value)
            }}
            className="flex-row items-center bg-background px-6 py-4 active:opacity-70"
          >
            <View className="flex-1 gap-0.5 pr-2">
              <Text className="text-[17px] font-normal text-foreground">
                {option.label}
              </Text>
              <Text className="text-[13px] leading-5 text-muted">
                {option.description}
              </Text>
            </View>
            {loggingLevel === option.value ? (
              <LocalTickIcon
                fill="none"
                width={24}
                height={24}
                color={theme.accent}
              />
            ) : null}
          </PressableFeedback>
        ))}
      </View>
    </ScrollView>
  )
}
