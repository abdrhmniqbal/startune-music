import { useLocalSearchParams, useNavigation, useRouter } from "expo-router"
import { Input, PressableFeedback } from "heroui-native"
import * as React from "react"
import { useEffect, useLayoutEffect, useRef, useState } from "react"
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
  SearchResults,
  type SearchTab,
} from "@/components/blocks/search-results"
import LocalArrowLeftIcon from "@/components/icons/local/arrow-left"
import LocalCancelCircleSolidIcon from "@/components/icons/local/cancel-circle-solid"
import { useThemeColors } from "@/modules/ui/theme"
import { useSearch } from "@/modules/library/library.queries"

interface HeaderSearchInputProps {
  theme: ReturnType<typeof useThemeColors>
  initialValue: string
  onChangeText: (text: string) => void
  onBack: () => void
}

function HeaderSearchInput({
  theme,
  initialValue,
  onChangeText,
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
  const [recentSearches, setRecentSearches] = useState<RecentSearchItem[]>([])
  const searchQueryRef = useRef(searchQuery)

  const { data: searchResults, isLoading, isFetching } = useSearch(searchQuery)
  const tracks = searchResults?.tracks ?? []
  const artists = searchResults?.artists ?? []
  const albums = searchResults?.albums ?? []
  const playlists = searchResults?.playlists ?? []

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
  }, [navigation, theme, handleBackNavigation, headerInputKey])

  function handleClearRecentSearches() {
    setRecentSearches([])
  }

  function handleRecentItemPress(item: RecentSearchItem) {
    setSearchQuery(item.title)
    setHeaderInputKey((prev) => prev + 1)
  }

  function handleRemoveRecentItem(id: string) {
    setRecentSearches((prev) => prev.filter((item) => item.id !== id))
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
          onArtistPress={(artist) =>
            router.push({
              pathname: "/artist/[name]",
              params: { name: artist.name },
            })
          }
          onAlbumPress={(album) =>
            router.push({
              pathname: "/album/[name]",
              params: { name: album.title },
            })
          }
          onPlaylistPress={(playlist) =>
            router.push({
              pathname: "/playlist/[id]",
              params: { id: playlist.id },
            })
          }
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
