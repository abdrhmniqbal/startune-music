import { useLocalSearchParams, useNavigation, useRouter } from "expo-router"
import { Input, PressableFeedback } from "heroui-native"
import * as React from "react"
import { useEffect, useLayoutEffect, useRef, useState } from "react"
import { useMutation } from "@tanstack/react-query"
import {
  BackHandler,
  Platform,
  ScrollView,
  type TextInput,
  View,
} from "react-native"

import {
  RecentSearches,
  type RecentSearchItem,
} from "@/components/blocks/recent-searches"
import {
  type SearchAlbumResult,
  type SearchArtistResult,
  type SearchPlaylistResult,
  SearchResults,
  type SearchTab,
} from "@/components/blocks/search-results"
import LocalArrowLeftIcon from "@/components/icons/local/arrow-left"
import LocalCancelCircleSolidIcon from "@/components/icons/local/cancel-circle-solid"
import { queryClient } from "@/lib/tanstack-query"
import { libraryKeys } from "@/modules/library/library.keys"
import {
  addRecentSearch,
  clearRecentSearches,
  deleteRecentSearch,
} from "@/modules/library/library.repository"
import { useThemeColors } from "@/modules/ui/theme"
import { useRecentSearches, useSearch } from "@/modules/library/library.queries"
import type { Track } from "@/modules/player/player.types"

interface HeaderSearchInputProps {
  theme: ReturnType<typeof useThemeColors>
  initialValue: string
  onChangeText: (text: string) => void
  onSubmit: () => void
  onBack: () => void
}

function HeaderSearchInput({
  theme,
  initialValue,
  onChangeText,
  onSubmit,
  onBack,
}: HeaderSearchInputProps) {
  const [inputValue, setInputValue] = useState(initialValue)
  const inputRef = useRef<TextInput>(null)
  const shouldAutoFocus = initialValue.trim().length === 0

  useEffect(() => {
    if (!shouldAutoFocus) {
      return
    }

    const timeoutId = setTimeout(() => {
      inputRef.current?.focus()
    }, 100)

    return () => {
      clearTimeout(timeoutId)
    }
  }, [shouldAutoFocus])

  function handleChangeText(text: string) {
    setInputValue(text)
    onChangeText(text)
  }

  function handleClear() {
    setInputValue("")
    onChangeText("")
  }

  return (
    <View className="flex-1">
      <View className="relative">
        <PressableFeedback
          onPress={onBack}
          className="absolute inset-y-0 left-2.5 z-10 justify-center p-1"
        >
          <LocalArrowLeftIcon
            fill="none"
            width={24}
            height={24}
            color={theme.foreground}
          />
        </PressableFeedback>
        <Input
          ref={inputRef}
          autoFocus={shouldAutoFocus}
          placeholder="Tracks, artists, albums..."
          placeholderTextColor={theme.muted}
          value={inputValue}
          onChangeText={handleChangeText}
          onSubmitEditing={onSubmit}
          variant="secondary"
          className="pr-9 pl-12"
          selectionColor={theme.accent}
          returnKeyType="search"
        />
        {inputValue.length > 0 && (
          <PressableFeedback
            onPress={handleClear}
            className="absolute inset-y-0 right-2.5 justify-center p-1"
          >
            <LocalCancelCircleSolidIcon
              fill="none"
              width={20}
              height={20}
              color={theme.muted}
            />
          </PressableFeedback>
        )}
      </View>
    </View>
  )
}

