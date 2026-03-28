import { useRouter } from "expo-router"
import { Input, PressableFeedback } from "heroui-native"
import * as React from "react"

import { RefreshControl, ScrollView, Text, View } from "react-native"
import { LibrarySkeleton } from "@/components/blocks/library-skeleton"
import LocalMusicNoteSolidIcon from "@/components/icons/local/music-note-solid"
import LocalSearchIcon from "@/components/icons/local/search"
import { GenreCard } from "@/components/patterns/genre-card"
import { EmptyState } from "@/components/ui/empty-state"
import {
  handleScroll,
  handleScrollStart,
  handleScrollStop,
} from "@/modules/ui/ui.store"
import { useThemeColors } from "@/hooks/use-theme-colors"
import { useGenres } from "@/modules/search/search.queries"
import type { Category } from "@/modules/search/search.types"
import { mapGenresToCategories } from "@/modules/search/search.utils"
import { startIndexing } from "@/modules/indexer/indexer.store"
import { useIndexerStore } from "@/modules/indexer/indexer.store"

export default function SearchScreen() {
  const theme = useThemeColors()
  const router = useRouter()
  const indexerState = useIndexerStore((state) => state.indexerState)
  const { data, refetch, isLoading, isFetching } = useGenres()

  const categories = mapGenresToCategories(data ?? [])

  async function refresh() {
    await startIndexing(false)
    await refetch()
  }

  function handleGenrePress(genre: Category) {
    router.push({
      pathname: "/(main)/(search)/genre/[name]",
      params: { name: genre.title },
    })
  }

  function handleSearchPress() {
    router.push("/search")
  }

  return (
    <ScrollView
      className="flex-1 bg-background"
      contentContainerStyle={{ padding: 20, paddingBottom: 200 }}
      showsVerticalScrollIndicator={false}
      keyboardShouldPersistTaps="handled"
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
          tintColor={theme.accent}
        />
      }
    >
      <View className="relative mb-6">
        <View className="absolute top-1/2 left-4 z-10 -translate-y-1/2">
          <LocalSearchIcon
            fill="none"
            width={24}
            height={24}
            color={theme.muted}
          />
        </View>
        <Input
          value=""
          editable={false}
          showSoftInputOnFocus={false}
          placeholder="Search for tracks, artists, albums..."
          className="pl-12"
        />
        <PressableFeedback
          onPress={handleSearchPress}
          className="absolute inset-0 z-20"
          accessibilityRole="button"
          accessibilityLabel="Open search"
        />
      </View>

      <Text className="mb-4 text-xl font-bold text-foreground">
        Browse by Genre
      </Text>

      {isLoading || isFetching ? (
        <LibrarySkeleton type="genres" itemCount={8} />
      ) : categories.length > 0 ? (
        <View className="flex-row flex-wrap justify-between gap-y-4">
          {categories.map((genre) => (
            <GenreCard
              key={genre.id}
              title={genre.title}
              color={genre.color}
              pattern={genre.pattern}
              onPress={() => handleGenrePress(genre)}
            />
          ))}
        </View>
      ) : (
        <EmptyState
          icon={
            <LocalMusicNoteSolidIcon
              fill="none"
              width={48}
              height={48}
              color={theme.muted}
            />
          }
          title="No genres found"
          message="Start playing music to see genres here!"
          className="mt-8"
        />
      )}
    </ScrollView>
  )
}
