import type { Album } from "@/components/blocks/album-grid"
import type { GenreAlbumInfo as BaseGenreAlbumInfo } from "@/modules/genres/genres.types"

export type GenreAlbumInfo = BaseGenreAlbumInfo

export type PatternType =
  | "circles"
  | "waves"
  | "grid"
  | "diamonds"
  | "triangles"
  | "rings"
  | "pills"

export interface Category {
  id: string
  title: string
  color: string
  pattern: PatternType
}

export interface GenreDetailsResult {
  topTracks: import("@/modules/player/player.types").Track[]
  albums: GenreAlbumInfo[]
}

export type GenreGridAlbum = Album
