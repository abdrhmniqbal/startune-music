export type FavoriteType = "track" | "artist" | "album" | "playlist"

export interface FavoriteEntry {
  id: string
  type: FavoriteType
  name: string
  subtitle?: string
  image?: string
  images?: string[]
  dateAdded: number
}
