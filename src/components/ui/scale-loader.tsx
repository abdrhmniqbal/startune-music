import { useEffect } from "react"
import { View } from "react-native"
import Animated, {
  useAnimatedStyle,
  useSharedValue,
  withDelay,
  withRepeat,
  withTiming,
} from "react-native-reanimated"

import { useThemeColors } from "@/hooks/use-theme-colors"

const BAR_COUNT = 3
const BAR_WIDTH = 3

interface ScaleLoaderProps {
  size?: number
}

function Bar({ delay, maxHeight }: { delay: number; maxHeight: number }) {
  const theme = useThemeColors()
  const scale = useSharedValue(0.3)

  useEffect(() => {
    // Start at initial scale
    scale.value = 0.3

    // Animate to 1 and ping-pong back to 0.3 smoothly
    scale.value = withDelay(
      delay,
      withRepeat(withTiming(1, { duration: 400 }), -1, true)
    )
  }, [delay, scale])

  const animatedStyle = useAnimatedStyle(() => ({
    height: scale.value * maxHeight,
  }))

  return (
    <Animated.View
      style={[
        {
          width: BAR_WIDTH,
          borderRadius: BAR_WIDTH / 2,
          backgroundColor: theme.accent,
        },
        animatedStyle,
      ]}
    />
  )
}

export function ScaleLoader({ size = 20 }: ScaleLoaderProps) {
  return (
    <View
      className="absolute inset-0 items-center justify-center rounded-lg"
      style={{ backgroundColor: "rgba(0,0,0,0.5)" }}
    >
      <View
        style={{
          flexDirection: "row",
          alignItems: "flex-end",
          gap: 2,
          height: size,
        }}
      >
        {Array.from({ length: BAR_COUNT }).map((_, i) => (
          <Bar key={i} delay={i * 150} maxHeight={size} />
        ))}
      </View>
    </View>
  )
}
