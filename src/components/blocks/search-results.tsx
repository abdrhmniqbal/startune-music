import type {
  SearchAlbumResult,
  SearchArtistResult,
  SearchPlaylistResult,
} from "@/modules/library/library.types"
import { LegendList, type LegendListRenderItemProps } from "@legendapp/list"
import { Chip, PressableFeedback } from "heroui-native"
import * as React from "react"
import { useCallback, useMemo, useState } from "react"

import { ScrollView, Text, View } from "react-native"
import { LEGEND_LIST_SECTION_CONFIG } from "@/components/blocks/legend-list-config"
import { LibrarySkeleton } from "@/components/blocks/library-skeleton"
import LocalCheckmarkCircleSolidIcon from "@/components/icons/local/checkmark-circle-solid"
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
import { ICON_SIZES } from "@/constants/icon-sizes"
import { useThemeColors } from "@/modules/ui/theme"
import { playTrack } from "@/modules/player/player.service"
import type { Track } from "@/modules/player/player.store"

const SEARCH_TABS = ["All", "Track", "Album", "Artist", "Playlist"] as const
export type SearchTab = (typeof SEARCH_TABS)[number]

interface SearchResultsProps {
  tracks: Track[]
  artists: SearchArtistResult[]
  albums: SearchAlbumResult[]
  playlists: SearchPlaylistResult[]
  query: string
  isLoading?: boolean
  activeTab?: SearchTab
  onActiveTabChange?: (tab: SearchTab) => void
  onArtistPress?: (artist: SearchArtistResult) => void
  onAlbumPress?: (album: SearchAlbumResult) => void
  onPlaylistPress?: (playlist: SearchPlaylistResult) => void
  onSeeMoreTracks?: () => void
}

type SearchResultsListItem =
  | { id: string; type: "section-spacer" }
  | { id: string; type: "section-header"; title: string; showSeeMore?: boolean }
  | { id: string; type: "artist"; artist: SearchArtistResult }
  | { id: string; type: "album"; album: SearchAlbumResult }
  | { id: string; type: "playlist"; playlist: SearchPlaylistResult }
  | { id: string; type: "track"; track: Track }

type SearchResultEntityItem = Exclude<
  SearchResultsListItem,
  { type: "section-spacer" } | { type: "section-header" }
>

function appendSection(
  listData: SearchResultsListItem[],
  options: {
    headerId: string
    title: string
    showHeader: boolean
    showSeeMore?: boolean
    items: SearchResultsListItem[]
  }
) {
  if (options.items.length === 0) {
    return
  }

  if (listData.length > 0) {
    listData.push({
      id: `section-spacer-${listData.length}`,
      type: "section-spacer",
    })
  }

  if (options.showHeader) {
    listData.push({
      id: options.headerId,
      type: "section-header",
      title: options.title,
      showSeeMore: options.showSeeMore,
    })
  }

  listData.push(...options.items)
}

interface SearchResultRowProps {
  item: SearchResultEntityItem
  theme: ReturnType<typeof useThemeColors>
  onArtistPress?: (artist: SearchArtistResult) => void
  onAlbumPress?: (album: SearchAlbumResult) => void
  onPlaylistPress?: (playlist: SearchPlaylistResult) => void
}

function SearchResultRow({
  item,
  theme,
  onArtistPress,
  onAlbumPress,
  onPlaylistPress,
}: SearchResultRowProps) {
  if (item.type === "artist") {
    return (
      <Item
        variant="list"
        className="py-1"
        onPress={() => onArtistPress?.(item.artist)}
      >
        <ItemImage
          icon={
            <LocalUserSolidIcon
              fill="none"
              width={ICON_SIZES.listFallback}
              height={ICON_SIZES.listFallback}
              color={theme.muted}
            />
          }
          image={item.artist.image}
          className="h-14 w-14 rounded-full bg-default"
        />
        <ItemContent>
          <ItemTitle className="text-lg">{item.artist.name}</ItemTitle>
          <Text className="text-xs text-muted">{item.artist.type}</Text>
        </ItemContent>
      </Item>
    )
  }

  if (item.type === "album") {
    return (
      <Item onPress={() => onAlbumPress?.(item.album)}>
        <ItemImage
          icon={
            <LocalVynilSolidIcon
              fill="none"
              width={ICON_SIZES.listFallback}
              height={ICON_SIZES.listFallback}
              color={theme.muted}
            />
          }
          image={item.album.image}
          className="rounded-md"
        />
        <ItemContent>
          <ItemTitle>{item.album.title || "Unknown Album"}</ItemTitle>
          <ItemDescription>
            {item.album.artist || "Unknown Artist"}
          </ItemDescription>
        </ItemContent>
        {item.album.isVerified && (
          <ItemAction>
            <LocalCheckmarkCircleSolidIcon
              fill="none"
              width={20}
              height={20}
              color={theme.accent}
            />
          </ItemAction>
        )}
      </Item>
    )
  }

  if (item.type === "playlist") {
    return (
      <Item onPress={() => onPlaylistPress?.(item.playlist)}>
        <ItemImage className="items-center justify-center overflow-hidden bg-default">
          <PlaylistArtwork
            images={resolvePlaylistArtworkImages(
              item.playlist.images,
              item.playlist.image
            )}
          />
        </ItemImage>
        <ItemContent>
          <ItemTitle>{item.playlist.title}</ItemTitle>
          <ItemDescription>
            {item.playlist.trackCount} {item.playlist.trackCount === 1 ? "track" : "tracks"}
          </ItemDescription>
        </ItemContent>
      </Item>
    )
  }

  return (
    <Item onPress={() => playTrack(item.track)}>
      <ItemImage
        icon={
          <LocalMusicNoteSolidIcon
            fill="none"
            width={ICON_SIZES.listFallback}
            height={ICON_SIZES.listFallback}
            color={theme.muted}
          />
        }
        image={item.track.image}
        className="rounded-md"
      />
      <ItemContent>
        <ItemTitle>{item.track.title}</ItemTitle>
        <ItemDescription>{item.track.artist || "Unknown Artist"}</ItemDescription>
      </ItemContent>
    </Item>
  )
}

