import type { Track } from "@/modules/player/player.types"
import {
  getAlbumsByGenre,
  getAllGenres,
  getAllTracksByGenre,
  getTopTracksByGenre,
} from "@/modules/genres/genres.repository"

import type { GenreAlbumInfo } from "./search.types"

export async function getGenres(): Promise<string[]> {
  return getAllGenres()
}

export async function getGenreDetails(
  genreName: string
): Promise<{ topTracks: Track[]; albums: GenreAlbumInfo[] }> {
  const [topTracks, albums] = await Promise.all([
    getTopTracksByGenre(genreName, 25),
    getAlbumsByGenre(genreName),
  ])

  return { topTracks, albums }
}

export async function getGenreTopTracks(genreName: string): Promise<Track[]> {
  return getAllTracksByGenre(genreName)
}

export async function getGenreAlbums(
  genreName: string
): Promise<GenreAlbumInfo[]> {
  return getAlbumsByGenre(genreName)
}
