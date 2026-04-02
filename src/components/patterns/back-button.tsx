import type { ComponentProps } from "react"
import { useRouter } from "expo-router"
import { Button } from "heroui-native"

import LocalArrowLeftIcon from "@/components/icons/local/arrow-left"
import { useThemeColors } from "@/modules/ui/theme"

interface BackButtonProps {
  onPress?: () => void
  variant?: ComponentProps<typeof Button>["variant"]
  className?: string
  fallbackHref?: "/"
}

export function BackButton({
  onPress,
  variant = "ghost",
  className,
  fallbackHref = "/",
}: BackButtonProps) {
  const theme = useThemeColors()
  const router = useRouter()

  function handlePress() {
    if (onPress) {
      onPress()
      return
    }

    if (router.canGoBack?.()) {
      router.back()
      return
    }

    router.replace(fallbackHref)
  }

  return (
    <Button
      onPress={handlePress}
      variant={variant}
      className={className}
      isIconOnly
    >
      <LocalArrowLeftIcon
        fill="none"
        width={24}
        height={24}
        color={theme.foreground}
      />
    </Button>
  )
}
