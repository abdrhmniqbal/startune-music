import { NativeModules, Platform } from "react-native"

import { logError, logInfo, logWarn } from "@/modules/logging/logging.service"

interface BatteryOptimizationNativeModule {
  isIgnoringBatteryOptimizations: (packageName?: string) => Promise<boolean>
  requestIgnoreBatteryOptimizations: (
    packageName?: string
  ) => Promise<
    "already_ignored" | "dialog_opened" | "settings_opened" | "unsupported"
  >
  openBatteryOptimizationSettings: () => Promise<"settings_opened">
}

const batteryOptimizationModule = NativeModules.BatteryOptimization as
  | BatteryOptimizationNativeModule
  | undefined

export async function isIgnoringBatteryOptimizations(
  packageName?: string
): Promise<boolean> {
  if (Platform.OS !== "android") {
    return true
  }

  if (!batteryOptimizationModule?.isIgnoringBatteryOptimizations) {
    logWarn("Battery optimization status check is unsupported", {
      packageName,
    })
    return false
  }

  try {
    const result = await batteryOptimizationModule.isIgnoringBatteryOptimizations(
      packageName
    )
    logInfo("Checked battery optimization ignore status", {
      packageName,
      isIgnoring: result,
    })
    return result
  } catch (error) {
    logError("Failed to check battery optimization ignore status", error, {
      packageName,
    })
    return false
  }
}

export async function requestIgnoreBatteryOptimizations(
  packageName?: string
): Promise<
  "already_ignored" | "dialog_opened" | "settings_opened" | "unsupported"
> {
  if (Platform.OS !== "android") {
    return "unsupported"
  }

  if (!batteryOptimizationModule?.requestIgnoreBatteryOptimizations) {
    logWarn("Battery optimization ignore request is unsupported", {
      packageName,
    })
    return "unsupported"
  }

  try {
    const result =
      await batteryOptimizationModule.requestIgnoreBatteryOptimizations(
        packageName
      )
    logInfo("Requested battery optimization ignore flow", {
      packageName,
      result,
    })
    return result
  } catch (error) {
    logError("Failed to request battery optimization ignore flow", error, {
      packageName,
    })
    return "unsupported"
  }
}

export async function openBatteryOptimizationSettings(): Promise<boolean> {
  if (Platform.OS !== "android") {
    return false
  }

  if (!batteryOptimizationModule?.openBatteryOptimizationSettings) {
    logWarn("Battery optimization settings opening is unsupported")
    return false
  }

  try {
    const result =
      await batteryOptimizationModule.openBatteryOptimizationSettings()
    logInfo("Opened battery optimization settings", { result })
    return result === "settings_opened"
  } catch (error) {
    logError("Failed to open battery optimization settings", error)
    return false
  }
}
