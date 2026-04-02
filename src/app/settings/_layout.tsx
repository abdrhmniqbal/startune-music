import { Stack, useRouter } from "expo-router"
import { PressableFeedback } from "heroui-native"

import LocalCancelIcon from "@/components/icons/local/cancel"
import { BackButton } from "@/components/patterns/back-button"
import { SETTINGS_SCREEN_TITLES } from "@/modules/settings/settings.routes"
import { useThemeColors } from "@/modules/ui/theme"

const DETAIL_SETTINGS_SCREENS = [
  "appearance",
  "library",
  "advanced",
  "about",
  "folder-filters",
  "track-duration-filter",
  "log-level",
] as const

export default function SettingsLayout() {
  const theme = useThemeColors()
  const router = useRouter()

  return (
    <Stack
      screenOptions={{
        headerStyle: {
          backgroundColor: theme.background,
        },
        headerTintColor: theme.foreground,
        headerShadowVisible: false,
        headerTitleAlign: "center",
        contentStyle: { backgroundColor: theme.background },
        animation: "default",
      }}
    >
      <Stack.Screen
        name="index"
        options={{
          title: "Settings",
          headerLargeTitle: true,
          headerLeft: () => (
            <PressableFeedback onPress={() => router.back()} hitSlop={20}>
              <LocalCancelIcon
                fill="none"
                width={24}
                height={24}
                color={theme.foreground}
              />
            </PressableFeedback>
          ),
        }}
      />
      {DETAIL_SETTINGS_SCREENS.map((screenName) => (
        <Stack.Screen
          key={screenName}
          name={screenName}
          options={{
            title: SETTINGS_SCREEN_TITLES[screenName],
            headerBackButtonMenuEnabled: false,
            headerBackVisible: false,
            headerLeft: () => <BackButton className="-ml-2" />,
          }}
        />
      ))}
    </Stack>
  )
}
