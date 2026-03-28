import type { Track } from "@/modules/player/player.types"
import { useDebouncedValue } from "@tanstack/react-pacer"
import { useMutation, useQuery } from "@tanstack/react-query"

import { useEffect, useMemo, useState } from "react"
import { queryClient } from "@/lib/tanstack-query"
import { getAllTracks } from "@/modules/player/player.repository"
import { invalidatePlaylistQueries } from "@/modules/playlist/playlist.keys"
import { usePlaylist } from "@/modules/playlist/playlist.queries"
import { createPlaylist, updatePlaylist } from "@/modules/playlist/playlist.repository"
import {
  clampPlaylistDescription,
  clampPlaylistName,
  toggleTrackSelection,
} from "@/modules/playlist/playlist.utils"

const SEARCH_DEBOUNCE_MS = 140
const TRACK_PICKER_LIMIT = 20
const LIBRARY_TRACKS_QUERY_KEY = ["library", "tracks"] as const

interface PlaylistFormPayload {
  id?: string
  name: string
  description?: string
  trackIds: string[]
}

function reorderIds(ids: string[], from: number, to: number): string[] {
  if (
    from < 0 ||
    to < 0 ||
    from >= ids.length ||
    to >= ids.length ||
    from === to
  ) {
    return ids
  }

  const next = [...ids]
  const [moved] = next.splice(from, 1)
  if (!moved) {
    return ids
  }

  next.splice(to, 0, moved)
  return next
}

