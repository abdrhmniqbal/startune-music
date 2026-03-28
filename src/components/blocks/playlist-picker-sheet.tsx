import { useBottomSheetInternal } from "@gorhom/bottom-sheet"
import { LegendList, type LegendListRenderItemProps } from "@legendapp/list"
import {
  BottomSheet,
  Button,
  Checkbox,
  Input,
  PressableFeedback,
  TextField,
} from "heroui-native"
import { useCallback, useRef, useState } from "react"
import {
  type BlurEvent,
  findNodeHandle,
  type FocusEvent,
  Text,
  TextInput,
  View,
} from "react-native"

import LocalAddIcon from "@/components/icons/local/add"
import LocalCancelCircleSolidIcon from "@/components/icons/local/cancel-circle-solid"
import LocalSearchIcon from "@/components/icons/local/search"
import { PlaylistArtwork } from "@/components/patterns/playlist-artwork"
import {
  MediaItem as Item,
  MediaItemContent as ItemContent,
  MediaItemDescription as ItemDescription,
  MediaItemImage as ItemImage,
  MediaItemTitle as ItemTitle,
} from "@/components/ui/media-item"
import { EmptyState } from "@/components/ui/empty-state"
import { useThemeColors } from "@/hooks/use-theme-colors"
import { usePlaylistsForTrack } from "@/modules/playlist/playlist.queries"

interface PlaylistPickerSheetProps {
  isOpen: boolean
  onOpenChange: (open: boolean) => void
  trackId?: string | null
  isSelecting?: boolean
  onSelectPlaylist: (playlist: {
    id: string
    name: string
    hasTrack: boolean
  }) => void
  onCreatePlaylist: () => void
}

const SNAP_POINTS = ["62%", "88%"]

function PlaylistPickerSearchInput({
  inputKey,
  searchQuery,
  setSearchQuery,
}: {
  inputKey: number
  searchQuery: string
  setSearchQuery: (value: string) => void
}) {
  const theme = useThemeColors()
  const { animatedKeyboardState, textInputNodesRef } = useBottomSheetInternal()
  const inputRef = useRef<TextInput>(null)

  const handleOnFocus = useCallback(
    (e: FocusEvent) => {
      animatedKeyboardState.set((state) => ({
        ...state,
        target: e.nativeEvent.target,
      }))
    },
    [animatedKeyboardState]
  )

  const handleOnBlur = useCallback(
    (e: BlurEvent) => {
      const keyboardState = animatedKeyboardState.get()
      const currentFocusedInput = findNodeHandle(
        TextInput.State.currentlyFocusedInput() as TextInput | null
      )
      const shouldRemoveCurrentTarget =
        keyboardState.target === e.nativeEvent.target
      const shouldIgnoreBlurEvent =
        currentFocusedInput &&
        textInputNodesRef.current.has(currentFocusedInput)

      if (shouldRemoveCurrentTarget && !shouldIgnoreBlurEvent) {
        animatedKeyboardState.set((state) => ({
          ...state,
          target: undefined,
        }))
      }
    },
    [animatedKeyboardState, textInputNodesRef]
  )

  return (
    <TextField className="absolute top-0 right-0 left-0 px-5 pt-2">
      <View className="w-full flex-row items-center">
        <Input
          key={inputKey}
          ref={inputRef}
          variant="secondary"
          placeholder="Search playlists..."
          onChangeText={setSearchQuery}
          className="flex-1 pr-10 pl-10"
          autoCapitalize="none"
          autoCorrect={false}
          onFocus={handleOnFocus}
          onBlur={handleOnBlur}
        />
        <View className="absolute left-3.5" pointerEvents="none">
          <LocalSearchIcon
            fill="none"
            width={20}
            height={20}
            color={theme.muted}
          />
        </View>
        {searchQuery.length > 0 ? (
          <PressableFeedback
            className="absolute right-3 p-1"
            onPress={() => {
              inputRef.current?.clear()
              setSearchQuery("")
            }}
            hitSlop={12}
          >
            <LocalCancelCircleSolidIcon
              fill="none"
              width={18}
              height={18}
              color={theme.muted}
            />
          </PressableFeedback>
        ) : null}
      </View>
    </TextField>
  )
}

