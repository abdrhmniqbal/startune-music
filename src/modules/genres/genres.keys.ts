export const GENRES_KEY = "genres"

export const genreKeys = {
  all: () => [GENRES_KEY] as const,
  detail: (genreId: string) => [GENRES_KEY, genreId] as const,
}
