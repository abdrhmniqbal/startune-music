import type { Album } from "@/components/blocks/album-grid"
import type {
  GenreAlbumInfo,
  GenreCategory,
  GenreVisual,
} from "@/modules/genres/genres.types"

export function mapGenresToCategories(genres: GenreVisual[]): GenreCategory[] {
  const mapped = genres.map((genre) => ({
    id: genre.name,
    title: genre.name,
    color: genre.color,
    pattern: genre.shape,
  }))

  return arrangeForGrid(mapped, 2)
}

function arrangeForGrid(
  categories: GenreCategory[],
  columns: number
): GenreCategory[] {
  const remaining = [...categories]
  const arranged: GenreCategory[] = []

  while (remaining.length > 0) {
    let bestIndex = 0
    let bestScore = Number.POSITIVE_INFINITY

    for (let i = 0; i < remaining.length; i++) {
      const candidate = remaining[i]
      const score = getPlacementScore(candidate, arranged, columns)

      if (score < bestScore) {
        bestScore = score
        bestIndex = i
      }
    }

    arranged.push(remaining.splice(bestIndex, 1)[0])
  }

  return arranged
}

function getPlacementScore(
  candidate: GenreCategory,
  arranged: GenreCategory[],
  columns: number
): number {
  const index = arranged.length
  let score = 0

  // Horizontal neighbor (side-by-side): stronger penalty.
  if (index % columns !== 0) {
    const left = arranged[index - 1]
    if (left) {
      if (left.color === candidate.color) score += 100
      if (left.pattern === candidate.pattern) score += 100
    }
  }

  // Vertical neighbor (above/below): also penalized.
  if (index >= columns) {
    const top = arranged[index - columns]
    if (top) {
      if (top.color === candidate.color) score += 80
      if (top.pattern === candidate.pattern) score += 80
    }
  }

  // Tie-breaker to keep overall distribution balanced.
  let sameColorCount = 0
  let samePatternCount = 0
  for (const item of arranged) {
    if (item.color === candidate.color) sameColorCount++
    if (item.pattern === candidate.pattern) samePatternCount++
  }
  score += sameColorCount * 3 + samePatternCount * 3

  return score
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
