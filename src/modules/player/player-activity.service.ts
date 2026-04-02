import { queryClient } from "@/lib/tanstack-query"
import type { Track } from "@/modules/player/player.types"
import {
  invalidateHistoryAfterPlayback,
  optimisticallyUpdateRecentlyPlayedHistory,
} from "@/modules/history/history-cache.service"
import { addTrackToHistory, incrementTrackPlayCount } from "@/modules/history/history.repository"

import {
  getPlaybackRefreshVersionState,
  setPlaybackRefreshVersionState,
} from "./player.store"

function bumpPlaybackRefreshVersion() {
  setPlaybackRefreshVersionState(getPlaybackRefreshVersionState() + 1)
}

export async function handleTrackActivated(track: Track) {
  optimisticallyUpdateRecentlyPlayedHistory(queryClient, track)
  await Promise.allSettled([
    addTrackToHistory(track.id),
    incrementTrackPlayCount(track.id),
  ])
  bumpPlaybackRefreshVersion()
  void invalidateHistoryAfterPlayback(queryClient)
}
