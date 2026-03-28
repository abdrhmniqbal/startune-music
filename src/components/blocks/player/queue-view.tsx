import { PressableFeedback } from "heroui-native"
import * as React from "react"
import { useEffect, useRef } from "react"
import { type FlatList, Text, View } from "react-native"
import Animated, { FadeIn, FadeOut, Layout } from "react-native-reanimated"
import ReorderableList, {
  useReorderableDrag,
} from "react-native-reorderable-list"
import { cn } from "tailwind-variants"

import LocalCancelIcon from "@/components/icons/local/cancel"
import LocalDragDropVerticalIcon from "@/components/icons/local/drag-drop-vertical"
import { TrackRow } from "@/components/patterns/track-row"
import { ScaleLoader } from "@/components/ui/scale-loader"
import { playTrack, type Track } from "@/modules/player/player.store"
import {
  getQueueState,
  moveInQueue,
  removeFromQueue,
  useQueueInfo,
} from "@/modules/player/queue.store"

interface QueueItemProps {
  track: Track
  isCurrentTrack: boolean
  isPlayedTrack: boolean
  onPress: () => void
  onRemove: () => void
}

export const QueueItem: React.FC<QueueItemProps> = ({
  track,
  isCurrentTrack,
  isPlayedTrack,
  onPress,
  onRemove,
}) => {
  const drag = useReorderableDrag()

  return (
    <TrackRow
      track={track}
      onPress={onPress}
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
            color="white"
          />
        </PressableFeedback>
      }
      className={cn(
        "rounded-xl px-2",
        isCurrentTrack ? "bg-white/10" : "active:bg-white/5",
        isPlayedTrack && "opacity-45"
      )}
      imageClassName="h-12 w-12 bg-white/10"
      imageOverlay={isCurrentTrack ? <ScaleLoader size={16} /> : undefined}
      titleClassName={isCurrentTrack ? "text-white" : "text-white/90"}
      descriptionClassName="text-white/50 text-sm"
      rightAction={
        <View className="flex-row items-center">
          {!isCurrentTrack ? (
            <PressableFeedback
              onPress={(event) => {
                event.stopPropagation()
                onRemove()
              }}
              className="p-2 opacity-60"
            >
              <LocalCancelIcon
                fill="none"
                width={24}
                height={24}
                color="white"
              />
            </PressableFeedback>
          ) : null}
        </View>
      }
    />
  )
}

interface QueueViewProps {
  currentTrack: Track | null
}

const ITEM_HEIGHT = 64
const ITEM_GAP = 6

export const QueueView: React.FC<QueueViewProps> = ({ currentTrack }) => {
  const queueInfo = useQueueInfo()
  const { queue, upNext, currentIndex } = queueInfo
  const listRef = useRef<FlatList>(null)

  useEffect(() => {
    if (currentIndex >= 0 && queue.length > 0) {
      const timer = setTimeout(() => {
        listRef.current?.scrollToIndex({
          index: currentIndex,
          animated: false,
          viewPosition: 0,
        })
      }, 50)
      return () => clearTimeout(timer)
    }
  }, [currentIndex, queue.length])

  if (!currentTrack || queue.length === 0) return null

  const handleRemove = async (trackId: string) => {
    await removeFromQueue(trackId)
  }

  const handleReorder = ({ from, to }: { from: number; to: number }) => {
    if (from === to) {
      return
    }
    void moveInQueue(from, to)
  }

  const handlePlayFromQueue = (track: Track) => {
    void playTrack(track, getQueueState())
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
          Up Next • {upNext.length} {upNext.length === 1 ? "track" : "tracks"}
        </Text>
      </View>
      <View className="flex-1">
        <ReorderableList
          ref={listRef}
          data={queue}
          keyExtractor={(item) => item.id}
          onReorder={handleReorder}
          renderItem={({ item, index }) => (
            <QueueItem
              track={item}
              isCurrentTrack={item.id === currentTrack.id}
              isPlayedTrack={currentIndex >= 0 && index < currentIndex}
              onPress={() => handlePlayFromQueue(item)}
              onRemove={() => handleRemove(item.id)}
            />
          )}
          getItemLayout={(_, index) => ({
            length: ITEM_HEIGHT,
            offset: (ITEM_HEIGHT + ITEM_GAP) * index,
            index,
          })}
          style={{ flex: 1, minHeight: 1 }}
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{ gap: ITEM_GAP, paddingBottom: 20 }}
        />
      </View>
    </Animated.View>
  )
}
