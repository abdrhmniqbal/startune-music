import type { Track } from "@/modules/player/player.types"

export interface SearchArtistResult {
  id: string
  name: string
  type: string
  followerCount: number
  isVerified: boolean
  image?: string
}

export interface SearchAlbumResult {
  id: string
  title: string
  artist: string
  isVerified: boolean
  image?: string
}

export interface SearchPlaylistResult {
  id: string
  title: string
  trackCount: number
  image?: string
  images?: string[]
}

export interface SearchResults {
  tracks: Track[]
  artists: SearchArtistResult[]
  albums: SearchAlbumResult[]
  playlists: SearchPlaylistResult[]
}

export interface RecentSearchEntry {
  id: string
  query: string
  title: string
  subtitle: string
  type?: "track" | "album" | "artist" | "playlist"
  targetId?: string
  image?: string
  createdAt: number
}

export interface AddRecentSearchInput {
  query: string
  title?: string
  subtitle?: string
  type?: "track" | "album" | "artist" | "playlist"
  targetId?: string
  image?: string
}
