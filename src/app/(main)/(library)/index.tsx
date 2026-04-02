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
import { FolderList } from "@/components/blocks/folder-list"
import { PlaylistList } from "@/components/blocks/playlist-list"
import { SortSheet } from "@/components/blocks/sort-sheet"
import { TracksTab } from "@/components/blocks/tracks-tab"
import { getTabBarHeight, MINI_PLAYER_HEIGHT } from "@/constants/layout"
import {
  handleScroll,
  handleScrollStart,
  handleScrollStop,
} from "@/modules/ui/ui.store"
import { useThemeColors } from "@/modules/ui/theme"
import { useFavorites } from "@/modules/favorites/favorites.queries"
import { startIndexing } from "@/modules/indexer/indexer.service"
import { useIndexerStore } from "@/modules/indexer/indexer.store"
import {
  buildFolderBrowserState,
  getParentFolderPath,
} from "@/modules/library/folder-browser"
import { useAlbums, useArtists } from "@/modules/library/library.queries"
import {
  ALBUM_SORT_OPTIONS,
  ARTIST_SORT_OPTIONS,
  FOLDER_SORT_OPTIONS,
  PLAYLIST_SORT_OPTIONS,
  TRACK_SORT_OPTIONS,
} from "@/modules/library/library-sort.constants"
import type { SortField } from "@/modules/library/library-sort.types"
import {
  sortGeneric,
  sortTracks,
} from "@/modules/library/library-sort.utils"
import {
  setSortConfig,
  useLibrarySortStore,
} from "@/modules/library/library-sort.store"
import { playTrack } from "@/modules/player/player.service"
import { type Track, usePlayerStore } from "@/modules/player/player.store"
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

function getAlbumOrderByField(
  field: SortField
): "title" | "artist" | "year" | "trackCount" | "dateAdded" {
  if (field === "artist") {
    return "artist"
  }

  if (field === "year") {
    return "year"
  }

  if (field === "trackCount") {
    return "trackCount"
  }

  if (field === "dateAdded") {
    return "dateAdded"
  }

  return "title"
}

function getArtistOrderByField(
  field: SortField
): "name" | "trackCount" | "dateAdded" {
  if (field === "trackCount") {
    return "trackCount"
  }

  if (field === "dateAdded") {
    return "dateAdded"
  }

  return "name"
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
  const hasMiniPlayer = usePlayerStore((state) => state.currentTrack !== null)
  const tracks = usePlayerStore((state) => state.tracks)
  const isIndexing = useIndexerStore((state) => state.indexerState.isIndexing)
  const tabBarHeight = getTabBarHeight(insets.bottom)
  const libraryListBottomPadding =
    tabBarHeight + (hasMiniPlayer ? MINI_PLAYER_HEIGHT : 0) + 200
  const [activeTab, setActiveTab] = React.useState<LibraryTab>("Tracks")
  const [currentFolderPath, setCurrentFolderPath] = React.useState("")
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

  const albumOrderByField = getAlbumOrderByField(allSortConfigs.Albums.field)
  const artistOrderByField = getArtistOrderByField(
    allSortConfigs.Artists.field
  )

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

  const playlists = React.useMemo<Playlist[]>(
    () => sortGeneric(playlistsData, allSortConfigs.Playlists),
    [allSortConfigs.Playlists, playlistsData]
  )

  const { folders, tracks: folderTracks, breadcrumbs: folderBreadcrumbs } =
    React.useMemo(
      () =>
        buildFolderBrowserState(
          tracks,
          currentFolderPath,
          allSortConfigs.Folders
        ),
      [allSortConfigs.Folders, currentFolderPath, tracks]
    )

  const showPlayButtons = activeTab === "Tracks" || activeTab === "Favorites"
  const currentSortOptions = LIBRARY_SORT_OPTIONS[activeTab]
  const handleListScroll = (event: NativeSyntheticEvent<NativeScrollEvent>) => {
    handleScroll(event.nativeEvent.contentOffset.y)
  }
  const isRefreshing = isPullRefreshing || isIndexing
  const sharedListEvents = {
    onScroll: handleListScroll,
    onScrollBeginDrag: handleScrollStart,
    onScrollEndDrag: handleScrollStop,
    onMomentumScrollEnd: handleScrollStop,
  } as const
  const listContentContainerStyle = {
    paddingBottom: libraryListBottomPadding,
  }
  const listResetScrollKey = `${sortConfig.field}-${sortConfig.order}`

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

  function openFolder(path: string) {
    setCurrentFolderPath(path)
  }

  function goBackFolder() {
    setCurrentFolderPath((currentPath) => getParentFolderPath(currentPath))
  }

  function navigateToFolderPath(path: string) {
    setCurrentFolderPath(path)
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

  const sortLabel = React.useMemo(() => {
    const selected = currentSortOptions.find(
      (option) => option.field === sortConfig.field
    )
    return selected?.label || "Sort"
  }, [currentSortOptions, sortConfig.field])

  const itemCount = React.useMemo(() => {
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
  }, [
    activeTab,
    albumsData.length,
    artistsData.length,
    favorites.length,
    folderTracks.length,
    folders.length,
    playlists.length,
    tracks.length,
  ])

  async function handleRefresh() {
    if (isIndexing) {
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
            {...sharedListEvents}
          />
        )
      case "Albums":
        return (
          <AlbumsTab
            sortConfig={sortConfig}
            onAlbumPress={(album) => openAlbum(album.title)}
            contentBottomPadding={libraryListBottomPadding}
            refreshControl={refreshControl}
            {...sharedListEvents}
          />
        )
      case "Artists":
        return (
          <ArtistsTab
            sortConfig={sortConfig}
            onArtistPress={(artist) => openArtist(artist.name)}
            contentBottomPadding={libraryListBottomPadding}
            refreshControl={refreshControl}
            {...sharedListEvents}
          />
        )
      case "Playlists":
        return (
          <PlaylistList
            data={playlists}
            onCreatePlaylist={openPlaylistForm}
            onPlaylistPress={(playlist) => openPlaylist(playlist.id)}
            contentContainerStyle={listContentContainerStyle}
            resetScrollKey={listResetScrollKey}
            refreshControl={refreshControl}
            {...sharedListEvents}
          />
        )
      case "Folders":
        return (
          <FolderList
            data={folders}
            tracks={folderTracks}
            breadcrumbs={folderBreadcrumbs}
            onFolderPress={(folder) => {
              if (folder.path) {
                openFolder(folder.path)
              }
            }}
            onBackPress={goBackFolder}
            onBreadcrumbPress={navigateToFolderPath}
            onTrackPress={playFolderTrack}
            contentContainerStyle={listContentContainerStyle}
            resetScrollKey={listResetScrollKey}
            refreshControl={refreshControl}
            {...sharedListEvents}
          />
        )
      case "Favorites":
        return (
          <FavoritesList
            data={favorites}
            contentContainerStyle={listContentContainerStyle}
            refreshControl={refreshControl}
            {...sharedListEvents}
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
              ? `${itemCount} Items`
              : `${itemCount} ${activeTab}`}
          </Text>
          {currentSortOptions.length > 0 && (
            <SortSheet.Trigger label={sortLabel} iconSize={16} />
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
