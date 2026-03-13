import type { Track } from "@/modules/player/player.store"
import { BottomSheetScrollView } from "@gorhom/bottom-sheet"
import { useStore } from "@nanostores/react"
import { PressableFeedback } from "heroui-native"
import * as React from "react"
import { Text, useWindowDimensions, View } from "react-native"

import Animated, { FadeIn, FadeOut, Layout } from "react-native-reanimated"
import LocalMicIcon from "@/components/icons/local/mic"
import { EmptyState } from "@/components/ui"
import { useThemeColors } from "@/hooks/use-theme-colors"
import { $currentTime, seekTo } from "@/modules/player/player.store"
import { parseSyncedLyricsLines, splitLyricsLines } from "@/utils/lyrics"

type LyricsMode = "static" | "synced"

interface LyricsViewProps {
  track: Track | null
}

const AUTO_SCROLL_RESUME_DELAY_MS = 100
const FONT_SCALE_VALUES = [1, 1.2, 1.4] as const
type FontScaleLevel = (typeof FONT_SCALE_VALUES)[number]

export const LyricsView: React.FC<LyricsViewProps> = ({ track }) => {
  const theme = useThemeColors()
  const { height } = useWindowDimensions()
  const currentTime = useStore($currentTime)
  const lines = splitLyricsLines(track?.lyrics)
  const syncedLines = parseSyncedLyricsLines(track?.lyrics)
  const hasStaticLyrics = lines.length > 0
  const hasSyncedLyrics = syncedLines.length > 0
  const [mode, setMode] = React.useState<LyricsMode>("static")
  const effectiveMode: LyricsMode =
    mode === "synced" && hasSyncedLyrics ? "synced" : "static"
  const [fontScale, setFontScale] = React.useState<FontScaleLevel>(1)
  const scrollViewRef = React.useRef<any>(null)
  const syncedLineOffsetRef = React.useRef<Record<string, number>>({})
  const isUserScrollingRef = React.useRef(false)
  const autoScrollResumeTimeoutRef = React.useRef<ReturnType<
    typeof setTimeout
  > | null>(null)
  const [viewportHeight, setViewportHeight] = React.useState(0)

  const handleToggleKaraoke = React.useCallback(() => {
    if (!hasSyncedLyrics) {
      return
    }
    setMode((previousMode) => (previousMode === "synced" ? "static" : "synced"))
  }, [hasSyncedLyrics])

  const handleToggleFontScale = React.useCallback(() => {
    const currentIndex = FONT_SCALE_VALUES.indexOf(fontScale)
    const nextIndex = (currentIndex + 1) % FONT_SCALE_VALUES.length
    const nextScale = FONT_SCALE_VALUES[nextIndex] ?? 1
    setFontScale(nextScale)
  }, [fontScale])

  const fontScaleLabel = React.useMemo(() => {
    const levelIndex = FONT_SCALE_VALUES.indexOf(fontScale)
    const level = levelIndex >= 0 ? levelIndex + 1 : 1
    return `×${level}`
  }, [fontScale])

  const activeSyncedLineIndex = syncedLines.findIndex((line, index) => {
    const nextLine = syncedLines[index + 1]
    return (
      currentTime >= line.time && (!nextLine || currentTime < nextLine.time)
    )
  })

  React.useEffect(() => {
    syncedLineOffsetRef.current = {}
  }, [track?.id, effectiveMode, fontScale])

  React.useEffect(() => {
    isUserScrollingRef.current = false
    if (!track?.id) {
      return
    }

    let frameB: number | null = null
    const frameA = requestAnimationFrame(() => {
      frameB = requestAnimationFrame(() => {
        scrollViewRef.current?.scrollTo({ y: 0, animated: true })
      })
    })

    return () => {
      cancelAnimationFrame(frameA)
      if (frameB !== null) {
        cancelAnimationFrame(frameB)
      }
    }
  }, [track?.id])

  const setSyncedLineOffset = React.useCallback((lineId: string, y: number) => {
    const current = syncedLineOffsetRef.current[lineId]
    if (current === undefined || Math.abs(current - y) > 1) {
      syncedLineOffsetRef.current[lineId] = y
    }
  }, [])

  const clearAutoScrollResumeTimeout = React.useCallback(() => {
    if (autoScrollResumeTimeoutRef.current !== null) {
      clearTimeout(autoScrollResumeTimeoutRef.current)
      autoScrollResumeTimeoutRef.current = null
    }
  }, [])

  const scheduleAutoScrollResume = React.useCallback(() => {
    clearAutoScrollResumeTimeout()
    autoScrollResumeTimeoutRef.current = setTimeout(() => {
      isUserScrollingRef.current = false
      autoScrollResumeTimeoutRef.current = null
    }, AUTO_SCROLL_RESUME_DELAY_MS)
  }, [clearAutoScrollResumeTimeout])

  const handleUserScrollStart = React.useCallback(() => {
    isUserScrollingRef.current = true
    clearAutoScrollResumeTimeout()
  }, [clearAutoScrollResumeTimeout])

  const handleUserScrollEnd = React.useCallback(() => {
    scheduleAutoScrollResume()
  }, [scheduleAutoScrollResume])

  React.useEffect(() => {
    return () => {
      clearAutoScrollResumeTimeout()
    }
  }, [clearAutoScrollResumeTimeout])

  React.useEffect(() => {
    if (
      effectiveMode !== "synced" ||
      activeSyncedLineIndex < 0 ||
      isUserScrollingRef.current
    ) {
      return
    }

    const activeLine = syncedLines[activeSyncedLineIndex]
    if (!activeLine) {
      return
    }

    const measuredY = syncedLineOffsetRef.current[activeLine.id]
    const fallbackY = activeSyncedLineIndex * 52 * fontScale
    const anchorY = Math.max(28, viewportHeight * 0.42)

    scrollViewRef.current?.scrollTo({
      y: Math.max(0, (measuredY ?? fallbackY) - anchorY),
      animated: true,
    })
  }, [
    activeSyncedLineIndex,
    effectiveMode,
    syncedLines,
    viewportHeight,
    fontScale,
  ])

  if (!track) {
    return null
  }

  if (!hasStaticLyrics) {
    return (
      <Animated.View
        entering={FadeIn.duration(200)}
        exiting={FadeOut.duration(200)}
        layout={Layout.duration(300)}
        className="-mx-2 my-3 flex-1 justify-center"
      >
        <EmptyState
          icon={
            <LocalMicIcon
              fill="none"
              width={36}
              height={36}
              color={theme.muted}
            />
          }
          title="No Lyrics"
          message="This track does not have embedded lyrics yet."
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
      <BottomSheetScrollView
        ref={scrollViewRef}
        showsVerticalScrollIndicator={false}
        keyboardShouldPersistTaps="handled"
        nestedScrollEnabled
        onScrollBeginDrag={handleUserScrollStart}
        onMomentumScrollBegin={handleUserScrollStart}
        onScrollEndDrag={handleUserScrollEnd}
        onMomentumScrollEnd={handleUserScrollEnd}
        onLayout={(event) => {
          setViewportHeight(event.nativeEvent.layout.height)
        }}
        contentContainerStyle={{
          paddingTop: 8,
          paddingBottom: Math.max(96, height * 0.24),
          paddingHorizontal: 8,
          gap: 10,
        }}
      >
        {effectiveMode === "static"
          ? lines.map((line) => {
              if (line.isSpacer) {
                return <View key={line.id} style={{ height: 14 }} />
              }

              return (
                <View key={line.id} className="py-1">
                  <Text
                    selectable={false}
                    style={{
                      color: "rgba(255,255,255,0.8)",
                      fontSize: 20 * fontScale,
                      lineHeight: 32 * fontScale,
                      fontWeight: "700",
                      letterSpacing: -0.4,
                    }}
                  >
                    {line.text}
                  </Text>
                </View>
              )
            })
          : syncedLines.map((line, index) => {
              const isActive = index === activeSyncedLineIndex
              const isPast =
                activeSyncedLineIndex >= 0 && index < activeSyncedLineIndex

              return (
                <PressableFeedback
                  key={line.id}
                  onPress={() => {
                    void seekTo(line.time)
                  }}
                  className="py-1 active:opacity-85"
                  onLayout={(event) =>
                    setSyncedLineOffset(line.id, event.nativeEvent.layout.y)
                  }
                >
                  <Text
                    selectable={false}
                    style={{
                      color: isActive
                        ? "rgba(255,255,255,0.96)"
                        : isPast
                          ? "rgba(255,255,255,0.48)"
                          : "rgba(255,255,255,0.22)",
                      fontSize: (isActive ? 22 : 18) * fontScale,
                      lineHeight: (isActive ? 34 : 28) * fontScale,
                      fontWeight: isActive ? "700" : "600",
                      letterSpacing: -0.4,
                    }}
                  >
                    {line.text}
                  </Text>
                </PressableFeedback>
              )
            })}
      </BottomSheetScrollView>

      {hasSyncedLyrics ? (
        <PressableFeedback
          onPress={handleToggleKaraoke}
          className="absolute bottom-3 left-2 rounded-full px-3 py-2 active:opacity-90"
          style={{
            backgroundColor:
              effectiveMode === "synced"
                ? theme.foreground
                : "rgba(255, 255, 255, 0.14)",
          }}
        >
          <Text
            className="text-xs font-semibold"
            style={{ color: effectiveMode === "synced" ? "#0A0A0A" : "white" }}
          >
            Karaoke {effectiveMode === "synced" ? "On" : "Off"}
          </Text>
        </PressableFeedback>
      ) : null}

      <PressableFeedback
        onPress={handleToggleFontScale}
        className="absolute right-2 bottom-3 rounded-full px-3 py-2 active:opacity-90"
        style={{ backgroundColor: "rgba(255, 255, 255, 0.14)" }}
      >
        <Text className="text-xs font-semibold text-white">
          {fontScaleLabel}
        </Text>
      </PressableFeedback>
    </Animated.View>
  )
}
