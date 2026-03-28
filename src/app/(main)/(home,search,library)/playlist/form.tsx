import { Stack, useLocalSearchParams, useRouter } from "expo-router"
import { BottomSheet, Button } from "heroui-native"
import * as React from "react"
import { View } from "react-native"

import { LibrarySkeleton } from "@/components/blocks/library-skeleton"
import { PlaylistForm } from "@/components/blocks/playlist-form/playlist-form"
import { TrackPickerSheetContent } from "@/components/blocks/playlist-form/track-picker-sheet-content"
import LocalTickIcon from "@/components/icons/local/tick"
import { useThemeColors } from "@/hooks/use-theme-colors"
import { usePlaylistFormScreen } from "@/modules/playlist/hooks/use-playlist-form"

export default function PlaylistFormScreen() {
  const router = useRouter()
  const { id } = useLocalSearchParams<{ id?: string }>()
  const theme = useThemeColors()
  const playlistId = typeof id === "string" ? id : undefined

  const {
    name,
    description,
    isFormLoading,
    draftSelectedTracks,
    isTrackSheetOpen,
    searchInputKey,
    searchQuery,
    filteredTracks,
    isEditMode,
    isSaving,
    canSave,
    selectedTracksList,
    setName,
    setDescription,
    setSearchQuery,
    toggleTrack,
    reorderSelectedTracks,
    toggleDraftTrack,
    openTrackSheet,
    handleTrackSheetOpenChange,
    applyTrackSheetSelection,
    clearDraftTrackSelection,
    save,
  } = usePlaylistFormScreen(() => router.back(), playlistId)

  if (isFormLoading) {
    return (
      <View className="flex-1 bg-background pt-4">
        <Stack.Screen
          options={{
            title: "Edit Playlist",
          }}
        />
        <LibrarySkeleton type="playlist-form" />
      </View>
    )
  }

  return (
    <View className="flex-1 bg-background">
      <Stack.Screen
        options={{
          title: isEditMode ? "Edit Playlist" : "Create Playlist",
          headerRight: () => (
            <Button
              onPress={save}
              variant="ghost"
              className="-mr-2"
              isIconOnly
              isDisabled={!canSave || isSaving}
            >
              <LocalTickIcon
                fill="none"
                width={24}
                height={24}
                color={canSave || isSaving ? theme.accent : theme.muted}
              />
            </Button>
          ),
        }}
      />

      <PlaylistForm
        name={name}
        description={description}
        selectedTracksList={selectedTracksList}
        setName={setName}
        setDescription={setDescription}
        toggleTrack={toggleTrack}
        reorderSelectedTracks={reorderSelectedTracks}
        openTrackSheet={openTrackSheet}
      />

      <BottomSheet
        isOpen={isTrackSheetOpen}
        onOpenChange={handleTrackSheetOpenChange}
      >
        <BottomSheet.Portal>
          <BottomSheet.Overlay />
          <TrackPickerSheetContent
            inputKey={searchInputKey}
            searchQuery={searchQuery}
            setSearchQuery={setSearchQuery}
            filteredTracks={filteredTracks}
            selectedTracks={draftSelectedTracks}
            onToggleTrack={toggleDraftTrack}
            onApply={applyTrackSheetSelection}
            onClearSelection={clearDraftTrackSelection}
          />
        </BottomSheet.Portal>
      </BottomSheet>
    </View>
  )
}
