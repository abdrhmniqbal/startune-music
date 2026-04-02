import { Stack } from "expo-router"

import { BackButton } from "@/components/patterns/back-button"
import {
  getMediaDetailTransitionOptions,
  getModalTaskTransitionOptions,
} from "@/modules/navigation/stack"
import { useThemeColors } from "@/modules/ui/theme"

export default function PlaylistLayout() {
  const theme = useThemeColors()

  return (
    <Stack
      screenOptions={getMediaDetailTransitionOptions(theme, () => (
        <BackButton className="-ml-2" />
      ))}
    >
      <Stack.Screen name="[id]" options={{ title: "Playlist" }} />
      <Stack.Screen
        name="form"
        options={getModalTaskTransitionOptions(theme, "Playlist", () => (
          <BackButton className="-ml-2" />
        ))}
      />
    </Stack>
  )
}
