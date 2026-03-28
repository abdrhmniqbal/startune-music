import { Image } from "expo-image"
import { LinearGradient } from "expo-linear-gradient"
import { Stack, useLocalSearchParams, useRouter } from "expo-router"
import { Button, PressableFeedback } from "heroui-native"
import * as React from "react"
import { useState } from "react"
import { Dimensions, ScrollView, Text, View } from "react-native"
import Animated, {
  Extrapolation,
  interpolate,
  useAnimatedStyle,
  useDerivedValue,
  useSharedValue,
  withTiming,
} from "react-native-reanimated"

import { PlaybackActionsRow } from "@/components/blocks/playback-actions-row"
import { type Album, AlbumGrid } from "@/components/blocks/album-grid"
import { LibrarySkeleton } from "@/components/blocks/library-skeleton"
import { SortSheet } from "@/components/blocks/sort-sheet"
import { TrackList } from "@/components/blocks/track-list"
import LocalArrowLeftIcon from "@/components/icons/local/arrow-left"
import LocalChevronLeftIcon from "@/components/icons/local/chevron-left"
import LocalFavouriteIcon from "@/components/icons/local/favourite"
import LocalFavouriteSolidIcon from "@/components/icons/local/favourite-solid"
import LocalUserSolidIcon from "@/components/icons/local/user-solid"
import { TrackRow } from "@/components/patterns/track-row"
import { ScaleLoader } from "@/components/ui/scale-loader"
import { SectionTitle } from "@/components/ui/section-header"
import {
  screenEnterTransition,
  screenExitTransition,
} from "@/constants/animations"
import {
  handleScroll,
  handleScrollStart,
  handleScrollStop,
} from "@/modules/ui/ui.store"
import { useThemeColors } from "@/hooks/use-theme-colors"
import { buildArtistAlbums } from "@/modules/artists/artists.utils"
import { useIsFavorite } from "@/modules/favorites/favorites.queries"
import { useToggleFavorite } from "@/modules/favorites/favorites.mutations"
import {
  ALBUM_SORT_OPTIONS,
  type SortField,
  setSortConfig,
  sortAlbums,
  sortTracks,
  TRACK_SORT_OPTIONS,
  useLibrarySortStore,
} from "@/modules/library/library-sort.store"
import { useTracksByArtistName } from "@/modules/library/library.queries"
import { playTrack, type Track, usePlayerStore } from "@/modules/player/player.store"
import { cn } from "@/utils/common"

const SCREEN_WIDTH = Dimensions.get("window").width
const HEADER_COLLAPSE_THRESHOLD = SCREEN_WIDTH - 120

function setAnimatedValue<T>(target: { value: T }, nextValue: T) {
  target.value = nextValue
}

function getSafeRouteName(value: string | string[] | undefined) {
  const raw = Array.isArray(value) ? (value[0] ?? "") : (value ?? "")
  try {
    return decodeURIComponent(raw)
  } catch {
    return raw
  }
}

