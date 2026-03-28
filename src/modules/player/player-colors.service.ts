import { getColors } from "react-native-image-colors"

import { logWarn } from "@/modules/logging/logging.service"

import {
  getCurrentImageUriState,
  getDefaultPlayerColors,
  setCurrentColorsState,
  setCurrentImageUriState,
  setIsLoadingColorsState,
  type ColorPalette,
} from "./player-colors.store"

const colorCache = new Map<string, ColorPalette>()

export async function getTrackColors(imageUri: string): Promise<ColorPalette> {
  const cachedColors = colorCache.get(imageUri)
  if (cachedColors) {
    return cachedColors
  }

  const fallbackColors = getDefaultPlayerColors()

  try {
    const result = await getColors(imageUri, {
      fallback: fallbackColors.bg,
      cache: true,
      key: imageUri,
    })

    const colors =
      result.platform === "android"
        ? {
            bg: (result as any).average || fallbackColors.bg,
            primary: (result as any).dominant || fallbackColors.primary,
            secondary: (result as any).darkVibrant || fallbackColors.secondary,
          }
        : {
            bg: (result as any).background || fallbackColors.bg,
            primary: (result as any).primary || fallbackColors.primary,
            secondary: (result as any).detail || fallbackColors.secondary,
          }

    colorCache.set(imageUri, colors)
    return colors
  } catch (error) {
    logWarn("Falling back to default player colors", {
      imageUri,
      error: error instanceof Error ? error.message : String(error),
    })
    return fallbackColors
  }
}

export async function updateColorsForImage(imageUri: string | undefined) {
  if (!imageUri) {
    setCurrentColorsState(getDefaultPlayerColors())
    setCurrentImageUriState(null)
    return
  }

  if (imageUri === getCurrentImageUriState()) {
    return
  }

  setIsLoadingColorsState(true)
  setCurrentImageUriState(imageUri)

  const colors = await getTrackColors(imageUri)
  setCurrentColorsState(colors)
  setIsLoadingColorsState(false)
}

export function getCachedColors(imageUri: string): ColorPalette | null {
  return colorCache.get(imageUri) || null
}

export function clearColorCache() {
  colorCache.clear()
}

export function getColorCacheSize() {
  return colorCache.size
}
