import { Stack, useRouter } from "expo-router"
import { PressableFeedback } from "heroui-native"

import LocalCancelIcon from "@/components/icons/local/cancel"
import { BackButton } from "@/components/patterns/back-button"
import { useThemeColors } from "@/hooks/use-theme-colors"

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
          title: "Appearance",
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
      <Stack.Screen
        name="library"
        options={{
          title: "Library",
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
      <Stack.Screen
        name="advanced"
        options={{
          title: "Advanced",
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
      <Stack.Screen
        name="about"
        options={{
          title: "About",
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
      <Stack.Screen
        name="folder-filters"
        options={{
          title: "Folder Filters",
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
      <Stack.Screen
        name="track-duration-filter"
        options={{
          title: "Track Duration Filter",
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
      <Stack.Screen
        name="log-level"
        options={{
          title: "Log Level",
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
    </Stack>
  )
}
