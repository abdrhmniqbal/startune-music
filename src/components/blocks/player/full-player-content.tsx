import type { PlayerExpandedView } from "@/modules/ui/ui.store"
import type { Track } from "@/modules/player/player.types"

import { LinearGradient } from "expo-linear-gradient"
import { StyleSheet, View } from "react-native"

import { usePlayerColorsStore } from "@/modules/player/player-colors.store"

import { AlbumArtView } from "./album-art-view"
import { LyricsView } from "./lyrics-view"
import { PlaybackControls } from "./playback-controls"
import { PlayerFooter } from "./player-footer"
import { PlayerHeader } from "./player-header"
import { ProgressBar } from "./progress-bar"
import { QueueView } from "./queue-view"
import { TrackInfo } from "./track-info"

const BACKGROUND_DARKEN_OVERLAY = "rgba(0, 0, 0, 0.15)"

interface FullPlayerContentProps {
  currentTrack: Track
  isPlaying: boolean
  playerExpandedView: PlayerExpandedView
  onClose: () => void
  onOpenMore: () => void
  onPressArtist: () => void
}

export function FullPlayerContent({
  currentTrack,
  isPlaying,
  playerExpandedView,
  onClose,
  onOpenMore,
  onPressArtist,
}: FullPlayerContentProps) {
  const colors = usePlayerColorsStore((state) => state.currentColors)
  const isCompactLayout = playerExpandedView !== "artwork"

  return (
    <View className="relative flex-1">
      <LinearGradient
        colors={[colors.bg, colors.secondary, "#09090B"]}
        locations={[0, 0.6, 1]}
        style={StyleSheet.absoluteFillObject}
      />
      <View
        pointerEvents="none"
        style={[
          StyleSheet.absoluteFillObject,
          { backgroundColor: BACKGROUND_DARKEN_OVERLAY },
        ]}
      />

      <View className="flex-1 justify-between px-6 pt-12 pb-8">
        <PlayerHeader onClose={onClose} onOpenMore={onOpenMore} />

        {playerExpandedView === "queue" ? (
          <QueueView currentTrack={currentTrack} />
        ) : playerExpandedView === "lyrics" ? (
          <LyricsView track={currentTrack} />
        ) : (
          <AlbumArtView currentTrack={currentTrack} />
        )}

        <TrackInfo
          track={currentTrack}
          compact={isCompactLayout}
          onPressArtist={onPressArtist}
        />

        <ProgressBar compact={isCompactLayout} />

        <PlaybackControls isPlaying={isPlaying} compact={isCompactLayout} />

        <PlayerFooter />
      </View>
    </View>
  )
}
