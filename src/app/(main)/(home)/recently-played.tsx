import { RefreshControl, View } from "react-native"

import { PlaybackActionsRow } from "@/components/blocks/playback-actions-row"
import { LibrarySkeleton } from "@/components/blocks/library-skeleton"
import { TrackList } from "@/components/blocks/track-list"
import LocalClockSolidIcon from "@/components/icons/local/clock-solid"
import { EmptyState } from "@/components/ui/empty-state"
import {
  handleScroll,
  handleScrollStart,
  handleScrollStop,
} from "@/modules/ui/ui.store"
import { useThemeColors } from "@/hooks/use-theme-colors"
import { useRecentlyPlayedTracks } from "@/modules/history/history.queries"
import { startIndexing } from "@/modules/indexer/indexer.store"
import { useIndexerStore } from "@/modules/indexer/indexer.store"
import { playTrack } from "@/modules/player/player.store"

const RECENTLY_PLAYED_SCREEN_LIMIT = 50

export default function RecentlyPlayedScreen() {
  const theme = useThemeColors()
  const indexerState = useIndexerStore((state) => state.indexerState)
  const { data: historyData, isLoading, isFetching, refetch } =
    useRecentlyPlayedTracks(RECENTLY_PLAYED_SCREEN_LIMIT)

  const history = historyData ?? []

  async function refresh() {
    await startIndexing(false)
    await refetch()
  }

  function playFirst() {
    if (history.length === 0) {
      return
    }

    playTrack(history[0], history)
  }

  function shuffle() {
    if (history.length === 0) {
      return
    }

    const randomIndex = Math.floor(Math.random() * history.length)
    playTrack(history[randomIndex], history)
  }

  if ((isLoading || isFetching) && history.length === 0) {
    return (
      <View className="flex-1 bg-background px-4 pt-4">
        <LibrarySkeleton type="tracks" itemCount={10} />
      </View>
    )
  }

  return (
    <View className="flex-1 bg-background">
      {history.length === 0 ? (
        <EmptyState
          icon={
            <LocalClockSolidIcon
              fill="none"
              width={48}
              height={48}
              color={theme.muted}
            />
          }
          title="No recently played"
          message="Your listening history will appear here once you start playing music."
          className="mt-12 px-4"
        />
      ) : (
        <TrackList
          data={history}
          contentContainerStyle={{ paddingBottom: 200, paddingHorizontal: 16 }}
          onScroll={(e) => handleScroll(e.nativeEvent.contentOffset.y)}
          onScrollBeginDrag={handleScrollStart}
          onMomentumScrollEnd={handleScrollStop}
          onScrollEndDrag={handleScrollStop}
          refreshControl={
            <RefreshControl
              refreshing={indexerState.isIndexing}
              onRefresh={refresh}
              tintColor={theme.accent}
            />
          }
          listHeader={
            <PlaybackActionsRow
              onPlay={playFirst}
              onShuffle={shuffle}
              className="px-0 py-4"
            />
          }
        />
      )}
    </View>
  )
}
