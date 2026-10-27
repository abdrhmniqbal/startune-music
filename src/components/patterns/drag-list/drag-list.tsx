import type { LegendListProps, LegendListRenderItemProps } from "@legendapp/list/react-native"
import { AnimatedLegendList } from "@legendapp/list/reanimated"
import React, { memo, useCallback, useEffect, useMemo, useRef, useState } from "react"
import { View } from "react-native"
import { Gesture, GestureDetector } from "react-native-gesture-handler"
import Animated, {
  runOnJS,
  scrollTo,
  useAnimatedRef,
  useAnimatedScrollHandler,
  useAnimatedStyle,
  useSharedValue,
  withTiming,
  type AnimatedRef,
  type SharedValue,
} from "react-native-reanimated"

const DRAG_PRESS_MS = 140
const EDGE_ZONE = 24
const MIN_DRAG_DISPLACEMENT = 30
const SCROLL_STEP = 5
const SHIFT_DURATION_MS = 80

const clampW = (v: number, lo: number, hi: number) => {
  "worklet"
  return v < lo ? lo : v > hi ? hi : v
}

interface DragListContextValue {
  activeIndex: number | null
}

const DragListContext = React.createContext<DragListContextValue>({
  activeIndex: null,
})

export function useIsDraggingItem(index: number): boolean {
  const { activeIndex } = React.useContext(DragListContext)
  return activeIndex === index
}

export function useDragStart(): (index: number) => void {
  // SAFETY: No-op fallback; row long-press activates drag directly across platforms.
  return useCallback((_index: number) => {}, [])
}

type ListPassThroughProps<T> = Pick<
  LegendListProps<T>,
  | "style"
  | "contentContainerStyle"
  | "ListHeaderComponent"
  | "ListEmptyComponent"
  | "ListFooterComponent"
  | "keyboardShouldPersistTaps"
  | "initialScrollIndex"
  | "scrollEnabled"
  | "showsVerticalScrollIndicator"
  | "extraData"
>

export interface DragListProps<T> extends ListPassThroughProps<T> {
  data: T[]
  keyExtractor: (item: T, index: number) => string
  renderItem: (info: { item: T; index: number }) => React.ReactNode
  estimatedItemSize: number
  onReordered: (from: number, to: number) => void
  onDragBegin?: () => void
  onDragEnd?: () => void
}

interface DraggableItemProps<T> {
  item: T
  index: number
  slotSize: number
  dataLengthSV: SharedValue<number>
  activeIndexSV: SharedValue<number>
  dragYSV: SharedValue<number>
  shiftedSV: SharedValue<number>
  startScrollYSV: SharedValue<number>
  scrollY: SharedValue<number>
  listTopSV: SharedValue<number>
  listHeightSV: SharedValue<number>
  scrollRef: AnimatedRef<Animated.ScrollView>
  setScrollEnabled: (enabled: boolean) => void
  setActiveIndex: (index: number | null) => void
  onDragBegin?: () => void
  onDragEnd?: () => void
  onReordered: (from: number, to: number) => void
  renderItem: (info: { item: T; index: number }) => React.ReactNode
}

function DraggableItemImpl<T>({
  item,
  index,
  slotSize,
  dataLengthSV,
  activeIndexSV,
  dragYSV,
  shiftedSV,
  startScrollYSV,
  scrollY,
  listTopSV,
  listHeightSV,
  scrollRef,
  setScrollEnabled,
  setActiveIndex,
  onDragBegin,
  onDragEnd,
  onReordered,
  renderItem,
}: DraggableItemProps<T>) {
  const pan = useMemo(() => {
    return Gesture.Pan()
      .activateAfterLongPress(DRAG_PRESS_MS)
      .onBegin(() => {
        activeIndexSV.value = index
        dragYSV.value = 0
        shiftedSV.value = 0
        startScrollYSV.value = scrollY.value

        runOnJS(setScrollEnabled)(false)
        runOnJS(setActiveIndex)(index)
        if (onDragBegin) {
          runOnJS(onDragBegin)()
        }
      })
      .onUpdate((e) => {
        const deltaScroll = scrollY.value - startScrollYSV.value
        const currentContentY = index * slotSize + e.translationY + deltaScroll
        dragYSV.value = currentContentY - index * slotSize

        // Auto-scroll ONLY when user has deliberately moved finger and reached list boundary
        if (listHeightSV.value > 100) {
          const listTop = listTopSV.value
          const listBottom = listTop + listHeightSV.value
          const fingerY = e.absoluteY

          let nextScroll = scrollY.value
          if (
            e.translationY < -MIN_DRAG_DISPLACEMENT &&
            fingerY < listTop + EDGE_ZONE &&
            scrollY.value > 0
          ) {
            nextScroll = Math.max(0, scrollY.value - SCROLL_STEP)
          } else if (e.translationY > MIN_DRAG_DISPLACEMENT && fingerY > listBottom - EDGE_ZONE) {
            nextScroll = scrollY.value + SCROLL_STEP
          }

          if (nextScroll !== scrollY.value) {
            scrollTo(scrollRef, 0, nextScroll, false)
            scrollY.value = nextScroll
          }
        }

        const targetIndex = clampW(
          Math.round(currentContentY / slotSize),
          0,
          dataLengthSV.value - 1
        )
        shiftedSV.value = targetIndex - index
      })
      .onFinalize(() => {
        const fromIndex = activeIndexSV.value
        const toIndex = activeIndexSV.value + shiftedSV.value

        // Immediate drop with zero delay
        activeIndexSV.value = -1
        dragYSV.value = 0
        shiftedSV.value = 0

        runOnJS(setScrollEnabled)(true)
        runOnJS(setActiveIndex)(null)
        if (onDragEnd) {
          runOnJS(onDragEnd)()
        }
        if (fromIndex !== -1 && fromIndex !== toIndex) {
          runOnJS(onReordered)(fromIndex, toIndex)
        }
      })
  }, [
    activeIndexSV,
    dataLengthSV,
    dragYSV,
    index,
    listHeightSV,
    listTopSV,
    onDragBegin,
    onDragEnd,
    onReordered,
    scrollRef,
    scrollY,
    setActiveIndex,
    setScrollEnabled,
    shiftedSV,
    slotSize,
    startScrollYSV,
  ])

  const animatedStyle = useAnimatedStyle(() => {
    const activeIdx = activeIndexSV.value

    if (activeIdx === -1) {
      return {
        transform: [{ translateY: 0 }],
        zIndex: 0,
        opacity: 1,
      }
    }

    if (activeIdx === index) {
      return {
        transform: [{ translateY: dragYSV.value }],
        zIndex: 999,
        opacity: 0.96,
      }
    }

    const shift = shiftedSV.value
    let targetOffset = 0
    if (shift > 0 && index > activeIdx && index <= activeIdx + shift) {
      targetOffset = -slotSize
    } else if (shift < 0 && index < activeIdx && index >= activeIdx + shift) {
      targetOffset = slotSize
    }

    return {
      transform: [
        {
          translateY: withTiming(targetOffset, {
            duration: SHIFT_DURATION_MS,
          }),
        },
      ],
      zIndex: 0,
      opacity: 1,
    }
  })

  return (
    <GestureDetector gesture={pan}>
      <Animated.View style={animatedStyle}>{renderItem({ item, index })}</Animated.View>
    </GestureDetector>
  )
}

