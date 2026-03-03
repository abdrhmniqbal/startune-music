import type { ReactNode } from "react"
import * as React from "react"
import { createContext, use } from "react"
import { Image } from "expo-image"
import { PressableFeedback } from "heroui-native"
import { Text, View, type TextProps, type ViewProps } from "react-native"
import { cn, tv, type VariantProps } from "tailwind-variants"

const mediaItemStyles = tv({
  slots: {
    base: "border-none bg-transparent",
    imageContainer:
      "items-center justify-center overflow-hidden rounded-lg bg-surface",
    content: "flex-1 justify-center gap-0.5",
    title: "text-foreground font-bold",
    description: "text-xs text-muted",
    rank: "w-8 text-center text-lg font-bold text-foreground",
  },
  variants: {
    variant: {
      list: {
        base: "flex-row items-center gap-3 bg-transparent py-2.5",
        imageContainer: "h-14 w-14",
        title: "text-base",
      },
      grid: {
        base: "w-36 gap-2",
        imageContainer: "aspect-square w-full",
        content: "w-full",
        title: "text-base uppercase leading-tight",
      },
    },
  },
  defaultVariants: {
    variant: "list",
  },
})

type MediaItemVariant = VariantProps<typeof mediaItemStyles>

interface MediaItemContextValue {
  variant: NonNullable<MediaItemVariant["variant"]>
}

const MediaItemContext = createContext<MediaItemContextValue>({
  variant: "list",
})

type MediaItemProps = React.ComponentProps<typeof PressableFeedback> &
  MediaItemVariant

function MediaItemRoot({
  className,
  variant = "list",
  children,
  ...props
}: MediaItemProps) {
  const { base } = mediaItemStyles({ variant })

  return (
    <MediaItemContext value={{ variant }}>
      <PressableFeedback className={cn(base(), className)} {...props}>
        {children}
      </PressableFeedback>
    </MediaItemContext>
  )
}

type MediaItemImageProps = ViewProps & {
  icon?: ReactNode
  image?: string
  overlay?: ReactNode
}

function MediaItemImage({
  className,
  icon,
  image,
  overlay,
  children,
  ...props
}: MediaItemImageProps) {
  const { variant } = use(MediaItemContext)
  const { imageContainer } = mediaItemStyles({ variant })

  return (
    <View className={cn(imageContainer(), className)} {...props}>
      {image ? (
        <View className="h-full w-full overflow-hidden rounded-lg">
          <Image
            source={{ uri: image }}
            style={{ width: "100%", height: "100%" }}
            contentFit="cover"
          />
        </View>
      ) : (
        icon || children
      )}
      {overlay}
    </View>
  )
}

function MediaItemContent({ className, children, ...props }: ViewProps) {
  const { variant } = use(MediaItemContext)
  const { content } = mediaItemStyles({ variant })

  return (
    <View className={cn(content(), className)} {...props}>
      {children}
    </View>
  )
}

function MediaItemTitle({ className, children, ...props }: TextProps) {
  const { variant } = use(MediaItemContext)
  const { title } = mediaItemStyles({ variant })

  return (
    <Text className={cn(title(), className)} numberOfLines={1} {...props}>
      {children}
    </Text>
  )
}

function MediaItemDescription({ className, children, ...props }: TextProps) {
  const { variant } = use(MediaItemContext)
  const { description } = mediaItemStyles({ variant })

  return (
    <Text className={cn(description(), className)} numberOfLines={1} {...props}>
      {children}
    </Text>
  )
}

function MediaItemRank({ className, children, ...props }: TextProps) {
  const { rank } = mediaItemStyles()

  return (
    <Text className={cn(rank(), className)} {...props}>
      {children}
    </Text>
  )
}

function MediaItemAction({
  className,
  ...props
}: React.ComponentProps<typeof PressableFeedback>) {
  return (
    <PressableFeedback
      className={cn("active:opacity-50", className)}
      {...props}
    />
  )
}

type MediaItemCompoundComponent = typeof MediaItemRoot & {
  Image: typeof MediaItemImage
  Content: typeof MediaItemContent
  Title: typeof MediaItemTitle
  Description: typeof MediaItemDescription
  Rank: typeof MediaItemRank
  Action: typeof MediaItemAction
}

const MediaItem = MediaItemRoot as MediaItemCompoundComponent
MediaItem.Image = MediaItemImage
MediaItem.Content = MediaItemContent
MediaItem.Title = MediaItemTitle
MediaItem.Description = MediaItemDescription
MediaItem.Rank = MediaItemRank
MediaItem.Action = MediaItemAction

export { MediaItem }

export {
  MediaItemAction,
  MediaItemContent,
  MediaItemDescription,
  MediaItemImage,
  MediaItemRank,
  MediaItemRoot,
  MediaItemTitle,
}
