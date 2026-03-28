import { useQuery } from "@tanstack/react-query"

import { queryClient } from "@/lib/tanstack-query"

import { genreKeys } from "./genres.keys"
import { getGenreById, listGenres } from "./genres.repository"

export function useGenres() {
  return useQuery(
    {
      queryKey: genreKeys.all(),
      queryFn: listGenres,
    },
    queryClient
  )
}

export function useGenre(id: string) {
  return useQuery(
    {
      queryKey: genreKeys.detail(id),
      queryFn: async () => await getGenreById(id),
    },
    queryClient
  )
}