// SAFETY: React.memo loses generic component type parameters; cast preserves generic signature.
const DraggableItem = memo(DraggableItemImpl) as typeof DraggableItemImpl

export function DragList<T>({
  data,
  keyExtractor,
  renderItem,
  estimatedItemSize,
  onReordered,
  onDragBegin,
  onDragEnd,
  scrollEnabled = true,
  ...passThrough
}: DragListProps<T>) {
  const containerRef = useRef<View>(null)
  const [scrollEnabledState, setScrollEnabled] = useState(true)
  const [activeIndex, setActiveIndex] = useState<number | null>(null)

  const activeIndexSV = useSharedValue(-1)
  const dragYSV = useSharedValue(0)
  const shiftedSV = useSharedValue(0)
  const startScrollYSV = useSharedValue(0)
  const scrollY = useSharedValue(0)
  const listTopSV = useSharedValue(0)
  const listHeightSV = useSharedValue(0)
  const dataLengthSV = useSharedValue(data.length)
  const scrollRef = useAnimatedRef<Animated.ScrollView>()

  useEffect(() => {
    dataLengthSV.value = data.length
    activeIndexSV.value = -1
    dragYSV.value = 0
    shiftedSV.value = 0
  }, [data, activeIndexSV, dataLengthSV, dragYSV, shiftedSV])

  const measureContainer = useCallback(() => {
    containerRef.current?.measureInWindow((_x, y, _w, height) => {
      listTopSV.value = y
      listHeightSV.value = height
    })
  }, [listHeightSV, listTopSV])

  const scrollHandler = useAnimatedScrollHandler({
    onScroll: (e) => {
      scrollY.value = e.contentOffset.y
    },
  })

  const contextValue = useMemo(() => ({ activeIndex }), [activeIndex])

  const renderLegendItem = useCallback(
    ({ item, index }: LegendListRenderItemProps<T>) => (
      <DraggableItem
        item={item}
        index={index}
        slotSize={estimatedItemSize}
        dataLengthSV={dataLengthSV}
        activeIndexSV={activeIndexSV}
        dragYSV={dragYSV}
        shiftedSV={shiftedSV}
        startScrollYSV={startScrollYSV}
        scrollY={scrollY}
        listTopSV={listTopSV}
        listHeightSV={listHeightSV}
        scrollRef={scrollRef}
        setScrollEnabled={setScrollEnabled}
        setActiveIndex={setActiveIndex}
        onDragBegin={onDragBegin}
        onDragEnd={onDragEnd}
        onReordered={onReordered}
        renderItem={renderItem}
      />
    ),
    [
      activeIndexSV,
      dataLengthSV,
      dragYSV,
      estimatedItemSize,
      listHeightSV,
      listTopSV,
      onDragBegin,
      onDragEnd,
      onReordered,
      renderItem,
      scrollRef,
      scrollY,
      shiftedSV,
      startScrollYSV,
    ]
  )

  const minListHeight = scrollEnabled ? undefined : data.length * estimatedItemSize

  return (
    <DragListContext.Provider value={contextValue}>
      <View
        ref={containerRef}
        style={[scrollEnabled ? { flex: 1 } : { minHeight: minListHeight }, passThrough.style]}
        onLayout={measureContainer}
      >
        <AnimatedLegendList
          {...passThrough}
          // SAFETY: refScrollView expects the Reanimated scroll-view ref from this module;
          // pnpm's duplicated reanimated typings collapse its public type to Ref<never>.
          refScrollView={scrollRef as never}
          data={data}
          keyExtractor={keyExtractor}
          renderItem={renderLegendItem}
          estimatedItemSize={estimatedItemSize}
          extraData={passThrough.extraData ?? renderItem}
          // SAFETY: AnimatedLegendList onScroll expects worklet scroll handler from Reanimated
          onScroll={scrollHandler as never}
          scrollEnabled={scrollEnabled && scrollEnabledState}
          recycleItems
          drawDistance={400}
          maintainVisibleContentPosition={false}
          overScrollMode="never"
          showsHorizontalScrollIndicator={false}
          bounces={false}
        />
      </View>
    </DragListContext.Provider>
  )
}
