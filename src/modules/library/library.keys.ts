export const ARTISTS_KEY = "artists"
export const ALBUMS_KEY = "albums"
export const SEARCH_KEY = "search"
export const RECENT_SEARCHES_KEY = "recent-searches"

export const libraryKeys = {
  artists: (
    orderByField: "name" | "trackCount" | "dateAdded",
    order: "asc" | "desc"
  ) => [ARTISTS_KEY, orderByField, order] as const,
  artist: (artistId: string) => [ARTISTS_KEY, artistId] as const,
  albums: (
    orderByField: "title" | "artist" | "year" | "trackCount" | "dateAdded",
    order: "asc" | "desc"
  ) => [ALBUMS_KEY, orderByField, order] as const,
  album: (albumId: string) => [ALBUMS_KEY, albumId] as const,
  tracksByAlbumName: (albumName: string) =>
    ["tracks", "album-name", albumName] as const,
  tracksByArtistName: (artistName: string) =>
    ["tracks", "artist-name", artistName] as const,
  search: (query: string) => [SEARCH_KEY, query] as const,
  recentSearches: () => [RECENT_SEARCHES_KEY] as const,
}
