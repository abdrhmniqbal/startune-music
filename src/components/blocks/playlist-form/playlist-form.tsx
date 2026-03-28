import type { PlaylistFormProps } from "./types"
import type { Track } from "@/modules/player/player.types"
import { Button, Input, PressableFeedback, TextArea } from "heroui-native"

import { Text, View } from "react-native"
import ReorderableList, {
  useReorderableDrag,
} from "react-native-reorderable-list"
import LocalAddIcon from "@/components/icons/local/add"
import LocalCancelIcon from "@/components/icons/local/cancel"
import LocalDragDropVerticalIcon from "@/components/icons/local/drag-drop-vertical"
import LocalMusicNoteSolidIcon from "@/components/icons/local/music-note-solid"
import { TrackRow } from "@/components/patterns/track-row"
import { EmptyState } from "@/components/ui/empty-state"
import { useThemeColors } from "@/hooks/use-theme-colors"

import {
  MAX_PLAYLIST_DESCRIPTION_LENGTH,
  MAX_PLAYLIST_NAME_LENGTH,
} from "@/modules/playlist/playlist.utils"

interface ReorderableSelectedTrackRowProps {
  track: Track
  onToggle: (trackId: string) => void
}

function ReorderableSelectedTrackRow({
  track,
  onToggle,
}: ReorderableSelectedTrackRowProps) {
  const drag = useReorderableDrag()
  const theme = useThemeColors()

  return (
    <TrackRow
      track={track}
      className="w-full py-2"
      leftAction={
        <PressableFeedback
          onPressIn={(event) => {
            event.stopPropagation()
            drag()
          }}
          className="p-2 opacity-60"
        >
          <LocalDragDropVerticalIcon
            fill="none"
            width={24}
            height={24}
            color={theme.foreground}
          />
        </PressableFeedback>
      }
      rightAction={
        <PressableFeedback
          onPress={(event) => {
            event.stopPropagation()
            onToggle(track.id)
          }}
          className="p-2 opacity-60"
        >
          <LocalCancelIcon
            fill="none"
            width={24}
            height={24}
            color={theme.muted}
          />
        </PressableFeedback>
      }
    />
  )
}

export function PlaylistForm({
  name,
  description,
  selectedTracksList,
  setName,
  setDescription,
  toggleTrack,
  reorderSelectedTracks,
  openTrackSheet,
}: PlaylistFormProps) {
  const theme = useThemeColors()
  const header = (
    <View className="gap-4 pb-3">
      <View className="gap-2">
        <View className="flex-row items-center justify-between">
          <Text className="text-sm font-medium text-foreground">
            Playlist Name
          </Text>
          <Text className="text-xs text-muted">
            {name.length}/{MAX_PLAYLIST_NAME_LENGTH}
          </Text>
        </View>
        <Input
          variant="secondary"
          placeholder="Playlist name"
          value={name}
          onChangeText={setName}
          maxLength={MAX_PLAYLIST_NAME_LENGTH}
        />
      </View>

      <View className="gap-2">
        <View className="flex-row items-center justify-between">
          <Text className="text-sm font-medium text-foreground">
            Description
          </Text>
          <Text className="text-xs text-muted">
            {description.length}/{MAX_PLAYLIST_DESCRIPTION_LENGTH}
          </Text>
        </View>
        <TextArea
          variant="secondary"
          placeholder="Add a description"
          value={description}
          onChangeText={setDescription}
          maxLength={MAX_PLAYLIST_DESCRIPTION_LENGTH}
          className="min-h-20"
        />
      </View>

      <View className="flex-row items-center justify-between">
        <Text className="text-lg font-bold text-foreground">
          {selectedTracksList.length} Tracks
        </Text>
        <Button variant="ghost" onPress={openTrackSheet}>
          <View className="flex-row items-center gap-2">
            <LocalAddIcon
              fill="none"
              width={18}
              height={18}
              color={theme.foreground}
            />
            <Text className="font-semibold text-foreground">Add Tracks</Text>
          </View>
        </Button>
      </View>
    </View>
  )

  return (
    <ReorderableList
      data={selectedTracksList}
      onReorder={({ from, to }) => reorderSelectedTracks(from, to)}
      renderItem={({ item }) => (
        <View className="mb-2">
          <ReorderableSelectedTrackRow track={item} onToggle={toggleTrack} />
        </View>
      )}
      keyExtractor={(item) => item.id}
      ListHeaderComponent={header}
      ListEmptyComponent={() => (
        <EmptyState
          icon={
            <LocalMusicNoteSolidIcon
              fill="none"
              width={48}
              height={48}
              color={theme.muted}
            />
          }
          title="No tracks selected"
          message="Tap Add Tracks to start building this playlist."
          className="py-8"
        />
      )}
      style={{ flex: 1, minHeight: 1 }}
      contentContainerStyle={{ padding: 16, paddingBottom: 200 }}
      showsVerticalScrollIndicator={false}
      keyboardShouldPersistTaps="handled"
      contentInsetAdjustmentBehavior="automatic"
    />
  )
}
