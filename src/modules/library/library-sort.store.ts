import type { Track } from "@/modules/player/player.store"

import { create } from "zustand"

export type TrackSortField =
  | "title"
  | "artist"
  | "album"
  | "year"
  | "dateAdded"
  | "filename"
  | "playCount"
export type AlbumTrackSortField = TrackSortField | "trackNumber"
export type AlbumSortField =
  | "title"
  | "artist"
  | "year"
  | "dateAdded"
  | "trackCount"
export type ArtistSortField = "name" | "dateAdded" | "trackCount"
export type PlaylistSortField = "name" | "dateAdded" | "trackCount"
export type FolderSortField = "name" | "dateAdded" | "trackCount"

export type SortField =
  | AlbumTrackSortField
  | AlbumSortField
  | ArtistSortField
  | PlaylistSortField
  | FolderSortField
export type SortOrder = "asc" | "desc"
export type TabName =
  | "Tracks"
  | "Albums"
  | "Artists"
  | "Genres"
  | "Playlists"
  | "Folders"
  | "Favorites"
  | "ArtistTracks"
  | "ArtistAlbums"
  | "AlbumTracks"

export interface SortConfig {
  field: SortField
  order: SortOrder
}

const DEFAULT_SORT_CONFIG: Record<TabName, SortConfig> = {
  Tracks: { field: "title", order: "asc" },
  Albums: { field: "title", order: "asc" },
  Artists: { field: "name", order: "asc" },
  Genres: { field: "name", order: "asc" },
  Playlists: { field: "name", order: "asc" },
  Folders: { field: "name", order: "asc" },
  Favorites: { field: "dateAdded", order: "desc" },
  ArtistTracks: { field: "playCount", order: "desc" },
  ArtistAlbums: { field: "year", order: "desc" },
  AlbumTracks: { field: "trackNumber", order: "asc" },
}

interface LibrarySortState {
  sortConfig: Record<TabName, SortConfig>
}

export const useLibrarySortStore = create<LibrarySortState>(() => ({
  sortConfig: DEFAULT_SORT_CONFIG,
}))

function getSortConfigState() {
  return useLibrarySortStore.getState().sortConfig
}

function setSortConfigState(value: Record<TabName, SortConfig>) {
  useLibrarySortStore.setState({ sortConfig: value })
}

export function setSortConfig(
  tab: TabName,
  field: SortField,
  order?: SortOrder
) {
  const current = getSortConfigState()[tab]
  if (current.field === field && !order) {
    setSortConfigState({
      ...getSortConfigState(),
      [tab]: { field, order: current.order === "asc" ? "desc" : "asc" },
    })
  } else {
    setSortConfigState({
      ...getSortConfigState(),
      [tab]: { field, order: order || "asc" },
    })
  }
}

export const TRACK_SORT_OPTIONS: { label: string; field: TrackSortField }[] = [
  { label: "Title", field: "title" },
  { label: "Artist", field: "artist" },
  { label: "Album", field: "album" },
  { label: "Year", field: "year" },
  { label: "Play Count", field: "playCount" },
  { label: "Date Added", field: "dateAdded" },
  { label: "Filename", field: "filename" },
]

export const ALBUM_TRACK_SORT_OPTIONS: {
  label: string
  field: AlbumTrackSortField
}[] = [
  { label: "Track Number", field: "trackNumber" },
  { label: "Title", field: "title" },
  { label: "Artist", field: "artist" },
  { label: "Year", field: "year" },
  { label: "Play Count", field: "playCount" },
  { label: "Date Added", field: "dateAdded" },
  { label: "Filename", field: "filename" },
]

export const ALBUM_SORT_OPTIONS: { label: string; field: AlbumSortField }[] = [
  { label: "Title", field: "title" },
  { label: "Artist", field: "artist" },
  { label: "Year", field: "year" },
  { label: "Date Added", field: "dateAdded" },
  { label: "Number of Tracks", field: "trackCount" },
]

export const ARTIST_SORT_OPTIONS: { label: string; field: ArtistSortField }[] =
  [
    { label: "Name", field: "name" },
    { label: "Date Added", field: "dateAdded" },
    { label: "Number of Tracks", field: "trackCount" },
  ]

