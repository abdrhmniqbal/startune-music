import type { ReactNode } from "react"
import { Image } from "expo-image"
import { View } from "react-native"
import { cn } from "tailwind-variants"

import { ICON_SIZES } from "@/constants/icon-sizes"
import { useThemeColors } from "@/modules/ui/theme"

import LocalPlaylistSolidIcon from "../icons/local/playlist-solid"

interface PlaylistArtworkProps {
  images?: string[]
  className?: string
  fallback?: ReactNode
}

export function resolvePlaylistArtworkImages(
  images?: string[],
  image?: string
) {
  if (images && images.length > 0) {
    return images
  }

  return image ? [image] : undefined
}

function normalizeImages(images?: string[]): string[] {
  if (!images?.length) {
    return []
  }

  const uniqueImages: string[] = []

  for (const image of images) {
    if (!image || uniqueImages.includes(image)) {
      continue
    }

    uniqueImages.push(image)

    if (uniqueImages.length >= 4) {
      break
    }
  }

  return uniqueImages
}

function buildGridImages(images: string[]): string[] {
  if (images.length === 0) {
    return []
  }

  if (images.length >= 4) {
    return images.slice(0, 4)
  }

  const gridImages: string[] = []

  for (let i = 0; i < 4; i += 1) {
    gridImages.push(images[i % images.length])
  }

  return gridImages
}

export function PlaylistArtwork({
  images,
  className,
  fallback,
}: PlaylistArtworkProps) {
  const theme = useThemeColors()
  const gridImages = buildGridImages(normalizeImages(images))
  const imageKeyCounter = new Map<string, number>()

  if (gridImages.length === 0) {
    return (
      <View
        className={cn(
          "h-full w-full items-center justify-center bg-surface",
          className
        )}
      >
        {fallback || (
          <LocalPlaylistSolidIcon
            fill="none"
            width={ICON_SIZES.listFallback}
            height={ICON_SIZES.listFallback}
            color={theme.muted}
          />
        )}
      </View>
    )
  }

  return (
    <View
      className={cn(
        "h-full w-full flex-row flex-wrap overflow-hidden",
        className
      )}
    >
      {gridImages.map((image) => {
        const nextCount = (imageKeyCounter.get(image) || 0) + 1
        imageKeyCounter.set(image, nextCount)
        return (
          <Image
            key={`${image}-${nextCount}`}
            source={{ uri: image }}
            style={{ width: "50%", height: "50%" }}
            contentFit="cover"
          />
        )
      })}
    </View>
  )
}