export const SearchResults: React.FC<SearchResultsProps> = ({
  tracks,
  artists,
  albums,
  playlists,
  query,
  isLoading = false,
  activeTab: activeTabProp,
  onActiveTabChange,
  onArtistPress,
  onAlbumPress,
  onPlaylistPress,
  onSeeMoreTracks,
}) => {
  const theme = useThemeColors()
  const [internalActiveTab, setInternalActiveTab] = useState<SearchTab>("All")
  const activeTab = activeTabProp ?? internalActiveTab

  const setActiveTab = useCallback(
    (tab: SearchTab) => {
      if (onActiveTabChange) {
        onActiveTabChange(tab)
        return
      }

      setInternalActiveTab(tab)
    },
    [onActiveTabChange]
  )

  const showArtists = activeTab === "All" || activeTab === "Artist"
  const showAlbums = activeTab === "All" || activeTab === "Album"
  const showPlaylists = activeTab === "All" || activeTab === "Playlist"
  const showTracks = activeTab === "All" || activeTab === "Track"
  const isAllTab = activeTab === "All"

  const hasQuery = query.trim().length > 0
  const listData = useMemo(() => {
    const nextListData: SearchResultsListItem[] = []

    if (hasQuery && showArtists && artists.length > 0) {
      appendSection(nextListData, {
        headerId: "artists-header",
        title: "Artists",
        showHeader: isAllTab,
        items: artists.map((artist) => ({
          id: `artist-${artist.id}`,
          type: "artist" as const,
          artist,
        })),
      })
    }

    if (hasQuery && showAlbums && albums.length > 0) {
      appendSection(nextListData, {
        headerId: "albums-header",
        title: "Albums",
        showHeader: isAllTab,
        items: albums.map((album) => ({
          id: `album-${album.id}`,
          type: "album" as const,
          album,
        })),
      })
    }

    if (hasQuery && showPlaylists && playlists.length > 0) {
      appendSection(nextListData, {
        headerId: "playlists-header",
        title: "Playlists",
        showHeader: isAllTab,
        items: playlists.map((playlist) => ({
          id: `playlist-${playlist.id}`,
          type: "playlist" as const,
          playlist,
        })),
      })
    }

    if (hasQuery && showTracks && tracks.length > 0) {
      appendSection(nextListData, {
        headerId: "tracks-header",
        title: "Tracks",
        showHeader: isAllTab || Boolean(onSeeMoreTracks),
        showSeeMore: Boolean(onSeeMoreTracks),
        items: tracks.map((track) => ({
          id: `track-${track.id}`,
          type: "track" as const,
          track,
        })),
      })
    }

    return nextListData
  }, [
    albums,
    artists,
    hasQuery,
    isAllTab,
    onSeeMoreTracks,
    playlists,
    showAlbums,
    showArtists,
    showPlaylists,
    showTracks,
    tracks,
  ])

  const showInitialLoading = isLoading && hasQuery && listData.length === 0

  const renderListItem = useCallback(
    ({ item }: LegendListRenderItemProps<SearchResultsListItem>) => {
      switch (item.type) {
        case "section-spacer":
          return <View className="h-5" />
        case "section-header":
          return (
            <View className="mb-2 flex-row items-center justify-between">
              <Text className="text-lg font-bold text-foreground">
                {item.title}
              </Text>
              {item.showSeeMore && onSeeMoreTracks && (
                <PressableFeedback onPress={onSeeMoreTracks}>
                  <Text className="text-xs text-muted">See more</Text>
                </PressableFeedback>
              )}
            </View>
          )
        default:
          return (
            <SearchResultRow
              item={item}
              theme={theme}
              onArtistPress={onArtistPress}
              onAlbumPress={onAlbumPress}
              onPlaylistPress={onPlaylistPress}
            />
          )
      }
    },
    [onAlbumPress, onArtistPress, onPlaylistPress, onSeeMoreTracks, theme]
  )

  return (
    <View className="flex-1">
      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={{ paddingHorizontal: 16, gap: 8 }}
        contentInsetAdjustmentBehavior="never"
        automaticallyAdjustContentInsets={false}
        className="pt-3 pb-4"
        style={{ flexGrow: 0 }}
      >
        {SEARCH_TABS.map((tab) => (
          <Chip
            key={tab}
            onPress={() => setActiveTab(tab)}
            variant={activeTab === tab ? "primary" : "soft"}
            color={activeTab === tab ? "accent" : "default"}
            size="lg"
          >
            <Chip.Label className="font-medium">{tab}</Chip.Label>
          </Chip>
        ))}
      </ScrollView>
      <LegendList
        data={listData}
        renderItem={renderListItem}
        keyExtractor={(item) => item.id}
        getItemType={(item) => item.type}
        style={{ flex: 1, minHeight: 1 }}
        contentContainerStyle={{
          paddingTop: 6,
          paddingHorizontal: 16,
          paddingBottom: 104,
        }}
        keyboardShouldPersistTaps="handled"
        contentInsetAdjustmentBehavior="never"
        automaticallyAdjustContentInsets={false}
        {...LEGEND_LIST_SECTION_CONFIG}
      />
      {showInitialLoading ? (
        <View className="absolute inset-x-0 top-16 px-4">
          <LibrarySkeleton type="search-results" />
        </View>
      ) : null}
    </View>
  )
}