export function PlaylistPickerSheet({
  isOpen,
  onOpenChange,
  trackId,
  isSelecting = false,
  onSelectPlaylist,
  onCreatePlaylist,
}: PlaylistPickerSheetProps) {
  const theme = useThemeColors()
  const [searchInputKey, setSearchInputKey] = useState(0)
  const [searchQuery, setSearchQuery] = useState("")

  const { data: playlists = [] } = usePlaylistsForTrack(trackId ?? null, isOpen)

  const handleOpenChange = useCallback(
    (open: boolean) => {
      onOpenChange(open)
      if (open) {
        return
      }

      setSearchQuery("")
      setSearchInputKey((previous) => previous + 1)
    },
    [onOpenChange]
  )

  const normalizedQuery = searchQuery.trim().toLowerCase()
  const filteredPlaylists =
    normalizedQuery.length > 0
      ? playlists.filter((playlist) =>
          playlist.name.toLowerCase().includes(normalizedQuery)
        )
      : playlists

  return (
    <BottomSheet isOpen={isOpen} onOpenChange={handleOpenChange}>
      <BottomSheet.Portal>
        <BottomSheet.Overlay />
        <BottomSheet.Content
          index={1}
          snapPoints={SNAP_POINTS}
          enableOverDrag={false}
          enableDynamicSizing={false}
          contentContainerClassName="h-full pt-16 pb-2"
          keyboardBehavior="extend"
          backgroundClassName="bg-surface"
        >
          <PlaylistPickerSearchInput
            inputKey={searchInputKey}
            searchQuery={searchQuery}
            setSearchQuery={setSearchQuery}
          />

          <LegendList
            data={filteredPlaylists}
            getItemType={() => "playlist"}
            keyExtractor={(item) => item.id}
            style={{ flex: 1, minHeight: 1 }}
            contentContainerStyle={{
              gap: 8,
              paddingTop: 6,
              paddingHorizontal: 16,
              paddingBottom: 20,
            }}
            keyboardShouldPersistTaps="handled"
            showsVerticalScrollIndicator={false}
            recycleItems={true}
            initialContainerPoolRatio={3}
            estimatedItemSize={72}
            drawDistance={180}
            renderItem={({
              item,
            }: LegendListRenderItemProps<
              (typeof filteredPlaylists)[number]
            >) => {
              const hasTrack = Boolean(item.hasTrack)
              const handleSelect = () => {
                if (isSelecting) {
                  return
                }

                onSelectPlaylist({
                  id: item.id,
                  name: item.name,
                  hasTrack,
                })
              }

              return (
                <Item onPress={handleSelect}>
                  <View className="pr-2">
                    <Checkbox
                      variant="secondary"
                      isSelected={hasTrack}
                      isDisabled={isSelecting}
                      onSelectedChange={handleSelect}
                      accessibilityLabel={`Select playlist ${item.name}`}
                    />
                  </View>

                  <ItemImage className="items-center justify-center overflow-hidden bg-default">
                    <PlaylistArtwork
                      images={
                        item.images && item.images.length > 0
                          ? item.images
                          : item.image
                            ? [item.image]
                            : undefined
                      }
                    />
                  </ItemImage>

                  <ItemContent>
                    <ItemTitle>{item.name}</ItemTitle>
                    <ItemDescription>
                      {item.trackCount}{" "}
                      {item.trackCount === 1 ? "track" : "tracks"}
                    </ItemDescription>
                  </ItemContent>
                </Item>
              )
            }}
            ListEmptyComponent={() => (
              <View className="pt-6">
                <EmptyState
                  icon={
                    <LocalSearchIcon
                      fill="none"
                      width={40}
                      height={40}
                      color={theme.muted}
                    />
                  }
                  title="No playlists found"
                  message={
                    normalizedQuery.length > 0
                      ? "Try a different keyword."
                      : "Create your first playlist to organize tracks."
                  }
                  className="py-6"
                />
              </View>
            )}
          />

          <View className="border-t border-border/60 px-4 pt-3 pb-3">
            <Button
              variant="secondary"
              onPress={onCreatePlaylist}
              isDisabled={isSelecting}
            >
              <View className="flex-row items-center gap-2">
                <LocalAddIcon
                  fill="none"
                  width={18}
                  height={18}
                  color={theme.foreground}
                />
                <Text className="font-semibold text-foreground">
                  Create New Playlist
                </Text>
              </View>
            </Button>
          </View>
        </BottomSheet.Content>
      </BottomSheet.Portal>
    </BottomSheet>
  )
}
