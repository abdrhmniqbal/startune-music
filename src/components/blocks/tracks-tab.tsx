import type { NativeScrollEvent, NativeSyntheticEvent } from "react-native"
import type { Track } from "@/modules/player/player.store"

import type { DBTrack } from "@/types/database"
import * as React from "react"
import { LibrarySkeleton } from "@/components/blocks/library-skeleton"
import { TrackList } from "@/components/blocks/track-list"
import LocalMusicNoteSolidIcon from "@/components/icons/local/music-note-solid"
import { EmptyState } from "@/components/ui"
import { useThemeColors } from "@/hooks/use-theme-colors"
import {
  type SortConfig,
  sortTracks,
} from "@/modules/library/library-sort.store"
import { useTracks } from "@/modules/tracks/tracks.queries"
import { transformDBTrackToTrack } from "@/utils/transformers"

interface TracksTabProps {
  onTrackPress?: (track: Track, queue: Track[]) => void
  sortConfig?: SortConfig
  contentBottomPadding?: number
  onScroll?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollBeginDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onScrollEndDrag?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
  onMomentumScrollEnd?: (event: NativeSyntheticEvent<NativeScrollEvent>) => void
}

export const TracksTab: React.FC<TracksTabProps> = ({
  onTrackPress,
  sortConfig,
  contentBottomPadding = 0,
  onScroll,
  onScrollBeginDrag,
  onScrollEndDrag,
  onMomentumScrollEnd,
}) => {
  const theme = useThemeColors()

  const { data: dbTracks = [], isLoading, isPending } = useTracks()

  const tracks = (dbTracks as DBTrack[]).map(transformDBTrackToTrack)
  const effectiveSortConfig: SortConfig = sortConfig ?? {
    field: "title",
    order: "asc",
  }
  const sortedTracks = sortTracks(tracks, effectiveSortConfig)

  const handleTrackPress = (track: Track) => {
    onTrackPress?.(track, sortedTracks)
  }

  if (isLoading || isPending) {
    return <LibrarySkeleton type="tracks" />
  }

  if (tracks.length === 0) {
    return (
      <EmptyState
        icon={
          <LocalMusicNoteSolidIcon
            fill="none"
            width={48}
            height={48}
            color={theme.muted}
          />
        }
        title="No Tracks"
        message="Tracks you add to your library will appear here."
      />
    )
  }

  return (
    <TrackList
      data={sortedTracks}
      onTrackPress={handleTrackPress}
      contentContainerStyle={{ paddingBottom: contentBottomPadding }}
      resetScrollKey={`${effectiveSortConfig.field}-${effectiveSortConfig.order}`}
      onScroll={onScroll}
      onScrollBeginDrag={onScrollBeginDrag}
      onScrollEndDrag={onScrollEndDrag}
      onMomentumScrollEnd={onMomentumScrollEnd}
    />
  )
}
