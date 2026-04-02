import { Stack } from "expo-router"

import { BackButton } from "@/components/patterns/back-button"
import {
  getMediaDetailTransitionOptions,
} from "@/modules/navigation/stack"
import { useThemeColors } from "@/modules/ui/theme"

export default function AlbumLayout() {
  const theme = useThemeColors()

  return (
    <Stack
      screenOptions={getMediaDetailTransitionOptions(theme, () => (
        <BackButton className="-ml-2" />
      ))}
    />
  )
}
