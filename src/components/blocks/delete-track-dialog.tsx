import type { Track } from "@/modules/player/player.store"
import * as MediaLibrary from "expo-media-library"
import { Button, Dialog, Toast, useToast } from "heroui-native"
import { useState } from "react"

import { View } from "react-native"
import { requestMediaLibraryPermission } from "@/core/storage/media-library.service"
import { startIndexing } from "@/modules/indexer/indexer.service"
import { logError, logInfo, logWarn } from "@/modules/logging/logging.service"
import { removeFromQueue } from "@/modules/player/queue.service"
import { hardDeleteTrack } from "@/modules/tracks/track-cleanup.repository"

interface DeleteTrackDialogProps {
  track: Track | null
  isOpen: boolean
  onOpenChange: (open: boolean) => void
  onDeleted?: (track: Track) => void
}

export function DeleteTrackDialog({
  track,
  isOpen,
  onOpenChange,
  onDeleted,
}: DeleteTrackDialogProps) {
  const { toast } = useToast()
  const [isDeleting, setIsDeleting] = useState(false)

  function showToast(title: string, description?: string) {
    toast.show({
      duration: 1800,
      component: (props) => (
        <Toast {...props} variant="accent" placement="bottom">
          <Toast.Title className="text-sm font-semibold">{title}</Toast.Title>
          {description ? (
            <Toast.Description className="text-xs text-muted">
              {description}
            </Toast.Description>
          ) : null}
        </Toast>
      ),
    })
  }

  async function handleConfirmDelete() {
    if (!track || isDeleting) {
      return
    }

    setIsDeleting(true)
    try {
      logInfo("Deleting track from device", {
        trackId: track.id,
        title: track.title,
      })
      const { status } = await requestMediaLibraryPermission()
      if (status !== "granted") {
        logWarn("Track deletion blocked by media permission", {
          trackId: track.id,
          permissionStatus: status,
        })
        showToast(
          "Media permission required",
          "Allow media access to delete tracks from your device."
        )
        return
      }

      const isDeleted = await MediaLibrary.deleteAssetsAsync([track.id])
      if (!isDeleted) {
        logWarn("MediaLibrary reported track deletion failure", {
          trackId: track.id,
        })
        showToast("Failed to delete track")
        return
      }

      try {
        await removeFromQueue(track.id)
      } catch (error) {
        logWarn("Queue cleanup failed after track deletion", {
          trackId: track.id,
          error: error instanceof Error ? error.message : String(error),
        })
      }

      try {
        await hardDeleteTrack(track.id)
      } catch (error) {
        logWarn("Database cleanup failed after track deletion", {
          trackId: track.id,
          error: error instanceof Error ? error.message : String(error),
        })
      }

      onOpenChange(false)
      onDeleted?.(track)
      logInfo("Deleted track from device", {
        trackId: track.id,
        title: track.title,
      })
      showToast("Deleted from device", track.title)
      void startIndexing(false, false)
    } catch (error) {
      logError("Failed to delete track from device", error, {
        trackId: track.id,
        title: track.title,
      })
      showToast("Failed to delete track")
    } finally {
      setIsDeleting(false)
    }
  }

  return (
    <Dialog isOpen={isOpen} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay />
        <Dialog.Content className="gap-4">
          <View className="gap-1.5">
            <Dialog.Title>Delete track from device?</Dialog.Title>
            <Dialog.Description>
              {`"${track?.title || "This track"}" will be permanently deleted from your device storage.`}
            </Dialog.Description>
          </View>
          <View className="flex-row justify-end gap-3">
            <Button
              variant="ghost"
              onPress={() => onOpenChange(false)}
              isDisabled={isDeleting}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              onPress={() => {
                void handleConfirmDelete()
              }}
              isDisabled={isDeleting}
            >
              Delete
            </Button>
          </View>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog>
  )
}
