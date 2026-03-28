import * as React from "react"
import { useEffect } from "react"
import { Text, TextInput, View } from "react-native"
import { Gesture, GestureDetector } from "react-native-gesture-handler"
import Animated, {
  Layout,
  runOnJS,
  useAnimatedProps,
  useAnimatedStyle,
  useSharedValue,
  withTiming,
} from "react-native-reanimated"

import { seekTo, usePlayerStore } from "@/modules/player/player.store"

const AnimatedTextInput = Animated.createAnimatedComponent(TextInput)

interface ProgressBarProps {
  compact?: boolean
}

export const ProgressBar: React.FC<ProgressBarProps> = ({
  compact = false,
}) => {
  const currentTime = usePlayerStore((state) => state.currentTime)
  const duration = usePlayerStore((state) => state.duration)
  const progress = useSharedValue(0)
  const isSeeking = useSharedValue(false)
  const barWidth = useSharedValue(0)
  const pressed = useSharedValue(false)
  const durationSv = useSharedValue(0)

  useEffect(() => {
    durationSv.value = duration
  }, [duration, durationSv])

  useEffect(() => {
    if (duration > 0) {
      progress.value = currentTime / duration
    }
  }, [currentTime, duration, progress])

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins}:${secs < 10 ? "0" : ""}${secs}`
  }

  const animatedTextProps = useAnimatedProps(() => {
    const seconds = progress.value * durationSv.value
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    const text = `${mins}:${secs < 10 ? "0" : ""}${secs}`
    return {
      text,
    } as any
  })

  const seekGesture = Gesture.Pan()
    .onStart((e) => {
      isSeeking.value = true
      pressed.value = true
      if (barWidth.value > 0) {
        progress.value = Math.max(0, Math.min(1, e.x / barWidth.value))
      }
    })
    .onUpdate((e) => {
      if (barWidth.value > 0) {
        progress.value = Math.max(0, Math.min(1, e.x / barWidth.value))
      }
    })
    .onEnd(() => {
      const seekTime = progress.value * duration
      runOnJS(seekTo)(seekTime)
      isSeeking.value = false
      pressed.value = false
    })

  const tapGesture = Gesture.Tap()
    .onStart((e) => {
      isSeeking.value = true
      pressed.value = true
      if (barWidth.value > 0) {
        progress.value = Math.max(0, Math.min(1, e.x / barWidth.value))
      }
    })
    .onEnd(() => {
      const seekTime = progress.value * duration
      runOnJS(seekTo)(seekTime)
      isSeeking.value = false
      pressed.value = false
    })

  const progressStyle = useAnimatedStyle(() => ({
    width: `${progress.value * 100}%`,
  }))

  const barContainerStyle = useAnimatedStyle(() => ({
    height: withTiming(pressed.value ? 12 : 4, { duration: 200 }),
  }))

  return (
    <Animated.View
      layout={Layout.duration(300)}
      className={compact ? "mb-4" : "mb-6"}
    >
      <GestureDetector gesture={Gesture.Simultaneous(seekGesture, tapGesture)}>
        <View
          className={compact ? "py-2" : "py-4"}
          onLayout={(e) => {
            barWidth.value = e.nativeEvent.layout.width
          }}
        >
          <Animated.View
            style={barContainerStyle}
            className="w-full overflow-hidden rounded-full bg-white/20"
          >
            <Animated.View
              style={[progressStyle, { backgroundColor: "#FFFFFF" }]}
              className="h-full rounded-full"
            />
          </Animated.View>
        </View>
      </GestureDetector>
      <View className="mt-2 flex-row justify-between">
        <AnimatedTextInput
          animatedProps={animatedTextProps}
          className="font-variant-numeric-tabular-nums p-0 text-xs text-white/50"
          editable={false}
          value={formatTime(currentTime)}
          style={{ color: "rgba(255, 255, 255, 0.5)" }}
        />
        <Text className="text-xs text-white/50">{formatTime(duration)}</Text>
      </View>
    </Animated.View>
  )
}
