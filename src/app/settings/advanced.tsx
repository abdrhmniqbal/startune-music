import { useStore } from "@nanostores/react"
import Constants from "expo-constants"
import { BottomSheet, PressableFeedback, Toast, useToast } from "heroui-native"
import { useEffect, useState } from "react"
import { Linking, Platform, ScrollView, Text, View } from "react-native"

import LocalChevronRightIcon from "@/components/icons/local/chevron-right"
import LocalTickIcon from "@/components/icons/local/tick"
import { useThemeColors } from "@/hooks/use-theme-colors"
import {
  $loggingConfig,
  type AppLogLevel,
  ensureLoggingConfigLoaded,
  setAppLogLevel,
  shareCrashLogs,
} from "@/modules/logging"

export default function AdvancedSettingsScreen() {
  const theme = useThemeColors()
  const { toast } = useToast()
  const loggingConfig = useStore($loggingConfig)
  const [isLogLevelSheetOpen, setIsLogLevelSheetOpen] = useState(false)

  useEffect(() => {
    void ensureLoggingConfigLoaded()
  }, [])

  const logLevelLabel = loggingConfig.level === "extra" ? "Extra" : "Minimal"

  async function handleLogLevelSelect(level: AppLogLevel) {
    await setAppLogLevel(level)
    setIsLogLevelSheetOpen(false)
  }

  async function handleShareCrashLogs() {
    const result = await shareCrashLogs()
    toast.show({
      duration: 2200,
      component: (props) => (
        <Toast {...props} variant="accent" placement="bottom">
          <Toast.Title className="text-sm font-semibold">
            {result.shared ? "Logs ready to share" : "Unable to share logs"}
          </Toast.Title>
          <Toast.Description className="text-xs text-muted">
            {result.shared
              ? "Share sheet opened with the latest captured logs."
              : result.reason || "Try again in a moment."}
          </Toast.Description>
        </Toast>
      ),
    })
  }

  async function openBatteryOptimizationSettings() {
    const appPackage = Constants.expoConfig?.android?.package

    try {
      if (Platform.OS !== "android") {
        await Linking.openSettings()
        return
      }

      if (appPackage) {
        try {
          await Linking.sendIntent(
            "android.settings.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
            [
              {
                key: "android.provider.extra.APP_PACKAGE",
                value: appPackage,
              },
            ]
          )
          return
        } catch {
          // Fall through to settings list.
        }
      }

      await Linking.sendIntent(
        "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"
      )
      return
    } catch {
      // Fallback to app settings.
    }

    await Linking.openSettings()
  }

  return (
    <ScrollView className="flex-1 bg-background">
      <View className="py-2">
        <PressableFeedback
          onPress={() => setIsLogLevelSheetOpen(true)}
          className="flex-row items-center bg-background px-6 py-4 active:opacity-70"
        >
          <View className="flex-1 gap-1">
            <Text className="text-[17px] font-normal text-foreground">
              Log Level
            </Text>
            <Text className="text-[13px] leading-5 text-muted">
              {logLevelLabel === "Extra"
                ? "Extra: capture debug, info, warnings, and errors."
                : "Minimal: capture critical and error logs only."}
            </Text>
          </View>
          <View className="flex-row items-center gap-2">
            <LocalChevronRightIcon
              fill="none"
              width={20}
              height={20}
              color={theme.muted}
            />
          </View>
        </PressableFeedback>

        <PressableFeedback
          onPress={() => {
            void handleShareCrashLogs()
          }}
          className="flex-row items-center bg-background px-6 py-4 active:opacity-70"
        >
          <View className="flex-1 gap-1">
            <Text className="text-[17px] font-normal text-foreground">
              Share crash logs
            </Text>
            <Text className="text-[13px] leading-5 text-muted">
              Saves error logs to a local file and opens a share sheet.
            </Text>
          </View>
          <LocalChevronRightIcon
            fill="none"
            width={20}
            height={20}
            color={theme.muted}
          />
        </PressableFeedback>

        <PressableFeedback
          onPress={() => {
            void openBatteryOptimizationSettings()
          }}
          className="flex-row items-center bg-background px-6 py-4 active:opacity-70"
        >
          <View className="flex-1 gap-1">
            <Text className="text-[17px] font-normal text-foreground">
              Disable Battery Optimization
            </Text>
            <Text className="text-[13px] leading-5 text-muted">
              {Platform.OS === "android"
                ? "Prevent background restrictions so indexing and playback stay reliable."
                : "Open system settings."}
            </Text>
          </View>
          <LocalChevronRightIcon
            fill="none"
            width={20}
            height={20}
            color={theme.muted}
          />
        </PressableFeedback>
      </View>

      <BottomSheet
        isOpen={isLogLevelSheetOpen}
        onOpenChange={setIsLogLevelSheetOpen}
      >
        <BottomSheet.Portal>
          <BottomSheet.Overlay />
          <BottomSheet.Content
            backgroundClassName="bg-surface"
            className="gap-1"
          >
            <BottomSheet.Title className="mb-1 text-xl">
              Log Level
            </BottomSheet.Title>

            <PressableFeedback
              onPress={() => {
                void handleLogLevelSelect("minimal")
              }}
              className="h-14 flex-row items-center justify-between active:opacity-50"
            >
              <View className="gap-0.5">
                <Text className="text-base font-medium text-foreground">
                  Minimal
                </Text>
                <Text className="text-xs text-muted">
                  Critical errors only.
                </Text>
              </View>
              {loggingConfig.level === "minimal" ? (
                <LocalTickIcon
                  fill="none"
                  width={22}
                  height={22}
                  color={theme.accent}
                />
              ) : null}
            </PressableFeedback>

            <PressableFeedback
              onPress={() => {
                void handleLogLevelSelect("extra")
              }}
              className="h-14 flex-row items-center justify-between active:opacity-50"
            >
              <View className="gap-0.5">
                <Text className="text-base font-medium text-foreground">
                  Extra
                </Text>
                <Text className="text-xs text-muted">Log everything.</Text>
              </View>
              {loggingConfig.level === "extra" ? (
                <LocalTickIcon
                  fill="none"
                  width={22}
                  height={22}
                  color={theme.accent}
                />
              ) : null}
            </PressableFeedback>
          </BottomSheet.Content>
        </BottomSheet.Portal>
      </BottomSheet>
    </ScrollView>
  )
}
