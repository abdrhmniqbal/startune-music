import { useRouter } from "expo-router"
import { BottomSheet } from "heroui-native"
import { useState } from "react"

import { closePlayer, useUIStore } from "@/modules/ui/ui.store"
import { usePlayerStore } from "@/modules/player/player.store"

import { FullPlayerContent } from "./full-player-content"
import { PlayerActionSheet } from "./player-action-sheet"

const FULL_PLAYER_SNAP_POINTS = ["100%"]

export function PlayerSheet() {
  const router = useRouter()
  const isExpanded = useUIStore((state) => state.isPlayerExpanded)
  const playerExpandedView = useUIStore((state) => state.playerExpandedView)
  const currentTrack = usePlayerStore((state) => state.currentTrack)
  const isPlaying = usePlayerStore((state) => state.isPlaying)
  const [isActionSheetOpen, setIsActionSheetOpen] = useState(false)

  const dismissPlayer = () => {
    closePlayer()
    setIsActionSheetOpen(false)
  }

  const handleNavigateAway = () => {
    closePlayer()
    setIsActionSheetOpen(false)
  }

  const handleArtistPress = () => {
    const artistName = currentTrack?.artist?.trim()
    if (!artistName) {
      return
    }

    handleNavigateAway()
    router.push({
      pathname: "/artist/[name]",
      params: { name: artistName },
    })
  }

  if (!currentTrack) {
    return null
  }

  return (
    <>
      <BottomSheet
        isOpen={isExpanded}
        onOpenChange={(open) => {
          if (!open) {
            dismissPlayer()
          }
        }}
      >
        <BottomSheet.Portal disableFullWindowOverlay>
          <BottomSheet.Overlay />
          <BottomSheet.Content
            index={0}
            snapPoints={FULL_PLAYER_SNAP_POINTS}
            enableDynamicSizing={false}
            enableContentPanningGesture={playerExpandedView !== "lyrics"}
            topInset={0}
            bottomInset={0}
            backgroundClassName="bg-transparent"
            backgroundStyle={{ borderRadius: 0 }}
            contentContainerClassName="h-full p-0"
            handleComponent={() => null}
            handleHeight={0}
          >
            <FullPlayerContent
              currentTrack={currentTrack}
              isPlaying={isPlaying}
              playerExpandedView={playerExpandedView}
              onClose={dismissPlayer}
              onOpenMore={() => setIsActionSheetOpen(true)}
              onPressArtist={handleArtistPress}
            />
          </BottomSheet.Content>
        </BottomSheet.Portal>
      </BottomSheet>

      <PlayerActionSheet
        visible={isActionSheetOpen}
        onOpenChange={setIsActionSheetOpen}
        track={currentTrack}
        onNavigate={handleNavigateAway}
      />
    </>
  )
}
