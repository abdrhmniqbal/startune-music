import { useStore } from "@nanostores/react"
import { Tabs } from "heroui-native"
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

import { PlaybackActionsRow } from "@/components/blocks"
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
} from "@/hooks/scroll-bars.store"
import { useThemeColors } from "@/hooks/use-theme-colors"
import { $indexerState, startIndexing } from "@/modules/indexer"
import {
  LIBRARY_TAB_SORT_OPTIONS,
  LIBRARY_TABS,
  type LibraryTab,
  useLibraryScreen,
} from "@/modules/library/hooks/use-library-screen"
import { $currentTrack } from "@/modules/player/player.store"

export default function LibraryScreen() {
  const theme = useThemeColors()
  const insets = useSafeAreaInsets()
  const currentTrack = useStore($currentTrack)
  const indexerState = useStore($indexerState)
  const tabBarHeight = getTabBarHeight(insets.bottom)
  const hasMiniPlayer = currentTrack !== null
  const libraryListBottomPadding =
    tabBarHeight + (hasMiniPlayer ? MINI_PLAYER_HEIGHT : 0) + 200
  const [isPullRefreshing, setIsPullRefreshing] = React.useState(false)

  const {
    activeTab,
    setActiveTab,
    sortModalVisible,
    setSortModalVisible,
    closeSortModal,
    sortConfig,
    favorites,
    playlists,
    folders,
    folderTracks,
    folderBreadcrumbs,
    openArtist,
    openAlbum,
    openPlaylist,
    openPlaylistForm,
    openFolder,
    goBackFolder,
    navigateToFolderPath,
    playFolderTrack,
    playSingleTrack,
    playAll,
    shuffle,
    handleSortSelect,
    getSortLabel,
    getItemCount,
  } = useLibraryScreen()

  const showPlayButtons = activeTab === "Tracks" || activeTab === "Favorites"
  const currentSortOptions = LIBRARY_TAB_SORT_OPTIONS[activeTab]
  const handleListScroll = (event: NativeSyntheticEvent<NativeScrollEvent>) => {
    handleScroll(event.nativeEvent.contentOffset.y)
  }
  const isRefreshing = isPullRefreshing || indexerState.isIndexing

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
