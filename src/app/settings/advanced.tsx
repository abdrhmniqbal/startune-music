import * as Application from "expo-application"
import Constants from "expo-constants"
import { useRouter } from "expo-router"
import { PressableFeedback, Toast, useToast } from "heroui-native"
import { useEffect } from "react"
import { Linking, Platform, ScrollView, Text, View } from "react-native"

import LocalChevronRightIcon from "@/components/icons/local/chevron-right"
import { useThemeColors } from "@/hooks/use-theme-colors"
import {
  isIgnoringBatteryOptimizations,
  openBatteryOptimizationSettings as openNativeBatteryOptimizationSettings,
  requestIgnoreBatteryOptimizations,
} from "@/modules/device/battery-optimization"
import {
  ensureLoggingConfigLoaded,
  shareCrashLogs,
  useLoggingStore,
} from "@/modules/logging/logger"

export default function AdvancedSettingsScreen() {
  const theme = useThemeColors()
  const router = useRouter()
  const { toast } = useToast()
  const loggingLevel = useLoggingStore((state) => state.loggingConfig.level)

  useEffect(() => {
    void ensureLoggingConfigLoaded()
  }, [])

  const logLevelLabel = loggingLevel === "extra" ? "Extra" : "Minimal"

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
    const appPackage =
      Application.applicationId || Constants.expoConfig?.android?.package
    const BATTERY_SETTINGS_ACTION =
      "android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS"

    try {
      if (Platform.OS !== "android") {
        await Linking.openSettings()
        return
      }

      if (await isIgnoringBatteryOptimizations(appPackage)) {
        toast.show({
          duration: 2200,
          component: (props) => (
            <Toast {...props} variant="accent" placement="bottom">
              <Toast.Title className="text-sm font-semibold">
                Battery optimization already disabled
              </Toast.Title>
              <Toast.Description className="text-xs text-muted">
                No additional action is needed.
              </Toast.Description>
            </Toast>
          ),
        })
        return
      }

      const requestResult = await requestIgnoreBatteryOptimizations(appPackage)
      if (
        requestResult === "dialog_opened" ||
        requestResult === "settings_opened"
      ) {
        return
      }

      if (await openNativeBatteryOptimizationSettings()) {
        return
      }

      try {
        await Linking.sendIntent(BATTERY_SETTINGS_ACTION)
        return
      } catch {
        // Fall through to app settings.
      }
    } catch {
      // Fallback to app settings.
    }

    await Linking.openSettings()
  }

  async function openDontKillMyApp() {
    try {
      await Linking.openURL("https://dontkillmyapp.com")
    } catch {
      toast.show({
        duration: 2200,
        component: (props) => (
          <Toast {...props} variant="accent" placement="bottom">
            <Toast.Title className="text-sm font-semibold">
              Unable to open link
            </Toast.Title>
            <Toast.Description className="text-xs text-muted">
              Please try again in a moment.
            </Toast.Description>
          </Toast>
        ),
      })
    }
  }

  return (
    <ScrollView className="flex-1 bg-background">
      <View className="py-2">
        <PressableFeedback
          onPress={() => router.push("/settings/log-level")}
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

        <Text className="px-6 pt-4 pb-2 text-xs font-medium tracking-wide text-accent uppercase">
          Background Activity
        </Text>

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

        <PressableFeedback
          onPress={() => {
            void openDontKillMyApp()
          }}
          className="flex-row items-center gap-1 bg-background px-6 py-4 active:opacity-70"
        >
          <View className="flex-1 gap-1">
            <Text className="text-[17px] font-normal text-foreground">
              Don't Kill My App!
            </Text>
            <Text className="text-[13px] leading-5 text-muted">
              Open device-specific battery and background process guidance.
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
    </ScrollView>
  )
}
