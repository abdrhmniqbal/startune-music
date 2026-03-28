export interface TrackFilter {
  artistId?: string
  albumId?: string
  genreId?: string
  isFavorite?: boolean
  searchQuery?: string
  sortBy?: "title" | "artist" | "album" | "dateAdded" | "playCount" | "rating"
  sortOrder?: "asc" | "desc"
}
