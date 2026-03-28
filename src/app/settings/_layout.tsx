import { Stack, useRouter } from "expo-router"
import { PressableFeedback } from "heroui-native"

import LocalCancelIcon from "@/components/icons/local/cancel"
import { BackButton } from "@/components/patterns/back-button"
import { SETTINGS_SCREEN_TITLES } from "@/modules/settings/settings.routes"
import { useThemeColors } from "@/modules/ui/theme"

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
      <Stack.Screen
        name="appearance"
        options={{
          title: SETTINGS_SCREEN_TITLES.appearance,
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
      <Stack.Screen
        name="library"
        options={{
          title: SETTINGS_SCREEN_TITLES.library,
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
      <Stack.Screen
        name="advanced"
        options={{
          title: SETTINGS_SCREEN_TITLES.advanced,
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
      <Stack.Screen
        name="about"
        options={{
          title: SETTINGS_SCREEN_TITLES.about,
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
      <Stack.Screen
        name="folder-filters"
        options={{
          title: SETTINGS_SCREEN_TITLES["folder-filters"],
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
      <Stack.Screen
        name="track-duration-filter"
        options={{
          title: SETTINGS_SCREEN_TITLES["track-duration-filter"],
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
      <Stack.Screen
        name="log-level"
        options={{
          title: SETTINGS_SCREEN_TITLES["log-level"],
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
    </Stack>
  )
}