export default function SearchInteractionScreen() {
  const theme = useThemeColors()
  const navigation = useNavigation()
  const router = useRouter()
  const { query: initialQuery } = useLocalSearchParams<{ query?: string }>()

  const initialValue = initialQuery || ""
  const [searchQuery, setSearchQuery] = useState(initialValue)
  const [activeSearchTab, setActiveSearchTab] = useState<SearchTab>("All")
  const [headerInputKey, setHeaderInputKey] = useState(0)
  const searchQueryRef = useRef(searchQuery)

  const { data: searchResults, isLoading, isFetching } = useSearch(searchQuery)
  const { data: recentSearches = [] } = useRecentSearches()
  const tracks = searchResults?.tracks ?? []
  const artists = searchResults?.artists ?? []
  const albums = searchResults?.albums ?? []
  const playlists = searchResults?.playlists ?? []

  const addRecentSearchMutation = useMutation(
    {
      mutationFn: addRecentSearch,
      onSuccess: async (nextRecentSearches) => {
        queryClient.setQueryData(libraryKeys.recentSearches(), nextRecentSearches)
      },
    },
    queryClient
  )

  const deleteRecentSearchMutation = useMutation(
    {
      mutationFn: deleteRecentSearch,
      onSuccess: async (nextRecentSearches) => {
        queryClient.setQueryData(libraryKeys.recentSearches(), nextRecentSearches)
      },
    },
    queryClient
  )

  const clearRecentSearchesMutation = useMutation(
    {
      mutationFn: clearRecentSearches,
      onSuccess: async () => {
        queryClient.setQueryData(libraryKeys.recentSearches(), [])
      },
    },
    queryClient
  )

  const isSearching = searchQuery.trim().length > 0

  useEffect(() => {
    searchQueryRef.current = searchQuery
  }, [searchQuery])

  const handleBackNavigation = React.useCallback(() => {
    if (navigation.canGoBack()) {
      router.back()
      return true
    }

    router.replace("/(main)/(search)")
    return true
  }, [navigation, router])

  useEffect(() => {
    if (Platform.OS !== "android") {
      return
    }

    const subscription = BackHandler.addEventListener(
      "hardwareBackPress",
      handleBackNavigation
    )

    return () => {
      subscription.remove()
    }
  }, [handleBackNavigation])

  useEffect(() => {
    const unsubscribe = navigation.addListener("focus", () => {
      setHeaderInputKey((prev) => prev + 1)
    })

    return unsubscribe
  }, [navigation])

  useLayoutEffect(() => {
    navigation.setOptions({
      headerShown: true,
      title: "Search",
      headerTitleAlign: "left",
      headerTitle: () => (
        <HeaderSearchInput
          key={headerInputKey}
          theme={theme}
          initialValue={searchQueryRef.current}
          onChangeText={setSearchQuery}
          onSubmit={handleSubmitSearch}
          onBack={handleBackNavigation}
        />
      ),
      headerBackVisible: false,
      headerLeft: () => null,
      headerStyle: {
        backgroundColor: theme.background,
      },
      headerShadowVisible: false,
    })
  }, [
    navigation,
    theme,
    handleBackNavigation,
    headerInputKey,
    handleSubmitSearch,
  ])

  function pushRecentSearch(item: {
    query: string
    title?: string
    subtitle?: string
    type?: RecentSearchItem["type"]
    targetId?: string
    image?: string
  }) {
    if (!item.query.trim()) {
      return
    }

    void addRecentSearchMutation.mutateAsync(item)
  }

  function handleSubmitSearch() {
    const query = searchQuery.trim()
    if (!query) {
      return
    }

    pushRecentSearch({
      query,
      title: query,
      subtitle: "Search",
    })
  }

  function handleClearRecentSearches() {
    void clearRecentSearchesMutation.mutateAsync()
  }

  function handleRecentItemPress(item: RecentSearchItem) {
    if (item.type === "artist" && item.query.trim()) {
      pushRecentSearch(item)
      router.push({
        pathname: "artist/[name]",
        params: { name: item.query },
      })
      return
    }

    if (item.type === "album" && item.query.trim()) {
      pushRecentSearch(item)
      router.push({
        pathname: "album/[name]",
        params: { name: item.query },
      })
      return
    }

    if (item.type === "playlist" && item.targetId) {
      pushRecentSearch(item)
      router.push({
        pathname: "playlist/[id]",
        params: { id: item.targetId },
      })
      return
    }

    setSearchQuery(item.query || item.title)
    setHeaderInputKey((prev) => prev + 1)
    pushRecentSearch({
      query: item.query || item.title,
      title: item.title,
      subtitle: item.subtitle,
      type: item.type,
      targetId: item.targetId,
      image: item.image,
    })
  }

  function handleRemoveRecentItem(id: string) {
    void deleteRecentSearchMutation.mutateAsync(id)
  }

  function handleTrackPress(track: Track) {
    const title = track.title || "Unknown Track"
    pushRecentSearch({
      query: title,
      title,
      subtitle: track.artist || "Track",
      type: "track",
    })
  }

  function handleArtistPress(artist: SearchArtistResult) {
    pushRecentSearch({
      query: artist.name,
      title: artist.name,
      subtitle: "Artist",
      type: "artist",
      targetId: artist.id,
      image: artist.image,
    })

    router.push({
      pathname: "artist/[name]",
      params: { name: artist.name },
    })
  }

  function handleAlbumPress(album: SearchAlbumResult) {
    pushRecentSearch({
      query: album.title,
      title: album.title,
      subtitle: album.artist || "Album",
      type: "album",
      targetId: album.id,
      image: album.image,
    })

    router.push({
      pathname: "album/[name]",
      params: { name: album.title },
    })
  }

  function handlePlaylistPress(playlist: SearchPlaylistResult) {
    pushRecentSearch({
      query: playlist.title,
      title: playlist.title,
      subtitle: "Playlist",
      type: "playlist",
      targetId: playlist.id,
      image: playlist.image || playlist.images?.[0],
    })

    router.push({
      pathname: "playlist/[id]",
      params: { id: playlist.id },
    })
  }

  return (
    <View className="flex-1 bg-background">
      {isSearching ? (
        <SearchResults
          tracks={tracks}
          artists={artists}
          albums={albums}
          playlists={playlists}
          query={searchQuery}
          isLoading={isLoading || isFetching}
          activeTab={activeSearchTab}
          onActiveTabChange={setActiveSearchTab}
          onTrackPress={handleTrackPress}
          onArtistPress={handleArtistPress}
          onAlbumPress={handleAlbumPress}
          onPlaylistPress={handlePlaylistPress}
        />
      ) : (
        <ScrollView
          className="flex-1"
          contentContainerStyle={{ paddingBottom: 160 }}
          keyboardShouldPersistTaps="handled"
        >
          <RecentSearches
            searches={recentSearches}
            onClear={handleClearRecentSearches}
            onItemPress={handleRecentItemPress}
            onRemoveItem={handleRemoveRecentItem}
          />
        </ScrollView>
      )}
    </View>
  )
}