export default function ArtistDetailsScreen() {
  const theme = useThemeColors()
  const router = useRouter()
  const { name } = useLocalSearchParams<{ name: string }>()
  const toggleFavoriteMutation = useToggleFavorite()

  const [isHeaderSolid, setIsHeaderSolid] = useState(false)
  const [activeView, setActiveView] = useState<
    "overview" | "tracks" | "albums"
  >("overview")
  const [sortModalVisible, setSortModalVisible] = useState(false)
  const scrollY = useSharedValue(0)
  const currentTrack = usePlayerStore((state) => state.currentTrack)
  const allTracks = usePlayerStore((state) => state.tracks)
  const allSortConfigs = useLibrarySortStore((state) => state.sortConfig)
  const artistName = getSafeRouteName(name).trim() || "Unknown Artist"
  const normalizedArtistName = artistName.toLowerCase()
  const {
    data: artistTracksFromQuery = [],
    isLoading: isArtistTracksLoading,
    isFetching: isArtistTracksFetching,
  } = useTracksByArtistName(artistName)
  const artistTracks =
    artistTracksFromQuery.length > 0
      ? artistTracksFromQuery
      : allTracks.filter(
          (track) =>
            (track.artist || track.albumArtist || "").trim().toLowerCase() ===
            normalizedArtistName
        )
  const artistId = artistTracks[0]?.artistId
  const artistImage = artistTracks.find((track) => track.image)?.image
  const { data: isArtistFavorite = false } = useIsFavorite(
    "artist",
    artistId || ""
  )
  const isLoading =
    (isArtistTracksLoading || isArtistTracksFetching) &&
    artistTracks.length === 0
  const albums = buildArtistAlbums(artistTracks)
  const sortedArtistTracks = sortTracks(
    artistTracks,
    allSortConfigs.ArtistTracks
  )
  const popularTracks = sortedArtistTracks.slice(0, 5)
  const sortedAlbums = sortAlbums(
    albums.map((album) => ({ ...album, id: album.title }) as Album),
    allSortConfigs.ArtistAlbums
  )
  const currentTab =
    activeView === "tracks"
      ? "ArtistTracks"
      : activeView === "albums"
        ? "ArtistAlbums"
        : "ArtistTracks"
  const sortConfig = allSortConfigs[currentTab]

  const smoothScrollY = useDerivedValue(() =>
    withTiming(scrollY.value, { duration: 90 })
  )

  const heroArtworkStyle = useAnimatedStyle(() => {
    const y = smoothScrollY.value
    return {
      transform: [
        {
          translateY: interpolate(
            y,
            [-220, 0, 220],
            [-52, 0, 0],
            Extrapolation.CLAMP
          ),
        },
        {
          scale: interpolate(
            y,
            [-220, 0, 220],
            [1.22, 1.08, 1],
            Extrapolation.CLAMP
          ),
        },
      ],
    }
  })

  if (isLoading) {
    return (
      <View className="flex-1 bg-background">
        <LibrarySkeleton type="artist-detail" />
      </View>
    )
  }

  function handleBack() {
    router.back()
  }

  function handleSortSelect(field: SortField, order?: "asc" | "desc") {
    setSortConfig(currentTab, field, order)
  }

  function navigateTo(view: "overview" | "tracks" | "albums") {
    setActiveView(view)
  }

  function playArtistTrack(track: Track) {
    playTrack(track, sortedArtistTracks)
  }

  function playAllTracks() {
    if (sortedArtistTracks.length === 0) {
      return
    }

    playTrack(sortedArtistTracks[0], sortedArtistTracks)
  }

  function shuffleTracks() {
    if (sortedArtistTracks.length === 0) {
      return
    }

    const randomIndex = Math.floor(Math.random() * sortedArtistTracks.length)
    playTrack(sortedArtistTracks[randomIndex], sortedArtistTracks)
  }

  function openAlbum(album: Album) {
    router.push({
      pathname: "/album/[name]",
      params: { name: album.title },
    })
  }

  function getSortLabel() {
    const options =
      activeView === "tracks" ? TRACK_SORT_OPTIONS : ALBUM_SORT_OPTIONS
    return (
      options.find((option) => option.field === sortConfig.field)?.label ||
      "Sort"
    )
  }

  const renderHeroSection = () => (
    <View style={{ height: SCREEN_WIDTH }} className="relative overflow-hidden">
      <Animated.View
        style={[
          { position: "absolute", top: 0, right: 0, bottom: 0, left: 0 },
          heroArtworkStyle,
        ]}
      >
        {artistImage ? (
          <Image
            source={{ uri: artistImage }}
            style={{ width: "100%", height: "100%" }}
            contentFit="cover"
          />
        ) : (
          <View className="h-full w-full items-center justify-center bg-surface-secondary">
            <LocalUserSolidIcon
              fill="none"
              width={120}
              height={120}
              color={theme.muted}
            />
          </View>
        )}
      </Animated.View>

      <LinearGradient
        colors={["transparent", "rgba(0,0,0,0.7)", theme.background]}
        locations={[0.3, 0.7, 1]}
        style={{
          position: "absolute",
          left: 0,
          right: 0,
          bottom: 0,
          height: "60%",
        }}
      />

      <View className="absolute right-6 bottom-8 left-6">
        <Text className="mb-2 text-4xl font-bold text-white">{artistName}</Text>
        <Text className="text-base text-white/70">
          {artistTracks.length} tracks
        </Text>
      </View>
    </View>
  )

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
            headerTransparent: true,
            headerStyle: {
              backgroundColor: isHeaderSolid ? theme.background : "transparent",
            },
            title: isHeaderSolid ? artistName : "",
            headerTintColor: isHeaderSolid ? theme.foreground : "white",
            headerBackVisible: false,
            headerLeft: () => (
              <Button
                onPress={handleBack}
                variant="ghost"
                className={cn("-ml-2", !isHeaderSolid && "bg-overlay/30")}
                isIconOnly
              >
                <LocalArrowLeftIcon
                  fill="none"
                  width={24}
                  height={24}
                  color={isHeaderSolid ? theme.foreground : "white"}
                />
              </Button>
            ),
            headerRight: () =>
              artistId ? (
                <Button
                  onPress={() => {
                    if (!artistId) {
                      return
                    }

                    void toggleFavoriteMutation.mutateAsync({
                      type: "artist",
                      itemId: artistId,
                      isCurrentlyFavorite: isArtistFavorite,
                      name: artistName,
                      subtitle: `${artistTracks.length} tracks`,
                      image: artistImage,
                    })
                  }}
                  isDisabled={toggleFavoriteMutation.isPending}
                  variant="ghost"
                  className={cn("-ml-2", !isHeaderSolid && "bg-overlay/30")}
                  isIconOnly
                >
                  {isArtistFavorite ? (
                    <LocalFavouriteSolidIcon
                      fill="none"
                      width={24}
                      height={24}
                      color="#ef4444"
                    />
                  ) : (
                    <LocalFavouriteIcon
                      fill="none"
                      width={24}
                      height={24}
                      color={isHeaderSolid ? theme.foreground : "white"}
                    />
                  )}
                </Button>
              ) : undefined,
          }}
        />

        {activeView === "overview" ? (
          <ScrollView
            className="flex-1"
            showsVerticalScrollIndicator={false}
            contentContainerStyle={{ paddingBottom: 200 }}
            onScroll={(e) => {
              const y = e.nativeEvent.contentOffset.y
              setAnimatedValue(scrollY, y)
              handleScroll(y)
              const nextHeaderSolid = y > HEADER_COLLAPSE_THRESHOLD
              if (nextHeaderSolid !== isHeaderSolid) {
                setIsHeaderSolid(nextHeaderSolid)
              }
            }}
            onScrollBeginDrag={handleScrollStart}
            onMomentumScrollEnd={handleScrollStop}
            onScrollEndDrag={handleScrollStop}
            scrollEventThrottle={16}
          >
            {renderHeroSection()}

            <Animated.View
              key={activeView}
              entering={screenEnterTransition()}
              exiting={screenExitTransition()}
              className="pt-4"
            >
              <View className="px-6">
                <SectionTitle
                  title="Tracks"
                  onViewMore={() => navigateTo("tracks")}
                />
                <View style={{ gap: 8 }}>
                  {popularTracks.map((track) => (
                    <TrackRow
                      key={track.id}
                      track={track}
                      onPress={() => playArtistTrack(track)}
                      titleClassName={
                        currentTrack?.id === track.id
                          ? "text-accent"
                          : undefined
                      }
                      imageOverlay={
                        currentTrack?.id === track.id ? (
                          <ScaleLoader size={16} />
                        ) : undefined
                      }
                    />
                  ))}
                </View>
              </View>

              {albums.length > 0 && (
                <View className="mt-8 px-6">
                  <SectionTitle
                    title="Albums"
                    onViewMore={() => navigateTo("albums")}
                  />
                  <AlbumGrid
                    horizontal
                    data={albums.map(
                      (album) => ({ ...album, id: album.title }) as Album
                    )}
                    onAlbumPress={openAlbum}
                  />
                </View>
              )}
            </Animated.View>
          </ScrollView>
        ) : activeView === "tracks" ? (
          <TrackList
            data={sortedArtistTracks}
            onTrackPress={playArtistTrack}
            resetScrollKey={`${artistId || artistName}-tracks-${sortConfig.field}-${sortConfig.order}`}
            contentContainerStyle={{
              paddingBottom: 200,
              paddingHorizontal: 24,
            }}
            onScroll={(e) => {
              const y = e.nativeEvent.contentOffset.y
              setAnimatedValue(scrollY, y)
              handleScroll(y)
              const nextHeaderSolid = y > HEADER_COLLAPSE_THRESHOLD
              if (nextHeaderSolid !== isHeaderSolid) {
                setIsHeaderSolid(nextHeaderSolid)
              }
            }}
            onScrollBeginDrag={handleScrollStart}
            onMomentumScrollEnd={handleScrollStop}
            onScrollEndDrag={handleScrollStop}
            listHeader={
              <>
                <View style={{ marginHorizontal: -24 }}>
                  {renderHeroSection()}
                </View>
                <Animated.View
                  entering={screenEnterTransition()}
                  className="pt-4"
                >
                  <View className="mb-6 flex-row items-center justify-between">
                    <View className="flex-row items-center gap-3">
                      <PressableFeedback
                        onPress={() => navigateTo("overview")}
                        className="mr-2 active:opacity-50"
                      >
                        <LocalChevronLeftIcon
                          fill="none"
                          width={20}
                          height={20}
                          color={theme.muted}
                        />
                      </PressableFeedback>
                      <Text className="text-lg font-bold text-foreground">
                        All Tracks
                      </Text>
                    </View>
                    <SortSheet.Trigger label={getSortLabel()} iconSize={14} />
                  </View>
                  <PlaybackActionsRow
                    onPlay={playAllTracks}
                    onShuffle={shuffleTracks}
                  />
                </Animated.View>
              </>
            }
          />
        ) : (
          <AlbumGrid
            data={sortedAlbums}
            onAlbumPress={openAlbum}
            resetScrollKey={`${artistId || artistName}-albums-${sortConfig.field}-${sortConfig.order}`}
            contentContainerStyle={{
              paddingBottom: 200,
              paddingHorizontal: 16,
            }}
            onScroll={(e) => {
              const y = e.nativeEvent.contentOffset.y
              setAnimatedValue(scrollY, y)
              handleScroll(y)
              const nextHeaderSolid = y > HEADER_COLLAPSE_THRESHOLD
              if (nextHeaderSolid !== isHeaderSolid) {
                setIsHeaderSolid(nextHeaderSolid)
              }
            }}
            onScrollBeginDrag={handleScrollStart}
            onMomentumScrollEnd={handleScrollStop}
            onScrollEndDrag={handleScrollStop}
            listHeader={
              <>
                <View style={{ marginHorizontal: -16 }}>
                  {renderHeroSection()}
                </View>
                <Animated.View
                  entering={screenEnterTransition()}
                  className="px-2 pt-4"
                >
                  <View className="mb-6 flex-row items-center justify-between">
                    <View className="flex-row items-center gap-3">
                      <PressableFeedback
                        onPress={() => navigateTo("overview")}
                        className="mr-2 active:opacity-50"
                      >
                        <LocalChevronLeftIcon
                          fill="none"
                          width={20}
                          height={20}
                          color={theme.muted}
                        />
                      </PressableFeedback>
                      <Text className="text-lg font-bold text-foreground">
                        All Albums
                      </Text>
                    </View>
                    <SortSheet.Trigger label={getSortLabel()} iconSize={14} />
                  </View>
                </Animated.View>
              </>
            }
          />
        )}

        <SortSheet.Content
          options={
            activeView === "tracks"
              ? TRACK_SORT_OPTIONS
              : activeView === "albums"
                ? ALBUM_SORT_OPTIONS
                : []
          }
        />
      </View>
    </SortSheet>
  )
}
