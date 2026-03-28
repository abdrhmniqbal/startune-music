import type { GenreShape } from "./genres.constants"

export interface GenreAlbumInfo {
  name: string
  artist?: string
  image?: string
  trackCount: number
  year?: number
}

export interface GenreVisual {
  name: string
  color: string
  shape: GenreShape
}

export type PatternType = GenreShape

export interface GenreCategory {
  id: string
  title: string
  color: string
  pattern: PatternType
}
