import type {
  FavoriteEntry,
  FavoriteType,
} from "@/modules/favorites/favorites.types"
import { LegendList, type LegendListRenderItemProps } from "@legendapp/list"
import { Image } from "expo-image"
import { useRouter } from "expo-router"
import { Chip, PressableFeedback } from "heroui-native"
import * as React from "react"
import { useCallback } from "react"

import {
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  type RefreshControlProps,
  type StyleProp,
  View,
  type ViewStyle,
} from "react-native"
import { LEGEND_LIST_ROW_CONFIG } from "@/components/blocks/legend-list-config"
import LocalFavouriteSolidIcon from "@/components/icons/local/favourite-solid"
import LocalMusicNoteSolidIcon from "@/components/icons/local/music-note-solid"
import LocalUserSolidIcon from "@/components/icons/local/user-solid"
import LocalVynilSolidIcon from "@/components/icons/local/vynil-solid"
import {
  PlaylistArtwork,
  resolvePlaylistArtworkImages,
} from "@/components/patterns/playlist-artwork"
import {
  MediaItem as Item,
  MediaItemAction as ItemAction,
  MediaItemContent as ItemContent,
  MediaItemDescription as ItemDescription,
  MediaItemImage as ItemImage,
  MediaItemTitle as ItemTitle,
} from "@/components/ui/media-item"
import { EmptyState } from "@/components/ui/empty-state"
import { ICON_SIZES } from "@/constants/icon-sizes"
import { useThemeColors } from "@/modules/ui/theme"
import { useToggleFavorite } from "@/modules/favorites/favorites.mutations"
import { playTrack } from "@/modules/player/player.service"
import { usePlayerStore } from "@/modules/player/player.store"

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
            images={resolvePlaylistArtworkImages(favorite.images, favorite.image)}
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
  const tracks = usePlayerStore((state) => state.tracks)
  const toggleFavoriteMutation = useToggleFavorite()
  const router = useRouter()

  const handlePress = useCallback((favorite: FavoriteEntry) => {
    switch (favorite.type) {
      case "track": {
        const track = tracks.find((item) => item.id === favorite.id)
        if (track) {
          playTrack(track)
        }
        break
      }
      case "artist": {
        router.push({
          pathname: "/artist/[name]",
          params: { name: favorite.name },
        })
        break
      }
      case "album": {
        router.push({
          pathname: "/album/[name]",
          params: { name: favorite.name },
        })
        break
      }
      case "playlist": {
        router.push(`./playlist/${favorite.id}`)
        break
      }
    }
  }, [router, tracks])

  const handleRemoveFavorite = useCallback((favorite: FavoriteEntry) => {
    void toggleFavoriteMutation.mutateAsync({
      type: favorite.type,
      itemId: favorite.id,
      isCurrentlyFavorite: true,
      name: favorite.name,
      subtitle: favorite.subtitle,
      image: favorite.image,
    })
  }, [toggleFavoriteMutation])

  const renderFavoriteItem = useCallback(
    (item: FavoriteEntry) => (
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
            onPress={(event) => {
              event.stopPropagation()
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
    ),
    [handlePress, handleRemoveFavorite]
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
        ListEmptyComponent={
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
        }
        {...LEGEND_LIST_ROW_CONFIG}
        style={{ flex: 1, minHeight: 1 }}
      />
    </View>
  )
}
