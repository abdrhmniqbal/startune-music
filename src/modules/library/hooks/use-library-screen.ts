import type { Playlist } from "@/components/blocks/playlist-list"
import { useStore } from "@nanostores/react"
import { useRouter } from "expo-router"

import { useState } from "react"
import { useAlbums } from "@/modules/albums/albums.queries"
import { useArtists } from "@/modules/artists/artists.queries"
import { useFavorites } from "@/modules/favorites/favorites.queries"
import { useFolderBrowser } from "@/modules/library/hooks/use-folder-browser"
import {
  $sortConfig,
  ALBUM_SORT_OPTIONS,
  ARTIST_SORT_OPTIONS,
  FOLDER_SORT_OPTIONS,
  PLAYLIST_SORT_OPTIONS,
  setSortConfig,
  type SortField,
  sortGeneric,
  sortTracks,
  TRACK_SORT_OPTIONS,
} from "@/modules/library/library-sort.store"
import { $tracks, playTrack, type Track } from "@/modules/player/player.store"
import { usePlaylistsWithOptions } from "@/modules/playlist/playlist.queries"

export const LIBRARY_TABS = [
  "Tracks",
  "Albums",
  "Artists",
  "Playlists",
  "Folders",
  "Favorites",
] as const
export type LibraryTab = (typeof LIBRARY_TABS)[number]
interface LibrarySortOption {
  label: string
  field: SortField
}

export const LIBRARY_TAB_SORT_OPTIONS: Record<LibraryTab, LibrarySortOption[]> =
  {
    Tracks: TRACK_SORT_OPTIONS,
    Albums: ALBUM_SORT_OPTIONS,
    Artists: ARTIST_SORT_OPTIONS,
    Playlists: PLAYLIST_SORT_OPTIONS,
    Folders: FOLDER_SORT_OPTIONS,
    Favorites: [],
  }

export function useLibraryScreen() {
  const router = useRouter()
  const [activeTab, setActiveTab] = useState<LibraryTab>("Tracks")
  const [sortModalVisible, setSortModalVisible] = useState(false)

  const allSortConfigs = useStore($sortConfig)
  const tracks = useStore($tracks)
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
    const options = LIBRARY_TAB_SORT_OPTIONS[activeTab]
    const selected = options.find((option) => option.field === sortConfig.field)
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

  return {
    activeTab,
    setActiveTab,
    sortModalVisible,
    setSortModalVisible,
    closeSortModal,
    sortConfig,
    tracks,
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
  }
}
