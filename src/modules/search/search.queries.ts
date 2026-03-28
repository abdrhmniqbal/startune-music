import { useQuery } from "@tanstack/react-query"

import { queryClient } from "@/lib/tanstack-query"

import { searchKeys } from "./search.keys"
import {
  getGenreAlbums,
  getGenreDetails,
  getGenres,
  getGenreTopTracks,
} from "./search.repository"
import type { GenreAlbumInfo, GenreDetailsResult } from "./search.types"

export function useGenres() {
  return useQuery(
    {
      queryKey: searchKeys.genres(),
      queryFn: getGenres,
      placeholderData: (previousData) => previousData,
    },
    queryClient
  )
}

export function useGenreDetails(genreName: string) {
  return useQuery<GenreDetailsResult>(
    {
      queryKey: searchKeys.genreDetails(genreName),
      queryFn: async () => await getGenreDetails(genreName),
      enabled: genreName.length > 0,
      placeholderData: (previousData) => previousData,
    },
    queryClient
  )
}

export function useGenreTopTracks(genreName: string) {
  return useQuery(
    {
      queryKey: searchKeys.genreTopTracks(genreName),
      queryFn: async () => await getGenreTopTracks(genreName),
      enabled: genreName.length > 0,
      placeholderData: (previousData) => previousData,
    },
    queryClient
  )
}

export function useGenreAlbums(genreName: string) {
  return useQuery<GenreAlbumInfo[]>(
    {
      queryKey: searchKeys.genreAlbums(genreName),
      queryFn: async () => await getGenreAlbums(genreName),
      enabled: genreName.length > 0,
      placeholderData: (previousData) => previousData,
    },
    queryClient
  )
}
