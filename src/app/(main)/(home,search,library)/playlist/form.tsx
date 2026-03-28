import { Stack, useLocalSearchParams, useRouter } from "expo-router"
import { BottomSheet, Button } from "heroui-native"
import * as React from "react"
import { View } from "react-native"
import { useDebouncedValue } from "@tanstack/react-pacer"
import { useMutation, useQuery } from "@tanstack/react-query"

import { LibrarySkeleton } from "@/components/blocks/library-skeleton"
import { PlaylistForm } from "@/components/blocks/playlist-form/playlist-form"
import { TrackPickerSheetContent } from "@/components/blocks/playlist-form/track-picker-sheet-content"
import LocalTickIcon from "@/components/icons/local/tick"
import { useThemeColors } from "@/hooks/use-theme-colors"
import { queryClient } from "@/lib/tanstack-query"
import { logError } from "@/modules/logging/logger"
import { getAllTracks } from "@/modules/player/player.repository"
import type { Track } from "@/modules/player/player.types"
import { invalidatePlaylistQueries } from "@/modules/playlist/playlist.keys"
import {
  buildSelectedTracksList,
  buildTrackPickerResults,
  reorderTrackIds,
} from "@/modules/playlist/playlist-form"
import { usePlaylist } from "@/modules/playlist/playlist.queries"
import { createPlaylist, updatePlaylist } from "@/modules/playlist/playlist.repository"
import {
  clampPlaylistDescription,
  clampPlaylistName,
  toggleTrackSelection,
} from "@/modules/playlist/playlist.utils"

const SEARCH_DEBOUNCE_MS = 140
const LIBRARY_TRACKS_QUERY_KEY = ["library", "tracks"] as const

interface PlaylistFormPayload {
  id?: string
  name: string
  description?: string
  trackIds: string[]
}

interface PlaylistFormEditorProps {
  playlistId?: string
  initialName: string
  initialDescription: string
  initialSelectedTrackIds: string[]
  isEditMode: boolean
  onSaved: () => void
}

