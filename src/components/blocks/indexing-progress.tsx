import {
  PressableFeedback,
  Toast,
  type ToastComponentProps,
  useToast,
} from "heroui-native"
import { useEffect, useRef } from "react"
import { Text, View } from "react-native"
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withTiming,
} from "react-native-reanimated"

import LocalCancelIcon from "@/components/icons/local/cancel"
import { useThemeColors } from "@/hooks/use-theme-colors"
import { stopIndexing } from "@/modules/indexer/indexer.store"
import { useIndexerStore } from "@/modules/indexer/indexer.store"

const TOAST_ID = "indexing-progress-toast"
const COMPLETE_HIDE_DELAY_MS = 1500

const PHASE_LABELS: Record<string, string> = {
  idle: "",
  scanning: "Scanning files...",
  processing: "Processing tracks...",
  cleanup: "Cleaning up...",
  complete: "Library Updated",
  paused: "Paused",
}

function IndexingProgressToast(props: ToastComponentProps) {
  const theme = useThemeColors()
  const state = useIndexerStore((store) => store.indexerState)

  const normalizedProgress = Math.min(Math.max(state.progress / 100, 0), 1)
  const animatedProgress = useSharedValue(normalizedProgress)

  useEffect(() => {
    animatedProgress.value = withTiming(normalizedProgress, {
      duration: 300,
    })
  }, [animatedProgress, normalizedProgress])

  const progressBarStyle = useAnimatedStyle(() => ({
    width: `${animatedProgress.value * 100}%`,
  }))

  if (state.phase === "complete") {
    return (
      <Toast {...props} variant="accent" placement="bottom" isSwipeable={false}>
        <View className="flex-row items-center gap-3 px-4 py-3">
          <View className="flex-1">
            <Toast.Title className="text-sm font-semibold">
              {PHASE_LABELS.complete}
            </Toast.Title>
            <Toast.Description className="text-xs text-muted">
              {state.totalFiles} tracks indexed
            </Toast.Description>
          </View>
        </View>
      </Toast>
    )
  }

  return (
    <Toast {...props} variant="accent" placement="bottom" isSwipeable={false}>
      <View className="gap-2 px-4 py-3">
        <View className="flex-row items-center justify-between">
          <View className="flex-row items-center gap-2">
            <Toast.Title className="text-sm font-semibold">
              {PHASE_LABELS[state.phase]}
            </Toast.Title>
          </View>

          <PressableFeedback
            onPress={stopIndexing}
            className="p-1 active:opacity-50"
            hitSlop={8}
          >
            <LocalCancelIcon
              fill="none"
              width={18}
              height={18}
              color={theme.muted}
            />
          </PressableFeedback>
        </View>

        <View className="h-1.5 w-full overflow-hidden rounded-full bg-muted/20">
          <Animated.View
            style={progressBarStyle}
            className="h-full rounded-full bg-accent"
          />
        </View>

        <View className="flex-row items-center justify-between">
          <Text
            className="flex-1 text-xs text-muted"
            numberOfLines={1}
            ellipsizeMode="tail"
          >
            {state.currentFile || "Preparing..."}
          </Text>
          <Text className="ml-2 text-xs text-muted">
            {state.processedFiles}/{state.totalFiles}
          </Text>
        </View>
      </View>
    </Toast>
  )
}

export function IndexingProgress() {
  const state = useIndexerStore((store) => store.indexerState)
  const { toast } = useToast()
  const isToastVisibleRef = useRef(false)
  const isCompleteDismissedRef = useRef(false)

  useEffect(() => {
    const shouldShowToast =
      state.showProgress && (state.isIndexing || state.phase === "complete")

    if (state.phase !== "complete") {
      isCompleteDismissedRef.current = false
    }

    if (
      shouldShowToast &&
      !isToastVisibleRef.current &&
      !(state.phase === "complete" && isCompleteDismissedRef.current)
    ) {
      toast.show({
        id: TOAST_ID,
        duration: "persistent",
        component: (props) => <IndexingProgressToast {...props} />,
      })
      isToastVisibleRef.current = true
      return
    }

    if (!shouldShowToast) {
      if (isToastVisibleRef.current) {
        toast.hide(TOAST_ID)
        isToastVisibleRef.current = false
      }
    }
  }, [state.isIndexing, state.phase, state.showProgress, toast])

  useEffect(() => {
    if (state.phase !== "complete") {
      return
    }

    const timeoutId = setTimeout(() => {
      toast.hide(TOAST_ID)
      isToastVisibleRef.current = false
      isCompleteDismissedRef.current = true
    }, COMPLETE_HIDE_DELAY_MS)

    return () => {
      clearTimeout(timeoutId)
    }
  }, [state.phase, toast])

  useEffect(() => {
    return () => {
      toast.hide(TOAST_ID)
      isToastVisibleRef.current = false
    }
  }, [toast])

  return null
}
