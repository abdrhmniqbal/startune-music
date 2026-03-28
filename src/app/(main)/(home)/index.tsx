import { useRouter } from "expo-router"
import * as React from "react"
import { RefreshControl, ScrollView, View } from "react-native"

import { ContentSection } from "@/components/blocks/content-section"
import { LibrarySkeleton } from "@/components/blocks/library-skeleton"
import { MediaCarousel } from "@/components/blocks/media-carousel"
import { RankedTrackCarousel } from "@/components/blocks/ranked-track-carousel"
import LocalClockSolidIcon from "@/components/icons/local/clock-solid"
import LocalMusicNoteSolidIcon from "@/components/icons/local/music-note-solid"
import { TrackRow } from "@/components/patterns/track-row"
import { ScaleLoader } from "@/components/ui/scale-loader"
import {
  handleScroll,
  handleScrollStart,
  handleScrollStop,
} from "@/modules/ui/ui.store"
import { useThemeColors } from "@/hooks/use-theme-colors"
import {
  useRecentlyPlayedTracks,
  useTopTracksByPeriod,
} from "@/modules/history/history.queries"
import { startIndexing } from "@/modules/indexer/indexer.store"
import { useIndexerStore } from "@/modules/indexer/indexer.store"
import {
  playTrack,
  usePlayerStore,
  type Track,
} from "@/modules/player/player.store"

const CHUNK_SIZE = 5
const RECENTLY_PLAYED_LIMIT = 8
const TOP_TRACKS_LIMIT = 25

export default function HomeScreen() {
  const router = useRouter()
  const theme = useThemeColors()
  const indexerState = useIndexerStore((state) => state.indexerState)
  const currentTrack = usePlayerStore((state) => state.currentTrack)
  const {
    data: recentlyPlayedTracksData,
    isLoading: isRecentlyPlayedLoading,
    isFetching: isRecentlyPlayedFetching,
    refetch: refetchRecentlyPlayedTracks,
  } = useRecentlyPlayedTracks(RECENTLY_PLAYED_LIMIT)
  const {
    data: topTracksData,
    isLoading: isTopTracksLoading,
    isFetching: isTopTracksFetching,
    refetch: refetchTopTracks,
  } = useTopTracksByPeriod("all", TOP_TRACKS_LIMIT)

  const recentlyPlayedTracks = recentlyPlayedTracksData ?? []
  const topTracks = topTracksData ?? []
  const isLoading =
    (isRecentlyPlayedLoading ||
      isRecentlyPlayedFetching ||
      isTopTracksLoading ||
      isTopTracksFetching) &&
    recentlyPlayedTracks.length === 0 &&
    topTracks.length === 0

  async function refresh() {
    await startIndexing(false)
    await Promise.all([refetchRecentlyPlayedTracks(), refetchTopTracks()])
  }

  if (isLoading) {
    return (
      <View className="flex-1 bg-background pt-6">
        <LibrarySkeleton type="home" />
      </View>
    )
  }

  function renderRecentlyPlayedItem(item: Track) {
    return (
      <TrackRow
        track={item}
        variant="grid"
        onPress={() => playTrack(item, recentlyPlayedTracks)}
        titleClassName={
          currentTrack?.id === item.id ? "text-accent" : undefined
        }
        imageOverlay={
          currentTrack?.id === item.id ? <ScaleLoader size={16} /> : undefined
        }
      />
    )
  }

  return (
    <ScrollView
      className="flex-1 bg-background"
      contentContainerStyle={{ paddingBottom: 200 }}
      contentInsetAdjustmentBehavior="automatic"
      onScroll={(e) => handleScroll(e.nativeEvent.contentOffset.y)}
      onScrollBeginDrag={handleScrollStart}
      onMomentumScrollEnd={handleScrollStop}
      onScrollEndDrag={handleScrollStop}
      scrollEventThrottle={16}
      refreshControl={
        <RefreshControl
          refreshing={indexerState.isIndexing}
          onRefresh={refresh}
          colors={[theme.accent]}
          tintColor={theme.accent}
          progressBackgroundColor={theme.default}
        />
      }
    >
      <View className="pt-6">
        <ContentSection
          title="Recently Played"
          data={recentlyPlayedTracks}
          onViewMore={() => router.push("/(main)/(home)/recently-played")}
          emptyState={{
            icon: (
              <LocalClockSolidIcon
                fill="none"
                width={48}
                height={48}
                color={theme.muted}
              />
            ),
            title: "No recently played",
            message: "Start playing music!",
          }}
          renderContent={(data) => (
            <MediaCarousel
              data={data}
              renderItem={renderRecentlyPlayedItem}
              keyExtractor={(item, index) => `${item.id}-${index}`}
              gap={10}
            />
          )}
        />

        <ContentSection
          title="Top Tracks"
          data={topTracks}
          onViewMore={() => router.push("/(main)/(home)/top-tracks")}
          emptyState={{
            icon: (
              <LocalMusicNoteSolidIcon
                fill="none"
                width={48}
                height={48}
                color={theme.muted}
              />
            ),
            title: "No top tracks",
            message: "Play more music together!",
          }}
          renderContent={(data) => (
            <RankedTrackCarousel data={data} chunkSize={CHUNK_SIZE} />
          )}
        />
      </View>
    </ScrollView>
  )
}
