import type { PatternType } from "@/modules/genres/genres.types"
import { Card, PressableFeedback } from "heroui-native"
import { Text, View } from "react-native"

import { cn } from "tailwind-variants"

interface GenreCardProps {
  title: string
  color: string
  pattern: PatternType
  onPress?: () => void
}

const GRID_PATTERN_KEYS = [
  "grid-1",
  "grid-2",
  "grid-3",
  "grid-4",
  "grid-5",
  "grid-6",
  "grid-7",
  "grid-8",
  "grid-9",
  "grid-10",
  "grid-11",
  "grid-12",
]

export function GenreCard({ title, color, pattern, onPress }: GenreCardProps) {
  return (
    <PressableFeedback
      onPress={onPress}
      className="w-[47.5%] active:opacity-80"
    >
      <Card
        className={cn(
          "relative h-24 justify-start overflow-hidden border-none p-4",
          color
        )}
      >
        <Text className="z-10 text-[17px] leading-tight font-bold text-white">
          {title}
        </Text>
        <View className="absolute inset-0">
          {pattern === "circles" && (
            <>
              <View className="absolute -top-4 -right-4 h-20 w-20 rounded-full bg-white/15" />
              <View className="absolute top-1/2 left-1/2 h-12 w-12 -translate-x-6 -translate-y-6 rounded-full bg-white/5" />
              <View className="absolute right-4 bottom-[-10] h-16 w-16 rounded-full bg-white/10" />
            </>
          )}
          {pattern === "waves" && (
            <>
              <View className="absolute bottom-[-20] -left-12 h-40 w-40 rounded-full border-20 border-white/10" />
              <View className="absolute top-[-20] right-[-20] h-28 w-28 rounded-full border-12 border-white/10" />
            </>
          )}
          {pattern === "grid" && (
            <View className="absolute inset-0 flex-row flex-wrap gap-2 p-1.5">
              {GRID_PATTERN_KEYS.map((key) => (
                <View key={key} className="h-6 w-6 rounded-sm bg-white/5" />
              ))}
            </View>
          )}
          {pattern === "diamonds" && (
            <>
              <View className="absolute top-4 right-[-10] h-16 w-16 rotate-45 bg-white/15" />
              <View className="absolute bottom-0 left-[-20] h-24 w-24 rotate-45 bg-white/5" />
            </>
          )}
          {pattern === "triangles" && (
            <>
              <View className="absolute top-0 right-0 h-0 w-0 border-t-40 border-l-40 border-t-white/15 border-l-transparent" />
              <View className="absolute bottom-[-10] left-4 h-0 w-0 border-r-60 border-b-60 border-r-transparent border-b-white/10" />
            </>
          )}
          {pattern === "rings" && (
            <>
              <View className="absolute -top-2 -right-2 h-16 w-16 rounded-full border-4 border-white/20" />
              <View className="absolute -top-6 -right-6 h-24 w-24 rounded-full border-4 border-white/10" />
            </>
          )}
          {pattern === "pills" && (
            <>
              <View className="absolute top-2 right-0 h-8 w-20 rotate-[-15deg] rounded-full bg-white/15" />
              <View className="absolute bottom-4 -left-4 h-10 w-24 rotate-25 rounded-full bg-white/10" />
            </>
          )}
          {pattern === "stripes" && (
            <>
              <View className="absolute top-0 -left-6 h-28 w-3 rotate-12 bg-white/10" />
              <View className="absolute top-0 left-6 h-28 w-3 rotate-12 bg-white/15" />
              <View className="absolute top-0 left-18 h-28 w-3 rotate-12 bg-white/10" />
              <View className="absolute top-0 left-30 h-28 w-3 rotate-12 bg-white/15" />
            </>
          )}
          {pattern === "stars" && (
            <>
              <View className="absolute top-4 right-6 h-12 w-3 rounded-full bg-white/15" />
              <View className="absolute top-8 right-1.5 h-3 w-12 rounded-full bg-white/15" />
              <View className="absolute bottom-3 left-7 h-8 w-2 rounded-full bg-white/10" />
              <View className="absolute bottom-6 left-4 h-2 w-8 rounded-full bg-white/10" />
            </>
          )}
          {pattern === "zigzag" && (
            <>
              <View className="absolute top-6 right-[-8] h-2 w-14 rotate-45 bg-white/15" />
              <View className="absolute top-12 right-2 h-2 w-14 -rotate-45 bg-white/10" />
              <View className="absolute top-18 right-[-8] h-2 w-14 rotate-45 bg-white/10" />
              <View className="absolute bottom-6 left-[-8] h-2 w-12 -rotate-45 bg-white/10" />
            </>
          )}
          {pattern === "crosses" && (
            <>
              <View className="absolute top-4 right-5 h-10 w-2 rounded-full bg-white/15" />
              <View className="absolute top-8 right-1 h-2 w-10 rounded-full bg-white/15" />
              <View className="absolute bottom-4 left-5 h-8 w-2 rounded-full bg-white/10" />
              <View className="absolute bottom-7 left-2 h-2 w-8 rounded-full bg-white/10" />
            </>
          )}
        </View>
      </Card>
    </PressableFeedback>
  )
}
