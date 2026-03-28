import { Stack, useRouter } from "expo-router"
import { Button } from "heroui-native"
import { View } from "react-native"

import LocalSearchIcon from "@/components/icons/local/search"
import LocalSettingsIcon from "@/components/icons/local/settings"
import { BackButton } from "@/components/patterns/back-button"
import { useThemeColors } from "@/hooks/use-theme-colors"

export default function HomeLayout() {
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
          title: "Home",
          headerLargeTitle: true,
          headerTitleAlign: "left",
          headerRight: () => (
            <View className="-mr-2 flex-row gap-4">
              <Button
                onPress={() => router.push("/search")}
                variant="ghost"
                isIconOnly
              >
                <LocalSearchIcon
                  fill="none"
                  width={24}
                  height={24}
                  color={theme.foreground}
                />
              </Button>
              <Button
                onPress={() => router.push("/settings")}
                variant="ghost"
                isIconOnly
              >
                <LocalSettingsIcon
                  fill="none"
                  width={24}
                  height={24}
                  color={theme.foreground}
                />
              </Button>
            </View>
          ),
        }}
      />
      <Stack.Screen
        name="recently-played"
        options={{
          title: "Recently Played",
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
      <Stack.Screen
        name="top-tracks"
        options={{
          title: "Top Tracks",
          headerBackButtonMenuEnabled: false,
          headerBackVisible: false,
          headerLeft: () => <BackButton className="-ml-2" />,
        }}
      />
      <Stack.Screen name="album" options={{ headerShown: false }} />
      <Stack.Screen name="artist" options={{ headerShown: false }} />
      <Stack.Screen name="playlist" options={{ headerShown: false }} />
      <Stack.Screen name="search" options={{ headerShown: false }} />
    </Stack>
  )
}