export const PLAYLIST_SORT_OPTIONS: {
  label: string
  field: PlaylistSortField
}[] = [
  { label: "Name", field: "name" },
  { label: "Date Added", field: "dateAdded" },
  { label: "Number of Tracks", field: "trackCount" },
]

export const FOLDER_SORT_OPTIONS: { label: string; field: FolderSortField }[] =
  [
    { label: "Name", field: "name" },
    { label: "Date Added", field: "dateAdded" },
    { label: "Number of Files", field: "trackCount" },
  ]

export const GENRE_SORT_OPTIONS: { label: string; field: ArtistSortField }[] = [
  { label: "Name", field: "name" },
  { label: "Number of Tracks", field: "trackCount" },
]

function compareValues(a: any, b: any, order: SortOrder) {
  if (a === b) return 0
  if (a === undefined || a === null) return 1
  if (b === undefined || b === null) return -1

  if (typeof a === "string" && typeof b === "string") {
    return order === "asc"
      ? a.localeCompare(b, undefined, { sensitivity: "base" })
      : b.localeCompare(a, undefined, { sensitivity: "base" })
  }

  if (a < b) return order === "asc" ? -1 : 1
  if (a > b) return order === "asc" ? 1 : -1
  return 0
}

export function sortTracks(tracks: Track[], config: SortConfig): Track[] {
  const { field, order } = config
  return [...tracks].sort((a, b) => {
    if (field === "trackNumber") {
      const discCompare = compareValues(
        a.discNumber || 1,
        b.discNumber || 1,
        order
      )
      if (discCompare !== 0) {
        return discCompare
      }

      const trackCompare = compareValues(
        a.trackNumber || 0,
        b.trackNumber || 0,
        order
      )
      if (trackCompare !== 0) {
        return trackCompare
      }

      const titleA = (a.title || a.filename || "").toLowerCase()
      const titleB = (b.title || b.filename || "").toLowerCase()
      return compareValues(titleA, titleB, order)
    }

    let aVal: any = a[field as keyof Track]
    let bVal: any = b[field as keyof Track]

    if (field === "filename") {
      aVal = a.filename || a.uri.split("/").pop()
      bVal = b.filename || b.uri.split("/").pop()
    } else if (field === "title") {
      aVal = (a.title || a.filename || "").toLowerCase()
      bVal = (b.title || b.filename || "").toLowerCase()
    } else if (field === "artist") {
      aVal = (a.artist || "Unknown Artist").toLowerCase()
      bVal = (b.artist || "Unknown Artist").toLowerCase()
    } else if (field === "album") {
      aVal = (a.album || "Unknown Album").toLowerCase()
      bVal = (b.album || "Unknown Album").toLowerCase()
    }

    const primaryResult = compareValues(aVal, bVal, order)
    if (field === "playCount" && primaryResult === 0) {
      return compareValues(a.lastPlayedAt || 0, b.lastPlayedAt || 0, "desc")
    }

    return primaryResult
  })
}

export function sortAlbums(albums: any[], config: SortConfig): any[] {
  const { field, order } = config
  return [...albums].sort((a, b) => {
    const aVal = field in a ? a[field] : undefined
    const bVal = field in b ? b[field] : undefined

    if (field === "title" || field === "artist") {
      return compareValues(
        (aVal || "").toString().toLowerCase(),
        (bVal || "").toString().toLowerCase(),
        order
      )
    }

    return compareValues(aVal, bVal, order)
  })
}

export function sortArtists(artists: any[], config: SortConfig): any[] {
  const { field, order } = config
  return [...artists].sort((a, b) => {
    const aVal = field in a ? a[field] : undefined
    const bVal = field in b ? b[field] : undefined

    if (field === "name") {
      return compareValues(
        (aVal || "").toString().toLowerCase(),
        (bVal || "").toString().toLowerCase(),
        order
      )
    }

    return compareValues(aVal, bVal, order)
  })
}

export function sortGeneric(items: any[], config: SortConfig): any[] {
  return sortAlbums(items, config)
}

export function sortGenres(genres: any[], config: SortConfig): any[] {
  return sortArtists(genres, config)
}
