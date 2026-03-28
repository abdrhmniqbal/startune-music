import { Stack, useLocalSearchParams, useRouter } from "expo-router"
import { RefreshControl, ScrollView, View } from "react-native"

import Animated from "react-native-reanimated"
import { ContentSection } from "@/components/blocks/content-section"
import { LibrarySkeleton } from "@/components/blocks/library-skeleton"
import { MediaCarousel } from "@/components/blocks/media-carousel"
import { RankedTrackCarousel } from "@/components/blocks/ranked-track-carousel"
import LocalMusicNoteSolidIcon from "@/components/icons/local/music-note-solid"
import LocalVynilSolidIcon from "@/components/icons/local/vynil-solid"
import { MusicCard } from "@/components/patterns/music-card"
import { screenEnterTransition } from "@/constants/animations"
import {
  handleScroll,
  handleScrollStart,
  handleScrollStop,
} from "@/modules/ui/ui.store"
import { useThemeColors } from "@/hooks/use-theme-colors"
import {
  getPreviewAlbums,
} from "@/modules/search/search.utils"
import { useGenreDetails } from "@/modules/search/search.queries"
import type { GenreAlbumInfo } from "@/modules/search/search.types"
import { startIndexing } from "@/modules/indexer/indexer.store"
import { useIndexerStore } from "@/modules/indexer/indexer.store"

const CHUNK_SIZE = 5

export default function GenreDetailsScreen() {
  const { name } = useLocalSearchParams<{ name: string }>()
  const router = useRouter()
  const theme = useThemeColors()
  const indexerState = useIndexerStore((state) => state.indexerState)

  const genreName = decodeURIComponent(name || "")
  const { data, isLoading, isFetching, refetch } = useGenreDetails(genreName)
  const topTracks = data?.topTracks ?? []
  const albums = data?.albums ?? []
  const previewAlbums = getPreviewAlbums(albums)

  async function refresh() {
    await startIndexing(false)
    await refetch()
  }

  if ((isLoading || isFetching) && topTracks.length === 0 && albums.length === 0) {
    return (
      <View className="flex-1 bg-background pt-5">
        <Stack.Screen
          options={{
            title: genreName,
          }}
        />
        <LibrarySkeleton type="genre-overview" />
      </View>
    )
  }

  function renderAlbumItem(album: GenreAlbumInfo) {
    const subtitle = `${album.artist || "Unknown Artist"} · ${album.trackCount} tracks`

    return (
      <MusicCard
        title={album.name}
        subtitle={subtitle}
        image={album.image}
        icon={
          <LocalMusicNoteSolidIcon
            fill="none"
            width={48}
            height={48}
            color={theme.muted}
          />
        }
        onPress={() =>
          router.push({
            pathname: "/album/[name]",
            params: { name: album.name },
          })
        }
      />
    )
  }

  return (
    <View className="flex-1 bg-background">
      <Stack.Screen
        options={{
          title: genreName,
        }}
      />
      <ScrollView
        className="flex-1"
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{
          paddingBottom: 200,
        }}
        onScroll={(e) => handleScroll(e.nativeEvent.contentOffset.y)}
        onScrollBeginDrag={handleScrollStart}
        onMomentumScrollEnd={handleScrollStop}
        onScrollEndDrag={handleScrollStop}
        scrollEventThrottle={16}
        refreshControl={
          <RefreshControl
            refreshing={indexerState.isIndexing || isLoading || isFetching}
            onRefresh={refresh}
            tintColor={theme.accent}
          />
        }
      >
        <Animated.View entering={screenEnterTransition()}>
          <ContentSection
            title="Top Tracks"
            data={topTracks}
            onViewMore={() =>
              router.push({
                pathname: "./top-tracks",
                params: { name: genreName },
              })
            }
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
              message: `Play some ${genreName} music to see top tracks!`,
            }}
            renderContent={(data) => (
              <RankedTrackCarousel data={data} chunkSize={CHUNK_SIZE} />
            )}
          />
        </Animated.View>

        <Animated.View entering={screenEnterTransition()}>
          <ContentSection
            title="Recommended Albums"
            data={previewAlbums}
            onViewMore={() =>
              router.push({ pathname: "./albums", params: { name: genreName } })
            }
            emptyState={{
              icon: (
                <LocalVynilSolidIcon
                  fill="none"
                  width={48}
                  height={48}
                  color={theme.muted}
                />
              ),
              title: "No albums found",
              message: `No albums available in ${genreName}`,
            }}
            renderContent={(data) => (
              <MediaCarousel
                data={data}
                renderItem={renderAlbumItem}
                keyExtractor={(album, index) => `${album.name}-${index}`}
              />
            )}
          />
        </Animated.View>
      </ScrollView>
    </View>
  )
}
