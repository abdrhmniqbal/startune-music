import { Stack, useRouter } from "expo-router"
import { Button } from "heroui-native"
import { View } from "react-native"

import LocalSearchIcon from "@/components/icons/local/search"
import LocalSettingsIcon from "@/components/icons/local/settings"
import {
  getDefaultNativeStackOptions,
  getLargeTitleRootScreenOptions,
} from "@/modules/navigation/stack"
import { useThemeColors } from "@/modules/ui/theme"

export default function LibraryLayout() {
  const theme = useThemeColors()
  const router = useRouter()

  return (
    <Stack screenOptions={getDefaultNativeStackOptions(theme)}>
      <Stack.Screen
        name="index"
        options={getLargeTitleRootScreenOptions({
          title: "Library",
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
        })}
      />
      <Stack.Screen name="album" options={{ headerShown: false }} />
      <Stack.Screen name="artist" options={{ headerShown: false }} />
      <Stack.Screen name="playlist" options={{ headerShown: false }} />
      <Stack.Screen name="search" options={{ headerShown: false }} />
    </Stack>
  )
}
