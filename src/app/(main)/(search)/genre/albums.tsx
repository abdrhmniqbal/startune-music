import { Stack, useLocalSearchParams, useRouter } from "expo-router"
import { useEffect, useMemo, useState } from "react"
import { RefreshControl, Text, View } from "react-native"
import Animated from "react-native-reanimated"

import { type Album, AlbumGrid } from "@/components/blocks/album-grid"
import { LibrarySkeleton } from "@/components/blocks/library-skeleton"
import { SortSheet } from "@/components/blocks/sort-sheet"
import LocalVynilSolidIcon from "@/components/icons/local/vynil-solid"
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
import {
  mapAlbumsToGridData,
} from "@/modules/search/search.utils"
import { useGenreAlbums } from "@/modules/search/search.queries"
import { startIndexing } from "@/modules/indexer/indexer.service"
import { useIndexerStore } from "@/modules/indexer/indexer.store"
import { logWarn } from "@/modules/logging/logging.service"
import { ALBUM_SORT_OPTIONS } from "@/modules/library/library-sort.constants"
import type {
  AlbumSortField,
  SortOrder,
} from "@/modules/library/library-sort.types"
import { sortAlbums } from "@/modules/library/library-sort.utils"

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

export default function GenreAlbumsScreen() {
  const { name } = useLocalSearchParams<{ name: string }>()
  const router = useRouter()
  const isIndexing = useIndexerStore((state) => state.indexerState.isIndexing)
  const theme = useThemeColors()
  const [sortModalVisible, setSortModalVisible] = useState(false)
  const [sortConfig, setSortConfig] = useState<{
    field: AlbumSortField
    order: SortOrder
  }>({
    field: "year",
    order: "desc",
  })

  const parsedGenreRouteName = useMemo(() => getSafeRouteName(name), [name])
  const genreName = parsedGenreRouteName.value
  const normalizedGenreName = genreName.trim()

  useEffect(() => {
    if (!normalizedGenreName) {
      logWarn("Genre albums route missing name param", {
        route: "/genre/albums",
      })
      return
    }

    if (parsedGenreRouteName.decodeFailed) {
      logWarn("Genre albums route name decode failed", {
        route: "/genre/albums",
        rawName: parsedGenreRouteName.raw,
      })
    }
  }, [normalizedGenreName, parsedGenreRouteName.decodeFailed, parsedGenreRouteName.raw])

  const { data, isLoading, isFetching, refetch } =
    useGenreAlbums(normalizedGenreName)
  const albumData = mapAlbumsToGridData(data ?? [])
  const sortedAlbumData = sortAlbums(albumData, sortConfig) as Album[]

  async function refresh() {
    await startIndexing(false)
    await refetch()
  }

  if ((isLoading || isFetching) && sortedAlbumData.length === 0) {
    return (
      <View className="flex-1 bg-background px-4 pt-4">
        <Stack.Screen
          options={{
            title: `${genreName.trim()} Albums`,
          }}
        />
        <LibrarySkeleton type="albums" itemCount={8} />
      </View>
    )
  }

  function handleAlbumPress(album: Album) {
    router.push({
      pathname: "/album/[name]",
      params: { name: album.title },
    })
  }

  function handleSortSelect(field: AlbumSortField, order?: SortOrder) {
    setSortConfig((current) => {
      const nextOrder =
        order ||
        (current.field === field
          ? current.order === "asc"
            ? "desc"
            : "asc"
          : "asc")
      return { field, order: nextOrder }
    })
    setSortModalVisible(false)
  }

  function getSortLabel() {
    const selected = ALBUM_SORT_OPTIONS.find(
      (option) => option.field === sortConfig.field
    )
    return selected?.label || "Sort"
  }

  return (
    <SortSheet
      visible={sortModalVisible}
      onOpenChange={setSortModalVisible}
      currentField={sortConfig.field}
      currentOrder={sortConfig.order}
      onSelect={handleSortSelect}
    >
      <View className="flex-1 bg-background">
        <Stack.Screen
          options={{
            title: `${genreName.trim()} Albums`,
          }}
        />

        {sortedAlbumData.length === 0 ? (
          <Animated.View
            entering={screenEnterTransition()}
            exiting={screenExitTransition()}
            className="px-6 py-4"
          >
            <EmptyState
              icon={
                <LocalVynilSolidIcon
                  fill="none"
                  width={48}
                  height={48}
                  color={theme.muted}
                />
              }
              title="No albums found"
              message={`No albums available in ${genreName}`}
              className="mt-12"
            />
          </Animated.View>
        ) : (
          <AlbumGrid
            data={sortedAlbumData}
            onAlbumPress={handleAlbumPress}
            resetScrollKey={`${genreName}-${sortConfig.field}-${sortConfig.order}`}
            contentContainerStyle={{
              paddingHorizontal: 16,
              paddingBottom: 200,
            }}
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
              <Animated.View
                entering={screenEnterTransition()}
                exiting={screenExitTransition()}
                className="py-4"
              >
                <View className="mb-4 flex-row items-center justify-between">
                  <Text className="text-lg font-bold text-foreground">
                    {sortedAlbumData.length} Albums
                  </Text>
                  <SortSheet.Trigger label={getSortLabel()} iconSize={14} />
                </View>
              </Animated.View>
            }
          />
        )}

        <SortSheet.Content options={ALBUM_SORT_OPTIONS} />
      </View>
    </SortSheet>
  )
}
