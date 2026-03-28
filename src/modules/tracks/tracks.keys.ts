export const TRACKS_KEY = "tracks"

export const trackKeys = {
  all: (filters?: unknown) => [TRACKS_KEY, filters] as const,
  detail: (trackId: string) => [TRACKS_KEY, trackId] as const,
}
