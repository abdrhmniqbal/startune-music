import type { Track } from "@/modules/player/player.store"
import * as MediaLibrary from "expo-media-library"
import { Button, Dialog, Toast, useToast } from "heroui-native"
import { useState } from "react"

import { View } from "react-native"
import { requestMediaLibraryPermission } from "@/core/storage/media-library.service"
import { startIndexing } from "@/modules/indexer/indexer.store"
import { removeFromQueue } from "@/modules/player/queue.store"
import { hardDeleteTrack } from "@/modules/tracks/track-cleanup.api"

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
      const { status } = await requestMediaLibraryPermission()
      if (status !== "granted") {
        showToast(
          "Media permission required",
          "Allow media access to delete tracks from your device."
        )
        return
      }

      const isDeleted = await MediaLibrary.deleteAssetsAsync([track.id])
      if (!isDeleted) {
        showToast("Failed to delete track")
        return
      }

      try {
        await removeFromQueue(track.id)
      } catch {
        // Queue cleanup failure should not block deletion flow.
      }

      try {
        await hardDeleteTrack(track.id)
      } catch {
        // DB cleanup failure should not block deletion success feedback.
      }

      onOpenChange(false)
      onDeleted?.(track)
      showToast("Deleted from device", track.title)
      void startIndexing(false, false)
    } catch {
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
