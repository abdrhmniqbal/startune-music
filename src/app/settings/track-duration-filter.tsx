import { PressableFeedback, Slider } from "heroui-native"
import * as React from "react"
import { ScrollView, Text, View } from "react-native"

import LocalTickIcon from "@/components/icons/local/tick"
import { useThemeColors } from "@/modules/ui/theme"
import {
  setTrackDurationFilterConfig,
  type TrackDurationFilterMode,
} from "@/modules/indexer/track-duration-filter"
import { startIndexing } from "@/modules/indexer/indexer.service"
import { useIndexerStore } from "@/modules/indexer/indexer.store"
import { useSettingsStore } from "@/modules/settings/settings.store"

interface DurationOption {
  label: string
  value: TrackDurationFilterMode
  description?: string
}

const DURATION_OPTIONS: DurationOption[] = [
  { label: "No Filter", value: "off", description: "Include all tracks." },
  {
    label: "At Least 30 Seconds",
    value: "min30s",
    description: "Exclude very short clips.",
  },
  {
    label: "At Least 1 Minute",
    value: "min60s",
    description: "Keep normal music lengths.",
  },
  {
    label: "At Least 2 Minutes",
    value: "min120s",
    description: "Focus on full-length tracks.",
  },
  {
    label: "Custom",
    value: "custom",
    description: "Set your own minimum duration.",
  },
]

function formatDuration(seconds: number): string {
  const total = Math.max(0, Math.round(seconds))
  if (total < 60) {
    return `${total}s`
  }

  const minutes = Math.floor(total / 60)
  const remainder = total % 60
  return remainder > 0 ? `${minutes}m ${remainder}s` : `${minutes}m`
}

function getSliderNumericValue(value: number | number[]): number {
  return Array.isArray(value) ? (value[0] ?? 0) : value
}

export default function TrackDurationFilterScreen() {
  const theme = useThemeColors()
  const indexerState = useIndexerStore((state) => state.indexerState)
  const config = useSettingsStore(
    (state) => state.trackDurationFilterConfig
  )
  const [customSliderValue, setCustomSliderValue] = React.useState<
    number | null
  >(null)
  const resolvedCustomSliderValue =
    customSliderValue ?? config.customMinimumSeconds

  async function handleModeSelect(mode: TrackDurationFilterMode) {
    await setTrackDurationFilterConfig({ mode })

    if (mode !== "custom") {
      setCustomSliderValue(null)
      await startIndexing(false, true)
    }
  }

  async function handleCustomSlidingComplete(value: number) {
    await setTrackDurationFilterConfig({
      mode: "custom",
      customMinimumSeconds: value,
    })
    await startIndexing(false, true)
  }

  return (
    <ScrollView className="flex-1 bg-background">
      <View className="py-2">
        {DURATION_OPTIONS.map((option) => (
          <PressableFeedback
            key={option.value}
            onPress={() => {
              void handleModeSelect(option.value)
            }}
            className="flex-row items-center bg-background px-6 py-4 active:opacity-70"
            isDisabled={indexerState.isIndexing}
          >
            <View className="flex-1 gap-0.5 pr-2">
              <Text className="text-[17px] font-normal text-foreground">
                {option.label}
              </Text>
              {option.description ? (
                <Text className="text-[13px] leading-5 text-muted">
                  {option.description}
                </Text>
              ) : null}
            </View>
            {config.mode === option.value && (
              <LocalTickIcon
                fill="none"
                width={24}
                height={24}
                color={theme.accent}
              />
            )}
          </PressableFeedback>
        ))}

        {config.mode === "custom" ? (
          <View className="px-6 pt-4 pb-2">
            <View className="mb-3 flex-row items-center justify-between">
              <Text className="text-sm text-muted">Minimum duration</Text>
              <Text className="text-sm font-medium text-foreground">
                {formatDuration(resolvedCustomSliderValue)}
              </Text>
            </View>
            <Slider
              minValue={0}
              maxValue={600}
              step={5}
              value={resolvedCustomSliderValue}
              isDisabled={indexerState.isIndexing}
              onChange={(value) => {
                setCustomSliderValue(getSliderNumericValue(value))
              }}
              onChangeEnd={(value) => {
                void handleCustomSlidingComplete(getSliderNumericValue(value))
              }}
            >
              <Slider.Track className="h-2 rounded-full bg-border">
                <Slider.Fill className="rounded-full bg-accent" />
                <Slider.Thumb />
              </Slider.Track>
            </Slider>
            <Text className="mt-2 text-xs text-muted">
              Changes apply to indexing and remove tracks below this duration on
              next scan.
            </Text>
          </View>
        ) : null}
      </View>
    </ScrollView>
  )
}
