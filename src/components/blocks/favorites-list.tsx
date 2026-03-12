import type {
  FavoriteEntry,
  FavoriteType,
} from "@/modules/favorites/favorites.api"
import { LegendList, type LegendListRenderItemProps } from "@legendapp/list"
import { useStore } from "@nanostores/react"
import { Image } from "expo-image"
import { useRouter } from "expo-router"
import { Chip, PressableFeedback } from "heroui-native"
import * as React from "react"

import {
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  type RefreshControlProps,
  type StyleProp,
  View,
  type ViewStyle,
} from "react-native"
import LocalFavouriteSolidIcon from "@/components/icons/local/favourite-solid"
import LocalMusicNoteSolidIcon from "@/components/icons/local/music-note-solid"
import LocalUserSolidIcon from "@/components/icons/local/user-solid"
import LocalVynilSolidIcon from "@/components/icons/local/vynil-solid"
import { PlaylistArtwork } from "@/components/patterns"
import {
  EmptyState,
  Item,
  ItemAction,
  ItemContent,
  ItemDescription,
  ItemImage,
  ItemTitle,
} from "@/components/ui"
import { ICON_SIZES } from "@/constants/icon-sizes"
import { useThemeColors } from "@/hooks/use-theme-colors"
import { useToggleFavorite } from "@/modules/favorites/favorites.queries"
import { $tracks, playTrack } from "@/modules/player/player.store"

interface FavoritesListProps {
  data: FavoriteEntry[]
  scrollEnabled?: boolean
  contentContainerStyle?: StyleProp<ViewStyle>
  refreshControl?: React.ReactElement<RefreshControlProps> | null
  onScroll?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollBeginDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollEndDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onMomentumScrollEnd?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
}

const FavoriteItemImage: React.FC<{ favorite: FavoriteEntry }> = ({
  favorite,
}) => {
  const theme = useThemeColors()

  switch (favorite.type) {
    case "artist":
      return (
        <ItemImage className="overflow-hidden rounded-full">
          {favorite.image ? (
            <Image
              source={{ uri: favorite.image }}
              style={{ width: "100%", height: "100%" }}
              contentFit="cover"
            />
          ) : (
            <View className="h-full w-full items-center justify-center rounded-full bg-surface">
              <LocalUserSolidIcon
                fill="none"
                width={ICON_SIZES.listFallback}
                height={ICON_SIZES.listFallback}
                color={theme.muted}
              />
            </View>
          )}
        </ItemImage>
      )

    case "playlist":
      return (
        <ItemImage className="items-center justify-center overflow-hidden bg-default">
          <PlaylistArtwork
            images={
              favorite.images && favorite.images.length > 0
                ? favorite.images
                : favorite.image
                  ? [favorite.image]
                  : undefined
            }
          />
        </ItemImage>
      )

    case "album":
      return (
        <ItemImage
          icon={
            <LocalVynilSolidIcon
              fill="none"
              width={ICON_SIZES.listFallback}
              height={ICON_SIZES.listFallback}
              color={theme.muted}
            />
          }
          image={favorite.image}
          className="rounded-lg"
        />
      )

    case "track":
    default:
      return (
        <ItemImage
          icon={
            <LocalMusicNoteSolidIcon
              fill="none"
              width={ICON_SIZES.listFallback}
              height={ICON_SIZES.listFallback}
              color={theme.muted}
            />
          }
          image={favorite.image}
        />
      )
  }
}

function getTypeLabel(type: FavoriteType): string {
  switch (type) {
    case "track":
      return "Track"
    case "artist":
      return "Artist"
    case "album":
      return "Album"
    case "playlist":
      return "Playlist"
    default:
      return type
  }
}

const TypeBadge: React.FC<{ type: FavoriteType }> = ({ type }) => {
  return (
    <Chip size="sm" variant="secondary" color="default" className="mr-2">
      <Chip.Label>{getTypeLabel(type)}</Chip.Label>
    </Chip>
  )
}

export const FavoritesList: React.FC<FavoritesListProps> = ({
  data,
  scrollEnabled = true,
  contentContainerStyle,
  refreshControl,
  onScroll,
  onScrollBeginDrag,
  onScrollEndDrag,
  onMomentumScrollEnd,
}) => {
  const tracks = useStore($tracks)
  const toggleFavoriteMutation = useToggleFavorite()
  const router = useRouter()

  if (data.length === 0) {
    return (
      <EmptyState
        icon={
          <LocalFavouriteSolidIcon
            fill="none"
            width={ICON_SIZES.emptyState}
            height={ICON_SIZES.emptyState}
            color="#ef4444"
          />
        }
        title="No Favorites"
        message="Your favorite tracks, artists, and albums will appear here."
      />
    )
  }

  const handlePress = (favorite: FavoriteEntry) => {
    switch (favorite.type) {
      case "track": {
        const track = tracks.find((t) => t.id === favorite.id)
        if (track) {
          playTrack(track)
        }
        break
      }
      case "artist":
        router.push({
          pathname: "/artist/[name]",
          params: { name: favorite.name },
        })
        break
      case "album":
        router.push({
          pathname: "/album/[name]",
          params: { name: favorite.name },
        })
        break
      case "playlist":
        router.push(`./playlist/${favorite.id}`)
        break
    }
  }

  const handleRemoveFavorite = (favorite: FavoriteEntry) => {
    void toggleFavoriteMutation.mutateAsync({
      type: favorite.type,
      itemId: favorite.id,
      isCurrentlyFavorite: true,
      name: favorite.name,
      subtitle: favorite.subtitle,
      image: favorite.image,
    })
  }

  const renderFavoriteItem = (item: FavoriteEntry) => (
    <Item key={item.id} onPress={() => handlePress(item)}>
      <FavoriteItemImage favorite={item} />
      <ItemContent>
        <ItemTitle>{item.name}</ItemTitle>
        <View className="flex-row items-center">
          <TypeBadge type={item.type} />
          <ItemDescription>{item.subtitle || ""}</ItemDescription>
        </View>
      </ItemContent>
      <ItemAction>
        <PressableFeedback
          onPress={(e) => {
            e.stopPropagation()
            handleRemoveFavorite(item)
          }}
          className="p-2 active:opacity-50"
        >
          <LocalFavouriteSolidIcon
            fill="none"
            width={22}
            height={22}
            color="#ef4444"
          />
        </PressableFeedback>
      </ItemAction>
    </Item>
  )

  return (
    <View style={{ flex: 1, minHeight: 1 }}>
      <LegendList
        data={data}
        renderItem={({ item }: LegendListRenderItemProps<FavoriteEntry>) =>
          renderFavoriteItem(item)
        }
        keyExtractor={(item) => item.id}
        scrollEnabled={scrollEnabled}
        contentContainerStyle={[{ gap: 8 }, contentContainerStyle]}
        onScroll={onScroll}
        onScrollBeginDrag={onScrollBeginDrag}
        onScrollEndDrag={onScrollEndDrag}
        onMomentumScrollEnd={onMomentumScrollEnd}
        scrollEventThrottle={16}
        refreshControl={refreshControl || undefined}
        recycleItems={true}
        initialContainerPoolRatio={3}
        estimatedItemSize={68}
        drawDistance={180}
        style={{ flex: 1, minHeight: 1 }}
      />
    </View>
  )
}
