export type { HistoryTopTracksPeriod as PlayPeriod } from "@/modules/history/history.types"

export {
  useTrackHistory as usePlayHistory,
  useTopTracksByPeriod as useTopTracks,
} from "@/modules/history/history.queries"
export {
  useAddToHistory,
  useIncrementPlayCount,
} from "@/modules/history/history.mutations"
