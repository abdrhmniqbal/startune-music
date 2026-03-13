import type { Track } from "@/modules/player/player.store"
import { BottomSheetScrollView } from "@gorhom/bottom-sheet"
import { useQuery } from "@tanstack/react-query"
import { useProgress } from "@weights-ai/react-native-track-player"
import { PressableFeedback } from "heroui-native"
import * as React from "react"

import { Text, useWindowDimensions, View } from "react-native"
import Animated, { FadeIn, FadeOut, Layout } from "react-native-reanimated"
import LocalMicIcon from "@/components/icons/local/mic"
import { EmptyState } from "@/components/ui"
import { useThemeColors } from "@/hooks/use-theme-colors"
import { queryClient } from "@/lib/tanstack-query"
import {
  parseSyncedLyricsLines,
  parseTTMLLines,
  splitLyricsLines,
  type TTMLLine,
} from "@/modules/lyrics/lyrics"
import { resolveTrackLyricsSource } from "@/modules/lyrics/lyrics-source"
import { $currentTime, seekTo } from "@/modules/player/player.store"

type LyricsMode = "static" | "synced" | "ttml"

interface LyricsViewProps {
  track: Track | null
}

const AUTO_SCROLL_RESUME_DELAY_MS = 100
const FONT_SCALE_VALUES = [1, 1.2, 1.4] as const
type FontScaleLevel = (typeof FONT_SCALE_VALUES)[number]

import {
  cancelAnimation,
  Easing,
  useAnimatedStyle,
  useSharedValue,
  withTiming,
} from "react-native-reanimated"

const TTMLWordSpan: React.FC<{
  text: string
  begin: number
  end: number
  currentTime: number
  lineActive: boolean
  linePast: boolean
  fontScale: number
}> = ({ text, begin, end, currentTime, lineActive, linePast, fontScale }) => {
  const progress = useSharedValue(
    linePast || (lineActive && currentTime > end) ? 1 : 0
  )
  const [textWidth, setTextWidth] = React.useState(0)

  React.useEffect(() => {
    if (linePast || (lineActive && currentTime > end)) {
      cancelAnimation(progress)
      progress.value = 1
    } else if (lineActive && currentTime >= begin && currentTime <= end) {
      const duration = end - begin
      const elapsed = Math.max(0, currentTime - begin)
      const currentPercent = duration > 0 ? Math.min(1, elapsed / duration) : 1

      // Native animation smoothly interpolates the exact 50ms gap between frames
      progress.value = withTiming(currentPercent, {
        duration: 60,
        easing: Easing.linear,
      })
    } else {
      cancelAnimation(progress)
      progress.value = 0
    }
  }, [currentTime, lineActive, linePast, begin, end, progress])

  const baseColor = lineActive
    ? "rgba(255,255,255,0.28)"
    : linePast
      ? "rgba(255,255,255,0.48)"
      : "rgba(255,255,255,0.22)"

  const activeColor = "rgba(255,255,255,0.96)"

  const fontSize = (lineActive ? 22 : 18) * fontScale
  const lineHeight = (lineActive ? 34 : 28) * fontScale
  const fontWeight = lineActive ? "700" : "600"

  const foregroundStyle = useAnimatedStyle(() => {
    return {
      position: "absolute",
      left: 0,
      top: 0,
      bottom: 0,
      width: progress.value * textWidth,
      overflow: "hidden",
    }
  })

  // We map regular spaces to non-breaking spaces so they are preserved accurately by onLayout
  const displayText = text.replace(/ /g, "\u00A0")

  return (
    <View style={{ position: "relative", justifyContent: "center" }}>
      <Text
        onLayout={(e) => setTextWidth(e.nativeEvent.layout.width)}
        style={{
          color: baseColor,
          fontSize,
          lineHeight,
          fontWeight,
          letterSpacing: -0.4,
          paddingHorizontal: 0,
          marginHorizontal: 0,
        }}
      >
        {displayText}
      </Text>
      {textWidth > 0 && (
        <Animated.View style={foregroundStyle}>
          <View
            style={{
              width: textWidth,
              position: "absolute",
              top: 0,
              bottom: 0,
              left: 0,
            }}
          >
            <Text
              style={{
                color: activeColor,
                fontSize,
                lineHeight,
                fontWeight,
                letterSpacing: -0.4,
                paddingHorizontal: 0,
                marginHorizontal: 0,
                width: textWidth,
              }}
            >
              {displayText}
            </Text>
          </View>
        </Animated.View>
      )}
    </View>
  )
}

