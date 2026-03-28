import { Tabs } from "heroui-native"
import { useRouter } from "expo-router"
import * as React from "react"
import {
  type NativeScrollEvent,
  type NativeSyntheticEvent,
  RefreshControl,
  Text,
  View,
} from "react-native"
import { useSafeAreaInsets } from "react-native-safe-area-context"
import { cn } from "tailwind-variants"

import { PlaybackActionsRow } from "@/components/blocks/playback-actions-row"
import { AlbumsTab } from "@/components/blocks/albums-tab"
import { ArtistsTab } from "@/components/blocks/artists-tab"
import { FavoritesList } from "@/components/blocks/favorites-list"
import { FolderTab } from "@/components/blocks/folder-tab"
import { PlaylistList } from "@/components/blocks/playlist-list"
import { SortSheet } from "@/components/blocks/sort-sheet"
import { TracksTab } from "@/components/blocks/tracks-tab"
import { getTabBarHeight, MINI_PLAYER_HEIGHT } from "@/constants/layout"
import {
  handleScroll,
  handleScrollStart,
  handleScrollStop,
} from "@/modules/ui/ui.store"
import { useThemeColors } from "@/hooks/use-theme-colors"
import { useFavorites } from "@/modules/favorites/favorites.queries"
import { startIndexing } from "@/modules/indexer/indexer.store"
import { useIndexerStore } from "@/modules/indexer/indexer.store"
import { useAlbums, useArtists } from "@/modules/library/library.queries"
import {
  ALBUM_SORT_OPTIONS,
  ARTIST_SORT_OPTIONS,
  FOLDER_SORT_OPTIONS,
  PLAYLIST_SORT_OPTIONS,
  setSortConfig,
  sortGeneric,
  sortTracks,
  TRACK_SORT_OPTIONS,
  type SortField,
  useLibrarySortStore,
} from "@/modules/library/library-sort.store"
import { useFolderBrowser } from "@/modules/library/hooks/use-folder-browser"
import { playTrack, type Track, usePlayerStore } from "@/modules/player/player.store"
import { usePlaylistsWithOptions } from "@/modules/playlist/playlist.queries"
import type { Playlist } from "@/components/blocks/playlist-list"

const LIBRARY_TABS = [
  "Tracks",
  "Albums",
  "Artists",
  "Playlists",
  "Folders",
  "Favorites",
] as const
type LibraryTab = (typeof LIBRARY_TABS)[number]

interface LibrarySortOption {
  label: string
  field: SortField
}

const LIBRARY_SORT_OPTIONS: Record<LibraryTab, LibrarySortOption[]> = {
  Tracks: TRACK_SORT_OPTIONS,
  Albums: ALBUM_SORT_OPTIONS,
  Artists: ARTIST_SORT_OPTIONS,
  Playlists: PLAYLIST_SORT_OPTIONS,
  Folders: FOLDER_SORT_OPTIONS,
  Favorites: [],
}

