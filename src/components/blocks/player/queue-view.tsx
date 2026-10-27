import { PressableFeedback } from "heroui-native"
import * as React from "react"
import { useCallback } from "react"
import { useTranslation } from "react-i18next"
import { Text, View } from "react-native"
import Animated, { FadeIn, FadeOut, Layout } from "react-native-reanimated"
import { cn } from "tailwind-variants"

import LocalCancel01Icon from "@/components/icons/local/cancel-01"
import LocalDragDropVerticalIcon from "@/components/icons/local/drag-drop-vertical"
import { DragList, useDragStart } from "@/components/patterns/drag-list"
import { TrackRow } from "@/components/patterns/track-row"
import { EmptyState } from "@/components/ui/empty-state"
import { ScaleLoader } from "@/components/ui/scale-loader"
import { skipToQueueItem } from "@/playback/controls"
import { moveInQueue, removeFromQueue } from "@/playback/queue-actions"
import { useCurrentTrack, usePlayerQueueInfo, usePlayerTrackByKey } from "@/playback/selectors"
import { extractTrackId } from "@/playback/playback-store"

interface QueueItemProps {
  trackKey: string
  index: number
  isCurrent: boolean
  isPlayed: boolean
  onPress: (index: number) => void
  onRemove: (trackId: string) => void
}

export const QueueItem: React.FC<QueueItemProps> = ({
  trackKey,
  index,
  isCurrent,
  isPlayed,
  onPress,
  onRemove,
}) => {
  const trackId = extractTrackId(trackKey)
  const track = usePlayerTrackByKey(trackKey)

  const startDrag = useDragStart()
  const handleDragPress = useCallback(
    (event: { stopPropagation: () => void }) => {
      event.stopPropagation()
      startDrag(index)
    },
    [startDrag, index]
  )
  const handleRemovePress = useCallback(
    (event: { stopPropagation: () => void }) => {
      event.stopPropagation()
      onRemove(trackId)
    },
    [onRemove, trackId]
  )
  const handlePress = useCallback(() => {
    onPress(index)
  }, [onPress, index])

  if (!track) {
    return <View style={{ height: 64 }} className="justify-center px-4" />
  }

  return (
    <TrackRow
      track={track}
      onPress={handlePress}
      leftAction={
        <PressableFeedback onPressIn={handleDragPress} className="p-2 opacity-60">
          <LocalDragDropVerticalIcon fill="none" width={24} height={24} color="white" />
        </PressableFeedback>
      }
      className={cn(
        "rounded-xl px-2",
        isCurrent ? "bg-white/10" : "active:bg-white/5",
        isPlayed && "opacity-45"
      )}
      imageClassName="h-12 w-12 bg-white/10"
      imageOverlay={isCurrent ? <ScaleLoader size={16} /> : undefined}
      titleClassName={isCurrent ? "text-white" : "text-white/90"}
      descriptionClassName="text-white/50 text-sm"
      rightAction={
        <View className="flex-row items-center">
          {!isCurrent ? (
            <PressableFeedback onPress={handleRemovePress} className="p-2 opacity-60">
              <LocalCancel01Icon fill="none" width={24} height={24} color="white" />
            </PressableFeedback>
          ) : null}
        </View>
      }
    />
  )
}

const MemoizedQueueItem = React.memo(QueueItem)

const ITEM_GAP = 6

export const QueueView: React.FC = () => {
  const { t } = useTranslation()
  const currentTrack = useCurrentTrack()
  const { queue, upNext, currentIndex } = usePlayerQueueInfo()
  const handleRemove = useCallback((trackId: string) => {
    void removeFromQueue(trackId)
  }, [])
  const handleReorder = useCallback((from: number, to: number) => {
    if (from === to) {
      return
    }
    moveInQueue(from, to)
  }, [])
  const handlePlayFromQueue = useCallback((index: number) => {
    void skipToQueueItem(index)
  }, [])
  const renderItem = useCallback(
    ({ item, index }: { item: string; index: number }) => {
      return (
        <MemoizedQueueItem
          trackKey={item}
          index={index}
          isCurrent={index === currentIndex}
          isPlayed={index < currentIndex}
          onPress={handlePlayFromQueue}
          onRemove={handleRemove}
        />
      )
    },
    [handlePlayFromQueue, handleRemove, currentIndex]
  )
  if (!currentTrack || queue.length === 0) {
    return (
      <Animated.View
        entering={FadeIn.duration(200)}
        exiting={FadeOut.duration(200)}
        layout={Layout.duration(300)}
        className="-mx-2 my-3 flex-1 justify-center"
      >
        <EmptyState
          title={t("player.queue.emptyTitle")}
          message={t("player.queue.emptyMessage")}
          className="py-0"
        />
      </Animated.View>
    )
  }

  return (
    <Animated.View
      entering={FadeIn.duration(200)}
      exiting={FadeOut.duration(200)}
      layout={Layout.duration(300)}
      className="-mx-2 my-3 flex-1 overflow-hidden"
    >
      <View className="mb-2 flex-row items-center justify-between px-2">
        <Text className="text-sm text-white/60">
          {t("player.upNext")} • {t("library.count.track", { count: upNext.length })}
        </Text>
      </View>
      <View className="flex-1">
        <DragList
          data={queue}
          keyExtractor={(item) => item}
          initialScrollIndex={currentIndex >= 0 ? currentIndex : undefined}
          onReordered={handleReorder}
          renderItem={renderItem}
          // Row height is 76, not 68: the tallest child is the drag-handle
          // wrapper (icon 24 + p-2 16 + wrapper py-2 16 = 56), plus the row's
          // own py-2.5 (20). The drag drop math steps in slotSize units, so a
          // wrong pitch misplaces drops by an amount that grows with distance.
          estimatedItemSize={76 + ITEM_GAP}
          extraData={currentIndex}
          style={{ flex: 1, minHeight: 1 }}
          contentContainerStyle={{ gap: ITEM_GAP, paddingBottom: 20 }}
        />
      </View>
    </Animated.View>
  )
}
