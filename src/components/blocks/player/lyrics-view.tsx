import type { Track } from "@/modules/player/player.types"
import { BottomSheetScrollView } from "@gorhom/bottom-sheet"
import { useQuery } from "@tanstack/react-query"
import { PressableFeedback } from "heroui-native"
import * as React from "react"

import { Text, useWindowDimensions, View } from "react-native"
import Animated, {
  FadeIn,
  FadeOut,
  Layout,
  type SharedValue,
  useAnimatedStyle,
  useSharedValue,
} from "react-native-reanimated"
import LocalMicIcon from "@/components/icons/local/mic"
import { EmptyState } from "@/components/ui/empty-state"
import { useThemeColors } from "@/hooks/use-theme-colors"
import { queryClient } from "@/lib/tanstack-query"
import {
  hasMeaningfulSyncedLyricsTiming,
  hasMeaningfulTTMLTiming,
  parseSyncedLyricsLines,
  parseTTMLLines,
  splitLyricsLines,
  type TTMLLine,
} from "@/modules/lyrics/lyrics"
import { resolveTrackLyricsSource } from "@/modules/lyrics/lyrics-source"
import { seekTo, usePlayerStore } from "@/modules/player/player.store"

type LyricsMode = "static" | "synced" | "ttml"

interface LyricsViewProps {
  track: Track | null
}

const AUTO_SCROLL_RESUME_DELAY_MS = 100
const FONT_SCALE_VALUES = [1, 1.2, 1.4] as const
type FontScaleLevel = (typeof FONT_SCALE_VALUES)[number]

function findTTMLLineIndex(lines: TTMLLine[], time: number) {
  return lines.findIndex((line, index) => {
    const nextLine = lines[index + 1]
    return time >= line.begin && (!nextLine || time < nextLine.begin)
  })
}

function findSyncedLineIndex(
  lines: Array<{ time: number }>,
  time: number
) {
  return lines.findIndex((line, index) => {
    const nextLine = lines[index + 1]
    return time >= line.time && (!nextLine || time < nextLine.time)
  })
}

const TTMLWordSpan: React.FC<{
  text: string
  begin: number
  end: number
  currentTimeSv: SharedValue<number>
  lineActive: boolean
  linePast: boolean
  fontScale: number
}> = ({ text, begin, end, currentTimeSv, lineActive, linePast, fontScale }) => {
  const [textWidth, setTextWidth] = React.useState(0)

  const baseColor = lineActive
    ? "rgba(255,255,255,0.46)"
    : linePast
      ? "rgba(255,255,255,0.54)"
      : "rgba(255,255,255,0.20)"

  const activeColor = "rgba(255,255,255,0.96)"

  const fontSize = (lineActive ? 24 : 18) * fontScale
  const lineHeight = (lineActive ? 36 : 28) * fontScale
  const fontWeight = lineActive ? "700" : "600"

  const displayText = text.replace(/ /g, "\u00A0")
  const foregroundStyle = useAnimatedStyle(() => {
    const wordDuration = Math.max(end - begin, 0.001)
    const currentTime = currentTimeSv.value
    const wordProgress = linePast
      ? 1
      : lineActive
        ? Math.max(0, Math.min(1, (currentTime - begin) / wordDuration))
        : 0

    return {
      width: wordProgress * textWidth,
    }
  }, [begin, end, lineActive, linePast, textWidth])

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
        <Animated.View
          style={[
            {
              position: "absolute",
              left: 0,
              top: 0,
              bottom: 0,
              overflow: "hidden",
            },
            foregroundStyle,
          ]}
        >
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
  isActive: boolean
  isPast: boolean
  fontScale: number
  onSeek: (time: number) => void
  onLayoutLine: (id: string, y: number) => void
  currentTimeSv: SharedValue<number>
}> = ({
  line,
  isActive,
  isPast,
  fontScale,
  onSeek,
  onLayoutLine,
  currentTimeSv,
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
            currentTimeSv={currentTimeSv}
            lineActive={isActive}
            linePast={isPast}
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
          } catch {
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
  const hasTimedTTML = React.useMemo(
    () => hasMeaningfulTTMLTiming(ttmlLines),
    [ttmlLines]
  )
  const hasTTML = ttmlLines.length > 0
  const lines = hasTTML ? [] : splitLyricsLines(resolvedLyrics)
  const syncedLines = hasTTML ? [] : parseSyncedLyricsLines(resolvedLyrics)
  const hasTimedSyncedLyrics = React.useMemo(
    () => hasMeaningfulSyncedLyricsTiming(syncedLines),
    [syncedLines]
  )

  const hasStaticLyrics = lines.length > 0 || hasTTML
  const hasSyncedLyrics = hasTimedSyncedLyrics || hasTimedTTML

  const [mode, setMode] = React.useState<LyricsMode>("static")
  const effectiveMode: LyricsMode = hasTimedTTML
    ? mode === "static"
      ? "static"
      : "ttml"
    : mode === "synced" && hasTimedSyncedLyrics
      ? "synced"
      : "static"

  const [fontScale, setFontScale] = React.useState<FontScaleLevel>(1)
  const scrollViewRef = React.useRef<any>(null)
  const syncedLineOffsetRef = React.useRef<Record<string, number>>({})
  const isUserScrollingRef = React.useRef(false)
  const activeSyncedLineIndexRef = React.useRef(-1)
  const autoScrollResumeTimeoutRef = React.useRef<ReturnType<
    typeof setTimeout
  > | null>(null)
  const [viewportHeight, setViewportHeight] = React.useState(0)
  const currentTimeSv = useSharedValue(usePlayerStore.getState().currentTime)

  const handleToggleKaraoke = React.useCallback(() => {
    if (!hasSyncedLyrics) {
      return
    }
    setMode((previousMode) =>
      previousMode === "static" ? (hasTimedTTML ? "ttml" : "synced") : "static"
    )
  }, [hasSyncedLyrics, hasTimedTTML])

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
    },
    []
  )

  React.useEffect(() => {
    const getActiveIndex = (time: number) => {
      if (effectiveMode === "ttml") {
        return findTTMLLineIndex(ttmlLines, time)
      }

      if (effectiveMode === "synced") {
        return findSyncedLineIndex(syncedLines, time)
      }

      return -1
    }

    const syncPlaybackTime = (time: number) => {
      currentTimeSv.value = time
      const nextIndex = getActiveIndex(time)

      if (activeSyncedLineIndexRef.current !== nextIndex) {
        activeSyncedLineIndexRef.current = nextIndex
        setActiveSyncedLineIndex(nextIndex)
      }
    }

    syncPlaybackTime(usePlayerStore.getState().currentTime)

    const unsubscribe = usePlayerStore.subscribe((state, previousState) => {
      if (state.currentTime === previousState.currentTime) {
        return
      }

      syncPlaybackTime(state.currentTime)
    })

    return unsubscribe
  }, [currentTimeSv, effectiveMode, syncedLines, ttmlLines])

  React.useEffect(() => {
    syncedLineOffsetRef.current = {}
    activeSyncedLineIndexRef.current = -1
    setActiveSyncedLineIndex(-1)
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
    if (hasTimedTTML) {
      setMode("ttml")
    }
  }, [hasTimedTTML])

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

              return (
                <TTMLLineRow
                  key={line.id}
                  line={line}
                  isActive={isActive}
                  isPast={isPast}
                  fontScale={fontScale}
                  onSeek={handleSeek}
                  onLayoutLine={setSyncedLineOffset}
                  currentTimeSv={currentTimeSv}
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