function PlaylistFormEditor({
  playlistId,
  initialName,
  initialDescription,
  initialSelectedTrackIds,
  isEditMode,
  onSaved,
}: PlaylistFormEditorProps) {
  const theme = useThemeColors()
  const [name, setName] = React.useState(() => clampPlaylistName(initialName))
  const [description, setDescription] = React.useState(() =>
    clampPlaylistDescription(initialDescription)
  )
  const [selectedTrackIds, setSelectedTrackIds] = React.useState<string[]>(
    () => initialSelectedTrackIds
  )
  const [draftSelectedTracks, setDraftSelectedTracks] = React.useState(
    () => new Set(initialSelectedTrackIds)
  )
  const [isTrackSheetOpen, setIsTrackSheetOpen] = React.useState(false)
  const [searchInputKey, setSearchInputKey] = React.useState(0)
  const [searchQuery, setSearchQuery] = React.useState("")

  const savePlaylistMutation = useMutation(
    {
      mutationFn: async (payload: PlaylistFormPayload) => {
        if (payload.id) {
          await updatePlaylist(
            payload.id,
            payload.name,
            payload.description,
            payload.trackIds
          )
          return
        }

        await createPlaylist(
          payload.name,
          payload.description,
          payload.trackIds
        )
      },
      onSuccess: async () => {
        await invalidatePlaylistQueries(queryClient, {
          playlistId: isEditMode ? playlistId ?? null : null,
        })
      },
    },
    queryClient
  )

  const { data: allTracks = [] } = useQuery<Track[]>(
    {
      queryKey: LIBRARY_TRACKS_QUERY_KEY,
      queryFn: getAllTracks,
      enabled: isTrackSheetOpen || isEditMode || selectedTrackIds.length > 0,
      staleTime: 5 * 60 * 1000,
      placeholderData: (previousData) => previousData,
    },
    queryClient
  )
  const [debouncedSearchQuery] = useDebouncedValue(searchQuery, {
    wait: SEARCH_DEBOUNCE_MS,
  })
  const normalizedQuery = debouncedSearchQuery.trim().toLowerCase()

  const filteredTracks = React.useMemo(
    () =>
      buildTrackPickerResults({
        allTracks,
        selectedTrackIds,
        draftSelectedTracks,
        normalizedQuery,
      }),
    [allTracks, draftSelectedTracks, normalizedQuery, selectedTrackIds]
  )

  const selectedTracksList = React.useMemo(
    () => buildSelectedTracksList(allTracks, selectedTrackIds),
    [allTracks, selectedTrackIds]
  )

  function updateName(value: string) {
    setName(clampPlaylistName(value))
  }

  function updateDescription(value: string) {
    setDescription(clampPlaylistDescription(value))
  }

  function toggleSelectedTrack(trackId: string) {
    setSelectedTrackIds((prev) => {
      if (prev.includes(trackId)) {
        return prev.filter((id) => id !== trackId)
      }

      return [...prev, trackId]
    })
  }

  function toggleDraftTrack(trackId: string) {
    setDraftSelectedTracks((prev) => toggleTrackSelection(prev, trackId))
  }

  function openTrackSheet() {
    setDraftSelectedTracks(new Set(selectedTrackIds))
    setIsTrackSheetOpen(true)
  }

  function handleTrackSheetOpenChange(open: boolean) {
    if (open) {
      setDraftSelectedTracks(new Set(selectedTrackIds))
      setIsTrackSheetOpen(true)
      return
    }

    setIsTrackSheetOpen(false)
    setSearchQuery("")
    setSearchInputKey((prev) => prev + 1)
    setDraftSelectedTracks(new Set(selectedTrackIds))
  }

  function applyTrackSheetSelection() {
    setSelectedTrackIds((prev) => {
      const draftIds = draftSelectedTracks
      const previousSet = new Set(prev)
      const preservedOrder = prev.filter((id) => draftIds.has(id))
      const appended = allTracks
        .map((track) => track.id)
        .filter((id) => draftIds.has(id) && !previousSet.has(id))

      return [...preservedOrder, ...appended]
    })
    setIsTrackSheetOpen(false)
    setSearchQuery("")
    setSearchInputKey((prev) => prev + 1)
  }

  function clearDraftTrackSelection() {
    setDraftSelectedTracks(new Set())
  }

  async function save() {
    if (!name.trim() || savePlaylistMutation.isPending) {
      return
    }

    try {
      await savePlaylistMutation.mutateAsync({
        id: isEditMode ? playlistId : undefined,
        name,
        description: description.trim().length > 0 ? description : undefined,
        trackIds: selectedTrackIds,
      })
      onSaved()
    } catch (error) {
      logError("Playlist form save failed", error, {
        playlistId: playlistId ?? null,
        isEditMode,
      })
    }
  }

  const isSaving = savePlaylistMutation.isPending
  const canSave = name.trim().length > 0 && !isSaving

  return (
    <>
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
        setName={updateName}
        setDescription={updateDescription}
        toggleTrack={toggleSelectedTrack}
        reorderSelectedTracks={(from, to) => {
          setSelectedTrackIds((prev) => reorderTrackIds(prev, from, to))
        }}
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
    </>
  )
}

export default function PlaylistFormScreen() {
  const router = useRouter()
  const { id } = useLocalSearchParams<{ id?: string }>()
  const playlistId = typeof id === "string" ? id : undefined
  const isEditMode = Boolean(playlistId?.trim())
  const { data: playlistToEdit, isLoading: isEditPlaylistLoading } =
    usePlaylist(playlistId?.trim() ?? "", isEditMode)

  if (isEditMode && isEditPlaylistLoading) {
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

  if (isEditMode && !playlistToEdit) {
    return (
      <View className="flex-1 bg-background">
        <Stack.Screen
          options={{
            title: "Edit Playlist",
          }}
        />
      </View>
    )
  }

  return (
    <View className="flex-1 bg-background">
      <PlaylistFormEditor
        key={playlistId ?? "create"}
        playlistId={playlistId}
        initialName={playlistToEdit?.name ?? ""}
        initialDescription={playlistToEdit?.description ?? ""}
        initialSelectedTrackIds={
          playlistToEdit?.tracks?.map((playlistTrack) => playlistTrack.trackId) ??
          []
        }
        isEditMode={isEditMode}
        onSaved={() => router.back()}
      />
    </View>
  )
}