const TTMLLineRow: React.FC<{
  line: TTMLLine
  currentTime: number
  isActive: boolean
  isPast: boolean
  fontScale: number
  onSeek: (time: number) => void
  onLayoutLine: (id: string, y: number) => void
}> = ({
  line,
  currentTime,
  isActive,
  isPast,
  fontScale,
  onSeek,
  onLayoutLine,
}) => {
  const handlePress = React.useCallback(
    () => onSeek(line.begin),
    [line.begin, onSeek]
  )
  const handleLayout = React.useCallback(
    (event: any) => onLayoutLine(line.id, event.nativeEvent.layout.y),
    [line.id, onLayoutLine]
  )

  return (
    <PressableFeedback
      onPress={handlePress}
      className="py-1 active:opacity-85"
      onLayout={handleLayout}
    >
      <View style={{ flexDirection: "row", flexWrap: "wrap" }}>
        {line.words.map((word, wordIndex) => (
          <TTMLWordSpan
            key={`${line.id}-w${wordIndex}`}
            text={word.text}
            begin={word.begin}
            end={word.end}
            currentTime={currentTime}
            lineActive={isActive}
            linePast={isPast}
            fontScale={fontScale}
          />
        ))}
      </View>
    </PressableFeedback>
  )
}

const ActiveTTMLLineRow: React.FC<{
  line: TTMLLine
  fontScale: number
  onSeek: (time: number) => void
  onLayoutLine: (id: string, y: number) => void
}> = ({ line, fontScale, onSeek, onLayoutLine }) => {
  const { position: currentTime } = useProgress(50)

  const handlePress = React.useCallback(
    () => onSeek(line.begin),
    [line.begin, onSeek]
  )
  const handleLayout = React.useCallback(
    (event: any) => onLayoutLine(line.id, event.nativeEvent.layout.y),
    [line.id, onLayoutLine]
  )

  return (
    <PressableFeedback
      onPress={handlePress}
      className="py-1 active:opacity-85"
      onLayout={handleLayout}
    >
      <View style={{ flexDirection: "row", flexWrap: "wrap" }}>
        {line.words.map((word, wordIndex) => (
          <TTMLWordSpan
            key={`${line.id}-w${wordIndex}`}
            text={word.text}
            begin={word.begin}
            end={word.end}
            currentTime={currentTime}
            lineActive={true}
            linePast={false}
            fontScale={fontScale}
          />
        ))}
      </View>
    </PressableFeedback>
  )
}

