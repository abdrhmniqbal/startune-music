import type { Album } from "@/components/blocks/album-grid"

import type { Category, GenreAlbumInfo, PatternType } from "./search.types"

const RAINBOW_COLORS = [
  "bg-rainbow-lime",
  "bg-rainbow-teal",
  "bg-rainbow-cyan",
  "bg-rainbow-blue",
  "bg-rainbow-indigo",
  "bg-rainbow-purple",
  "bg-rainbow-magenta",
  "bg-rainbow-red",
  "bg-rainbow-orange",
  "bg-rainbow-amber",
] as const

const PATTERNS: PatternType[] = [
  "circles",
  "waves",
  "diamonds",
  "triangles",
  "rings",
  "grid",
  "pills",
]

export function mapGenresToCategories(genres: string[]): Category[] {
  return genres.map((genre, index) => ({
    id: genre,
    title: genre,
    color: RAINBOW_COLORS[index % RAINBOW_COLORS.length],
    pattern: PATTERNS[index % PATTERNS.length],
  }))
}

export function getPreviewAlbums(
  albums: GenreAlbumInfo[],
  limit = 8
): GenreAlbumInfo[] {
  return [...albums]
    .sort((a, b) => (b.year || 0) - (a.year || 0))
    .slice(0, limit)
}

export function mapAlbumsToGridData(albums: GenreAlbumInfo[]): Album[] {
  return [...albums]
    .sort((a, b) => (b.year || 0) - (a.year || 0))
    .map((album, index) => ({
      id: `${album.name}-${index}`,
      title: album.name,
      artist: album.artist || "Unknown Artist",
      albumArtist: album.artist,
      image: album.image,
      trackCount: album.trackCount,
      year: album.year || 0,
      dateAdded: 0,
    }))
}
