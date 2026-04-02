import { Stack, useRouter } from "expo-router"

import LocalSearchIcon from "@/components/icons/local/search"
import LocalSettingsIcon from "@/components/icons/local/settings"
import { StackHeaderActions } from "@/components/patterns/stack-header-actions"
import {
  getDefaultNativeStackOptions,
  HIDDEN_STACK_SCREEN_OPTIONS,
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
            <StackHeaderActions
              actions={[
                {
                  key: "search",
                  onPress: () => router.push("/search"),
                  icon: (
                    <LocalSearchIcon
                      fill="none"
                      width={24}
                      height={24}
                      color={theme.foreground}
                    />
                  ),
                },
                {
                  key: "settings",
                  onPress: () => router.push("/settings"),
                  icon: (
                    <LocalSettingsIcon
                      fill="none"
                      width={24}
                      height={24}
                      color={theme.foreground}
                    />
                  ),
                },
              ]}
            />
          ),
        })}
      />
      <Stack.Screen name="album" options={HIDDEN_STACK_SCREEN_OPTIONS} />
      <Stack.Screen name="artist" options={HIDDEN_STACK_SCREEN_OPTIONS} />
      <Stack.Screen name="playlist" options={HIDDEN_STACK_SCREEN_OPTIONS} />
      <Stack.Screen name="search" options={HIDDEN_STACK_SCREEN_OPTIONS} />
    </Stack>
  )
}