export const LyricsView: React.FC<LyricsViewProps> = ({ track }) => {
  const theme = useThemeColors()
  const { height } = useWindowDimensions()
  const { data: resolvedLyrics = null } = useQuery(
    {
      queryKey: [
        "track-lyrics-source",
        track?.id ?? "",
        track?.uri ?? "",
        track?.fileHash ?? "",
        track?.scanTime ?? 0,
      ],
      enabled: Boolean(track?.id),
      staleTime: 0,
      queryFn: async () => {
        let sourceTrack = track
        // Fast Refresh can reset in-memory Nanostores, causing the active track to lose DB-only fields like 'lyrics'
        // If we don't have lyrics in memory, double check the DB just in case.
        if (sourceTrack?.id && !sourceTrack.lyrics) {
          try {
            const { db } = await import("@/db/client")
            const { tracks } = await import("@/db/schema")
            const { eq } = await import("drizzle-orm")
            const dbTrack = await db.query.tracks.findFirst({
              where: eq(tracks.id, sourceTrack.id),
              columns: { lyrics: true },
            })
            if (dbTrack?.lyrics) {
              sourceTrack = { ...sourceTrack, lyrics: dbTrack.lyrics }
            }
          } catch (e) {
            // Ignore fallback errors
          }
        }

        const source = await resolveTrackLyricsSource(sourceTrack)
        return source ?? null
      },
      placeholderData: () => {
        const metadataLyrics = track?.lyrics?.trim()
        return metadataLyrics ? track?.lyrics : null
      },
    },
    queryClient
  )

  const ttmlLines = React.useMemo(
    () => (resolvedLyrics ? parseTTMLLines(resolvedLyrics) : []),
    [resolvedLyrics]
  )
  const hasTTML = ttmlLines.length > 0
  const lines = hasTTML ? [] : splitLyricsLines(resolvedLyrics)
  const syncedLines = hasTTML ? [] : parseSyncedLyricsLines(resolvedLyrics)

  const hasStaticLyrics = lines.length > 0 || hasTTML
  const hasSyncedLyrics = syncedLines.length > 0 || hasTTML

  const [mode, setMode] = React.useState<LyricsMode>("static")
  const effectiveMode: LyricsMode = hasTTML
    ? mode === "static"
      ? "static"
      : "ttml"
    : mode === "synced" && syncedLines.length > 0
      ? "synced"
      : "static"

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
    setMode((previousMode) =>
      previousMode === "static" ? (hasTTML ? "ttml" : "synced") : "static"
    )
  }, [hasSyncedLyrics, hasTTML])

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

  const [activeSyncedLineIndex, setActiveSyncedLineIndex] = React.useState(-1)

  const handleSeek = React.useCallback(
    (time: number) => {
      void seekTo(time)

      // Immediately override the visual state so it jumps without delay
      let newIndex = -1
      if (effectiveMode === "ttml") {
        newIndex = ttmlLines.findIndex((line, index) => {
          const nextLine = ttmlLines[index + 1]
          return time >= line.begin && (!nextLine || time < nextLine.begin)
        })
      } else {
        newIndex = syncedLines.findIndex((line, index) => {
          const nextLine = syncedLines[index + 1]
          return time >= line.time && (!nextLine || time < nextLine.time)
        })
      }
      setActiveSyncedLineIndex(newIndex)
    },
    [effectiveMode, ttmlLines, syncedLines]
  )

  React.useEffect(() => {
    const isSyncedMode = effectiveMode === "synced" || effectiveMode === "ttml"
    if (!isSyncedMode) {
      setActiveSyncedLineIndex(-1)
      return
    }

    return $currentTime.subscribe((time) => {
      let newIndex = -1
      if (effectiveMode === "ttml") {
        newIndex = ttmlLines.findIndex((line, index) => {
          const nextLine = ttmlLines[index + 1]
          return time >= line.begin && (!nextLine || time < nextLine.begin)
        })
      } else {
        newIndex = syncedLines.findIndex((line, index) => {
          const nextLine = syncedLines[index + 1]
          return time >= line.time && (!nextLine || time < nextLine.time)
        })
      }

      setActiveSyncedLineIndex((prev) => (prev !== newIndex ? newIndex : prev))
    })
  }, [effectiveMode, ttmlLines, syncedLines])

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
    const isSyncedMode = effectiveMode === "synced" || effectiveMode === "ttml"
    if (
      !isSyncedMode ||
      activeSyncedLineIndex < 0 ||
      isUserScrollingRef.current
    ) {
      return
    }

    const activeLines = effectiveMode === "ttml" ? ttmlLines : syncedLines
    const activeLine = activeLines[activeSyncedLineIndex]
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
    ttmlLines,
    viewportHeight,
    fontScale,
  ])

  React.useEffect(() => {
    if (hasTTML) {
      setMode("ttml")
    }
  }, [hasTTML])

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

  const ttmlStaticLines = hasTTML
    ? ttmlLines.map((line) => ({
        id: line.id,
        text: line.words.map((w) => w.text).join(""),
        isSpacer: false,
      }))
    : []

  const staticDisplayLines = hasTTML ? ttmlStaticLines : lines

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
        onLayout={(event: any) => {
          setViewportHeight(event.nativeEvent.layout.height)
        }}
        contentContainerStyle={{
          paddingTop: 8,
          paddingBottom: Math.max(96, height * 0.24),
          paddingHorizontal: 8,
          gap: 10,
        }}
      >
        {effectiveMode === "ttml"
          ? ttmlLines.map((line, index) => {
              const isActive = index === activeSyncedLineIndex
              const isPast =
                activeSyncedLineIndex >= 0 && index < activeSyncedLineIndex

              if (isActive) {
                return (
                  <ActiveTTMLLineRow
                    key={line.id}
                    line={line}
                    fontScale={fontScale}
                    onSeek={handleSeek}
                    onLayoutLine={setSyncedLineOffset}
                  />
                )
              }

              return (
                <TTMLLineRow
                  key={line.id}
                  line={line}
                  currentTime={0}
                  isActive={false}
                  isPast={isPast}
                  fontScale={fontScale}
                  onSeek={handleSeek}
                  onLayoutLine={setSyncedLineOffset}
                />
              )
            })
          : effectiveMode === "static"
            ? staticDisplayLines.map((line) => {
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
              effectiveMode !== "static"
                ? theme.foreground
                : "rgba(255, 255, 255, 0.14)",
          }}
        >
          <Text
            className="text-xs font-semibold"
            style={{ color: effectiveMode !== "static" ? "#0A0A0A" : "white" }}
          >
            Karaoke {effectiveMode !== "static" ? "On" : "Off"}
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
