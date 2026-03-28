export const SEARCH_GENRES_KEY = "search-genres"
export const GENRE_DETAILS_KEY = "genre-details"
export const GENRE_TOP_TRACKS_KEY = "genre-top-tracks"
export const GENRE_ALBUMS_KEY = "genre-albums"

export const searchKeys = {
  genres: () => [SEARCH_GENRES_KEY] as const,
  genreDetails: (genreName: string) => [GENRE_DETAILS_KEY, genreName] as const,
  genreTopTracks: (genreName: string) =>
    [GENRE_TOP_TRACKS_KEY, genreName] as const,
  genreAlbums: (genreName: string) => [GENRE_ALBUMS_KEY, genreName] as const,
}