export default function LibraryScreen() {
  const router = useRouter()
  const theme = useThemeColors()
  const insets = useSafeAreaInsets()
  const currentTrack = usePlayerStore((state) => state.currentTrack)
  const tracks = usePlayerStore((state) => state.tracks)
  const indexerState = useIndexerStore((state) => state.indexerState)
  const tabBarHeight = getTabBarHeight(insets.bottom)
  const hasMiniPlayer = currentTrack !== null
  const libraryListBottomPadding =
    tabBarHeight + (hasMiniPlayer ? MINI_PLAYER_HEIGHT : 0) + 200
  const [activeTab, setActiveTab] = React.useState<LibraryTab>("Tracks")
  const [sortModalVisible, setSortModalVisible] = React.useState(false)
  const [isPullRefreshing, setIsPullRefreshing] = React.useState(false)
  const allSortConfigs = useLibrarySortStore((state) => state.sortConfig)
  const sortConfig = allSortConfigs[activeTab]
  const shouldLoadFavorites = activeTab === "Favorites"
  const shouldLoadAlbums = activeTab === "Albums"
  const shouldLoadArtists = activeTab === "Artists"
  const shouldLoadPlaylists = activeTab === "Playlists"

  const { data: favorites = [] } = useFavorites(undefined, {
    enabled: shouldLoadFavorites,
  })

  const albumOrderByField =
    allSortConfigs.Albums.field === "artist"
      ? "artist"
      : allSortConfigs.Albums.field === "year"
        ? "year"
        : allSortConfigs.Albums.field === "trackCount"
          ? "trackCount"
          : allSortConfigs.Albums.field === "dateAdded"
            ? "dateAdded"
            : "title"

  const artistOrderByField =
    allSortConfigs.Artists.field === "trackCount"
      ? "trackCount"
      : allSortConfigs.Artists.field === "dateAdded"
        ? "dateAdded"
        : "name"

  const { data: albumsData = [] } = useAlbums(
    albumOrderByField,
    allSortConfigs.Albums.order,
    { enabled: shouldLoadAlbums }
  )
  const { data: artistsData = [] } = useArtists(
    artistOrderByField,
    allSortConfigs.Artists.order,
    { enabled: shouldLoadArtists }
  )
  const { data: playlistsData = [] } =
    usePlaylistsWithOptions(shouldLoadPlaylists)

  const playlists: Playlist[] = sortGeneric(
    playlistsData,
    allSortConfigs.Playlists
  )

  const {
    folders,
    folderTracks,
    folderBreadcrumbs,
    openFolder,
    goBackFolder,
    navigateToFolderPath,
  } = useFolderBrowser(tracks, allSortConfigs.Folders)

  const showPlayButtons = activeTab === "Tracks" || activeTab === "Favorites"
  const currentSortOptions = LIBRARY_SORT_OPTIONS[activeTab]
  const handleListScroll = (event: NativeSyntheticEvent<NativeScrollEvent>) => {
    handleScroll(event.nativeEvent.contentOffset.y)
  }
  const isRefreshing = isPullRefreshing || indexerState.isIndexing

  function closeSortModal() {
    setSortModalVisible(false)
  }

  function openArtist(name: string) {
    router.push({
      pathname: "/artist/[name]",
      params: { name },
    })
  }

  function openAlbum(title: string) {
    router.push({
      pathname: "/album/[name]",
      params: { name: title },
    })
  }

  function openPlaylist(id: string) {
    router.push(`./playlist/${id}`)
  }

  function openPlaylistForm() {
    router.push("/playlist/form")
  }

  function playFolderTrack(track: Track) {
    playTrack(track, folderTracks)
  }

  function playSingleTrack(track: Track, queue?: Track[]) {
    if (queue && queue.length > 0) {
      playTrack(track, queue)
      return
    }

    const sortedTracksQueue = sortTracks(tracks, allSortConfigs.Tracks)
    if (sortedTracksQueue.length > 0) {
      playTrack(track, sortedTracksQueue)
      return
    }

    playTrack(track)
  }

  function playAll() {
    if (activeTab === "Tracks") {
      const sortedTracksQueue = sortTracks(tracks, allSortConfigs.Tracks)
      if (sortedTracksQueue.length > 0) {
        playTrack(sortedTracksQueue[0], sortedTracksQueue)
      }
      return
    }

    if (activeTab === "Favorites") {
      const firstTrack = favorites.find((favorite) => favorite.type === "track")
      if (firstTrack) {
        const track = tracks.find((candidate) => candidate.id === firstTrack.id)
        if (track) {
          playTrack(track)
        }
      }
      return
    }

    if (tracks.length > 0) {
      playTrack(tracks[0])
    }
  }

  function shuffle() {
    if (activeTab === "Tracks") {
      const sortedTracksQueue = sortTracks(tracks, allSortConfigs.Tracks)
      if (sortedTracksQueue.length > 0) {
        const randomIndex = Math.floor(Math.random() * sortedTracksQueue.length)
        playTrack(sortedTracksQueue[randomIndex], sortedTracksQueue)
      }
      return
    }

    if (activeTab === "Favorites") {
      const trackFavorites = favorites.filter(
        (favorite) => favorite.type === "track"
      )
      if (trackFavorites.length > 0) {
        const randomIndex = Math.floor(Math.random() * trackFavorites.length)
        const track = tracks.find(
          (candidate) => candidate.id === trackFavorites[randomIndex].id
        )
        if (track) {
          playTrack(track)
        }
      }
      return
    }

    if (tracks.length > 0) {
      const randomIndex = Math.floor(Math.random() * tracks.length)
      playTrack(tracks[randomIndex])
    }
  }

  function handleSortSelect(field: SortField, order?: "asc" | "desc") {
    setSortConfig(activeTab, field, order)
    if (!order) {
      setSortModalVisible(false)
    }
  }

  function getSortLabel() {
    const selected = currentSortOptions.find(
      (option) => option.field === sortConfig.field
    )
    return selected?.label || "Sort"
  }

  function getItemCount() {
    switch (activeTab) {
      case "Tracks":
        return tracks.length
      case "Albums":
        return albumsData.length
      case "Artists":
        return artistsData.length
      case "Favorites":
        return favorites.length
      case "Playlists":
        return playlists.length
      case "Folders":
        return folders.length + folderTracks.length
      default:
        return 0
    }
  }

  async function handleRefresh() {
    if (indexerState.isIndexing) {
      return
    }

    setIsPullRefreshing(true)
    try {
      await startIndexing(false, true)
    } finally {
      setIsPullRefreshing(false)
    }
  }

  const refreshControl = (
    <RefreshControl
      refreshing={isRefreshing}
      onRefresh={() => {
        void handleRefresh()
      }}
      colors={[theme.accent]}
      tintColor={theme.accent}
      progressBackgroundColor={theme.default}
    />
  )

  function renderTabContent() {
    switch (activeTab) {
      case "Tracks":
        return (
          <TracksTab
            sortConfig={sortConfig}
            onTrackPress={playSingleTrack}
            contentBottomPadding={libraryListBottomPadding}
            refreshControl={refreshControl}
            onScroll={handleListScroll}
            onScrollBeginDrag={handleScrollStart}
            onScrollEndDrag={handleScrollStop}
            onMomentumScrollEnd={handleScrollStop}
          />
        )
      case "Albums":
        return (
          <AlbumsTab
            sortConfig={sortConfig}
            onAlbumPress={(album) => openAlbum(album.title)}
            contentBottomPadding={libraryListBottomPadding}
            refreshControl={refreshControl}
            onScroll={handleListScroll}
            onScrollBeginDrag={handleScrollStart}
            onScrollEndDrag={handleScrollStop}
            onMomentumScrollEnd={handleScrollStop}
          />
        )
      case "Artists":
        return (
          <ArtistsTab
            sortConfig={sortConfig}
            onArtistPress={(artist) => openArtist(artist.name)}
            contentBottomPadding={libraryListBottomPadding}
            refreshControl={refreshControl}
            onScroll={handleListScroll}
            onScrollBeginDrag={handleScrollStart}
            onScrollEndDrag={handleScrollStop}
            onMomentumScrollEnd={handleScrollStop}
          />
        )
      case "Playlists":
        return (
          <PlaylistList
            data={playlists}
            onCreatePlaylist={openPlaylistForm}
            onPlaylistPress={(playlist) => openPlaylist(playlist.id)}
            contentContainerStyle={{ paddingBottom: libraryListBottomPadding }}
            resetScrollKey={`${sortConfig.field}-${sortConfig.order}`}
            refreshControl={refreshControl}
            onScroll={handleListScroll}
            onScrollBeginDrag={handleScrollStart}
            onScrollEndDrag={handleScrollStop}
            onMomentumScrollEnd={handleScrollStop}
          />
        )
      case "Folders":
        return (
          <FolderTab
            folders={folders}
            folderTracks={folderTracks}
            folderBreadcrumbs={folderBreadcrumbs}
            onOpenFolder={openFolder}
            onBackFolder={goBackFolder}
            onNavigateToFolderPath={navigateToFolderPath}
            onTrackPress={playFolderTrack}
            contentContainerStyle={{ paddingBottom: libraryListBottomPadding }}
            resetScrollKey={`${sortConfig.field}-${sortConfig.order}`}
            refreshControl={refreshControl}
            onScroll={handleListScroll}
            onScrollBeginDrag={handleScrollStart}
            onScrollEndDrag={handleScrollStop}
            onMomentumScrollEnd={handleScrollStop}
          />
        )
      case "Favorites":
        return (
          <FavoritesList
            data={favorites}
            contentContainerStyle={{ paddingBottom: libraryListBottomPadding }}
            refreshControl={refreshControl}
            onScroll={handleListScroll}
            onScrollBeginDrag={handleScrollStart}
            onScrollEndDrag={handleScrollStop}
            onMomentumScrollEnd={handleScrollStop}
          />
        )
      default:
        return null
    }
  }

  return (
    <SortSheet
      visible={sortModalVisible}
      onOpenChange={(open) =>
        open ? setSortModalVisible(true) : closeSortModal()
      }
      currentField={sortConfig.field}
      currentOrder={sortConfig.order}
      onSelect={handleSortSelect}
    >
      <View className="flex-1 bg-background">
        <Tabs
          value={activeTab}
          onValueChange={(value) => setActiveTab(value as LibraryTab)}
          variant="secondary"
          className="gap-1.5 px-4 py-4"
        >
          <Tabs.List className="w-full">
            <Tabs.ScrollView
              scrollAlign="start"
              showsHorizontalScrollIndicator={false}
              contentContainerClassName="px-1 gap-4"
            >
              <Tabs.Indicator />
              {LIBRARY_TABS.map((tab) => (
                <Tabs.Trigger key={tab} value={tab} className="py-2">
                  {({ isSelected }) => (
                    <Tabs.Label
                      className={cn(
                        "text-lg font-semibold",
                        isSelected ? "text-foreground" : "text-muted"
                      )}
                    >
                      {tab}
                    </Tabs.Label>
                  )}
                </Tabs.Trigger>
              ))}
            </Tabs.ScrollView>
          </Tabs.List>
        </Tabs>

        <View className="flex-row items-center justify-between px-4 pb-4">
          <Text className="text-lg font-bold text-foreground">
            {activeTab === "Folders"
              ? `${getItemCount()} Items`
              : `${getItemCount()} ${activeTab}`}
          </Text>
          {currentSortOptions.length > 0 && (
            <SortSheet.Trigger label={getSortLabel()} iconSize={16} />
          )}
        </View>

        <View className="flex-1 px-4">
          {showPlayButtons && (
            <View className="mb-2">
              <PlaybackActionsRow onPlay={playAll} onShuffle={shuffle} />
            </View>
          )}
          <View className="flex-1">{renderTabContent()}</View>
        </View>
      </View>

      <SortSheet.Content options={currentSortOptions} />
    </SortSheet>
  )
}
