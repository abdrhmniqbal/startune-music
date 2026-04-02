import { Stack, useRouter } from "expo-router"

import LocalSettingsIcon from "@/components/icons/local/settings"
import { StackHeaderActions } from "@/components/patterns/stack-header-actions"
import {
  getDefaultNativeStackOptions,
  getDrillDownScreenOptions,
  HIDDEN_STACK_SCREEN_OPTIONS,
  getLargeTitleRootScreenOptions,
} from "@/modules/navigation/stack"
import { BackButton } from "@/components/patterns/back-button"
import { useThemeColors } from "@/modules/ui/theme"

export default function SearchLayout() {
  const theme = useThemeColors()
  const router = useRouter()

  return (
    <Stack screenOptions={getDefaultNativeStackOptions(theme)}>
      <Stack.Screen
        name="index"
        options={getLargeTitleRootScreenOptions({
          title: "Search",
          headerRight: () => (
            <StackHeaderActions
              actions={[
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
      <Stack.Screen
        name="genre/[name]"
        options={getDrillDownScreenOptions("Genre", () => (
          <BackButton className="-ml-2" />
        ))}
      />
      <Stack.Screen
        name="genre/albums"
        options={getDrillDownScreenOptions("Recommended Albums", () => (
          <BackButton className="-ml-2" />
        ))}
      />
      <Stack.Screen
        name="genre/top-tracks"
        options={getDrillDownScreenOptions("Top Tracks", () => (
          <BackButton className="-ml-2" />
        ))}
      />
      <Stack.Screen name="album" options={HIDDEN_STACK_SCREEN_OPTIONS} />
      <Stack.Screen name="artist" options={HIDDEN_STACK_SCREEN_OPTIONS} />
      <Stack.Screen name="playlist" options={HIDDEN_STACK_SCREEN_OPTIONS} />
      <Stack.Screen name="search" options={HIDDEN_STACK_SCREEN_OPTIONS} />
    </Stack>
  )
}
