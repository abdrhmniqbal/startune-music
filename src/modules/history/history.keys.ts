export const HISTORY_KEY = "history"
export const HISTORY_TOP_TRACKS_KEY = "history-top-tracks"
export const HISTORY_RECENTLY_PLAYED_KEY = "history-recently-played"

export const historyKeys = {
  tracks: () => [HISTORY_KEY] as const,
  topTracks: (period: string, limit: number) =>
    [HISTORY_TOP_TRACKS_KEY, period, limit] as const,
  recentlyPlayed: (limit: number) =>
    [HISTORY_RECENTLY_PLAYED_KEY, limit] as const,
}
