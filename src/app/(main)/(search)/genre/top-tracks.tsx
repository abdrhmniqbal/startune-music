import { Stack, useLocalSearchParams } from "expo-router"
import { useEffect, useMemo } from "react"
import { RefreshControl, View } from "react-native"
import Animated from "react-native-reanimated"

import { PlaybackActionsRow } from "@/components/blocks/playback-actions-row"
import { LibrarySkeleton } from "@/components/blocks/library-skeleton"
import { TrackList } from "@/components/blocks/track-list"
import LocalMusicNoteSolidIcon from "@/components/icons/local/music-note-solid"
import { EmptyState } from "@/components/ui/empty-state"
import {
  screenEnterTransition,
  screenExitTransition,
} from "@/constants/animations"
import {
  handleScroll,
  handleScrollStart,
  handleScrollStop,
} from "@/modules/ui/ui.store"
import { useThemeColors } from "@/modules/ui/theme"
import { startIndexing } from "@/modules/indexer/indexer.service"
import { useIndexerStore } from "@/modules/indexer/indexer.store"
import { logWarn } from "@/modules/logging/logging.service"
import { playTrack } from "@/modules/player/player.service"
import { useGenreTopTracks } from "@/modules/search/search.queries"

function getSafeRouteName(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? (value[0] ?? "") : (value ?? "")
  try {
    return {
      value: decodeURIComponent(raw),
      raw,
      decodeFailed: false,
    }
  } catch {
    return {
      value: raw,
      raw,
      decodeFailed: true,
    }
  }
}

export default function GenreTopTracksScreen() {
  const { name } = useLocalSearchParams<{ name: string }>()
  const isIndexing = useIndexerStore((state) => state.indexerState.isIndexing)
  const theme = useThemeColors()

  const parsedGenreRouteName = useMemo(() => getSafeRouteName(name), [name])
  const genreName = parsedGenreRouteName.value
  const normalizedGenreName = genreName.trim()

  useEffect(() => {
    if (!normalizedGenreName) {
      logWarn("Genre top-tracks route missing name param", {
        route: "/genre/top-tracks",
      })
      return
    }

    if (parsedGenreRouteName.decodeFailed) {
      logWarn("Genre top-tracks route name decode failed", {
        route: "/genre/top-tracks",
        rawName: parsedGenreRouteName.raw,
      })
    }
  }, [normalizedGenreName, parsedGenreRouteName.decodeFailed, parsedGenreRouteName.raw])

  const { data, isLoading, isFetching, refetch } =
    useGenreTopTracks(normalizedGenreName)
  const tracks = data ?? []

  async function refresh() {
    await startIndexing(false)
    await refetch()
  }

  function playAll() {
    if (tracks.length === 0) {
      return
    }

    playTrack(tracks[0], tracks)
  }

  function shuffle() {
    if (tracks.length === 0) {
      return
    }

    const randomIndex = Math.floor(Math.random() * tracks.length)
    playTrack(tracks[randomIndex], tracks)
  }

  if ((isLoading || isFetching) && tracks.length === 0) {
    return (
      <View className="flex-1 bg-background px-4 pt-4">
        <Stack.Screen
          options={{
            title: `${genreName} Top Tracks`,
          }}
        />
        <LibrarySkeleton type="tracks" itemCount={8} />
      </View>
    )
  }

  return (
    <View className="flex-1 bg-background">
      <Stack.Screen
        options={{
          title: `${genreName} Top Tracks`,
        }}
      />
      {tracks.length === 0 ? (
        <Animated.View
          entering={screenEnterTransition()}
          exiting={screenExitTransition()}
          className="px-4"
        >
          <EmptyState
            icon={
              <LocalMusicNoteSolidIcon
                fill="none"
                width={48}
                height={48}
                color={theme.muted}
              />
            }
            title="No top tracks yet"
            message={`Play some ${genreName} music to see your most played tracks here!`}
            className="mt-12"
          />
        </Animated.View>
      ) : (
        <TrackList
          data={tracks}
          showNumbers
          contentContainerStyle={{ paddingBottom: 200, paddingHorizontal: 16 }}
          onScroll={(e) => handleScroll(e.nativeEvent.contentOffset.y)}
          onScrollBeginDrag={handleScrollStart}
          onMomentumScrollEnd={handleScrollStop}
          onScrollEndDrag={handleScrollStop}
          refreshControl={
            <RefreshControl
              refreshing={isIndexing || isLoading || isFetching}
              onRefresh={refresh}
              tintColor={theme.accent}
            />
          }
          listHeader={
            <PlaybackActionsRow
              onPlay={playAll}
              onShuffle={shuffle}
              className="px-0 py-4"
            />
          }
        />
      )}
    </View>
  )
}