export function usePlaylistFormScreen(
  onSaved: () => void,
  playlistId?: string
) {
  const normalizedPlaylistId = playlistId?.trim() ?? ""
  const isEditMode = normalizedPlaylistId.length > 0
  const [name, setName] = useState("")
  const [description, setDescription] = useState("")
  const [selectedTrackIds, setSelectedTrackIds] = useState<string[]>([])
  const [draftSelectedTracks, setDraftSelectedTracks] = useState<Set<string>>(
    () => new Set()
  )
  const [isTrackSheetOpen, setIsTrackSheetOpen] = useState(false)
  const [searchInputKey, setSearchInputKey] = useState(0)
  const [searchQuery, setSearchQuery] = useState("")
  const [hasInitializedEditState, setHasInitializedEditState] = useState(false)

  const { data: playlistToEdit, isLoading: isEditPlaylistLoading } =
    usePlaylist(normalizedPlaylistId, isEditMode)

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
          playlistId: isEditMode ? normalizedPlaylistId : null,
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

  useEffect(() => {
    let isCancelled = false

    queueMicrotask(() => {
      if (isCancelled) {
        return
      }

      setName("")
      setDescription("")
      setSelectedTrackIds([])
      setDraftSelectedTracks(new Set())

      if (!isEditMode) {
        setHasInitializedEditState(true)
        return
      }

      setHasInitializedEditState(false)
    })

    return () => {
      isCancelled = true
    }
  }, [isEditMode, normalizedPlaylistId])

  useEffect(() => {
    if (!isEditMode || hasInitializedEditState || isEditPlaylistLoading) {
      return
    }

    let isCancelled = false

    queueMicrotask(() => {
      if (isCancelled) {
        return
      }

      if (!playlistToEdit) {
        setHasInitializedEditState(true)
        return
      }

      setName(clampPlaylistName(playlistToEdit.name))
      setDescription(clampPlaylistDescription(playlistToEdit.description || ""))
      setSelectedTrackIds(
        (playlistToEdit.tracks || []).map(
          (playlistTrack) => playlistTrack.trackId
        )
      )
      setDraftSelectedTracks(
        new Set(
          (playlistToEdit.tracks || []).map(
            (playlistTrack) => playlistTrack.trackId
          )
        )
      )
      setHasInitializedEditState(true)
    })

    return () => {
      isCancelled = true
    }
  }, [
    isEditMode,
    hasInitializedEditState,
    isEditPlaylistLoading,
    playlistToEdit,
  ])

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
    if (
      !name.trim() ||
      savePlaylistMutation.isPending ||
      (isEditMode && !playlistToEdit)
    ) {
      return
    }

    try {
      await savePlaylistMutation.mutateAsync({
        id: isEditMode ? normalizedPlaylistId : undefined,
        name,
        description: description.trim().length > 0 ? description : undefined,
        trackIds: selectedTrackIds,
      })
      onSaved()
    } catch {
      // Keep screen state intact when save fails.
    }
  }

  const normalizedQuery = debouncedSearchQuery.trim().toLowerCase()
  const filteredTracks = useMemo(() => {
    const sortedTracks = [...allTracks].sort((a, b) => {
      const aLastPlayed = a.lastPlayedAt ?? 0
      const bLastPlayed = b.lastPlayedAt ?? 0
      if (bLastPlayed !== aLastPlayed) {
        return bLastPlayed - aLastPlayed
      }

      return a.title.localeCompare(b.title, undefined, {
        sensitivity: "base",
      })
    })
    const tracksById = new Map(sortedTracks.map((track) => [track.id, track]))
    const persistedSelectedIds = selectedTrackIds.filter((id) =>
      draftSelectedTracks.has(id)
    )
    const persistedSelectedSet = new Set(persistedSelectedIds)
    const newlySelectedIds = sortedTracks
      .map((track) => track.id)
      .filter(
        (id) => draftSelectedTracks.has(id) && !persistedSelectedSet.has(id)
      )
    const selectedTopTracks = [...persistedSelectedIds, ...newlySelectedIds]
      .map((id) => tracksById.get(id))
      .filter((track): track is Track => Boolean(track))
    const maxVisibleCount = Math.max(
      TRACK_PICKER_LIMIT,
      selectedTopTracks.length
    )

    function mergeUniqueById(primary: Track[], secondary: Track[]): Track[] {
      const merged = [...primary]
      const seen = new Set(primary.map((track) => track.id))
      for (const track of secondary) {
        if (seen.has(track.id)) {
          continue
        }
        merged.push(track)
        seen.add(track.id)
      }
      return merged
    }

    if (normalizedQuery.length === 0) {
      const recentlyPlayedTracks = sortedTracks.filter(
        (track) => (track.lastPlayedAt ?? 0) > 0
      )
      const remainingTracks = sortedTracks.filter(
        (track) => (track.lastPlayedAt ?? 0) <= 0
      )
      const suggestedTracks =
        recentlyPlayedTracks.length >= TRACK_PICKER_LIMIT
          ? recentlyPlayedTracks.slice(0, TRACK_PICKER_LIMIT)
          : recentlyPlayedTracks.concat(
              remainingTracks.slice(
                0,
                TRACK_PICKER_LIMIT - recentlyPlayedTracks.length
              )
            )

      return mergeUniqueById(selectedTopTracks, suggestedTracks).slice(
        0,
        maxVisibleCount
      )
    }

    return sortedTracks
      .filter((track) => {
        const title = track.title.toLowerCase()
        const artist = (track.artist || "").toLowerCase()
        const album = (track.album || "").toLowerCase()
        return (
          title.includes(normalizedQuery) ||
          artist.includes(normalizedQuery) ||
          album.includes(normalizedQuery)
        )
      })
      .slice(0, TRACK_PICKER_LIMIT)
  }, [allTracks, draftSelectedTracks, normalizedQuery, selectedTrackIds])

  const selectedTracks = useMemo(
    () => new Set(selectedTrackIds),
    [selectedTrackIds]
  )

  const selectedTracksList = useMemo(() => {
    const tracksById = new Map(allTracks.map((track) => [track.id, track]))
    return selectedTrackIds
      .map((id) => tracksById.get(id))
      .filter((track): track is Track => Boolean(track))
  }, [allTracks, selectedTrackIds])

  function reorderSelectedTracks(from: number, to: number) {
    setSelectedTrackIds((prev) => reorderIds(prev, from, to))
  }

  return {
    name,
    description,
    isFormLoading:
      isEditMode && (isEditPlaylistLoading || !hasInitializedEditState),
    selectedTracks,
    draftSelectedTracks,
    isTrackSheetOpen,
    searchInputKey,
    searchQuery,
    filteredTracks,
    isEditMode,
    isSaving: savePlaylistMutation.isPending,
    canSave:
      name.trim().length > 0 &&
      !savePlaylistMutation.isPending &&
      (!isEditMode || Boolean(playlistToEdit)),
    selectedTracksList,
    setName: updateName,
    setDescription: updateDescription,
    setSearchQuery,
    toggleTrack: toggleSelectedTrack,
    reorderSelectedTracks,
    toggleDraftTrack,
    openTrackSheet,
    handleTrackSheetOpenChange,
    applyTrackSheetSelection,
    clearDraftTrackSelection,
    save,
  }
}
